package threads.server;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
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
import threads.share.ThreadActionDialogFragment;
import threads.share.UserActionDialogFragment;
import threads.share.WebViewDialogFragment;

import static com.google.common.base.Preconditions.checkNotNull;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        PeersDialogFragment.ActionListener,
        ThreadsDialogFragment.ActionListener,
        UserActionDialogFragment.ActionListener,
        ThreadActionDialogFragment.ActionListener,
        EditMultihashDialogFragment.ActionListener,
        EditPeerDialogFragment.ActionListener,
        PeersFragment.ActionListener,
        NameDialogFragment.ActionListener,
        CallingDialogFragment.ActionListener {
    public static final String INCOMING_CALL_PID = "INCOMING_CALL_PID";
    public static final String INCOMING_CALL_NOTIFICATION_ID = "INCOMING_CALL_NOTIFICATION_ID";
    public static final String ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL";
    private static final Gson gson = new Gson();
    private static final int SELECT_FILES = 1;
    private static final int DOWNLOAD_EXTERNAL_STORAGE = 2;
    private static final int REQUEST_VIDEO_CAPTURE = 3;
    private static final int REQUEST_AUDIO_CAPTURE = 4;
    private final AtomicReference<Long> storedThread = new AtomicReference<>(null);
    private final AtomicReference<String> storedUser = new AtomicReference<>(null);
    private final AtomicBoolean idScan = new AtomicBoolean(false);
    boolean isReceiverRegistered = false;
    private DrawerLayout drawer_layout;
    private FloatingActionButton fab_daemon;
    private long mLastClickTime = 0;
    private ViewPager viewPager;
    private CallBroadcastReceiver callBroadcastReceiver;
    private SharedPreferences sharedPref;
    private String keyprefResolution;
    private String keyprefFps;
    private String keyprefVideoBitrateType;
    private String keyprefVideoBitrateValue;
    private String keyprefAudioBitrateType;
    private String keyprefAudioBitrateValue;
    private String keyprefRoomServerUrl;
    private String keyprefRoom;
    private String keyprefRoomList;


    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case DOWNLOAD_EXTERNAL_STORAGE: {
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
            if (requestCode == SELECT_FILES && resultCode == RESULT_OK && data != null) {

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

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
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
                    getString(R.string.select_files)), SELECT_FILES);
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

                                long timeout = ConnectService.getConnectionTimeout(
                                        getApplicationContext());

                                boolean value = ConnectService.connectUser(PID.create(pid), timeout);
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

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        keyprefResolution = getString(R.string.pref_resolution_key);
        keyprefFps = getString(R.string.pref_fps_key);
        keyprefVideoBitrateType = getString(R.string.pref_maxvideobitrate_key);
        keyprefVideoBitrateValue = getString(R.string.pref_maxvideobitratevalue_key);
        keyprefAudioBitrateType = getString(R.string.pref_startaudiobitrate_key);
        keyprefAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key);
        keyprefRoomServerUrl = getString(R.string.pref_room_server_url_key);
        keyprefRoom = getString(R.string.pref_room_key);
        keyprefRoomList = getString(R.string.pref_room_list_key);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


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

        callBroadcastReceiver = new CallBroadcastReceiver();
        registerReceiver();

        handleIncomingCallIntent(getIntent());
    }

    private void registerReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_INCOMING_CALL);

            LocalBroadcastManager.getInstance(this).registerReceiver(
                    callBroadcastReceiver, intentFilter);
            isReceiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(callBroadcastReceiver);
            isReceiverRegistered = false;
        }
    }

    private void handleIncomingCallIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_INCOMING_CALL)) {
                String pid = intent.getStringExtra(INCOMING_CALL_PID);
                if (pid != null && !pid.isEmpty()) {
                    receiveUserCall(pid);
                    intent.removeExtra(INCOMING_CALL_PID);
                }
            }
        }
    }

    @Override
    public void clickUserCall(@NonNull String pid) {

        checkNotNull(pid);

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
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_VIDEO_CAPTURE);
            storedUser.set(pid);
            return;
        }


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
            final THREADS threads = Singleton.getInstance().getThreads();
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        User user = threads.getUserByPID(PID.create(pid));
                        checkNotNull(user);

                        if (user.getStatus() == UserStatus.BLOCKED) {
                            Preferences.warning(getString(R.string.peer_is_blocked));
                        } else {

                            long timeout = ConnectService.getConnectionTimeout(
                                    getApplicationContext());

                            boolean value = ConnectService.connectUser(PID.create(pid), timeout);
                            if (value) {

                                Service.emitSessionCall(PID.create(pid));

                                try {
                                    call(pid, false);

                                    /*
                                    Intent intent = new Intent(MainActivity.this, VideoActivity.class);
                                    intent.putExtra(Content.USER, pid);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);*/
                                } catch (Throwable e) {
                                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                                }
                            } else {
                                Preferences.warning(getString(R.string.peer_is_offline));
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


    public void receiveUserCall(@NonNull String pid) {

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
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_VIDEO_CAPTURE);
            storedUser.set(pid);
            return;
        }


        CallingDialogFragment.newInstance(pid)
                .show(getSupportFragmentManager(), CallingDialogFragment.TAG);

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
                            user = threadsAPI.createUser(pid,
                                    pid.getPid(),
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
                            long timeout = ConnectService.getConnectionTimeout(
                                    getApplicationContext());
                            boolean value = ConnectService.connectUser(user.getPID(), timeout);
                            if (value) {
                                threadsAPI.setStatus(user, UserStatus.ONLINE);

                                // make a connection to peer
                                if (Preferences.isPubsubEnabled(getApplicationContext())) {
                                    checkNotNull(host);
                                    User hostUser = threadsAPI.getUserByPID(host);
                                    checkNotNull(hostUser);

                                    Map<String, String> map = new HashMap<>();
                                    map.put(Content.EST, Message.CONNECT.name());
                                    map.put(Content.ALIAS, hostUser.getAlias());

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
        final int timeout = ConnectService.getConnectionTimeout(getApplicationContext());
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

            Service.sendThreads(getApplicationContext(), idx);
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
                        DOWNLOAD_EXTERNAL_STORAGE);

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

    @Override
    public void acceptCall(@NonNull String pid) {

        try {
            final long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
            Service.emitSessionAccept(PID.create(pid), timeout);

            call(pid, false);

            /*
            Intent intent = new Intent(MainActivity.this, VideoActivity.class);
            intent.putExtra(Content.USER, pid);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);*/
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void rejectCall(@NonNull String pid) {
        try {
            final long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
            Service.emitSessionReject(PID.create(pid), timeout);

            /**
             if (activeCallInvite != null) {
             activeCallInvite.reject(VoiceDialogFragment.this);
             notificationManager.cancel(activeCallNotificationId);
             }*/

        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    @Override
    public void timeoutCall(@NonNull String pid) {
        try {
            final long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
            Service.emitSessionTimeout(PID.create(pid), timeout);

            /**
             if (activeCallInvite != null) {
             activeCallInvite.reject(VoiceDialogFragment.this);
             notificationManager.cancel(activeCallNotificationId);
             }*/

        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    @SuppressWarnings("StringSplitter")
    private void call(String pid, boolean useValuesFromIntent) {


        // Video call enabled flag.
        boolean videoCallEnabled = sharedPrefGetBoolean(R.string.pref_videocall_key,
                CallActivity.EXTRA_VIDEO_CALL, R.string.pref_videocall_default, useValuesFromIntent);


        // Use Camera2 option.
        boolean useCamera2 = sharedPrefGetBoolean(R.string.pref_camera2_key, CallActivity.EXTRA_CAMERA2,
                R.string.pref_camera2_default, useValuesFromIntent);

        // Get default codecs.
        String videoCodec = sharedPrefGetString(R.string.pref_videocodec_key,
                CallActivity.EXTRA_VIDEOCODEC, R.string.pref_videocodec_default, useValuesFromIntent);
        String audioCodec = sharedPrefGetString(R.string.pref_audiocodec_key,
                CallActivity.EXTRA_AUDIOCODEC, R.string.pref_audiocodec_default, useValuesFromIntent);

        // Check HW codec flag.
        boolean hwCodec = sharedPrefGetBoolean(R.string.pref_hwcodec_key,
                CallActivity.EXTRA_HWCODEC_ENABLED, R.string.pref_hwcodec_default, useValuesFromIntent);

        // Check Capture to texture.
        boolean captureToTexture = sharedPrefGetBoolean(R.string.pref_capturetotexture_key,
                CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, R.string.pref_capturetotexture_default,
                useValuesFromIntent);

        // Check FlexFEC.
        boolean flexfecEnabled = sharedPrefGetBoolean(R.string.pref_flexfec_key,
                CallActivity.EXTRA_FLEXFEC_ENABLED, R.string.pref_flexfec_default, useValuesFromIntent);

        // Check Disable Audio Processing flag.
        boolean noAudioProcessing = sharedPrefGetBoolean(R.string.pref_noaudioprocessing_key,
                CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, R.string.pref_noaudioprocessing_default,
                useValuesFromIntent);

        boolean aecDump = sharedPrefGetBoolean(R.string.pref_aecdump_key,
                CallActivity.EXTRA_AECDUMP_ENABLED, R.string.pref_aecdump_default, useValuesFromIntent);

        boolean saveInputAudioToFile =
                sharedPrefGetBoolean(R.string.pref_enable_save_input_audio_to_file_key,
                        CallActivity.EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED,
                        R.string.pref_enable_save_input_audio_to_file_default, useValuesFromIntent);

        // Check OpenSL ES enabled flag.
        boolean useOpenSLES = sharedPrefGetBoolean(R.string.pref_opensles_key,
                CallActivity.EXTRA_OPENSLES_ENABLED, R.string.pref_opensles_default, useValuesFromIntent);

        // Check Disable built-in AEC flag.
        boolean disableBuiltInAEC = sharedPrefGetBoolean(R.string.pref_disable_built_in_aec_key,
                CallActivity.EXTRA_DISABLE_BUILT_IN_AEC, R.string.pref_disable_built_in_aec_default,
                useValuesFromIntent);

        // Check Disable built-in AGC flag.
        boolean disableBuiltInAGC = sharedPrefGetBoolean(R.string.pref_disable_built_in_agc_key,
                CallActivity.EXTRA_DISABLE_BUILT_IN_AGC, R.string.pref_disable_built_in_agc_default,
                useValuesFromIntent);

        // Check Disable built-in NS flag.
        boolean disableBuiltInNS = sharedPrefGetBoolean(R.string.pref_disable_built_in_ns_key,
                CallActivity.EXTRA_DISABLE_BUILT_IN_NS, R.string.pref_disable_built_in_ns_default,
                useValuesFromIntent);

        // Check Disable gain control
        boolean disableWebRtcAGCAndHPF = sharedPrefGetBoolean(
                R.string.pref_disable_webrtc_agc_and_hpf_key, CallActivity.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF,
                R.string.pref_disable_webrtc_agc_and_hpf_key, useValuesFromIntent);

        // Get video resolution from settings.
        int videoWidth = 0;
        int videoHeight = 0;
        if (useValuesFromIntent) {
            videoWidth = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_WIDTH, 0);
            videoHeight = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_HEIGHT, 0);
        }
        if (videoWidth == 0 && videoHeight == 0) {
            String resolution =
                    sharedPref.getString(keyprefResolution, getString(R.string.pref_resolution_default));
            String[] dimensions = resolution.split("[ x]+");
            if (dimensions.length == 2) {
                try {
                    videoWidth = Integer.parseInt(dimensions[0]);
                    videoHeight = Integer.parseInt(dimensions[1]);
                } catch (NumberFormatException e) {
                    videoWidth = 0;
                    videoHeight = 0;

                }
            }
        }

        // Get camera fps from settings.
        int cameraFps = 0;
        if (useValuesFromIntent) {
            cameraFps = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_FPS, 0);
        }
        if (cameraFps == 0) {
            String fps = sharedPref.getString(keyprefFps, getString(R.string.pref_fps_default));
            String[] fpsValues = fps.split("[ x]+");
            if (fpsValues.length == 2) {
                try {
                    cameraFps = Integer.parseInt(fpsValues[0]);
                } catch (NumberFormatException e) {
                    cameraFps = 0;

                }
            }
        }

        // Get video and audio start bitrate.
        int videoStartBitrate = 0;
        if (useValuesFromIntent) {
            videoStartBitrate = getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_BITRATE, 0);
        }
        if (videoStartBitrate == 0) {
            String bitrateTypeDefault = getString(R.string.pref_maxvideobitrate_default);
            String bitrateType = sharedPref.getString(keyprefVideoBitrateType, bitrateTypeDefault);
            if (!bitrateType.equals(bitrateTypeDefault)) {
                String bitrateValue = sharedPref.getString(
                        keyprefVideoBitrateValue, getString(R.string.pref_maxvideobitratevalue_default));
                videoStartBitrate = Integer.parseInt(bitrateValue);
            }
        }

        int audioStartBitrate = 0;
        if (useValuesFromIntent) {
            audioStartBitrate = getIntent().getIntExtra(CallActivity.EXTRA_AUDIO_BITRATE, 0);
        }
        if (audioStartBitrate == 0) {
            String bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default);
            String bitrateType = sharedPref.getString(keyprefAudioBitrateType, bitrateTypeDefault);
            if (!bitrateType.equals(bitrateTypeDefault)) {
                String bitrateValue = sharedPref.getString(
                        keyprefAudioBitrateValue, getString(R.string.pref_startaudiobitratevalue_default));
                audioStartBitrate = Integer.parseInt(bitrateValue);
            }
        }


        boolean tracing = sharedPrefGetBoolean(R.string.pref_tracing_key, CallActivity.EXTRA_TRACING,
                R.string.pref_tracing_default, useValuesFromIntent);

        // Check Enable RTCEventLog.
        boolean rtcEventLogEnabled = sharedPrefGetBoolean(R.string.pref_enable_rtceventlog_key,
                CallActivity.EXTRA_ENABLE_RTCEVENTLOG, R.string.pref_enable_rtceventlog_default,
                useValuesFromIntent);

        // Get datachannel options
        boolean dataChannelEnabled = sharedPrefGetBoolean(R.string.pref_enable_datachannel_key,
                CallActivity.EXTRA_DATA_CHANNEL_ENABLED, R.string.pref_enable_datachannel_default,
                useValuesFromIntent);
        boolean ordered = sharedPrefGetBoolean(R.string.pref_ordered_key, CallActivity.EXTRA_ORDERED,
                R.string.pref_ordered_default, useValuesFromIntent);
        boolean negotiated = sharedPrefGetBoolean(R.string.pref_negotiated_key,
                CallActivity.EXTRA_NEGOTIATED, R.string.pref_negotiated_default, useValuesFromIntent);
        int maxRetrMs = sharedPrefGetInteger(R.string.pref_max_retransmit_time_ms_key,
                CallActivity.EXTRA_MAX_RETRANSMITS_MS, R.string.pref_max_retransmit_time_ms_default,
                useValuesFromIntent);
        int maxRetr =
                sharedPrefGetInteger(R.string.pref_max_retransmits_key, CallActivity.EXTRA_MAX_RETRANSMITS,
                        R.string.pref_max_retransmits_default, useValuesFromIntent);
        int id = sharedPrefGetInteger(R.string.pref_data_id_key, CallActivity.EXTRA_ID,
                R.string.pref_data_id_default, useValuesFromIntent);
        String protocol = sharedPrefGetString(R.string.pref_data_protocol_key,
                CallActivity.EXTRA_PROTOCOL, R.string.pref_data_protocol_default, useValuesFromIntent);


        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra(Content.USER, pid);
        intent.putExtra(CallActivity.EXTRA_ROOMID, "TODO");

        intent.putExtra(CallActivity.EXTRA_VIDEO_CALL, videoCallEnabled);
        intent.putExtra(CallActivity.EXTRA_CAMERA2, useCamera2);
        intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, videoWidth);
        intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, videoHeight);
        intent.putExtra(CallActivity.EXTRA_VIDEO_FPS, cameraFps);

        // adds a seek bar with quality
        intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE, videoStartBitrate);
        intent.putExtra(CallActivity.EXTRA_VIDEOCODEC, RTCPeerConnection.VIDEO_CODEC_H264_HIGH);
        intent.putExtra(CallActivity.EXTRA_HWCODEC_ENABLED, "false");
        intent.putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, captureToTexture);
        intent.putExtra(CallActivity.EXTRA_FLEXFEC_ENABLED, flexfecEnabled);
        intent.putExtra(CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, noAudioProcessing);
        intent.putExtra(CallActivity.EXTRA_AECDUMP_ENABLED, aecDump);
        intent.putExtra(CallActivity.EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, saveInputAudioToFile);
        intent.putExtra(CallActivity.EXTRA_OPENSLES_ENABLED, useOpenSLES);
        intent.putExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AEC, disableBuiltInAEC);
        intent.putExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AGC, disableBuiltInAGC);
        intent.putExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_NS, disableBuiltInNS);
        intent.putExtra(CallActivity.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, disableWebRtcAGCAndHPF);
        intent.putExtra(CallActivity.EXTRA_AUDIO_BITRATE, audioStartBitrate);
        intent.putExtra(CallActivity.EXTRA_AUDIOCODEC, RTCPeerConnection.AUDIO_CODEC_OPUS);
        intent.putExtra(CallActivity.EXTRA_TRACING, tracing);
        intent.putExtra(CallActivity.EXTRA_ENABLE_RTCEVENTLOG, rtcEventLogEnabled);
        intent.putExtra(CallActivity.EXTRA_DATA_CHANNEL_ENABLED, dataChannelEnabled);

        if (dataChannelEnabled) {
            intent.putExtra(CallActivity.EXTRA_ORDERED, ordered);
            intent.putExtra(CallActivity.EXTRA_MAX_RETRANSMITS_MS, maxRetrMs);
            intent.putExtra(CallActivity.EXTRA_MAX_RETRANSMITS, maxRetr);
            intent.putExtra(CallActivity.EXTRA_PROTOCOL, protocol);
            intent.putExtra(CallActivity.EXTRA_NEGOTIATED, negotiated);
            intent.putExtra(CallActivity.EXTRA_ID, id);
        }

        if (useValuesFromIntent) {
            if (getIntent().hasExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA)) {
                String videoFileAsCamera =
                        getIntent().getStringExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA);
                intent.putExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA, videoFileAsCamera);
            }

            if (getIntent().hasExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE)) {
                String saveRemoteVideoToFile =
                        getIntent().getStringExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);
                intent.putExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE, saveRemoteVideoToFile);
            }

            if (getIntent().hasExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH)) {
                int videoOutWidth =
                        getIntent().getIntExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
                intent.putExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, videoOutWidth);
            }

            if (getIntent().hasExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT)) {
                int videoOutHeight =
                        getIntent().getIntExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
                intent.putExtra(CallActivity.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, videoOutHeight);
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);

    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private String sharedPrefGetString(
            int attributeId, String intentName, int defaultId, boolean useFromIntent) {
        String defaultValue = getString(defaultId);
        if (useFromIntent) {
            String value = getIntent().getStringExtra(intentName);
            if (value != null) {
                return value;
            }
            return defaultValue;
        } else {
            String attributeName = getString(attributeId);
            return sharedPref.getString(attributeName, defaultValue);
        }
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private boolean sharedPrefGetBoolean(
            int attributeId, String intentName, int defaultId, boolean useFromIntent) {
        boolean defaultValue = Boolean.parseBoolean(getString(defaultId));
        if (useFromIntent) {
            return getIntent().getBooleanExtra(intentName, defaultValue);
        } else {
            String attributeName = getString(attributeId);
            return sharedPref.getBoolean(attributeName, defaultValue);
        }
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private int sharedPrefGetInteger(
            int attributeId, String intentName, int defaultId, boolean useFromIntent) {
        String defaultString = getString(defaultId);
        int defaultValue = Integer.parseInt(defaultString);
        if (useFromIntent) {
            return getIntent().getIntExtra(intentName, defaultValue);
        } else {
            String attributeName = getString(attributeId);
            String value = sharedPref.getString(attributeName, defaultString);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
                return defaultValue;
            }
        }
    }

    private class CallBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ACTION_INCOMING_CALL)) {
                handleIncomingCallIntent(intent);
            }
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