package threads.server;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.ipfs.IPFS;
import threads.server.event.Event;
import threads.server.event.Message;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private DrawerLayout drawer_layout;
    private FloatingActionButton server;

    private RecyclerView mRecyclerView;
    private MessageViewAdapter messageViewAdapter;
    private long mLastClickTime = 0;
    private EventViewModel eventViewModel;
    private EditText console_box;

    private AtomicBoolean networkAvailable = new AtomicBoolean(true);


    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        private Snackbar snackbar;
        private AtomicBoolean wasOffline = new AtomicBoolean(false);

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
                                getString(R.string.offline_modus), Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(R.string.network, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            }
                        });
                    }
                    snackbar.show();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.reyclerview_message_list);
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


        //traffic_light = findViewById(R.id.trafic_light);
        server = findViewById(R.id.server);
        server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                IPFS daemon = Application.getIpfs();
                if (!daemon.isRunning()) {

                    Intent intent = new Intent(MainActivity.this, DaemonService.class);
                    intent.setAction(DaemonService.ACTION_START_DAEMON_SERVICE);
                    startForegroundService(intent);


                    //evalueServerStatus();
                    server.setImageDrawable(getDrawable(R.drawable.stop));
                } else {
                    Intent intent = new Intent(MainActivity.this, DaemonService.class);
                    intent.setAction(DaemonService.ACTION_STOP_DAEMON_SERVICE);
                    startForegroundService(intent);


                    //evalueServerStatus();
                    server.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));
                }
            }
        });
        evalueServerStatus();


        MessagesViewModel messagesViewModel = ViewModelProviders.of(this).get(MessagesViewModel.class);
        messagesViewModel.getMessages().observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(@Nullable List<Message> messages) {
                try {
                    updateMessages(messages);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
            }
        });

        eventViewModel = ViewModelProviders.of(this).get(EventViewModel.class);
        eventViewModel.getDaemonServerOfflineEvent().observe(this, new Observer<Event>() {
            @Override
            public void onChanged(@Nullable Event event) {
                try {
                    if (event != null) {
                        evalueServerStatus();
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            }
        });
        eventViewModel.getDaemonServerOnlineEvent().observe(this, new Observer<Event>() {
            @Override
            public void onChanged(@Nullable Event event) {
                try {
                    if (event != null) {
                        evalueServerStatus();
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
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

            if (!Application.getIpfs().isRunning()) {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.daemon_server_not_running), Toast.LENGTH_LONG).show();
            } else {
                String text = console_box.getText().toString();

                console_box.setText("");

                String[] parts = text.split("\\s+");
                if (parts.length > 0) {

                    if (parts[0].equalsIgnoreCase("ipfs")) {
                        String[] commands = Arrays.copyOfRange(parts, 1, parts.length);
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                IPFS ipfs = Application.getIpfs();
                                Application.getCmdListener().info(text);
                                ipfs.cmd(commands);

                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            }
                        });
                    } else {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                IPFS ipfs = Application.getIpfs();
                                Application.getCmdListener().info(text);
                                ipfs.cmd(parts);

                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            }
                        });
                    }
                }

            }


        });

    }


    private void removeKeyboards() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(console_box.getWindowToken(), 0);
        }
    }


    private void evalueServerStatus() {

        try {
            IPFS daemon = Application.getIpfs();
            if (!daemon.isRunning()) {
                server.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));
            } else {
                server.setImageDrawable(getDrawable(R.drawable.stop));
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    private void updateMessages(List<Message> messages) {
        try {
            messageViewAdapter.updateData(messages);

            mRecyclerView.scrollToPosition(messageViewAdapter.getItemCount());
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


        MenuItem action_settings = menu.findItem(R.id.action_webui);
        drawable = action_settings.getIcon();
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
            case R.id.action_webui: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                if (!Application.getIpfs().isRunning()) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.daemon_server_not_running), Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("http://127.0.0.1:5001/webui"));
                    startActivity(intent);
                }
                return true;
            }
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
                    Toast.makeText(getApplicationContext(), getString(R.string.daemon_server_not_running), Toast.LENGTH_LONG).show();
                } else {
                    ServerInfoDialog.show(this, pid);
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
        int id = menuItem.getItemId();
        if (id == R.id.nav_donation) {
            Intent menuIntent = new Intent(MainActivity.this,
                    DonationActivity.class);
            startActivity(menuIntent);
        }

        drawer_layout.closeDrawer(GravityCompat.START);
        return true;
    }

}