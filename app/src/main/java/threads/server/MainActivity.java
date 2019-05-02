package threads.server;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import de.psdev.licensesdialog.LicensesDialogFragment;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Content;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.api.UserType;
import threads.core.mdl.EventViewModel;
import threads.ipfs.IPFS;
import threads.ipfs.Network;
import threads.ipfs.api.CID;
import threads.ipfs.api.Link;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;
import threads.share.ConnectService;
import threads.share.InfoDialogFragment;
import threads.share.NameDialogFragment;
import threads.share.PermissionAction;
import threads.share.RTCCallActivity;
import threads.share.ThreadActionDialogFragment;
import threads.share.UserActionDialogFragment;
import threads.share.WebViewDialogFragment;

import static androidx.core.util.Preconditions.checkNotNull;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        PeersDialogFragment.ActionListener,
        ThreadsDialogFragment.ActionListener,
        UserActionDialogFragment.ActionListener,
        ThreadActionDialogFragment.ActionListener,
        EditMultihashDialogFragment.ActionListener,
        EditPeerDialogFragment.ActionListener,
        PeersFragment.ActionListener,
        NameDialogFragment.ActionListener {

    private static final Gson gson = new Gson();
    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final int REQUEST_AUDIO_CAPTURE = 2;
    private static final int REQUEST_MODIFY_AUDIO_SETTINGS = 3;
    private static final int REQUEST_SELECT_FILES = 4;
    private static final int REQUEST_EXTERNAL_STORAGE = 5;

    private final AtomicReference<Long> storedThread = new AtomicReference<>(null);
    private final AtomicReference<String> storedUser = new AtomicReference<>(null);
    private final AtomicBoolean idScan = new AtomicBoolean(false);

    private DrawerLayout drawer_layout;
    private FloatingActionButton fab_daemon;
    private long mLastClickTime = 0;
    private ViewPager viewPager;


    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                for (int i = 0, len = permissions.length; i < len; i++) {

                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Service.localDownloadThread(getApplicationContext(), storedThread.get());
                    }
                }

                break;
            }
            case REQUEST_AUDIO_CAPTURE: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar.make(drawer_layout,
                                getString(R.string.permission_audio_denied),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.app_settings, new PermissionAction()).show();
                    }
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        clickUserCall(storedUser.get());
                    }
                }

                break;
            }
            case REQUEST_MODIFY_AUDIO_SETTINGS: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar.make(drawer_layout,
                                getString(R.string.permission_audio_settings_denied),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.app_settings, new PermissionAction()).show();
                    }
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        clickUserCall(storedUser.get());
                    }
                }

                break;
            }
            case REQUEST_VIDEO_CAPTURE: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar.make(drawer_layout,
                                getString(R.string.permission_camera_denied),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.app_settings, new PermissionAction()).show();
                    }
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        clickUserCall(storedUser.get());
                    }
                }

                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == REQUEST_SELECT_FILES && resultCode == RESULT_OK && data != null) {

                if (data.getData() != null) {
                    Uri uri = data.getData();
                    Service.storeData(getApplicationContext(), uri);
                } else {
                    if (data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        for (int i = 0; i < mClipData.getItemCount(); i++) {
                            ClipData.Item item = mClipData.getItemAt(i);
                            Uri uri = item.getUri();
                            Service.storeData(getApplicationContext(), uri);
                        }
                    }
                }
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
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    public void downloadMultihash(@NonNull String codec) {
        checkNotNull(codec);

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Preferences.error(getString(R.string.offline_mode));
            return;
        }

        // CHECKED
        if (!Preferences.isDaemonRunning(getApplicationContext())) {
            Preferences.error(getString(R.string.daemon_not_running));
            return;
        }


        try {
            CodecDecider codecDecider = CodecDecider.evaluate(codec);
            if (codecDecider.getCodex() == CodecDecider.Codec.MULTIHASH ||
                    codecDecider.getCodex() == CodecDecider.Codec.URI) {

                PID pid = Preferences.getPID(getApplicationContext());
                checkNotNull(pid);
                String multihash = codecDecider.getMultihash();
                Service.downloadMultihash(getApplicationContext(), pid, multihash, null);
            } else {
                Preferences.error(getString(R.string.codec_not_supported));
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_info: {
                return false;
            }
            case R.id.action_mark_all: {
                return false;
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
            case R.id.nav_webui: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Preferences.getWebUI(getApplicationContext())));
                    startActivity(intent);
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
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
                break;
            }
            case R.id.nav_cli: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://docs.ipfs.io/reference/api/cli"));
                    startActivity(intent);
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
                break;
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START);
        return true;
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
    public void clickMultihash() {
        PackageManager pm = getPackageManager();
        try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                idScan.set(false);
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
    public void clickEditMultihash() {
        try {
            FragmentManager fm = getSupportFragmentManager();
            EditMultihashDialogFragment editMultihashDialogFragment = new EditMultihashDialogFragment();
            editMultihashDialogFragment.show(fm, EditMultihashDialogFragment.TAG);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickUpload() {

        try {
            Intent intent = new Intent();
            intent.setType("*/*");

            String[] mimeTypes = {"*/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.select_files)), REQUEST_SELECT_FILES);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }


    @Override
    public void clickEditPeer() {
        try {
            FragmentManager fm = getSupportFragmentManager();
            EditPeerDialogFragment editPeerDialogFragment = new EditPeerDialogFragment();
            editPeerDialogFragment.show(fm, EditPeerDialogFragment.TAG);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickUserBlock(@NonNull String pid) {
        checkNotNull(pid);
        try {
            final THREADS threadsAPI = Singleton.getInstance().getThreads();

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
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickUserInfo(@NonNull String pid) {
        checkNotNull(pid);
        try {
            InfoDialogFragment.show(this, pid,
                    getString(R.string.peer_id),
                    getString(R.string.peer_access, pid));
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickUserDelete(@NonNull String pid) {
        checkNotNull(pid);
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    THREADS threadsAPI = Singleton.getInstance().getThreads();
                    threadsAPI.removeUserByPID(PID.create(pid));
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickUserConnect(@NonNull String pid) {
        checkNotNull(pid);

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Preferences.error(getString(R.string.offline_mode));
            return;
        }
        // CHECKED
        if (!Preferences.isDaemonRunning(getApplicationContext())) {
            Preferences.error(getString(R.string.daemon_not_running));
            return;
        }

        try {
            final IPFS ipfs = Singleton.getInstance().getIpfs();

            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {

                        THREADS threadsAPI = Singleton.getInstance().getThreads();
                        User user = threadsAPI.getUserByPID(PID.create(pid));
                        checkNotNull(user);

                        if (user.getStatus() == UserStatus.BLOCKED) {
                            Preferences.warning(getString(R.string.peer_is_blocked));
                        } else {

                            try {
                                threadsAPI.setStatus(user, UserStatus.DIALING);

                                try {
                                    int timeoutMillis = Preferences.getTimeoutPong(
                                            getApplicationContext());
                                    boolean checkPubsub = Preferences.isPubsubEnabled(
                                            getApplicationContext());
                                    ConnectService.wakeupCall(getApplicationContext(),
                                            NotificationFCMServer.getInstance(), user.getPID(),
                                            NotificationFCMServer.getAccessToken(
                                                    getApplicationContext(), R.raw.threads_server),
                                            checkPubsub, timeoutMillis);
                                } catch (Throwable e) {
                                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                                }

                                int timeout = Preferences.getConnectionTimeout(
                                        getApplicationContext());
                                int threshold = Preferences.getThresholdPong(
                                        getApplicationContext());
                                boolean value = ConnectService.connectUser(
                                        user.getPID(), true, timeout, threshold);
                                if (value) {
                                    threadsAPI.setStatus(user, UserStatus.ONLINE);
                                } else {
                                    threadsAPI.setStatus(user, UserStatus.OFFLINE);
                                }
                            } catch (Throwable e) {
                                threadsAPI.setStatus(user, UserStatus.OFFLINE);
                            }
                        }
                    } catch (Throwable e) {
                        Preferences.evaluateException(Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickUserEdit(@NonNull String pid) {

        try {
            FragmentManager fm = getSupportFragmentManager();
            NameDialogFragment.newInstance(pid, getString(R.string.peer_name))
                    .show(fm, NameDialogFragment.TAG);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Service.getInstance(getApplicationContext());

        TabLayout tabLayout = findViewById(R.id.tabLayout);


        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);


        viewPager = findViewById(R.id.viewPager);
        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
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

            if (DaemonService.DAEMON_RUNNING.get()) {
                DaemonService.DAEMON_RUNNING.set(false);
            } else {
                DaemonService.DAEMON_RUNNING.set(true);
            }
            DaemonService.invoke(getApplicationContext());
            serverStatus();

        });
        serverStatus();


        EventViewModel eventViewModel = ViewModelProviders.of(this).get(EventViewModel.class);


        eventViewModel.getIPFSInstallFailure().observe(this, (event) -> {
            try {
                if (event != null) {
                    Snackbar snackbar = Snackbar.make(drawer_layout,
                            R.string.ipfs_daemon_install_failure,
                            Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.info, (view) -> {

                        AlertDialog alertDialog = new AlertDialog.Builder(
                                MainActivity.this).create();
                        alertDialog.setMessage(event.getContent().concat("\n\n") +
                                getString(R.string.ipfs_no_data));
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
                                getString(android.R.string.ok),
                                (dialog, which) -> dialog.dismiss());
                        alertDialog.show();
                        eventViewModel.removeEvent(event);
                        snackbar.dismiss();

                    });
                    snackbar.show();
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });
        eventViewModel.getIPFSStartFailure().observe(this, (event) -> {
            try {
                if (event != null) {
                    Snackbar snackbar = Snackbar.make(drawer_layout,
                            R.string.ipfs_daemon_start_failure,
                            Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(R.string.info, (view) -> {

                        AlertDialog alertDialog = new AlertDialog.Builder(
                                MainActivity.this).create();
                        alertDialog.setMessage(event.getContent().concat("\n\n") +
                                getString(R.string.ipfs_no_data));
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
                                getString(android.R.string.ok),
                                (dialog, which) -> dialog.dismiss());
                        alertDialog.show();
                        eventViewModel.removeEvent(event);
                        snackbar.dismiss();

                    });
                    snackbar.show();
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });
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

    }


    @Override
    public void clickUserCall(@NonNull String pid) {

        checkNotNull(pid);


        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Preferences.error(getString(R.string.offline_mode));
            return;
        }
        // CHECKED
        if (!Preferences.isDaemonRunning(getApplicationContext())) {
            Preferences.error(getString(R.string.daemon_not_running));
            return;
        }


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO_CAPTURE);
            storedUser.set(pid);
            return;
        }


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.MODIFY_AUDIO_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                    REQUEST_MODIFY_AUDIO_SETTINGS);
            storedUser.set(pid);
            return;
        }
        try {
            final IPFS ipfs = Singleton.getInstance().getIpfs();
            final THREADS threads = Singleton.getInstance().getThreads();
            final PID host = Preferences.getPID(getApplicationContext());
            checkNotNull(host);
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        User user = threads.getUserByPID(PID.create(pid));
                        checkNotNull(user);

                        if (user.getStatus() == UserStatus.BLOCKED) {
                            Preferences.warning(getString(R.string.peer_is_blocked));
                        } else {

                            try {
                                int timeoutMillis = Preferences.getTimeoutPong(
                                        getApplicationContext());
                                boolean checkPubsub = Preferences.isPubsubEnabled(
                                        getApplicationContext());
                                ConnectService.wakeupCall(getApplicationContext(),
                                        NotificationFCMServer.getInstance(), user.getPID(),
                                        NotificationFCMServer.getAccessToken(
                                                getApplicationContext(), R.raw.threads_server),
                                        checkPubsub, timeoutMillis);
                            } catch (Throwable e) {
                                Preferences.evaluateException(Preferences.EXCEPTION, e);
                            }

                            Intent intent = RTCCallActivity.createIntent(MainActivity.this,
                                    pid, user.getAlias(), null, true);
                            intent.setAction(RTCCallActivity.ACTION_OUTGOING_CALL);
                            MainActivity.this.startActivity(intent);
                        }
                    } catch (Throwable e) {
                        Preferences.evaluateException(Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }


    @Override
    public void clickConnectPeer(@NonNull String multihash) {
        checkNotNull(multihash);
        try {
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


                        THREADS threadsAPI = Singleton.getInstance().getThreads();
                        User user = threadsAPI.getUserByPID(pid);
                        if (user == null) {
                            byte[] data = THREADS.getImage(getApplicationContext(),
                                    pid.getPid(), R.drawable.server_network);
                            CID image = ipfs.add(data, true, false);
                            user = threadsAPI.createUser(pid, "", // not yet known
                                    pid.getPid(), UserType.VERIFIED, image);
                            user.setStatus(UserStatus.OFFLINE);
                            threadsAPI.storeUser(user);

                        } else {
                            Preferences.warning(getString(R.string.peer_exists_with_pid));
                            return;
                        }
                        checkNotNull(user);

                        try {
                            threadsAPI.setStatus(user, UserStatus.DIALING);
                            int timeout = Preferences.getConnectionTimeout(
                                    getApplicationContext());
                            int threshold = Preferences.getThresholdPong(
                                    getApplicationContext());
                            boolean value = ConnectService.connectUser(
                                    user.getPID(), true, timeout, threshold);
                            if (value) {
                                threadsAPI.setStatus(user, UserStatus.ONLINE);

                                // make a connection to peer
                                if (Preferences.isPubsubEnabled(getApplicationContext())) {
                                    checkNotNull(host);
                                    User hostUser = threadsAPI.getUserByPID(host);
                                    checkNotNull(hostUser);

                                    Content map = new Content();
                                    map.put(Content.EST, Message.CONNECT.name());
                                    map.put(Content.ALIAS, hostUser.getAlias());
                                    map.put(Content.PKEY, hostUser.getPublicKey());

                                    ipfs.pubsub_pub(user.getPID().getPid(), gson.toJson(map));
                                }
                            } else {
                                threadsAPI.setStatus(user, UserStatus.OFFLINE);
                            }
                        } catch (Throwable e) {
                            threadsAPI.setStatus(user, UserStatus.OFFLINE);
                        }


                    } catch (Throwable e) {
                        Preferences.evaluateException(Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickThreadPublish(long idx) {

        try {
            final THREADS threadsAPI = Singleton.getInstance().getThreads();
            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {

                    threadsAPI.setThreadStatus(idx, ThreadStatus.PUBLISHING);

                    try {
                        Thread threadObject = threadsAPI.getThreadByIdx(idx);
                        checkNotNull(threadObject);
                        CID cid = threadObject.getCid();
                        checkNotNull(cid);
                        String multihash = cid.getCid();

                        ipfs.cmd("name", "publish", multihash);

                        PID pid = Preferences.getPID(getApplicationContext());
                        checkNotNull(pid);
                        ipfs.cmd("name", "resolve", pid.getPid());

                        Uri uri = Uri.parse("https://ipfs.io/ipns/" + pid.getPid());

                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                    } catch (Throwable e) {
                        Preferences.evaluateException(Preferences.EXCEPTION, e);
                    } finally {
                        threadsAPI.setThreadStatus(idx, ThreadStatus.ONLINE);
                    }
                });
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickThreadInfo(long idx) {
        try {
            final THREADS threadsAPI = Singleton.getInstance().getThreads();
            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        Thread threadObject = threadsAPI.getThreadByIdx(idx);
                        checkNotNull(threadObject);

                        CID cid = threadObject.getCid();
                        checkNotNull(cid);
                        String multihash = cid.getCid();

                        InfoDialogFragment.show(this, multihash,
                                getString(R.string.multihash),
                                getString(R.string.multihash_access, multihash));


                    } catch (Throwable e) {
                        Preferences.evaluateException(Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    @Override
    public void clickThreadPlay(long idx) {

        // CHECKED
        if (!Preferences.isDaemonRunning(getApplicationContext())) {
            Preferences.error(getString(R.string.daemon_not_running));
            return;
        }
        final int timeout = Preferences.getConnectionTimeout(getApplicationContext());
        final THREADS threadsAPI = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread threadObject = threadsAPI.getThreadByIdx(idx);
                    checkNotNull(threadObject);

                    CID cid = threadObject.getCid();
                    checkNotNull(cid);
                    String multihash = cid.getCid();

                    List<Link> links = threadsAPI.getLinks(ipfs, threadObject, timeout, true);
                    checkNotNull(links);
                    String path = "";
                    if (links.size() == 1) {
                        Link link = links.get(0);
                        path = "/" + link.getPath();
                    }

                    Uri uri = Uri.parse(Preferences.getGateway(getApplicationContext(),
                            IPFS.Style.ipfs) +
                            multihash + path);

                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, threadObject.getMimeType()); // TODO might not be right
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } catch (Throwable e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
                } catch (Throwable ex) {
                    Preferences.error(getString(R.string.no_activity_found_to_handle_uri));
                }
            });
        }
    }

    @Override
    public void clickThreadDelete(long idx) {

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Service.deleteThread(ipfs, idx);
                } catch (Throwable e) {
                    // ignore exception for now
                }
            });
        }
    }

    @Override
    public void clickThreadView(long idx) {

        final THREADS threadsAPI = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread threadObject = threadsAPI.getThreadByIdx(idx);
                    checkNotNull(threadObject);

                    CID cid = threadObject.getCid();
                    checkNotNull(cid);
                    String multihash = cid.getCid();

                    Uri uri = Uri.parse("https://ipfs.io/ipfs/" + multihash);

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
    public void clickThreadShare(long idx) {

        final THREADS threadsAPI = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread threadObject = threadsAPI.getThreadByIdx(idx);
                    checkNotNull(threadObject);

                    CID cid = threadObject.getCid();
                    checkNotNull(cid);
                    String multihash = cid.getCid();

                    Bitmap bitmap = Preferences.getBitmap(getApplicationContext(), multihash);
                    checkNotNull(bitmap);
                    File file = Service.getCacheFile(getApplicationContext(),
                            multihash + ".png");

                    FileOutputStream fileOutputStream = new FileOutputStream(file);

                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();

                    Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                            "threads.server.provider", file);

                    if (file.exists()) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, multihash);
                        shareIntent.putExtra(Intent.EXTRA_TEXT,
                                getString(R.string.multihash_access, multihash));
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.setType("image/png");
                        startActivity(Intent.createChooser(shareIntent,
                                getResources().getText(R.string.share)));
                    }


                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }

    }

    @Override
    public void clickThreadSend(long idx) {

        try {
            // CHECKED
            if (!Network.isConnected(getApplicationContext())) {
                Preferences.error(getString(R.string.offline_mode));
                return;
            }
            // CHECKED
            if (!Preferences.isDaemonRunning(getApplicationContext())) {
                Preferences.error(getString(R.string.daemon_not_running));
                return;
            }

            Service.getInstance(getApplicationContext()).sendThreads(
                    getApplicationContext(), Collections.singletonList(idx));
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void clickThreadDownload(long idx) {

        storedThread.set(idx);
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_EXTERNAL_STORAGE);

            } else {
                Service.localDownloadThread(getApplicationContext(), idx);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }


    }

    @Override
    public void name(@NonNull String pid, @NonNull String name) {
        checkNotNull(pid);
        checkNotNull(name);

        final THREADS threadsAPI = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();

        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    User user = threadsAPI.getUserByPID(PID.create(pid));
                    checkNotNull(user);

                    user.setAlias(name);
                    byte[] data = THREADS.getImage(getApplicationContext(),
                            name, R.drawable.server_network);
                    CID image = ipfs.add(data, true, false);
                    user.setImage(image);

                    threadsAPI.storeUser(user);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }


    }

    @Override
    public void clickUserInfo() {

        try {
            PID pid = Preferences.getPID(getApplicationContext());
            checkNotNull(pid);
            clickUserInfo(pid.getPid());
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    private class PagerAdapter extends FragmentStatePagerAdapter {
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