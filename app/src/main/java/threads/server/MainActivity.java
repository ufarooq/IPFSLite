package threads.server;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.tools.ant.types.Commandline;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import de.psdev.licensesdialog.LicensesDialog;
import io.ipfs.multihash.Multihash;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;

import static com.google.common.base.Preconditions.checkNotNull;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final int SELECT_MEDIA_FILE = 1;
    private static final int WRITE_EXTERNAL_STORAGE = 2;
    private static final String TAG = MainActivity.class.getSimpleName();
    private final AtomicBoolean networkAvailable = new AtomicBoolean(true);
    private final AtomicBoolean idScan = new AtomicBoolean(false);
    private DrawerLayout drawer_layout;
    private final BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        private final AtomicBoolean wasOffline = new AtomicBoolean(false);
        private Snackbar snackbar;

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (Application.isConnected(context)) {
                    findViewById(R.id.server).setVisibility(View.VISIBLE);
                    if (snackbar != null) {
                        snackbar.dismiss();
                        snackbar = null;
                    }


                    networkAvailable.set(true);

                } else {
                    findViewById(R.id.server).setVisibility(View.GONE);
                    networkAvailable.set(false);
                    wasOffline.set(true);
                    if (snackbar == null) {
                        snackbar = Snackbar.make(drawer_layout,
                                getString(R.string.offline), Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(R.string.network, (v) -> {

                            Intent nIntent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                            nIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            nIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(nIntent);

                        });
                    }
                    snackbar.show();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        }
    };
    private FloatingActionButton server;
    private RecyclerView mRecyclerView;
    private MessageViewAdapter messageViewAdapter;
    private long mLastClickTime = 0;
    private EditText console_box;

    public static File getStorageFile(@NonNull String name) {
        checkNotNull(name);
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dir, name);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new RuntimeException("File couldn't be created.");
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                throw new RuntimeException(e);
            }
        }
        return file;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.view_message_list);
        LinearLayoutManager linearLayout = new LinearLayoutManager(this);
        mRecyclerView.addOnLayoutChangeListener((View v,
                                                 int left, int top, int right, int bottom,
                                                 int oldLeft, int oldTop,
                                                 int oldRight, int oldBottom) -> {

            if (bottom < oldBottom) {
                mRecyclerView.postDelayed(() -> {

                    try {
                        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
                        if (adapter != null) {
                            mRecyclerView.smoothScrollToPosition(
                                    adapter.getItemCount());
                        }
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }

                }, 50);
            }

        });


        mRecyclerView.setLayoutManager(linearLayout);
        messageViewAdapter = new MessageViewAdapter();
        mRecyclerView.setAdapter(messageViewAdapter);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        drawer_layout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer_layout.addDrawerListener(toggle);
        toggle.syncState();

        server = findViewById(R.id.server);
        server.setOnClickListener((view) -> {


            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            if (!DaemonService.isIpfsRunning()) {

                Intent intent = new Intent(MainActivity.this, DaemonService.class);
                intent.setAction(DaemonService.ACTION_START_DAEMON_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }

                findViewById(R.id.server).setVisibility(View.INVISIBLE);

            } else {
                Intent intent = new Intent(MainActivity.this, DaemonService.class);
                intent.setAction(DaemonService.ACTION_STOP_DAEMON_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                findViewById(R.id.server).setVisibility(View.INVISIBLE);
            }

        });
        serverStatus();


        MessagesViewModel messagesViewModel = ViewModelProviders.of(this).get(MessagesViewModel.class);
        messagesViewModel.getMessages().observe(this, (messages) -> {

            try {
                if (messages != null) {
                    updateMessages(messages);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage());
            }

        });

        EventViewModel eventViewModel = ViewModelProviders.of(this).get(EventViewModel.class);
        eventViewModel.getDaemonServerOfflineEvent().observe(this, (event) -> {

            try {
                if (event != null) {
                    serverStatus();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
        eventViewModel.getDaemonServerOnlineEvent().observe(this, (event) -> {

            try {
                if (event != null) {
                    serverStatus();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });


        ImageView console_send = findViewById(R.id.console_send);
        console_send.setEnabled(false);


        console_box = findViewById(R.id.console_box);
        console_box.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() > 0) {
                    console_send.setEnabled(true);
                } else {
                    console_send.setEnabled(false);
                }


            }
        });


        console_send.setOnClickListener((view) -> {

            // mis-clicking prevention, using threshold of 1500 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1500) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            removeKeyboards();

            String text = console_box.getText().toString();

            console_box.setText("");

            String[] parts = Commandline.translateCommandline(text);
            if (parts.length > 0) {


                if (parts[0].equalsIgnoreCase("ipfs")) {
                    String[] commands = Arrays.copyOfRange(parts, 1, parts.length);
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        try {
                            IPFS ipfs = Application.getIpfs();

                            if (ipfs != null) {
                                ipfs.cmd(commands);
                            }

                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }
                    });
                } else {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        try {
                            IPFS ipfs = Application.getIpfs();

                            if (ipfs != null) {
                                ipfs.cmd(parts);
                            }

                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }
                    });
                }
            }


        });

    }

    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE: {
                for (int i = 0, len = permissions.length; i < len; i++) {

                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        nav_get();
                    }
                }

                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_MEDIA_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            storeData(uri);
        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() != null) {
                    String content = result.getContents();
                    final IPFS ipfs = Application.getIpfs();
                    if (ipfs != null) {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                if (idScan.get()) {
                                    PID pid = PID.create(content);
                                    ipfs.id(pid);
                                } else {
                                    File file = getStorageFile(content);
                                    checkNotNull(file);
                                    ipfs.cmd("get", "--output=" + file.getAbsolutePath(), content);

                                    new java.lang.Thread(() -> {
                                        Application.getEventsDatabase().insertMessage(
                                                MessageKind.INFO, getString(
                                                        R.string.content_downloaded, content));

                                    }).start();
                                }
                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            }
                        });
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void storeData(@NonNull final Uri uri) {
        Cursor returnCursor = getApplicationContext().getContentResolver().query(
                uri, null, null, null, null);

        checkNotNull(returnCursor);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        //int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        final String filename = returnCursor.getString(nameIndex);
        returnCursor.close();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                IPFS ipfs = Application.getIpfs();
                InputStream inputStream =
                        getApplicationContext().getContentResolver().openInputStream(uri);
                checkNotNull(inputStream);

                if (ipfs != null) {
                    Multihash multihash = ipfs.add(inputStream);
                    checkNotNull(multihash);
                    ipfs.files_cp(multihash, "/" + filename);

                    InfoDialog.show(this, multihash.toBase58(),
                            getString(R.string.multihash),
                            getString(R.string.multihash_add, multihash.toBase58()));

                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    private void removeKeyboards() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(console_box.getWindowToken(), 0);
        }
    }


    private void serverStatus() {

        try {
            if (!DaemonService.isIpfsRunning()) {
                server.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));
            } else {
                server.setImageDrawable(getDrawable(R.drawable.stop));
            }
            findViewById(R.id.server).setVisibility(View.VISIBLE);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    private void updateMessages(@NonNull List<Message> messages) {
        try {
            messageViewAdapter.updateData(messages);

            mRecyclerView.scrollToPosition(messageViewAdapter.getItemCount() - 1);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity, menu);


        MenuItem action_info = menu.findItem(R.id.action_info);
        Drawable drawable = action_info.getIcon();
        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }


        MenuItem action_clear = menu.findItem(R.id.action_clear);
        drawable = action_clear.getIcon();
        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_clear: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                new ClearConsoleService().execute();

                return true;
            }
            case R.id.action_info: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                String pid = Application.getPid(getApplicationContext());
                if (pid.isEmpty()) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.daemon_server_not_running), Toast.LENGTH_LONG).show();
                } else {
                    InfoDialog.show(this, pid,
                            getString(R.string.peer_id),
                            getString(R.string.daemon_server_access, pid));
                }
                return true;
            }


        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkChangeReceiver);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_add: {
                try {
                    Intent intent = new Intent();
                    intent.setType("*/*");

                    String[] mimetypes = {"*/*"};
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Media File"), SELECT_MEDIA_FILE);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
                break;
            }
            case R.id.nav_get: {
                try {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Permission is not granted
                        // Request for permission
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                WRITE_EXTERNAL_STORAGE);

                    } else {
                        nav_get();
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
                break;
            }
            case R.id.nav_connect: {
                try {
                    PackageManager pm = getPackageManager();

                    if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                        idScan.set(true);
                        IntentIntegrator integrator = new IntentIntegrator(this);
                        integrator.setOrientationLocked(false);
                        integrator.initiateScan();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.feature_camera_required), Toast.LENGTH_LONG).show();
                    }

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
                break;
            }
            case R.id.nav_privacy_policy: {
                try {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                    //alertDialogBuilder.setTitle(R.string.privacy_policy);

                    String file = "file:///android_res/raw/private_policy.html";

                    WebView wv = new WebView(this);
                    wv.loadUrl(file);
                    wv.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                            final Uri uri = request.getUrl();
                            view.loadUrl(uri.getPath());
                            return true;
                        }

                    });

                    alertDialogBuilder.setView(wv);

                    alertDialogBuilder
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel());


                    AlertDialog alertDialog = alertDialogBuilder.create();


                    alertDialog.show();
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
                break;
            }
            case R.id.nav_licences: {
                try {
                    new LicensesDialog.Builder(this)
                            .setNotices(R.raw.licenses)
                            .setTitle(R.string.licences)
                            .setIncludeOwnLicense(true)
                            .setCloseText(android.R.string.ok)
                            .build()
                            .showAppCompat();

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
                break;
            }
            case R.id.nav_settings: {
                try {
                    android.app.FragmentManager fm = getFragmentManager();
                    SettingsDialog messageActionDialogFragment = new SettingsDialog();
                    messageActionDialogFragment.show(fm, "SettingsDialog");
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
                break;
            }
            case R.id.nav_webui: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                if (!DaemonService.isIpfsRunning()) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.daemon_server_not_running), Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("http://127.0.0.1:5001/webui"));
                    startActivity(intent);
                }
            }
            case R.id.nav_help: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://docs.ipfs.io/reference/api/cli"));
                startActivity(intent);

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void nav_get() {
        // Check that the device will let you use the camera
        PackageManager pm = getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            idScan.set(false);
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        } else {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.feature_camera_required), Toast.LENGTH_LONG).show();
        }

    }

}