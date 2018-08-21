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
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import threads.iri.ITangleDaemon;
import threads.iri.Logs;
import threads.iri.ServerConfig;
import threads.iri.daemon.TangleDaemon;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final LogHandler LOG_HANDLER = new LogHandler();

    /**
     * Sets up the {@link Logs}, {@link Handler}, and prevents any parent Handlers from being
     * notified to avoid duplicated log messages.
     */
    private DrawerLayout drawer_layout;
    private NavigationView navigationView;
    private FloatingActionButton fab;
    private RecyclerView mRecyclerView;
    private MessageViewAdapter messageViewAdapter;
    private long mLastClickTime = 0;

    /**
     * Adds a {@link Handler} to a {@link Logs} if they are not already associated.
     */
    private void addHandler(@NonNull final java.util.logging.Logger logger,
                            @NonNull final Handler handler) {
        final Handler[] currentHandlers = logger.getHandlers();
        for (final Handler currentHandler : currentHandlers) {
            if (currentHandler.equals(handler)) {
                return;
            }
        }
        logger.addHandler(handler);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logs.LOGGER.setUseParentHandlers(false);
        Logs.LOGGER.setLevel(Level.ALL);
        LOG_HANDLER.setLevel(Level.ALL);
        /*
         * Mapping between Level.* and Log.*:
         * Level.FINEST  => Log.v
         * Level.FINER   => Log.v
         * Level.FINE    => Log.v
         * Level.CONFIG  => Log.d
         * Level.INFO    => Log.i
         * Level.WARNING => Log.w
         * Level.SEVERE  => Log.e
         */

        LogManager.getLogManager().addLogger(Logs.LOGGER);
        addHandler(Logs.LOGGER, LOG_HANDLER);


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


        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                ITangleDaemon daemon = TangleDaemon.getInstance();
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
        ITangleDaemon daemon = TangleDaemon.getInstance();
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

                DaemonCheckService task = new DaemonCheckService(new FinishResponse() {
                    @Override
                    public void finish(Boolean success) {
                        if (success) {
                            Toast.makeText(MainActivity.this, "Public IP is visible from outside.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "SHIT public IP is not visible.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                task.execute();

                ServerSettingsDialog.show(this);

                return true;
            }
            case R.id.action_clear: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();


                return true;
            }
            case R.id.action_info: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();


                ServerConfig serverConfig = null; // TODO

                ServerInfoDialog.show(this, serverConfig);
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
                    Application.getMessagesDatabase().insertMessage(finalMessage);
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