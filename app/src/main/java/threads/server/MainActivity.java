package threads.server;

import android.Manifest;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import de.psdev.licensesdialog.LicensesDialogFragment;
import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.ipfs.PeerInfo;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.AddressType;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.core.threads.Kind;
import threads.server.core.threads.Status;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.fragments.DetailsDialogFragment;
import threads.server.fragments.DontShowAgainFragmentDialog;
import threads.server.fragments.EditMultihashDialogFragment;
import threads.server.fragments.EditPeerDialogFragment;
import threads.server.fragments.InfoDialogFragment;
import threads.server.fragments.NameDialogFragment;
import threads.server.fragments.PeerActionDialogFragment;
import threads.server.fragments.PeersDialogFragment;
import threads.server.fragments.PeersFragment;
import threads.server.fragments.SendDialogFragment;
import threads.server.fragments.SettingsDialogFragment;
import threads.server.fragments.SwarmFragment;
import threads.server.fragments.ThreadActionDialogFragment;
import threads.server.fragments.ThreadsDialogFragment;
import threads.server.fragments.ThreadsFragment;
import threads.server.fragments.UserActionDialogFragment;
import threads.server.fragments.WebViewDialogFragment;
import threads.server.jobs.JobServiceContents;
import threads.server.jobs.JobServiceDeleteThreads;
import threads.server.jobs.JobServiceDownload;
import threads.server.jobs.JobServiceIdentity;
import threads.server.jobs.JobServiceLoadPublicKey;
import threads.server.jobs.JobServicePublish;
import threads.server.mdl.EventViewModel;
import threads.server.mdl.SelectionViewModel;
import threads.server.provider.FileDocumentsProvider;
import threads.server.services.ConnectService;
import threads.server.services.DaemonService;
import threads.server.services.GatewayService;
import threads.server.services.Service;
import threads.server.services.ThumbnailService;
import threads.server.services.UploadService;
import threads.server.utils.CodecDecider;
import threads.server.utils.CustomViewPager;
import threads.server.utils.MimeType;
import threads.server.utils.Network;
import threads.server.utils.PermissionAction;
import threads.server.utils.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        PeersDialogFragment.ActionListener,
        ThreadsDialogFragment.ActionListener,
        UserActionDialogFragment.ActionListener,
        ThreadActionDialogFragment.ActionListener,
        EditMultihashDialogFragment.ActionListener,
        EditPeerDialogFragment.ActionListener,
        ThreadsFragment.ActionListener,
        NameDialogFragment.ActionListener,
        PeerActionDialogFragment.ActionListener,
        InfoDialogFragment.ActionListener,
        DontShowAgainFragmentDialog.ActionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_SELECT_FILES = 1;
    private static final int FILE_EXPORT_REQUEST_CODE = 2;
    private static final int CONTENT_REQUEST_VIDEO_CAPTURE = 3;
    private static final int PEER_REQUEST_VIDEO_CAPTURE = 4;
    private static final int CLICK_OFFSET = 500;


    private final AtomicBoolean idScan = new AtomicBoolean(false);
    private CID threadContent = null;
    private DrawerLayout mDrawerLayout;
    private AppBarLayout mAppBarLayout;
    private long mLastClickTime = 0;
    private CustomViewPager mCustomViewPager;
    private BottomNavigationView mNavigation;
    private FloatingActionButton mMainFab;
    private SelectionViewModel mSelectionViewModel;
    private int mTrafficColorId = android.R.color.holo_red_dark;
    private Toolbar mToolbar;
    private AtomicBoolean mStartDaemon = new AtomicBoolean(false);

    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
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

        if (resultCode != RESULT_OK) {
            return;
        }

        try {

            switch (requestCode) {
                case FILE_EXPORT_REQUEST_CODE: {
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            IPFS ipfs = IPFS.getInstance(getApplicationContext());

                            OutputStream os = getContentResolver().openOutputStream(uri);
                            if (os != null) {
                                try {
                                    ipfs.storeToOutputStream(os, threadContent);
                                } catch (Throwable e) {
                                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                } finally {
                                    os.close();
                                }
                            }
                        }
                    }
                    return;
                }
                case REQUEST_SELECT_FILES: {

                    if (data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        for (int i = 0; i < mClipData.getItemCount(); i++) {
                            ClipData.Item item = mClipData.getItemAt(i);
                            Uri uri = item.getUri();
                            UploadService.invoke(getApplicationContext(), uri);
                        }
                    } else if (data.getData() != null) {
                        Uri uri = data.getData();
                        UploadService.invoke(getApplicationContext(), uri);
                    }
                    return;
                }
            }


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

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public void downloadMultihash(@NonNull String codec) {
        checkNotNull(codec);

        // CHECKED

        if (!Network.isConnected(getApplicationContext())) {
            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(getApplicationContext()).error(getString(R.string.offline_mode)));
            threadError.start();
            return;
        }


        try {
            CodecDecider codecDecider = CodecDecider.evaluate(codec);
            if (codecDecider.getCodex() == CodecDecider.Codec.MULTIHASH ||
                    codecDecider.getCodex() == CodecDecider.Codec.URI) {

                PID host = IPFS.getPID(getApplicationContext());
                checkNotNull(host);
                String multihash = codecDecider.getMultihash();

                JobServiceDownload.download(getApplicationContext(),
                        host, CID.create(multihash));
            } else {
                java.lang.Thread threadError = new java.lang.Thread(()
                        -> EVENTS.getInstance(getApplicationContext()).error(
                        getString(R.string.codec_not_supported)));
                threadError.start();

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

    @Override
    public void shareQRCode(@NonNull String code, @NonNull String message) {
        checkNotNull(code);
        checkNotNull(message);
        try {

            Uri uri = FileDocumentsProvider.getUriForString(code);
            ComponentName[] names = {new ComponentName(getApplicationContext(), MainActivity.class)};

            String mimeType = "image/png";
            Intent intent = ShareCompat.IntentBuilder.from(this)
                    .setStream(uri)
                    .setType(mimeType)
                    .getIntent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, code);
            intent.putExtra(Intent.EXTRA_TEXT, message);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType(mimeType);
            intent.putExtra(DocumentsContract.EXTRA_EXCLUDE_SELF, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                Intent chooser = Intent.createChooser(intent, getText(R.string.share));
                chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, names);
                startActivity(chooser);
            } else {
                java.lang.Thread threadError = new java.lang.Thread(()
                        -> EVENTS.getInstance(getApplicationContext()).error(
                        getString(R.string.no_activity_found_to_handle_uri)));
                threadError.start();
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
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
            Intent intent = ShareCompat.IntentBuilder.from(this).getIntent();
            intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("*/*");

            String[] mimeTypes = {"*/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.putExtra(DocumentsContract.EXTRA_EXCLUDE_SELF, true);
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);


            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(Intent.createChooser(intent,
                        getString(R.string.select_files)), REQUEST_SELECT_FILES);
            } else {
                java.lang.Thread threadError = new java.lang.Thread(()
                        -> EVENTS.getInstance(getApplicationContext()).error(
                        getString(R.string.no_activity_found_to_handle_uri)));
                threadError.start();
            }

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
                PEERS peers = PEERS.getInstance(getApplicationContext());
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
            final IPFS ipfs = IPFS.getInstance(getApplicationContext());
            final PEERS peers = PEERS.getInstance(getApplicationContext());
            final EVENTS events = EVENTS.getInstance(getApplicationContext());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    checkNotNull(ipfs, "IPFS is not valid");
                    User user = peers.getUserByPID(PID.create(pid));
                    if (user != null) {
                        peers.removeUser(ipfs, user);
                    }

                } catch (Throwable e) {
                    events.exception(e);
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clickUserConnect(@NonNull String pid) {
        checkNotNull(pid);

        if (!Network.isConnected(getApplicationContext())) {
            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(getApplicationContext()).error(getString(R.string.offline_mode)));
            threadError.start();
            return;
        }


        try {

            final PEERS peers = PEERS.getInstance(getApplicationContext());
            final EVENTS events = EVENTS.getInstance(getApplicationContext());
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    PID user = PID.create(pid);

                    if (peers.isUserBlocked(user)) {
                        events.invokeEvent(EVENTS.WARNING,
                                getString(R.string.peer_is_blocked));
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
                    events.exception(e);
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
                    IPFS ipfs = IPFS.getInstance(getApplicationContext());

                    String data = "<html><h2>Config</h2><pre>" + ipfs.config_show() + "</pre></html>";
                    WebViewDialogFragment.newInstance(WebViewDialogFragment.Type.HTML, data)
                            .show(getSupportFragmentManager(), WebViewDialogFragment.TAG);


                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                break;
            }
            case R.id.nav_inbox: {
                try {
                    PID pid = IPFS.getPID(this);
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
                    PID pid = IPFS.getPID(this);
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
                    ComponentName[] names = {new ComponentName(
                            getApplicationContext(), MainActivity.class)};
                    String mimeType = "text/plain";
                    Intent intent = ShareCompat.IntentBuilder.from(this)
                            .setType(mimeType)
                            .getIntent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType(mimeType);
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    String sAux = "\n" + getString(R.string.store_mail) + "\n\n";
                    if (BuildConfig.FDroid) {
                        sAux = sAux + getString(R.string.fdroid_url) + "\n\n";
                    } else {
                        sAux = sAux + getString(R.string.play_store_url) + "\n\n";
                    }
                    intent.putExtra(Intent.EXTRA_TEXT, sAux);

                    if (intent.resolveActivity(getPackageManager()) != null) {
                        Intent chooser = Intent.createChooser(intent, getText(R.string.share));
                        chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, names);
                        startActivity(chooser);
                    } else {
                        java.lang.Thread threadError = new java.lang.Thread(()
                                -> EVENTS.getInstance(getApplicationContext()).error(
                                getString(R.string.no_activity_found_to_handle_uri)));
                        threadError.start();
                    }

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
                        mMainFab.setImageResource(R.drawable.plus_thick);
                        mMainFab.setBackgroundTintList(
                                ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent));

                        break;
                    case R.id.navigation_peers:
                        mToolbar.setSubtitle(R.string.peers);
                        mMainFab.setImageResource(R.drawable.account_plus);
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
        mNavigation.setOnNavigationItemSelectedListener((item) -> {
            mAppBarLayout.setExpanded(true, false);
            switch (item.getItemId()) {
                case R.id.navigation_files:
                    mCustomViewPager.setCurrentItem(0);
                    mToolbar.setSubtitle(R.string.files);
                    mMainFab.setImageResource(R.drawable.plus_thick);
                    mMainFab.setBackgroundTintList(
                            ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent));

                    return true;
                case R.id.navigation_peers:
                    mCustomViewPager.setCurrentItem(1);
                    mToolbar.setSubtitle(R.string.peers);
                    mMainFab.setImageResource(R.drawable.account_plus);
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


        EventViewModel eventViewModel =
                new ViewModelProvider(this).get(EventViewModel.class);


        eventViewModel.getDaemon().observe(this, (event) -> {

            if (event != null) {
                try {
                    mStartDaemon.set(Boolean.parseBoolean(event.getContent()));
                } catch (Exception e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                invalidateOptionsMenu();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem actionDaemon = menu.findItem(R.id.action_daemon);

        if (!mStartDaemon.get()) {
            actionDaemon.setIcon(R.drawable.play_circle);
        } else {
            actionDaemon.setIcon(R.drawable.stop_circle);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_daemon: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {

                    try {
                        boolean startDaemon = mStartDaemon.get();
                        boolean newStartDaemonValue = !startDaemon;
                        mStartDaemon.set(newStartDaemonValue);

                        EVENTS events = EVENTS.getInstance(getApplicationContext());
                        events.invokeEvent(EVENTS.DAEMON, mStartDaemon.toString());

                        DaemonService.invoke(getApplicationContext(), newStartDaemonValue);
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }
                });
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    private void threadsFabAction() {
        Long idx = mSelectionViewModel.getParentThread().getValue();
        checkNotNull(idx);
        if (idx == 0L) {

            ThreadsDialogFragment.newInstance()
                    .show(getSupportFragmentManager(), ThreadsDialogFragment.TAG);


        } else {

            final THREADS threads = THREADS.getInstance(getApplicationContext());
            final EVENTS events = EVENTS.getInstance(getApplicationContext());
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    Thread thread = threads.getThreadByIdx(idx);
                    if (thread != null) {
                        long parent = thread.getParent();
                        mSelectionViewModel.setParentThread(parent);
                    }

                } catch (Throwable e) {
                    events.exception(e);
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
    public void clickUserDetails(@NonNull String pid) {

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(getApplicationContext()).error(getString(R.string.offline_mode)));
            threadError.start();
            return;
        }


        try {
            final int timeout = Preferences.getConnectionTimeout(getApplicationContext());
            final IPFS ipfs = IPFS.getInstance(getApplicationContext());
            final PEERS peers = PEERS.getInstance(getApplicationContext());
            final EVENTS events = EVENTS.getInstance(getApplicationContext());
            final PID host = IPFS.getPID(getApplicationContext());
            checkNotNull(host);

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
                    events.exception(e);
                } finally {
                    peers.setUserDialing(peer, false);
                }
            });

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void clickUserAutoConnect(@NonNull String pid, boolean autoConnect) {


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                PEERS peers = PEERS.getInstance(getApplicationContext());
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
        try {
            Multihash.fromBase58(pid);
        } catch (Throwable e) {
            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(getApplicationContext()).error(getString(R.string.multihash_not_valid)));
            threadError.start();
            return;
        }

        // CHECKED
        PID host = IPFS.getPID(getApplicationContext());
        PID user = PID.create(pid);

        if (user.equals(host)) {

            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(getApplicationContext()).error(
                    getString(R.string.same_pid_like_host)));
            threadError.start();

            return;
        }

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(getApplicationContext()).error(
                    getString(R.string.offline_mode)));
            threadError.start();

            return;
        }


        try {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Service.connectPeer(getApplicationContext(), user, false);
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
            final EVENTS events = EVENTS.getInstance(getApplicationContext());
            final THREADS threads = THREADS.getInstance(getApplicationContext());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    CID cid = threads.getThreadContent(idx);
                    checkNotNull(cid);
                    String multihash = cid.getCid();

                    InfoDialogFragment.newInstance(multihash,
                            getString(R.string.content_id),
                            getString(R.string.multi_hash_access, multihash))
                            .show(getSupportFragmentManager(), InfoDialogFragment.TAG);


                } catch (Throwable e) {
                    events.exception(e);
                }
            });

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    @Override
    public void clickThreadsSend(final long[] indices) {


        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {

            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(getApplicationContext()).error(
                    getString(R.string.offline_mode)));
            threadError.start();

            return;
        }


        final PEERS peers = PEERS.getInstance(getApplicationContext());
        final EVENTS events = EVENTS.getInstance(getApplicationContext());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                ArrayList<String> pids = Service.getInstance(getApplicationContext()).
                        getEnhancedUserPIDs(getApplicationContext());

                if (pids.isEmpty()) {
                    events.error(getString(R.string.no_sharing_peers));
                } else if (pids.size() == 1) {
                    List<User> users = new ArrayList<>();
                    users.add(peers.getUserByPID(PID.create(pids.get(0))));
                    Service.getInstance(getApplicationContext()).sendThreads(
                            getApplicationContext(), users, indices);
                } else {
                    FragmentManager fm = getSupportFragmentManager();
                    SendDialogFragment dialogFragment = new SendDialogFragment();
                    Bundle bundle = new Bundle();
                    bundle.putLongArray(SendDialogFragment.IDXS, indices);
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

        final THREADS threads = THREADS.getInstance(getApplicationContext());
        final EVENTS events = EVENTS.getInstance(getApplicationContext());
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
                events.exception(e);
            }
        });

    }

    public void storeText(@NonNull String text) {
        checkNotNull(text);

        final THREADS threads = THREADS.getInstance(getApplicationContext());
        final IPFS ipfs = IPFS.getInstance(getApplicationContext());


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                PID pid = IPFS.getPID(getApplicationContext());
                checkNotNull(pid);
                String alias = IPFS.getDeviceName();
                checkNotNull(alias);

                String mimeType = MimeType.PLAIN_MIME_TYPE;

                long size = text.length();

                Thread thread = threads.createThread(pid, alias,
                        Status.INIT, Kind.IN, 0L);
                thread.setSize(size);
                thread.setMimeType(mimeType);

                long idx = threads.storeThread(thread);


                try {
                    threads.setThreadLeaching(idx, true);

                    CID cid = ipfs.storeText(text);
                    checkNotNull(cid);


                    // cleanup of entries with same CID and hierarchy
                    List<Thread> sameEntries = threads.getThreadsByCIDAndParent(cid, 0L);
                    threads.removeThreads(ipfs, sameEntries);

                    threads.setThreadName(idx, cid.getCid());
                    threads.setThreadContent(idx, cid);
                    threads.setThreadStatus(idx, Status.SEEDING);


                } catch (Throwable e) {
                    threads.setThreadStatus(idx, Status.ERROR);
                } finally {
                    threads.setThreadLeaching(idx, false);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    private void handleIntents() {

        Intent intent = getIntent();
        final String action = intent.getAction();


        try {
            ShareCompat.IntentReader intentReader = ShareCompat.IntentReader.from(this);
            if (Intent.ACTION_SEND.equals(action) ||
                    Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                handleSend(intentReader);
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }


    }


    private void handleSend(ShareCompat.IntentReader intentReader) {

        try {

            if (intentReader.isMultipleShare()) {
                for (int i = 0; i < intentReader.getStreamCount(); i++) {
                    UploadService.invoke(getApplicationContext(), intentReader.getStream(i));
                }
            } else {
                Uri uri = intentReader.getStream();
                String type = intentReader.getType();
                if ("text/plain".equals(type)) {
                    String text = intentReader.getText().toString();
                    if (!text.isEmpty()) {
                        PID host = IPFS.getPID(getApplicationContext());
                        checkNotNull(host);
                        CodecDecider result = CodecDecider.evaluate(text);

                        if (result.getCodex() == CodecDecider.Codec.MULTIHASH) {
                            JobServiceContents.contents(getApplicationContext(),
                                    host, CID.create(result.getMultihash()));
                        } else if (result.getCodex() == CodecDecider.Codec.URI) {
                            JobServiceDownload.download(getApplicationContext(),
                                    host, CID.create(result.getMultihash()));

                        } else {
                            storeText(text);
                        }
                    }
                } else if ("text/html".equals(type)) {
                    String html = intentReader.getHtmlText();
                    if (!html.isEmpty()) {
                        storeText(html);
                    }
                } else {
                    UploadService.invoke(getApplicationContext(), uri);
                }
            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void clickThreadShare(long idx) {
        final EVENTS events = EVENTS.getInstance(getApplicationContext());
        final THREADS threads = THREADS.getInstance(getApplicationContext());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Thread thread = threads.getThreadByIdx(idx);
                checkNotNull(thread);
                ComponentName[] names = {new ComponentName(
                        getApplicationContext(), MainActivity.class)};
                CID cid = thread.getContent();
                checkNotNull(cid);
                Uri uri = FileDocumentsProvider.getUriForString(cid.getCid());
                Intent intent = ShareCompat.IntentBuilder.from(this)
                        .setStream(uri)
                        .setType(thread.getMimeType())
                        .getIntent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.content_id));
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.content_file_access,
                        cid.getCid(), thread.getName()));
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_TITLE, thread.getName());


                if (intent.resolveActivity(getPackageManager()) != null) {
                    Intent chooser = Intent.createChooser(intent, getText(R.string.share));
                    chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, names);
                    startActivity(chooser);
                } else {
                    events.error(getString(R.string.no_activity_found_to_handle_uri));
                }

            } catch (Throwable e) {
                events.exception(e);
            }
        });


    }

    @Override
    public void clickThreadSend(long idx) {
        long[] indices = {idx};
        clickThreadsSend(indices);
    }

    @Override
    public void clickThreadCopy(long idx) {

        try {
            final THREADS threadsAPI = THREADS.getInstance(getApplicationContext());
            final EVENTS events = EVENTS.getInstance(getApplicationContext());


            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {

                Thread thread = threadsAPI.getThreadByIdx(idx);
                checkNotNull(thread);

                threadContent = thread.getContent();

                Intent intent = ShareCompat.IntentBuilder.from(this)
                        .setType(thread.getMimeType())
                        .getIntent();
                intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_TITLE, thread.getName());
                intent.putExtra(DocumentsContract.EXTRA_EXCLUDE_SELF, true);
                intent.addCategory(Intent.CATEGORY_OPENABLE);


                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, FILE_EXPORT_REQUEST_CODE);
                } else {
                    events.error(getString(R.string.no_activity_found_to_handle_uri));
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


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
                    DontShowAgainFragmentDialog.newInstance(
                            getString(R.string.pin_service_notice), Service.PIN_SERVICE_KEY).show(
                            getSupportFragmentManager(), DontShowAgainFragmentDialog.TAG);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            }
        }
        final THREADS threads = THREADS.getInstance(getApplicationContext());
        final EVENTS events = EVENTS.getInstance(getApplicationContext());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                threads.setThreadPinned(idx, pinned);
            } catch (Throwable e) {
                events.exception(e);
            }
        });

    }

    @Override
    public void name(@NonNull String pid, @NonNull String name) {
        checkNotNull(pid);
        checkNotNull(name);

        final THREADS threads = THREADS.getInstance(getApplicationContext());
        final PEERS peers = PEERS.getInstance(getApplicationContext());
        final EVENTS events = EVENTS.getInstance(getApplicationContext());

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
                events.exception(e);
            }
        });


    }

    @Override
    public void clickInfoPeer() {

        try {
            JobServiceIdentity.identity(getApplicationContext());
            PID pid = IPFS.getPID(getApplicationContext());
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
        checkNotNull(pid);

        // CHECKED if pid is valid
        try {
            Multihash.fromBase58(pid);
        } catch (Throwable e) {
            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(getApplicationContext()).error(getString(R.string.multihash_not_valid)));
            threadError.start();
            return;
        }

        // CHECKED
        PID host = IPFS.getPID(getApplicationContext());
        PID user = PID.create(pid);

        if (user.equals(host)) {

            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(getApplicationContext()).error(
                    getString(R.string.same_pid_like_host)));
            threadError.start();

            return;
        }


        try {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Service.connectPeer(getApplicationContext(),
                            user, true);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });

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