package threads.server;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import threads.core.api.ILink;
import threads.iri.ITangleDaemon;
import threads.iri.daemon.ServerVisibility;
import threads.iri.dialog.ServerInfoDialog;
import threads.iri.dialog.ServerSettingsDialog;
import threads.iri.event.Event;
import threads.iri.server.ServerConfig;
import threads.iri.tangle.Pair;
import threads.iri.task.LoadDaemonConfigTask;
import threads.iri.task.LoadResponse;

import static com.google.common.base.Preconditions.checkNotNull;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private DrawerLayout drawer_layout;
    private NavigationView navigationView;
    private FloatingActionButton fab;
    private FloatingActionButton traffic_light;
    private RecyclerView mRecyclerView;
    private MessageViewAdapter messageViewAdapter;
    private long mLastClickTime = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.reyclerview_message_list);
        LinearLayoutManager linearLayout = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayout);
        messageViewAdapter = new MessageViewAdapter(this);
        mRecyclerView.setAdapter(messageViewAdapter);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        drawer_layout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer_layout.addDrawerListener(toggle);
        toggle.syncState();


        traffic_light = findViewById(R.id.trafic_light);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                ITangleDaemon daemon = Application.getTangleDaemon();
                if (!daemon.isDaemonRunning()) {

                    Intent intent = new Intent(MainActivity.this, DaemonService.class);
                    intent.setAction(DaemonService.ACTION_START_DAEMON_SERVICE);
                    startService(intent);

                    fab.setImageDrawable(getDrawable(R.drawable.stop));
                } else {
                    Intent intent = new Intent(MainActivity.this, DaemonService.class);
                    intent.setAction(DaemonService.ACTION_STOP_DAEMON_SERVICE);
                    startService(intent);

                    fab.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));
                }
            }
        });
        ITangleDaemon daemon = Application.getTangleDaemon();
        if (!daemon.isDaemonRunning()) {
            fab.setImageDrawable(getDrawable(android.R.drawable.ic_media_play));
        } else {
            fab.setImageDrawable(getDrawable(R.drawable.stop));
        }


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

        EventViewModel eventViewModel = ViewModelProviders.of(this).get(EventViewModel.class);
        eventViewModel.getDaemonServerOfflineEvent().observe(this, new Observer<Event>() {
            @Override
            public void onChanged(@Nullable Event event) {
                try {
                    if (event != null) {
                        evalueServerVisibilty();
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
                        evalueServerVisibilty();
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            }
        });


    }

    private void evalueServerVisibilty() {

        ITangleDaemon tangleDaemon = Application.getTangleDaemon();
        LoadDaemonConfigTask task = new LoadDaemonConfigTask(getApplicationContext(),
                new LoadResponse<Pair<ServerConfig, ServerVisibility>>() {
                    @Override
                    public void loaded(@NonNull Pair<ServerConfig, ServerVisibility> pair) {
                        long start = System.currentTimeMillis();
                        try {
                            if (pair != null) {
                                ServerVisibility serverVisibility = pair.second;
                                switch (serverVisibility) {
                                    case GLOBAL:
                                        traffic_light.setImageDrawable(getDrawable(R.drawable.traffic_light_green));
                                        break;
                                    case LOCAL:
                                        traffic_light.setImageDrawable(getDrawable(R.drawable.traffic_light_orange));
                                        break;
                                    case OFFLINE:
                                        traffic_light.setImageDrawable(getDrawable(R.drawable.traffic_light_red));
                                        break;
                                    default:
                                        traffic_light.setImageDrawable(getDrawable(R.drawable.traffic_light_red));
                                        break;
                                }
                            } else {
                                traffic_light.setImageDrawable(getDrawable(R.drawable.traffic_light_red));
                            }
                        } catch (Throwable e) {
                            Log.e(TAG, e.getLocalizedMessage());
                        } finally {
                            Log.e(TAG, " finish running [" + (System.currentTimeMillis() - start) + "]...");
                        }
                    }
                });
        task.execute(tangleDaemon);
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


        MenuItem action_settings = menu.findItem(R.id.action_settings);
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
            case R.id.action_settings: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();


                ServerSettingsDialog.show(this);

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

                String accountAddress = Application.getAccountAddress(getApplicationContext());
                checkNotNull(accountAddress);
                LoadLinkTask task = new LoadLinkTask(new LoadResponse<ILink>() {
                    @Override
                    public void loaded(ILink link) {
                        try {
                            ServerInfoDialog.show(MainActivity.this,
                                    link.getAddress(), new AesKey().getAesKey());
                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }

                    }
                });
                task.execute(accountAddress);
                return true;
            }


        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.nav_donation) {
            /*
           Intent menuIntent = new Intent(MainActivity.this,
                    DonationActivity.class);
            startActivity(menuIntent);*/
        }

        drawer_layout.closeDrawer(GravityCompat.START);
        return true;
    }

    private final class LogHandler extends Handler {
        private final Map<Level, Integer> LEVEL_TO_LOG = new HashMap<Level, Integer>(7);

        public LogHandler() {

            LEVEL_TO_LOG.put(Level.FINEST, Log.VERBOSE);
            LEVEL_TO_LOG.put(Level.FINER, Log.VERBOSE);
            LEVEL_TO_LOG.put(Level.FINE, Log.VERBOSE);
            LEVEL_TO_LOG.put(Level.CONFIG, Log.DEBUG);
            LEVEL_TO_LOG.put(Level.INFO, Log.INFO);
            LEVEL_TO_LOG.put(Level.WARNING, Log.WARN);
            LEVEL_TO_LOG.put(Level.SEVERE, Log.ERROR);
        }


        @Override
        @SuppressWarnings({"LogTagMismatch", "WrongConstant"})
        public void publish(final LogRecord logRecord) {

            String message = logRecord.getMessage();

            final Throwable error = logRecord.getThrown();
            if (error != null) {
                message += "\n" + Log.getStackTraceString(error);
            }


            final String finalMessage = message;
            new java.lang.Thread(new Runnable() {
                public void run() {
                    Application.getDaemonDatabase().insertMessage(finalMessage);
                }
            }).start();


        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

    }

}