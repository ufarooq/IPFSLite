package threads.server;

import android.Manifest;
import android.app.SearchManager;
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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import de.psdev.licensesdialog.LicensesDialogFragment;
import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.AddressType;
import threads.server.core.peers.PEERS;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.fragments.DontShowAgainFragmentDialog;
import threads.server.fragments.EditMultihashDialogFragment;
import threads.server.fragments.EditPeerDialogFragment;
import threads.server.fragments.InfoDialogFragment;
import threads.server.fragments.NameDialogFragment;
import threads.server.fragments.PeersFragment;
import threads.server.fragments.SettingsDialogFragment;
import threads.server.fragments.SwarmFragment;
import threads.server.fragments.ThreadsFragment;
import threads.server.fragments.WebViewDialogFragment;
import threads.server.jobs.JobServiceDownload;
import threads.server.jobs.JobServiceIdentity;
import threads.server.model.EventViewModel;
import threads.server.model.SelectionViewModel;
import threads.server.provider.FileDocumentsProvider;
import threads.server.services.BootstrapService;
import threads.server.services.DaemonService;
import threads.server.services.LiteService;
import threads.server.services.UploadService;
import threads.server.utils.CodecDecider;
import threads.server.utils.CustomViewPager;
import threads.server.utils.MimeType;
import threads.server.utils.Network;
import threads.server.utils.PermissionAction;

