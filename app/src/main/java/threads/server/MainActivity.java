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
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
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
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.events.EVENTS;
import threads.core.peers.AddressType;
import threads.core.peers.PEERS;
import threads.core.peers.User;
import threads.core.threads.THREADS;
import threads.core.threads.Thread;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;
import threads.ipfs.api.PeerInfo;
import threads.server.jobs.JobServiceDeleteThreads;
import threads.server.jobs.JobServiceDownload;
import threads.server.jobs.JobServiceIdentity;
import threads.server.jobs.JobServiceLoadPublicKey;
import threads.server.jobs.JobServicePublish;
import threads.server.mdl.ApplicationViewModel;
import threads.server.mdl.EventViewModel;
import threads.server.mdl.SelectionViewModel;
import threads.share.ConnectService;
import threads.share.DetailsDialogFragment;
import threads.share.DontShowAgainDialog;
import threads.share.GatewayService;
import threads.share.InfoDialogFragment;
import threads.share.NameDialogFragment;
import threads.share.Network;
import threads.share.PeerActionDialogFragment;
import threads.share.PermissionAction;
import threads.share.ThreadActionDialogFragment;
import threads.share.ThumbnailService;
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
        NameDialogFragment.ActionListener,
        PeerActionDialogFragment.ActionListener,
        DontShowAgainDialog.ActionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final int REQUEST_SELECT_FILES = 4;
    private static final int REQUEST_EXTERNAL_STORAGE = 5;

    private static final int CONTENT_REQUEST_VIDEO_CAPTURE = 9;
    private static final int PEER_REQUEST_VIDEO_CAPTURE = 10;

    private final AtomicReference<Long> storedThread = new AtomicReference<>(null);
    private final AtomicReference<String> storedUser = new AtomicReference<>(null);
    private final AtomicBoolean idScan = new AtomicBoolean(false);

    private DrawerLayout mDrawerLayout;
    private AppBarLayout mAppBarLayout;

    private long mLastClickTime = 0;
    private CustomViewPager mCustomViewPager;
    private BottomNavigationView mNavigation;
    private FloatingActionButton mMainFab;
    private SelectionViewModel mSelectionViewModel;
    private int mTrafficColorId = android.R.color.holo_red_dark;

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
            case REQUEST_VIDEO_CAPTURE: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar snackbar = Snackbar.make(mDrawerLayout,
                                getString(R.string.permission_camera_denied),
                                Snackbar.LENGTH_LONG);
                        snackbar.setAction(R.string.app_settings, new PermissionAction());
                        snackbar.setAnchorView(mNavigation);
                        snackbar.show();

                    }
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        clickUserCall(storedUser.get());
                    }
                }

                break;
            }
            case CONTENT_REQUEST_VIDEO_CAPTURE: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar snackbar = Snackbar.make(mDrawerLayout,
                                getString(R.string.permission_camera_denied),
                                Snackbar.LENGTH_LONG);
                        snackbar.setAction(R.string.app_settings, new PermissionAction());
                        snackbar.setAnchorView(mNavigation);
                        snackbar.show();

                    }
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        clickMultihashWork();
                    }
                }

                break;
            }
            case PEER_REQUEST_VIDEO_CAPTURE: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar snackbar = Snackbar.make(mDrawerLayout,
                                getString(R.string.permission_camera_denied),
                                Snackbar.LENGTH_LONG);
                        snackbar.setAction(R.string.app_settings, new PermissionAction());
                        snackbar.setAnchorView(mNavigation);
                        snackbar.show();

                    }
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        clickConnectPeerWork();
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
        Singleton singleton = Singleton.getInstance(getApplicationContext());
        if (!Network.isConnected(getApplicationContext())) {
            Preferences.error(singleton.getEvents(), getString(R.string.offline_mode));
            return;
        }


        try {
            CodecDecider codecDecider = CodecDecider.evaluate(codec);
            if (codecDecider.getCodex() == CodecDecider.Codec.MULTIHASH ||
                    codecDecider.getCodex() == CodecDecider.Codec.URI) {

                PID host = Preferences.getPID(getApplicationContext());
                checkNotNull(host);
                String multihash = codecDecider.getMultihash();

                JobServiceDownload.download(getApplicationContext(),
                        host, CID.create(multihash));
            } else {

                Preferences.error(singleton.getEvents(),
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


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    private Toolbar mToolbar;

    @Override
    public void clickConnectPeer() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PEER_REQUEST_VIDEO_CAPTURE);
            return;
        }

        clickConnectPeerWork();
    }

    public void clickConnectPeerWork() {
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

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CONTENT_REQUEST_VIDEO_CAPTURE);
            return;
        }
        clickMultihashWork();
    }

    private void clickMultihashWork() {
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
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
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
    public void clickUserBlock(@NonNull String pid, boolean value) {
        checkNotNull(pid);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                PEERS peers = Singleton.getInstance(getApplicationContext()).getPeers();

                if (!value) {
                    peers.unblockUser(PID.create(pid));
                } else {
                    peers.blockUser(PID.create(pid));
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });

    }

    @Override
    public void clickUserInfo(@NonNull String pid) {
        checkNotNull(pid);
        try {
            InfoDialogFragment.newInstance(pid,
                    getString(R.string.peer_id),
                    getString(R.string.peer_access, pid))
                    .show(getSupportFragmentManager(), InfoDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickUserDelete(@NonNull String pid) {
        checkNotNull(pid);
        try {
            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
            final PEERS peers = Singleton.getInstance(getApplicationContext()).getPeers();
            final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    checkNotNull(ipfs, "IPFS is not valid");
                    User user = peers.getUserByPID(PID.create(pid));
                    if (user != null) {
                        peers.removeUser(ipfs, user);
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(events, Preferences.EXCEPTION, e);
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
        Singleton singleton = Singleton.getInstance(getApplicationContext());

        if (!Network.isConnected(getApplicationContext())) {
            Preferences.error(singleton.getEvents(), getString(R.string.offline_mode));
            return;
        }


        try {

            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            final PEERS peers = Singleton.getInstance(getApplicationContext()).getPeers();
            final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    PID user = PID.create(pid);

                    if (peers.isUserBlocked(user)) {
                        Preferences.warning(events, getString(R.string.peer_is_blocked));
                    } else {

                        try {
                            peers.setUserDialing(user, true);

                            boolean peerDiscovery = Service.isSupportPeerDiscovery(
                                    getApplicationContext());
                            int timeout = Preferences.getConnectionTimeout(getApplicationContext());
                            boolean value = ConnectService.connectPeer(getApplicationContext(),
                                    user, peerDiscovery, true, timeout);

                            peers.setUserConnected(user, value);

                            if (value) {
                                String publicKey = peers.getUserPublicKey(pid);
                                checkNotNull(publicKey,
                                        "Public key should be at least not null");
                                if (publicKey.isEmpty()) {
                                    JobServiceLoadPublicKey.publicKey(getApplicationContext(), pid);
                                }
                            }


                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            peers.setUserConnected(user, false);
                        } finally {
                            peers.setUserDialing(user, false);
                        }
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(events, Preferences.EXCEPTION, e);
                }
            });

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
                    i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    String sAux = "\n" + getString(R.string.store_mail) + "\n\n";
                    if (BuildConfig.FDroid) {
                        sAux = sAux + getString(R.string.fdroid_url) + "\n\n";
                    } else {
                        sAux = sAux + getString(R.string.play_store_url) + "\n\n";
                    }
                    i.putExtra(Intent.EXTRA_TEXT, sAux);
                    startActivity(Intent.createChooser(i, getString(R.string.choose_one)));
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ThreadsTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = findViewById(R.id.toolbar);
        mAppBarLayout = findViewById(R.id.app_bar_layout);

        setSupportActionBar(mToolbar);
        mToolbar.setSubtitle(R.string.files);

        Service.getInstance(getApplicationContext());

        mSelectionViewModel = new ViewModelProvider(this).get(SelectionViewModel.class);


        mMainFab = findViewById(R.id.fab_main);
        mCustomViewPager = findViewById(R.id.customViewPager);
        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), 3);
        mCustomViewPager.setAdapter(adapter);
        // mCustomViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mCustomViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private MenuItem prevMenuItem;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null)
                    prevMenuItem.setChecked(false);
                else
                    mNavigation.getMenu().getItem(0).setChecked(false);

                mNavigation.getMenu().getItem(position).setChecked(true);
                mAppBarLayout.setExpanded(true, false);
                prevMenuItem = mNavigation.getMenu().getItem(position);
                switch (prevMenuItem.getItemId()) {
                    case R.id.navigation_files:
                        mToolbar.setSubtitle(R.string.files);
                        mMainFab.setImageResource(R.drawable.dots);
                        mMainFab.setBackgroundTintList(
                                ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent));

                        break;
                    case R.id.navigation_peers:
                        mToolbar.setSubtitle(R.string.peers);
                        mMainFab.setImageResource(R.drawable.dots);
                        mMainFab.setBackgroundTintList(
                                ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent));

                        break;
                    case R.id.navigation_swarm:
                        mToolbar.setSubtitle(R.string.swarm);
                        mMainFab.setImageResource(R.drawable.traffic_light);
                        mMainFab.setBackgroundTintList(
                                ContextCompat.getColorStateList(getApplicationContext(), mTrafficColorId));
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mNavigation = findViewById(R.id.navigation);
        mNavigation.refreshDrawableState();
        mNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                mAppBarLayout.setExpanded(true, false);
                switch (item.getItemId()) {
                    case R.id.navigation_files:
                        mCustomViewPager.setCurrentItem(0);
                        mToolbar.setSubtitle(R.string.files);
                        mMainFab.setImageResource(R.drawable.dots);
                        mMainFab.setBackgroundTintList(
                                ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent));

                        return true;
                    case R.id.navigation_peers:
                        mCustomViewPager.setCurrentItem(1);
                        mToolbar.setSubtitle(R.string.peers);
                        mMainFab.setImageResource(R.drawable.dots);
                        mMainFab.setBackgroundTintList(
                                ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent));

                        return true;
                    case R.id.navigation_swarm:
                        mCustomViewPager.setCurrentItem(2);
                        mToolbar.setSubtitle(R.string.swarm);
                        mMainFab.setImageResource(R.drawable.traffic_light);
                        mMainFab.setBackgroundTintList(
                                ContextCompat.getColorStateList(getApplicationContext(), mTrafficColorId));

                        return true;
                }
                return false;
            }
        });

        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        mMainFab.setOnClickListener((v) -> {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();


            switch (mNavigation.getSelectedItemId()) {
                case R.id.navigation_files:
                    threadsFabAction();
                    break;
                case R.id.navigation_peers:
                    PeersDialogFragment.newInstance()
                            .show(getSupportFragmentManager(), PeersDialogFragment.TAG);
                    break;
                case R.id.navigation_swarm:
                    swarmFabAction();
                    break;
            }

        });


        new ViewModelProvider(this).get(ApplicationViewModel.class);
        EventViewModel eventViewModel =
                new ViewModelProvider(this).get(EventViewModel.class);


        eventViewModel.getIPFSInstallFailure().observe(this, (event) -> {
            try {
                if (event != null) {
                    Snackbar snackbar = Snackbar.make(mDrawerLayout,
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
                    snackbar.setAnchorView(mNavigation);
                    snackbar.show();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
        eventViewModel.getIPFSStartFailure().observe(this, (event) -> {
            try {
                if (event != null) {
                    Snackbar snackbar = Snackbar.make(mDrawerLayout,
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
                    snackbar.setAnchorView(mNavigation);
                    snackbar.show();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
        eventViewModel.getException().observe(this, (event) -> {
            try {
                if (event != null) {
                    String content = event.getContent();
                    if (!content.isEmpty()) {
                        Snackbar snackbar = Snackbar.make(mDrawerLayout, content,
                                Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(android.R.string.ok, (view) -> snackbar.dismiss());
                        snackbar.setAnchorView(mNavigation);
                        snackbar.show();
                    }
                    eventViewModel.removeEvent(event);

                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });

        eventViewModel.getWarning().observe(this, (event) -> {
            try {
                if (event != null) {
                    String content = event.getContent();
                    if (!content.isEmpty()) {
                        Toast.makeText(
                                getApplicationContext(), content, Toast.LENGTH_LONG).show();
                    }
                    eventViewModel.removeEvent(event);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
        eventViewModel.getInfo().observe(this, (event) -> {
            try {
                if (event != null) {
                    String content = event.getContent();
                    if (!content.isEmpty()) {
                        Toast.makeText(getApplicationContext(), content, Toast.LENGTH_LONG).show();
                    }
                    eventViewModel.removeEvent(event);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
        eventViewModel.getEvent(SwarmFragment.TAG).observe(this, (event) -> {
            try {
                if (event != null) {
                    String content = event.getContent();
                    switch (content) {
                        case SwarmFragment.HIGH:
                            mTrafficColorId = android.R.color.holo_green_light;
                            break;
                        case SwarmFragment.MEDIUM:
                            mTrafficColorId = android.R.color.holo_orange_light;
                            break;
                        case SwarmFragment.LOW:
                            mTrafficColorId = android.R.color.holo_red_light;
                            break;
                        default:
                            mTrafficColorId = android.R.color.holo_red_dark;
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });

        handleIntents();


    }

    private void handleIntents() {

        Intent intent = getIntent();
        final String action = intent.getAction();


        try {

            if (Intent.ACTION_SEND.equals(action)) {
                String type = intent.getType();
                if ("text/plain".equals(type)) {
                    handleSendText(intent);
                } else {
                    handleSend(intent, false);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    handleSend(intent, true);
                } else {
                    String type = intent.getType();
                    if ("text/plain".equals(type)) {
                        handleSendText(intent);
                    } else {
                        handleSend(intent, true);
                    }
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }


    }

    private void handleSendText(Intent intent) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            try {
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text != null) {
                    Service.getInstance(this).storeData(getApplicationContext(), text);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    private void handleSend(Intent intent, boolean multi) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (multi) {
                    ClipData mClipData = intent.getClipData();
                    if (mClipData != null) {
                        for (int i = 0; i < mClipData.getItemCount(); i++) {
                            ClipData.Item item = mClipData.getItemAt(i);
                            Service.getInstance(this).storeData(getApplicationContext(), item.getUri());
                        }
                    } else if (uri != null) {
                        Service.getInstance(this).storeData(getApplicationContext(), uri);
                    }

                } else if (uri != null) {
                    Service.getInstance(this).storeData(getApplicationContext(), uri);
                }


            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    private void threadsFabAction() {
        Long idx = mSelectionViewModel.getParentThread().getValue();
        checkNotNull(idx);
        if (idx == 0L) {

            ThreadsDialogFragment.newInstance()
                    .show(getSupportFragmentManager(), ThreadsDialogFragment.TAG);


        } else {

            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    Thread thread = threads.getThreadByIdx(idx);
                    if (thread != null) {
                        long parent = thread.getThread();
                        mSelectionViewModel.setParentThread(parent, true);
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(events, Preferences.EXCEPTION, e);
                }
            });

        }
    }

    private void swarmFabAction() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                GatewayService.PeerSummary info = GatewayService.evaluateAllPeers(getApplicationContext());


                String html = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><body style=\"background-color:snow;\"><h3 style=\"text-align:center; color:teal;\">Quality</h3><ul>";


                String numPeers = "Number Peers : " + info.getNumPeers();
                html = html.concat("<li><div style=\"width: 80%;" +
                        "  word-wrap:break-word;\">").concat(numPeers).concat("</div></li>");


                String latency = "Average Latency : n.a.";
                if (info.getLatency() < Long.MAX_VALUE) {
                    latency = "Average Latency : " + info.getLatency() + " [ms]";
                }


                html = html.concat("<li><div style=\"width: 80%;" +
                        "  word-wrap:break-word;\">").concat(latency).concat("</div></li>");
                html = html.concat("</ul></body><footer style=\"color:tomato;\">"
                        + getString(R.string.quality_measurement) + "</footer></html>");


                DetailsDialogFragment.newInstance(
                        DetailsDialogFragment.Type.HTML, html).show(
                        getSupportFragmentManager(),
                        DetailsDialogFragment.TAG);

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    @Override
    public void clickUserCall(@NonNull String pid) {
    }

    @Override
    public void clickUserDetails(@NonNull String pid) {

        Singleton singleton = Singleton.getInstance(getApplicationContext());
        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Preferences.error(singleton.getEvents(), getString(R.string.offline_mode));
            return;
        }


        try {
            final int timeout = Preferences.getConnectionTimeout(getApplicationContext());
            final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            final PEERS peers = Singleton.getInstance(getApplicationContext()).getPeers();
            final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

            final PID host = Preferences.getPID(getApplicationContext());
            checkNotNull(host);
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    PID peer = PID.create(pid);
                    try {

                        peers.setUserDialing(peer, true);

                        String protocolVersion = "n.a.";
                        String agentVersion = "n.a.";
                        List<String> addresses = new ArrayList<>();


                        PeerInfo info = ipfs.id(peer, timeout);

                        if (info != null) {
                            agentVersion = info.getAgentVersion();
                            protocolVersion = info.getProtocolVersion();
                            addresses = info.getMultiAddresses();
                        }

                        String html = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><body style=\"background-color:snow;\"><h3 style=\"text-align:center; color:teal;\">Peer Details</h3>";


                        html = html.concat("<div style=\"width: 80%;" +
                                "  word-wrap:break-word;\">").concat(pid).concat("</div><br/>");

                        html = html.concat("<div style=\"width: 80%;" +
                                "  word-wrap:break-word;\">").concat("Protocol Version : ").concat(protocolVersion).concat("</div><br/>");

                        html = html.concat("<ul>");

                        for (String address : addresses) {
                            html = html.concat("<li><div style=\"width: 80%;" +
                                    "  word-wrap:break-word;\">").concat(address).concat("</div></li>");
                        }


                        html = html.concat("</ul><br/></body><footer>Agent : <strong style=\"color:teal;\">" + agentVersion + "</strong></footer></html>");


                        DetailsDialogFragment.newInstance(
                                DetailsDialogFragment.Type.HTML, html).show(
                                getSupportFragmentManager(),
                                DetailsDialogFragment.TAG);
                    } catch (Throwable e) {
                        Preferences.evaluateException(events, Preferences.EXCEPTION, e);
                    } finally {
                        peers.setUserDialing(peer, false);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void clickUserAutoConnect(@NonNull String pid, boolean autoConnect) {


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                PEERS peers = Singleton.getInstance(getApplicationContext()).getPeers();

                peers.setUserAutoConnect(PID.create(pid), autoConnect);

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });

    }


    @Override
    public void clickConnectPeer(@NonNull String pid) {
        checkNotNull(pid);

        // CHECKED if pid is valid
        Singleton singleton = Singleton.getInstance(getApplicationContext());
        try {
            Multihash.fromBase58(pid);
        } catch (Throwable e) {
            Preferences.error(singleton.getEvents(), getString(R.string.multihash_not_valid));
            return;
        }

        // CHECKED
        PID host = Preferences.getPID(getApplicationContext());
        PID user = PID.create(pid);

        if (user.equals(host)) {
            Preferences.error(singleton.getEvents(), getString(R.string.same_pid_like_host));
            return;
        }

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Preferences.error(singleton.getEvents(), getString(R.string.offline_mode));
            return;
        }


        try {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Service.connectPeer(getApplicationContext(), user);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    @Override
    public void clickThreadInfo(long idx) {
        try {
            final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

            final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
            final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        CID cid = threads.getThreadContent(idx);
                        checkNotNull(cid);
                        String multihash = cid.getCid();

                        InfoDialogFragment.newInstance(multihash,
                                getString(R.string.content_id),
                                getString(R.string.multihash_access, multihash))
                                .show(getSupportFragmentManager(), InfoDialogFragment.TAG);


                    } catch (Throwable e) {
                        Preferences.evaluateException(events, Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    @Override
    public void clickThreadsSend(final long[] idxs) {


        Singleton singleton = Singleton.getInstance(getApplicationContext());

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            Preferences.error(singleton.getEvents(), getString(R.string.offline_mode));
            return;
        }


        final THREADS threads = Singleton.getInstance(this).getThreads();
        final PEERS peers = Singleton.getInstance(getApplicationContext()).getPeers();
        final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                ArrayList<String> pids = Service.getInstance(getApplicationContext()).
                        getEnhancedUserPIDs(getApplicationContext());

                if (pids.isEmpty()) {
                    Preferences.error(events,
                            getApplicationContext().getString(R.string.no_sharing_peers));
                } else if (pids.size() == 1) {
                    List<User> users = new ArrayList<>();
                    users.add(peers.getUserByPID(PID.create(pids.get(0))));
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


    }

    @Override
    public void showBottomNavigation(boolean visible) {
        if (visible) {
            mNavigation.setVisibility(View.VISIBLE);
        } else {
            mNavigation.setVisibility(View.GONE);
        }
    }

    @Override
    public void showMainFab(boolean visible) {
        if (visible) {
            mMainFab.setVisibility(View.VISIBLE);
        } else {
            mMainFab.setVisibility(View.GONE);
        }
    }

    @Override
    public void setPagingEnabled(boolean enabled) {
        mCustomViewPager.setPagingEnabled(enabled);
    }

    @Override
    public void clickThreadDelete(long idx) {
        JobServiceDeleteThreads.removeThreads(getApplicationContext(), idx);
    }

    @Override
    public void clickThreadView(long idx) {

        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                CID cid = threads.getThreadContent(idx);
                checkNotNull(cid);

                JobServicePublish.publish(getApplicationContext(), cid, true);

                String gateway = Service.getGateway(getApplicationContext());
                Uri uri = Uri.parse(gateway + "/ipfs/" + cid.getCid());

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            } catch (Throwable e) {
                Preferences.evaluateException(events, Preferences.EXCEPTION, e);
            }
        });

    }

    @Override
    public void clickThreadShare(long idx) {
        final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
        final int timeout = Preferences.getConnectionTimeout(getApplicationContext());
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    CID cid = threads.getThreadContent(idx);
                    checkNotNull(cid);
                    String multihash = cid.getCid();

                    CID bitmap = Preferences.getBitmap(getApplicationContext(), multihash);
                    checkNotNull(bitmap);

                    File file = new File(ipfs.getCacheDir(), multihash + ".png");

                    if (!file.exists()) {
                        ipfs.storeToFile(file, bitmap, true, timeout, -1);
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
                    Preferences.evaluateException(events, Preferences.EXCEPTION, e);
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
    public void clickUserVideoCall(@NonNull String pid) {
    }


    @Override
    public void dontShowAgain(@NonNull String key, boolean notShowAgain) {
        Service.setDontShowAgain(this, key, notShowAgain);
    }

    @Override
    public void clickThreadPublish(long idx, boolean pinned) {

        if (pinned) {
            if (!Service.getDontShowAgain(this, Service.PIN_SERVICE_KEY)) {
                try {
                    DontShowAgainDialog.newInstance(
                            getString(R.string.pin_service_notice), Service.PIN_SERVICE_KEY).show(
                            getSupportFragmentManager(), DontShowAgainDialog.TAG);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            }
        }
        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                threads.setThreadPinned(idx, pinned);
            } catch (Throwable e) {
                Preferences.evaluateException(events, Preferences.EXCEPTION, e);
            }
        });

    }

    @Override
    public void name(@NonNull String pid, @NonNull String name) {
        checkNotNull(pid);
        checkNotNull(name);

        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
        final PEERS peers = Singleton.getInstance(getApplicationContext()).getPeers();
        final EVENTS events = Singleton.getInstance(getApplicationContext()).getEvents();

        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    PID user = PID.create(pid);

                    CID image = ThumbnailService.getImage(getApplicationContext(),
                            name, R.drawable.server_network);
                    peers.setUserImage(user, image);

                    peers.setUserAlias(user, name);


                    threads.setThreadSenderAlias(user, name);

                } catch (Throwable e) {
                    Preferences.evaluateException(events, Preferences.EXCEPTION, e);
                }
            });
        }


    }

    @Override
    public void clickInfoPeer() {

        try {
            JobServiceIdentity.identity(getApplicationContext());
            PID pid = Preferences.getPID(getApplicationContext());
            checkNotNull(pid);
            clickUserInfo(pid.getPid());
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void clickPeerInfo(@NonNull String pid) {
        clickUserInfo(pid);
    }

    @Override
    public void clickPeerDetails(@NonNull String pid) {
        clickUserDetails(pid);
    }

    @Override
    public void clickPeerAdd(@NonNull String pid) {
        clickConnectPeer(pid);
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
                    return new SwarmFragment();
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