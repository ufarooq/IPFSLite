package threads.server;

import android.Manifest;
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
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import de.psdev.licensesdialog.LicensesDialogFragment;
import threads.core.IThreadsAPI;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.api.UserType;
import threads.core.mdl.EventViewModel;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;
import threads.share.UserActionDialogFragment;
import threads.share.WebViewDialogFragment;

import static com.google.common.base.Preconditions.checkNotNull;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        ActionDialogFragment.ActionListener,
        UserActionDialogFragment.ActionListener {
    public static final int SELECT_MEDIA_FILE = 1;
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


    private long mLastClickTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Friends"));
        tabLayout.addTab(tabLayout.newTab().setText("Console"));


        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = findViewById(R.id.viewPager);
        final PagerAdapter adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


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

            if (!DaemonService.DAEMON_RUNNING.get()) {

                Intent intent = new Intent(MainActivity.this, DaemonService.class);
                intent.setAction(DaemonService.ACTION_START_DAEMON_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }

                findViewById(R.id.server).setVisibility(View.INVISIBLE);

            } else {
                Intent intent = new Intent(MainActivity.this, DaemonService.class);
                intent.setAction(DaemonService.ACTION_STOP_DAEMON_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                findViewById(R.id.server).setVisibility(View.INVISIBLE);
            }

        });
        serverStatus();


        EventViewModel eventViewModel = ViewModelProviders.of(this).get(EventViewModel.class);


        eventViewModel.getException().observe(this, (event) -> {
            try {
                if (event != null) {
                    Snackbar snackbar = Snackbar.make(drawer_layout, event.getContent(),
                            Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(android.R.string.ok, (view) -> {
                        eventViewModel.removeEvent(event);
                        snackbar.dismiss();

                    });
                    snackbar.show();
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });

        eventViewModel.getWarning().observe(this, (event) -> {
            try {
                if (event != null) {
                    Toast.makeText(
                            getApplicationContext(), event.getContent(), Toast.LENGTH_LONG).show();
                    eventViewModel.removeEvent(event);
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });



        eventViewModel.getIPFSServerOfflineEvent().observe(this, (event) -> {

            try {
                if (event != null) {
                    serverStatus();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
        eventViewModel.getIPFSServerOnlineEvent().observe(this, (event) -> {

            try {
                if (event != null) {
                    serverStatus();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
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

                    if (SystemClock.elapsedRealtime() - mLastClickTime < 2000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();


                    if (!idScan.get()) {
                        DownloadJobService.download(getApplicationContext(), content);
                    } else {
                        clickConnect(content);
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
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    InputStream inputStream =
                            getApplicationContext().getContentResolver().openInputStream(uri);
                    checkNotNull(inputStream);


                    CID cid = ipfs.add(inputStream, filename, true);
                    checkNotNull(cid);

                    InfoDialogFragment.show(this, cid.getCid(),
                            getString(R.string.multihash),
                            getString(R.string.multihash_add, cid.getCid()));


                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });
        }
    }


    private void serverStatus() {

        try {
            if (!DaemonService.DAEMON_RUNNING.get()) {
                server.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));
            } else {
                server.setImageDrawable(getDrawable(R.drawable.stop));
            }
            findViewById(R.id.server).setVisibility(View.VISIBLE);
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

                PID pid = Preferences.getPID(getApplicationContext());
                if (pid == null) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.daemon_server_not_running), Toast.LENGTH_LONG).show();
                } else {
                    InfoDialogFragment.show(this, pid.getPid(),
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
            case R.id.nav_privacy_policy: {
                try {
                    String url = "file:///android_res/raw/privacy_policy.html";
                    IPFS ipfs = Singleton.getInstance().getIpfs();
                    if (ipfs != null) {
                        InputStream inputStream = getResources().openRawResource(R.raw.privacy_policy);
                        CID cid = ipfs.add(inputStream);
                        url = Preferences.getGateway(this) + cid.getCid();
                    }
                    WebViewDialogFragment.newInstance(WebViewDialogFragment.Type.URL, url)
                            .show(getSupportFragmentManager(), WebViewDialogFragment.TAG);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
                break;
            }
            case R.id.nav_issues: {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://gitlab.com/remmer.wilts/threads-server/issues"));
                    startActivity(intent);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
                break;
            }
            case R.id.nav_licences: {
                try {

                    final LicensesDialogFragment fragment = new LicensesDialogFragment.Builder(this)
                            .setNotices(R.raw.licenses)
                            .setShowFullLicenseText(false)
                            .setIncludeOwnLicense(true)
                            .build();

                    fragment.show(getSupportFragmentManager(), null);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
                break;
            }
            case R.id.nav_settings: {
                try {
                    FragmentManager fm = getSupportFragmentManager();
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
                if (!DaemonService.DAEMON_RUNNING.get()) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.daemon_server_not_running), Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("http://127.0.0.1:5001/webui"));
                    startActivity(intent);
                }
                break;
            }
            case R.id.nav_share: {
                try {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    String sAux = "\n" + getString(R.string.playstore_email) + "\n\n";
                    sAux = sAux + getString(R.string.playstore_url) + "\n\n";
                    i.putExtra(Intent.EXTRA_TEXT, sAux);
                    startActivity(Intent.createChooser(i, getString(R.string.choose_one)));
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }
            case R.id.nav_cli: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://docs.ipfs.io/reference/api/cli"));
                startActivity(intent);
                break;
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

    @Override
    public void clickConnectPeer() {
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
    }

    @Override
    public void clickUploadFile() {
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
    }

    @Override
    public void clickDownloadFile() {
        try {

            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE);

            } else {
                nav_get();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }

    }

    @Override
    public void clickBlock(@NonNull String pid) {
        checkNotNull(pid);
    }

    @Override
    public void clickInfo(String pid) {
        checkNotNull(pid);

        InfoDialogFragment.show(this, pid,
                getString(R.string.peer_id),
                getString(R.string.daemon_server_access, pid));
    }

    @Override
    public void clickDelete(@NonNull final String pid) {
        checkNotNull(pid);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
                threadsAPI.removeUserByPid(PID.create(pid));
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    @Override
    public void clickConnect(String content) {
        checkNotNull(content);

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    PID pid = PID.create(content);


                    IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
                    User user = threadsAPI.getUserByPid(pid);
                    if (user == null) {
                        user = threadsAPI.createUser(pid,
                                pid.getPid(),
                                pid.getPid(),
                                pid.getPid(), UserType.VERIFIED, null);
                        user.setStatus(UserStatus.OFFLINE);
                        threadsAPI.storeUser(user);

                    }
                    checkNotNull(user);
                    ipfs.id(pid);
                    ipfs.swarm_connect(pid);

                    boolean value = ipfs.swarm_is_connected(pid);
                    if (value) {
                        threadsAPI.setStatus(user, UserStatus.ONLINE);
                    }

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });
        }
    }


    public class PagerAdapter extends FragmentStatePagerAdapter {
        int mNumOfTabs;

        public PagerAdapter(FragmentManager fm, int NumOfTabs) {
            super(fm);
            this.mNumOfTabs = NumOfTabs;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return new PeersFragment();
                case 1:
                    return new ConsoleFragment();
                default:
                    throw new RuntimeException("Not Supported position");
            }
        }

        @Override
        public int getCount() {
            return mNumOfTabs;
        }
    }

}