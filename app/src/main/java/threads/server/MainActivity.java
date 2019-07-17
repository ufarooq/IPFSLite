package threads.server;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import de.psdev.licensesdialog.LicensesDialogFragment;
import threads.core.ConnectService;
import threads.core.Network;
import threads.core.PeerService;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.AddressType;
import threads.core.api.ThreadStatus;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.mdl.EventViewModel;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.IpnsInfo;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;
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
        ThreadsFragment.ActionListener,
        NameDialogFragment.ActionListener {

    private static final String TAG = MainActivity.class.getSimpleName();
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


                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        Uri uri = item.getUri();
                        Service.getInstance(
                                getApplicationContext()).storeData(getApplicationContext(), uri);
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    Service.getInstance(
                            getApplicationContext()).storeData(getApplicationContext(), uri);
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
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public void downloadMultihash(@NonNull String codec) {
        checkNotNull(codec);

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Singleton singleton = Singleton.getInstance(getApplicationContext());
            Preferences.error(singleton.getThreads(), getString(R.string.offline_mode));
            return;
        }


        try {
            final Singleton singleton = Singleton.getInstance(this);
            CodecDecider codecDecider = CodecDecider.evaluate(codec);
            if (codecDecider.getCodex() == CodecDecider.Codec.MULTIHASH ||
                    codecDecider.getCodex() == CodecDecider.Codec.URI) {

                PID host = Preferences.getPID(getApplicationContext());
                checkNotNull(host);
                String multihash = codecDecider.getMultihash();

                // check if multihash is valid
                try {
                    Multihash.fromBase58(codec);
                } catch (Throwable e) {
                    Preferences.error(singleton.getThreads(),
                            this.getString(R.string.multihash_not_valid));
                    return;
                }

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        Service.downloadMultihash(getApplicationContext(), host,
                                CID.create(multihash), null, null, null);
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }
                });
            } else {

                Preferences.error(singleton.getThreads(),
                        getString(R.string.codec_not_supported));
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void daemonStatus() {

        try {
            if (!DaemonService.DAEMON_RUNNING.get()) {
                fab_daemon.setImageDrawable(getDrawable(R.drawable.play));
            } else {
                fab_daemon.setImageDrawable(getDrawable(R.drawable.stop));
            }
            findViewById(R.id.fab_daemon).setVisibility(View.VISIBLE);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_info: {
                return false;
            }
            case R.id.action_mark_all: {
                return false;
            }
            case R.id.action_delete: {
                return false;
            }
            case R.id.action_send: {
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
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }
            case R.id.nav_issues: {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://gitlab.com/remmer.wilts/threads-server/issues"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }
            case R.id.nav_documentation: {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://gitlab.com/remmer.wilts/threads-server"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }
            case R.id.nav_licences: {
                try {

                    LicensesDialogFragment fragment =
                            new LicensesDialogFragment.Builder(this)
                                    .setNotices(R.raw.licenses)
                                    .setShowFullLicenseText(false)
                                    .setIncludeOwnLicense(true)
                                    .build();

                    fragment.show(getSupportFragmentManager(), null);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }
            case R.id.nav_settings: {
                try {
                    FragmentManager fm = getSupportFragmentManager();
                    SettingsDialogFragment settingsDialogFragment = new SettingsDialogFragment();
                    settingsDialogFragment.show(fm, SettingsDialogFragment.TAG);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }
            case R.id.nav_config: {
                try {
                    IPFS ipfs = Singleton.getInstance(this).getIpfs();
                    if (ipfs != null) {
                        String data = "<html><h2>Config</h2><pre>" + ipfs.config_show() + "</pre></html>";
                        WebViewDialogFragment.newInstance(WebViewDialogFragment.Type.HTML, data)
                                .show(getSupportFragmentManager(), WebViewDialogFragment.TAG);
                    }

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }
            case R.id.nav_inbox: {
                try {
                    PID pid = Preferences.getPID(this);
                    checkNotNull(pid);
                    String address = AddressType.getAddress(pid, AddressType.NOTIFICATION);
                    Uri uri = Uri.parse(Service.getAddressLink(address));

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }

            case R.id.nav_outbox: {
                try {
                    PID pid = Preferences.getPID(this);
                    checkNotNull(pid);
                    String address = AddressType.getAddress(pid, AddressType.PEER);
                    Uri uri = Uri.parse(Service.getAddressLink(address));

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }

            case R.id.nav_share: {
                try {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name_full));
                    String sAux = "\n" + getString(R.string.store_mail) + "\n\n";
                    if (BuildConfig.FDroid) {
                        sAux = sAux + getString(R.string.fdroid_url) + "\n\n";
                    } else {
                        sAux = sAux + getString(R.string.playstore_url) + "\n\n";
                    }
                    i.putExtra(Intent.EXTRA_TEXT, sAux);
                    startActivity(Intent.createChooser(i, getString(R.string.choose_one)));
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickEditMultihash() {
        try {
            FragmentManager fm = getSupportFragmentManager();
            EditMultihashDialogFragment editMultihashDialogFragment = new EditMultihashDialogFragment();
            editMultihashDialogFragment.show(fm, EditMultihashDialogFragment.TAG);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    @Override
    public void clickEditPeer() {
        try {
            FragmentManager fm = getSupportFragmentManager();
            EditPeerDialogFragment editPeerDialogFragment = new EditPeerDialogFragment();
            editPeerDialogFragment.show(fm, EditPeerDialogFragment.TAG);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickUserBlock(@NonNull String pid) {
        checkNotNull(pid);
        try {
            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    User user = threads.getUserByPID(PID.create(pid));
                    checkNotNull(user);

                    if (user.isBlocked()) {
                        threads.unblockUser(user);
                    } else {
                        threads.blockUser(user);
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickUserInfo(@NonNull String pid) {
        checkNotNull(pid);
        try {
            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    PeerService.publishPeer(getApplicationContext(), BuildConfig.ApiAesKey);
                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });

            InfoDialogFragment.show(this, pid,
                    getString(R.string.peer_id),
                    getString(R.string.peer_access, pid));
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickUserDelete(@NonNull String pid) {
        checkNotNull(pid);
        try {
            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    threads.removeUserByPID(PID.create(pid));
                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickUserConnect(@NonNull String pid) {
        checkNotNull(pid);

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Singleton singleton = Singleton.getInstance(getApplicationContext());
            Preferences.error(singleton.getThreads(), getString(R.string.offline_mode));
            return;
        }


        try {
            final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {

                        PID user = PID.create(pid);

                        if (threads.isUserBlocked(user)) {
                            Preferences.warning(threads, getString(R.string.peer_is_blocked));
                        } else {

                            try {
                                threads.setUserStatus(user, UserStatus.DIALING);

                                boolean value = ConnectService.connectUser(
                                        getApplicationContext(), user, BuildConfig.ApiAesKey);

                                if (value) {
                                    threads.setUserStatus(user, UserStatus.ONLINE);
                                } else {
                                    threads.setUserStatus(user, UserStatus.OFFLINE);
                                }
                            } catch (Throwable e) {
                                threads.setUserStatus(user, UserStatus.OFFLINE);
                            }
                        }
                    } catch (Throwable e) {
                        Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickUserEdit(@NonNull String pid) {

        try {
            FragmentManager fm = getSupportFragmentManager();
            NameDialogFragment.newInstance(pid, getString(R.string.peer_name))
                    .show(fm, NameDialogFragment.TAG);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
            daemonStatus();

        });
        daemonStatus();


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
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });

    }


    @Override
    public void clickUserCall(@NonNull String pid) {

        checkNotNull(pid);


        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Singleton singleton = Singleton.getInstance(getApplicationContext());
            Preferences.error(singleton.getThreads(), getString(R.string.offline_mode));
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
            final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            final PID host = Preferences.getPID(getApplicationContext());
            checkNotNull(host);
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        User user = threads.getUserByPID(PID.create(pid));
                        checkNotNull(user);

                        if (threads.isUserBlocked(user.getPID())) {
                            Preferences.warning(threads, getString(R.string.peer_is_blocked));
                        } else {

                            Intent intent = RTCCallActivity.createIntent(MainActivity.this,
                                    pid, user.getAlias(), null, true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            intent.setAction(RTCCallActivity.ACTION_OUTGOING_CALL);
                            MainActivity.this.startActivity(intent);
                        }
                    } catch (Throwable e) {
                        Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    @Override
    public void clickConnectPeer(@NonNull String pid) {
        checkNotNull(pid);

        // CHECKED if pid is valid
        Singleton singleton = Singleton.getInstance(getApplicationContext());
        try {
            Multihash.fromBase58(pid);
        } catch (Throwable e) {
            Preferences.error(singleton.getThreads(), getString(R.string.multihash_not_valid));
            return;
        }

        // CHECKED
        PID host = Preferences.getPID(getApplicationContext());
        PID user = PID.create(pid);

        if (user.equals(host)) {
            Preferences.error(singleton.getThreads(), getString(R.string.same_pid_like_host));
            return;
        }

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {

            Preferences.error(singleton.getThreads(), getString(R.string.offline_mode));
            return;
        }


        try {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Service.connectUser(getApplicationContext(), user);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickThreadPublish(long idx) {

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Singleton singleton = Singleton.getInstance(getApplicationContext());
            Preferences.error(singleton.getThreads(), getString(R.string.offline_mode));
            return;
        }

        try {
            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {

                    threads.setThreadStatus(idx, ThreadStatus.PUBLISHING);

                    try {
                        CID cid = threads.getThreadCID(idx);
                        checkNotNull(cid);

                        IpnsInfo info = ipfs.name_publish(cid);

                        if (info != null) {
                            Uri uri = Uri.parse("https://ipfs.io/ipns/" + info.getName());

                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    } catch (Throwable e) {
                        Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                    } finally {
                        threads.setThreadStatus(idx, ThreadStatus.ONLINE);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickThreadInfo(long idx) {
        try {
            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        CID cid = threads.getThreadCID(idx);
                        checkNotNull(cid);
                        String multihash = cid.getCid();

                        InfoDialogFragment.show(this, multihash,
                                getString(R.string.content_id),
                                getString(R.string.multihash_access, multihash));


                    } catch (Throwable e) {
                        Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    @Override
    public void clickThreadsSend(final long[] idxs) {
        try {
            // CHECKED
            if (!Network.isConnected(getApplicationContext())) {
                Singleton singleton = Singleton.getInstance(getApplicationContext());
                Preferences.error(singleton.getThreads(), getString(R.string.offline_mode));
                return;
            }

            final THREADS threads = Singleton.getInstance(this).getThreads();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    ArrayList<String> pids = Service.getInstance(getApplicationContext()).
                            getEnhancedUserPIDs(getApplicationContext());

                    if (pids.isEmpty()) {
                        Preferences.error(threads,
                                getApplicationContext().getString(R.string.no_sharing_peers));
                    } else if (pids.size() == 1) {
                        List<User> users = new ArrayList<>();
                        users.add(threads.getUserByPID(PID.create(pids.get(0))));
                        Service.getInstance(getApplicationContext()).sendThreads(
                                getApplicationContext(), users, idxs);
                    } else {
                        FragmentManager fm = getSupportFragmentManager();
                        SendDialogFragment dialogFragment = new SendDialogFragment();
                        Bundle bundle = new Bundle();
                        bundle.putLongArray(SendDialogFragment.IDXS, idxs);
                        bundle.putStringArrayList(SendDialogFragment.PIDS, pids);
                        dialogFragment.setArguments(bundle);
                        dialogFragment.show(fm, SendDialogFragment.TAG);
                    }

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickThreadDelete(long idx) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Service.removeThreads(getApplicationContext(), idx);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });

    }

    @Override
    public void clickThreadView(long idx) {

        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    CID cid = threads.getThreadCID(idx);
                    checkNotNull(cid);
                    String multihash = cid.getCid();

                    Uri uri = Uri.parse("https://ipfs.io/ipfs/" + multihash);

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });
        }
    }

    @Override
    public void clickThreadShare(long idx) {

        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    CID cid = threads.getThreadCID(idx);
                    checkNotNull(cid);
                    String multihash = cid.getCid();

                    CID bitmap = Preferences.getBitmap(getApplicationContext(), multihash);
                    checkNotNull(bitmap);

                    File file = new File(ipfs.getCacheDir(), multihash + ".png");

                    if (!file.exists()) {
                        ipfs.store(file, bitmap, "");
                    }

                    Uri uri = FileProvider.getUriForFile(getApplicationContext(),
                            getApplicationContext().getPackageName() + ".provider", file);


                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, multihash);
                    shareIntent.putExtra(Intent.EXTRA_TEXT,
                            getString(R.string.multihash_access, multihash));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.setType("image/png");
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent,
                            getResources().getText(R.string.share)));


                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });
        }

    }

    @Override
    public void clickThreadSend(long idx) {
        long[] idxs = {idx};
        clickThreadsSend(idxs);
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
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }

    @Override
    public void clickThreadPin(long idx, boolean pinned) {
        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                threads.setThreadPinned(idx, pinned);
            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        });

    }

    @Override
    public void name(@NonNull String pid, @NonNull String name) {
        checkNotNull(pid);
        checkNotNull(name);

        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();

        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    PID user = PID.create(pid);

                    byte[] data = THREADS.getImage(getApplicationContext(),
                            name, R.drawable.server_network);
                    CID image = ipfs.add(data, "", true);
                    checkNotNull(image);
                    threads.setUserImage(user, image);

                    threads.setUserAlias(user, name);


                    threads.setThreadSenderAlias(user, name);

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });
        }


    }

    @Override
    public void clickInfoPeer() {

        try {
            PID pid = Preferences.getPID(getApplicationContext());
            checkNotNull(pid);
            clickUserInfo(pid.getPid());
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    private class PagerAdapter extends FragmentStatePagerAdapter {
        final int mNumOfTabs;

        PagerAdapter(FragmentManager fm, int NumOfTabs) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
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