package threads.server;


import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import threads.server.fragments.InfoDialogFragment;
import threads.server.fragments.NameDialogFragment;
import threads.server.fragments.PeersFragment;
import threads.server.fragments.PinsFragment;
import threads.server.fragments.SettingsDialogFragment;
import threads.server.fragments.SwarmFragment;
import threads.server.fragments.ThreadsFragment;
import threads.server.fragments.WebViewDialogFragment;
import threads.server.model.EventViewModel;
import threads.server.model.SelectionViewModel;
import threads.server.provider.FileDocumentsProvider;
import threads.server.services.DaemonService;
import threads.server.services.DiscoveryService;
import threads.server.services.DownloaderService;
import threads.server.services.LiteService;
import threads.server.services.RegistrationService;
import threads.server.services.UploadService;
import threads.server.utils.CodecDecider;
import threads.server.utils.CustomViewPager;
import threads.server.utils.MimeType;
import threads.server.utils.PermissionAction;
import threads.server.work.ConnectionWorker;
import threads.server.work.LoadNotificationsWorker;
import threads.server.work.LoadPeersWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        ThreadsFragment.ActionListener,
        PinsFragment.ActionListener,
        PeersFragment.ActionListener,
        NameDialogFragment.ActionListener,
        InfoDialogFragment.ActionListener,
        DontShowAgainFragmentDialog.ActionListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                ConnectionWorker.connect(getApplicationContext(), true);
                LoadPeersWorker.loadPeers(getApplicationContext());
                LoadNotificationsWorker.notifications(context);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
    };
    private DrawerLayout mDrawerLayout;
    private CustomViewPager mCustomViewPager;
    private BottomNavigationView mNavigation;


    private SelectionViewModel mSelectionViewModel;
    private Toolbar mToolbar;
    private NsdManager mNsdManager;

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        mNsdManager.unregisterService(RegistrationService.getInstance());
        mNsdManager.stopServiceDiscovery(DiscoveryService.getInstance());
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


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_daemon: {
                DaemonService.invoke(getApplicationContext(), true);
                break;
            }
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

    private void registerService(int port, String serviceName) {
        try {
            String serviceType = "_ipfs-discovery._udp";
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(serviceName);
            serviceInfo.setServiceType(serviceType);
            serviceInfo.setPort(port);
            mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
            checkNotNull(mNsdManager);
            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD,
                    RegistrationService.getInstance());

            PID host = IPFS.getPID(getApplicationContext());

            IPFS ipfs = IPFS.getInstance(getApplicationContext());

            DiscoveryService discovery = DiscoveryService.getInstance();
            discovery.setOnServiceFoundListener((info) -> {

                Log.e(TAG, info.toString());

                mNsdManager.resolveService(info, new NsdManager.ResolveListener() {

                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e(TAG, "onResolveFailed : " + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        Log.e(TAG, "onServiceResolved : " + serviceInfo.toString());

                        try {
                            String serviceName = serviceInfo.getServiceName();
                            boolean connect = true;
                            if (host != null) {
                                connect = !Objects.equals(host.getPid(), serviceName);
                            }
                            if (connect) {

                                Multihash.fromBase58(serviceName);

                                PID pid = PID.create(serviceName);
                                if (!ipfs.isConnected(pid)) {

                                    InetAddress inetAddress = serviceInfo.getHost();
                                    String pre = "/ip4";
                                    if (inetAddress instanceof Inet6Address) {
                                        pre = "/ip6";
                                    }
                                    String multiAddress = pre + serviceInfo.getHost().toString() +
                                            "/tcp/" + serviceInfo.getPort() + "/" +
                                            IPFS.Style.p2p + "/" + pid.getPid();

                                    int timeout = InitApplication.getConnectionTimeout(getApplicationContext());
                                    ExecutorService executor = Executors.newSingleThreadExecutor();
                                    executor.submit(() -> ipfs.swarmConnect(multiAddress, timeout));
                                }
                            }
                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }
                    }
                });

            });
            mNsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discovery);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.ThreadsTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        mToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        mToolbar.setSubtitle(R.string.files);

        LiteService.getInstance(getApplicationContext());


        mSelectionViewModel = new ViewModelProvider(this).get(SelectionViewModel.class);


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
                prevMenuItem = mNavigation.getMenu().getItem(position);

                switch (prevMenuItem.getItemId()) {
                    case R.id.navigation_files:
                        mToolbar.setSubtitle(R.string.files);
                        break;
                    case R.id.navigation_peers:
                        mToolbar.setSubtitle(R.string.peers);
                        break;
                    case R.id.navigation_swarm:
                        mToolbar.setSubtitle(R.string.swarm);
                    case R.id.navigation_pins:
                        mToolbar.setSubtitle(R.string.pins);
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
                case R.id.navigation_pins:
                    mCustomViewPager.setCurrentItem(3);
                    mToolbar.setSubtitle(R.string.pins);
                    return true;
            }
            return false;

        });

        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);

        toggle.syncState();


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

        eventViewModel.getPermission().observe(this, (event) -> {
            try {
                if (event != null) {
                    String content = event.getContent();
                    if (!content.isEmpty()) {
                        Snackbar snackbar = Snackbar.make(mDrawerLayout, content,
                                Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(R.string.app_settings, new PermissionAction());
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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);


        PID host = IPFS.getPID(getApplicationContext());
        checkNotNull(host);
        int port = IPFS.getSwarmPort(getApplicationContext());
        registerService(port, host.getPid());

        Intent intent = getIntent();
        handleIntents(intent);

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
                            DownloaderService.download(getApplicationContext(),
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
                case 3:
                    return new PinsFragment();
                default:
                    throw new RuntimeException("Not Supported position");
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

}