import static androidx.core.util.Preconditions.checkNotNull;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        EditMultihashDialogFragment.ActionListener,
        EditPeerDialogFragment.ActionListener,
        ThreadsFragment.ActionListener,
        PeersFragment.ActionListener,
        NameDialogFragment.ActionListener,
        InfoDialogFragment.ActionListener,
        SwarmFragment.ActionListener,
        DontShowAgainFragmentDialog.ActionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_SELECT_FILES = 1;

    private static final int CONTENT_REQUEST_VIDEO_CAPTURE = 3;
    private static final int PEER_REQUEST_VIDEO_CAPTURE = 4;
    private static final int CLICK_OFFSET = 500;


    private final AtomicBoolean idScan = new AtomicBoolean(false);

    private DrawerLayout mDrawerLayout;
    private AppBarLayout mAppBarLayout;
    private long mLastClickTime = 0;
    private CustomViewPager mCustomViewPager;
    private BottomNavigationView mNavigation;
    private FloatingActionButton mMainFab;
    private SelectionViewModel mSelectionViewModel;
    private Toolbar mToolbar;
    private boolean isTablet;

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

            if (requestCode == REQUEST_SELECT_FILES) {

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


        try {
            CodecDecider codecDecider = CodecDecider.evaluate(codec);
            if (codecDecider.getCodex() == CodecDecider.Codec.MULTIHASH ||
                    codecDecider.getCodex() == CodecDecider.Codec.URI) {

                String multihash = codecDecider.getMultihash();
                BootstrapService.bootstrap(getApplicationContext());
                JobServiceDownload.download(getApplicationContext(), CID.create(multihash));
            } else {
                EVENTS.getInstance(getApplicationContext()).postError(getString(R.string.codec_not_supported));
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
    public void clickScanPeer() {

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

            Uri uri = FileDocumentsProvider.getUriForBitmap(code);
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
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_privacy_policy: {

                try {

                    String data;
                    if (LiteService.isNightNode(getApplicationContext())) {
                        data = LiteService.loadRawData(getApplicationContext(), R.raw.privacy_policy_night);
                    } else {
                        data = LiteService.loadRawData(getApplicationContext(), R.raw.privacy_policy);
                    }

                    WebViewDialogFragment.newInstance(WebViewDialogFragment.Type.HTML, data)
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

                    String data = "<!doctype html><html><h2>Config</h2><pre>" + ipfs.config_show() + "</pre></html>";

                    if (LiteService.isNightNode(getApplicationContext())) {
                        data = "<!doctype html><html><head><style>body { background-color: DarkSlateGray; color: white; }</style></head><h2>Config</h2><pre>" + ipfs.config_show() + "</pre></html>";
                    }

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
                    Uri uri = Uri.parse(LiteService.getAddressLink(address));

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
                    Uri uri = Uri.parse(LiteService.getAddressLink(address));

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

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        isTablet = getResources().getBoolean(R.bool.isTablet);
        mToolbar = findViewById(R.id.toolbar);
        mAppBarLayout = findViewById(R.id.app_bar_layout);

        setSupportActionBar(mToolbar);
        mToolbar.setSubtitle(R.string.files);

        LiteService.getInstance(getApplicationContext());

        mMainFab = findViewById(R.id.fab_main);
        if (isTablet) {
            mMainFab.setVisibility(View.GONE);
        }

        mSelectionViewModel = new ViewModelProvider(this).get(SelectionViewModel.class);

        final Observer<Long> parentObserver = (threadIdx) -> {

            if (mNavigation != null) {
                redrawFab(mNavigation.getSelectedItemId());
            }

        };
        mSelectionViewModel.getParentThread().observe(this, parentObserver);


        mCustomViewPager = findViewById(R.id.customViewPager);
        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
        mCustomViewPager.setAdapter(adapter);

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
                showMainFab(true);
                redrawFab(prevMenuItem.getItemId());
                switch (prevMenuItem.getItemId()) {
                    case R.id.navigation_files:
                        mToolbar.setSubtitle(R.string.files);
                        break;
                    case R.id.navigation_peers:
                        mToolbar.setSubtitle(R.string.peers);
                        break;
                    case R.id.navigation_swarm:
                        mToolbar.setSubtitle(R.string.swarm);
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
            showMainFab(true);
            redrawFab(item.getItemId());

            switch (item.getItemId()) {
                case R.id.navigation_files:
                    mCustomViewPager.setCurrentItem(0);
                    mToolbar.setSubtitle(R.string.files);
                    return true;
                case R.id.navigation_peers:
                    mCustomViewPager.setCurrentItem(1);
                    mToolbar.setSubtitle(R.string.peers);
                    return true;
                case R.id.navigation_swarm:
                    mCustomViewPager.setCurrentItem(2);
                    mToolbar.setSubtitle(R.string.swarm);
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
                    clickScanPeer();
                    break;
                case R.id.navigation_swarm:
                    break;
            }

        });


        EventViewModel eventViewModel =
                new ViewModelProvider(this).get(EventViewModel.class);


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

        Intent intent = getIntent();
        handleIntents(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem actionDaemon = menu.findItem(R.id.action_daemon);
        boolean isDaemonRunning = DaemonService.isDaemonRunning(getApplicationContext());
        if (!isDaemonRunning) {
            actionDaemon.setIcon(R.drawable.play_circle);
        } else {
            actionDaemon.setIcon(R.drawable.stop_circle);
        }

        return true;
    }


    private void redrawFab(int id) {
        if (!isTablet) {
            switch (id) {
                case R.id.navigation_files:
                    if (mSelectionViewModel.isTopLevel()) {
                        mMainFab.setImageResource(R.drawable.plus_thick);
                    } else {
                        mMainFab.setImageResource(R.drawable.arrow_left_bold);
                    }

                    mMainFab.setBackgroundTintList(
                            ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent));

                    break;
                case R.id.navigation_peers:
                    mMainFab.setImageResource(R.drawable.account_plus);
                    mMainFab.setBackgroundTintList(
                            ContextCompat.getColorStateList(getApplicationContext(), R.color.colorAccent));
                    break;
                case R.id.navigation_swarm:
                    mMainFab.hide();
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_daemon) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            boolean startDaemon = DaemonService.isDaemonRunning(getApplicationContext());
            boolean newStartDaemonValue = !startDaemon;
            DaemonService.setDaemonRunning(getApplicationContext(), newStartDaemonValue);
            DaemonService.invoke(getApplicationContext(), newStartDaemonValue);
            invalidateOptionsMenu();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }


    private void threadsFabAction() {

        if (mSelectionViewModel.isTopLevel()) {

            clickUpload();


        } else {
            Long idx = mSelectionViewModel.getParentThread().getValue();
            checkNotNull(idx);
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


    @Override
    public void clickConnectPeer(@NonNull String pid) {
        checkNotNull(pid);

        // CHECKED if pid is valid
        try {
            Multihash.fromBase58(pid);
        } catch (Throwable e) {
            EVENTS.getInstance(getApplicationContext()).postError(getString(R.string.multihash_not_valid));

            return;
        }

        // CHECKED
        PID host = IPFS.getPID(getApplicationContext());
        PID user = PID.create(pid);

        if (user.equals(host)) {
            EVENTS.getInstance(getApplicationContext()).postError(
                    getString(R.string.same_pid_like_host));
            return;
        }

        // CHECKED
        if (!Network.isConnected(getApplicationContext())) {
            EVENTS.getInstance(getApplicationContext()).postWarning(
                    getString(R.string.offline_mode));
        }

        LiteService.connectPeer(getApplicationContext(), user, false);
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
        if (!isTablet) {
            if (visible) {
                mMainFab.show();
            } else {
                mMainFab.hide();
            }
        }
    }

    @Override
    public void setPagingEnabled(boolean enabled) {
        mCustomViewPager.setPagingEnabled(enabled);
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

                String mimeType = MimeType.PLAIN_MIME_TYPE;

                long size = text.length();

                Thread thread = threads.createThread(0L);
                thread.setSize(size);
                thread.setMimeType(mimeType);
                long idx = threads.storeThread(thread);


                try {
                    CID cid = ipfs.storeText(text);
                    checkNotNull(cid);


                    // cleanup of entries with same CID and hierarchy
                    List<Thread> sameEntries = threads.getThreadsByContentAndParent(cid, 0L);
                    threads.removeThreads(ipfs, sameEntries);

                    threads.setThreadName(idx, cid.getCid());
                    threads.setThreadContent(idx, cid);
                    threads.setThreadSeeding(idx);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                } finally {
                    threads.removeThreads(ipfs, idx);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntents(intent);
    }

    private void handleIntents(Intent intent) {

        final String action = intent.getAction();


        try {
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                Log.e(TAG, "" + query);
                mSelectionViewModel.setQuery(query);
            }
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
                        CodecDecider result = CodecDecider.evaluate(text);

                        if (result.getCodex() == CodecDecider.Codec.MULTIHASH ||
                                result.getCodex() == CodecDecider.Codec.URI) {
                            JobServiceDownload.download(getApplicationContext(),
                                    CID.create(result.getMultihash()));

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
    public void dontShowAgain(@NonNull String key, boolean notShowAgain) {
        InitApplication.setDontShowAgain(this, key, notShowAgain);
    }


    @Override
    public void name(@NonNull String pid, @NonNull String name) {
        checkNotNull(pid);
        checkNotNull(name);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                PEERS.getInstance(getApplicationContext()).setUserAlias(pid, name);
            } catch (Throwable e) {
                EVENTS.getInstance(getApplicationContext()).exception(e);
            }
        });


    }

    @Override
    public void clickInfoPeer() {

        try {
            JobServiceIdentity.publish(getApplicationContext());
            PID pid = IPFS.getPID(getApplicationContext());
            checkNotNull(pid);
            try {
                InfoDialogFragment.newInstance(pid.getPid(),
                        getString(R.string.your_peer_id),
                        getString(R.string.peer_access, pid.getPid()))
                        .show(getSupportFragmentManager(), InfoDialogFragment.TAG);

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    private class PagerAdapter extends FragmentStatePagerAdapter {


        PagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
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
            return 3;
        }
    }

}