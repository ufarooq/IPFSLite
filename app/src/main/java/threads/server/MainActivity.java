package threads.server;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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
import java.util.List;
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
import io.ipfs.multihash.Multihash;
import threads.core.IThreadsAPI;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.Kind;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.api.UserType;
import threads.core.mdl.EventViewModel;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.Link;
import threads.ipfs.api.PID;
import threads.share.NameDialogFragment;
import threads.share.ThreadActionDialogFragment;
import threads.share.UserActionDialogFragment;
import threads.share.WebViewDialogFragment;

import static com.google.common.base.Preconditions.checkNotNull;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        ActionDialogFragment.ActionListener,
        UserActionDialogFragment.ActionListener,
        ThreadActionDialogFragment.ActionListener,
        EditMultihashDialogFragment.ActionListener,
        ThreadsFragment.ActionListener,
        NameDialogFragment.ActionListener {
    public static final int SELECT_MEDIA_FILE = 1;
    private static final int WRITE_EXTERNAL_STORAGE = 2;
    private static final String TAG = MainActivity.class.getSimpleName();


    private final AtomicBoolean idScan = new AtomicBoolean(false);
    private DrawerLayout drawer_layout;

    private FloatingActionButton fab_daemon;

    private long mLastClickTime = 0;


    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE: {
                for (int i = 0, len = permissions.length; i < len; i++) {

                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        scanMultihash();
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
                    String multihash = result.getContents();

                    if (SystemClock.elapsedRealtime() - mLastClickTime < 2000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();


                    if (!idScan.get()) {
                        downloadMultihash(multihash);
                    } else {
                        clickConnectPeer(multihash);
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    public void downloadMultihash(@NonNull String multihash) {
        checkNotNull(multihash);

        // CHECKED
        if (!Application.isConnected(getApplicationContext())) {
            Preferences.error(getString(R.string.offline_mode));
            return;
        }

        // CHECKED
        if (!DaemonService.DAEMON_RUNNING.get()) {
            Preferences.error(getString(R.string.daemon_not_running));
            return;
        }

        PID pid = Preferences.getPID(getApplicationContext());
        checkNotNull(pid);
        Service.downloadMultihash(getApplicationContext(), pid, multihash);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        TabLayout tabLayout = findViewById(R.id.tabLayout);


        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);


        final ViewPager viewPager = findViewById(R.id.viewPager);
        final PagerAdapter adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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

        fab_daemon = findViewById(R.id.fab_daemon);
        fab_daemon.setOnClickListener((view) -> {


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

                findViewById(R.id.fab_daemon).setVisibility(View.INVISIBLE);

            } else {
                Intent intent = new Intent(MainActivity.this, DaemonService.class);
                intent.setAction(DaemonService.ACTION_STOP_DAEMON_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                findViewById(R.id.fab_daemon).setVisibility(View.INVISIBLE);
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
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });
        eventViewModel.getIPFSServerOnlineEvent().observe(this, (event) -> {

            try {
                if (event != null) {
                    serverStatus();
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });

        checkOnlineStatus();
    }

    private void checkOnlineStatus() {
        if (!Application.isConnected(getApplicationContext())) {
            Preferences.warning(getString(R.string.offline_mode));
        } else {
            if (!DaemonService.DAEMON_RUNNING.get()) {
                Preferences.warning(getString(R.string.offline_mode));
            }
        }
    }

    private void storeData(@NonNull final Uri uri) {
        Cursor returnCursor = getApplicationContext().getContentResolver().query(
                uri, null, null, null, null);

        checkNotNull(returnCursor);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();

        final String filename = returnCursor.getString(nameIndex);
        returnCursor.close();
        String mimeType = getApplicationContext().getContentResolver().getType(uri);
        checkNotNull(mimeType);

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    InputStream inputStream =
                            getApplicationContext().getContentResolver().openInputStream(uri);
                    checkNotNull(inputStream);

                    PID pid = Preferences.getPID(getApplicationContext());
                    checkNotNull(pid);
                    User user = threadsAPI.getUserByPID(pid);
                    checkNotNull(user);


                    byte[] bytes;
                    try {
                        bytes = IThreadsAPI.getPreviewImage(getApplicationContext(), uri);
                        if (bytes == null) {
                            bytes = IThreadsAPI.getImage(getApplicationContext(), user.getAlias(),
                                    R.drawable.file_document);
                        }
                    } catch (Throwable e) {
                        // ignore exception
                        bytes = IThreadsAPI.getImage(getApplicationContext(), user.getAlias(),
                                R.drawable.file_document);
                    }


                    Thread thread = threadsAPI.createThread(user, ThreadStatus.OFFLINE, Kind.IN,
                            filename, filename, bytes, false, false);
                    thread.setMimeType(mimeType);
                    threadsAPI.storeThread(thread);


                    try {
                        CID cid = ipfs.add(inputStream, filename, true);
                        checkNotNull(cid);

                        threadsAPI.setStatus(thread, ThreadStatus.ONLINE);
                        threadsAPI.setCID(thread, cid);

                    } catch (Throwable e) {
                        threadsAPI.setStatus(thread, ThreadStatus.ERROR);
                        throw e;
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }


    private void serverStatus() {

        try {
            if (!DaemonService.DAEMON_RUNNING.get()) {
                fab_daemon.setImageDrawable(getDrawable(R.drawable.play_circle_outline));
            } else {
                fab_daemon.setImageDrawable(getDrawable(R.drawable.stop_circle_outline));
            }
            findViewById(R.id.fab_daemon).setVisibility(View.VISIBLE);
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


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_download: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                scanMultihash();

                return true;
            }
            case R.id.action_info: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                PID pid = Preferences.getPID(getApplicationContext());
                checkNotNull(pid);
                InfoDialogFragment.show(this, pid.getPid(),
                        getString(R.string.peer_id),
                        getString(R.string.peer_access, pid));

                return true;
            }


        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_privacy_policy: {
                try {
                    String url = "file:///android_res/raw/privacy_policy.html";
                    WebViewDialogFragment.newInstance(WebViewDialogFragment.Type.URL, url)
                            .show(getSupportFragmentManager(), WebViewDialogFragment.TAG);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
                break;
            }
            case R.id.nav_issues: {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://gitlab.com/remmer.wilts/threads-server/issues"));
                    startActivity(intent);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
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
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
                break;
            }
            case R.id.nav_settings: {
                try {
                    FragmentManager fm = getSupportFragmentManager();
                    SettingsDialogFragment settingsDialogFragment = new SettingsDialogFragment();
                    settingsDialogFragment.show(fm, SettingsDialogFragment.TAG);
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
                break;
            }
            case R.id.nav_webui: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                // CHECKED
                if (!DaemonService.DAEMON_RUNNING.get()) {
                    Preferences.error(getString(R.string.daemon_not_running));
                    break;
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(Preferences.getWebUI(getApplicationContext())));
                startActivity(intent);
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

    private void scanMultihash() {
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
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickUploadMultihash() {
        try {
            Intent intent = new Intent();
            intent.setType("*/*");

            String[] mimeTypes = {"*/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Media File"), SELECT_MEDIA_FILE);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }


    /**
     * TODO
     */
    private void clickDownload() {

        try {

            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE);

            } else {
                // TODO
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    @Override
    public void clickEditPeer() {
        FragmentManager fm = getSupportFragmentManager();
        EditMultihashDialogFragment editMultihashDialogFragment = new EditMultihashDialogFragment();
        editMultihashDialogFragment.show(fm, EditMultihashDialogFragment.TAG);
    }

    @Override
    public void clickUserBlock(@NonNull String pid) {
        checkNotNull(pid);


        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                User user = threadsAPI.getUserByPID(PID.create(pid));
                checkNotNull(user);

                if (user.getStatus() == UserStatus.BLOCKED) {
                    threadsAPI.unblockUser(user, UserStatus.OFFLINE);
                } else {
                    threadsAPI.blockUser(user);
                }

            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        });

    }

    @Override
    public void clickUserInfo(@NonNull String pid) {
        checkNotNull(pid);

        InfoDialogFragment.show(this, pid,
                getString(R.string.peer_id),
                getString(R.string.peer_access, pid));
    }

    @Override
    public void clickUserDelete(@NonNull String pid) {
        checkNotNull(pid);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
                threadsAPI.removeUserByPID(PID.create(pid));
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    @Override
    public void clickUserConnect(@NonNull String multihash) {
        checkNotNull(multihash);

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    PID pid = PID.create(multihash);

                    IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
                    User user = threadsAPI.getUserByPID(pid);
                    checkNotNull(user);

                    if (user.getStatus() != UserStatus.BLOCKED) {
                        ipfs.id(pid);
                        ipfs.swarm_connect(pid);

                        try {
                            boolean value = ipfs.swarm_is_connected(pid);
                            if (value) {
                                threadsAPI.setStatus(user, UserStatus.ONLINE);
                            }
                        } catch (Throwable e) {
                            threadsAPI.setStatus(user, UserStatus.OFFLINE);
                            checkOnlineStatus();
                            // ignore exception when not connected
                        }
                    } else {
                        Preferences.warning(getString(R.string.peer_is_blocked));
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    @Override
    public void clickUserEdit(@NonNull String pid) {

        FragmentManager fm = getSupportFragmentManager();

        NameDialogFragment.newInstance(pid, getString(R.string.peer_name))
                .show(fm, NameDialogFragment.TAG);
    }


    @Override
    public void clickConnectPeer(@NonNull String multihash) {
        checkNotNull(multihash);

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    // check if multihash is valid
                    try {
                        Multihash.fromBase58(multihash);
                    } catch (Throwable e) {
                        Preferences.error(getString(R.string.multihash_not_valid));
                        return;
                    }

                    PID host = Preferences.getPID(getApplicationContext());
                    PID pid = PID.create(multihash);

                    if (pid.equals(host)) {
                        Preferences.warning(getString(R.string.same_pid_like_host));
                        return;
                    }


                    IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
                    User user = threadsAPI.getUserByPID(pid);
                    if (user == null) {
                        byte[] image = IThreadsAPI.getImage(getApplicationContext(),
                                pid.getPid(), R.drawable.server_network);
                        user = threadsAPI.createUser(pid,
                                pid.getPid(),
                                pid.getPid(),
                                pid.getPid(), UserType.VERIFIED, image, null);

                        user.setStatus(UserStatus.OFFLINE);
                        threadsAPI.storeUser(user);

                    } else {
                        Preferences.warning(getString(R.string.peer_exists_with_pid));
                        return;
                    }
                    checkNotNull(user);


                    ipfs.id(pid);
                    ipfs.swarm_connect(pid);

                    try {
                        boolean value = ipfs.swarm_is_connected(pid);
                        if (value) {
                            threadsAPI.setStatus(user, UserStatus.ONLINE);


                            // make a connection to peer
                            if (Preferences.isPubsubEnabled(getApplicationContext())) {
                                ipfs.pubsub_pub(user.getPid(),
                                        multihash.concat(System.lineSeparator()));
                            }
                        }
                    } catch (Throwable e) {
                        threadsAPI.setStatus(user, UserStatus.OFFLINE);
                        checkOnlineStatus();
                        // ignore exception when not connected
                    }


                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    @Override
    public void clickThreadPin(@NonNull String thread) {
        checkNotNull(thread);
        checkNotNull(thread);

        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    Thread threadObject = threadsAPI.getThreadByAddress(thread);
                    checkNotNull(threadObject);

                    // not yet activated

                    Preferences.warning(getString(R.string.sorry_not_yet_implemented));

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    @Override
    public void clickThreadInfo(@NonNull String thread) {
        checkNotNull(thread);
        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread threadObject = threadsAPI.getThreadByAddress(thread);
                    checkNotNull(threadObject);

                    String multihash = threadObject.getCid();

                    InfoDialogFragment.show(this, multihash,
                            getString(R.string.multihash),
                            getString(R.string.multihash_access, multihash));


                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }


    }

    @Override
    public void clickThreadPlay(@NonNull String thread) {
        checkNotNull(thread);

        // CHECKED
        if (!DaemonService.DAEMON_RUNNING.get()) {
            Preferences.error(getString(R.string.daemon_not_running));
            return;
        }

        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread threadObject = threadsAPI.getThreadByAddress(thread);
                    checkNotNull(threadObject);

                    String multihash = threadObject.getCid();

                    CID cid = CID.create(multihash);
                    List<Link> links = ipfs.ls(cid);
                    Link link = links.get(0);
                    String path = link.getPath();

                    Uri uri = Uri.parse(Preferences.getGateway(getApplicationContext()) + multihash + "/" + path);

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, threadObject.getMimeType());
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);


                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    @Override
    public void clickThreadDelete(@NonNull String thread) {
        checkNotNull(thread);


        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread threadObject = threadsAPI.getThreadByAddress(thread);
                    checkNotNull(threadObject);

                    // TODO threadsAPI.pin_rm(ipfs, threadObject.getCid(), true);

                    threadsAPI.removeThread(threadObject);


                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    @Override
    public void clickThreadView(@NonNull String thread) {
        checkNotNull(thread);


        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread threadObject = threadsAPI.getThreadByAddress(thread);
                    checkNotNull(threadObject);


                    String multihash = threadObject.getCid();

                    Uri uri = Uri.parse("https://gateway.ipfs.io/ipfs/" + multihash);

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    @Override
    public void clickThreadShare(@NonNull String thread) {
        checkNotNull(thread);

        // CHECKED
        if (!Application.isConnected(getApplicationContext())) {
            Preferences.error(getString(R.string.offline_mode));
            return;
        }
        // CHECKED
        if (!DaemonService.DAEMON_RUNNING.get()) {
            Preferences.error(getString(R.string.daemon_not_running));
            return;
        }
        final PID host = Preferences.getPID(getApplicationContext());
        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
        final IPFS ipfs = Singleton.getInstance().getIpfs();

        if (ipfs != null) {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread threadObject = threadsAPI.getThreadByAddress(thread);
                    checkNotNull(threadObject);


                    String multihash = threadObject.getCid();
                    List<User> users = threadsAPI.getUsers();
                    for (User user : users) {
                        if (user.getStatus() != UserStatus.BLOCKED) {
                            PID userPID = PID.create(user.getPid());
                            if (!userPID.equals(host)) {
                                if (threadsAPI.connect(ipfs, userPID, null)) {
                                    ipfs.pubsub_pub(user.getPid(),
                                            multihash.concat(System.lineSeparator()));
                                }
                            }
                        }
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });


        }

    }

    @Override
    public void name(@NonNull String pid, @NonNull String name) {
        checkNotNull(pid);
        checkNotNull(name);

        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                User user = threadsAPI.getUserByPID(PID.create(pid));
                checkNotNull(user);

                user.setAlias(name);
                byte[] image = IThreadsAPI.getImage(getApplicationContext(),
                        name, R.drawable.server_network);
                user.setImage(image);

                threadsAPI.storeUser(user);

            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        });


    }


    public class PagerAdapter extends FragmentStatePagerAdapter {
        final int mNumOfTabs;

        PagerAdapter(FragmentManager fm, int NumOfTabs) {
            super(fm);
            this.mNumOfTabs = NumOfTabs;
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return new ThreadsFragment();
                case 1:
                    return new PeersFragment();
                case 2:
                    return new ConsoleFragment();
                case 3:
                    return new InfoFragment();
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