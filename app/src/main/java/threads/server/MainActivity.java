package threads.server;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import threads.iri.Logs;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final LogHandler LOG_HANDLER = new LogHandler();

    /**
     * Sets up the {@link Logs}, {@link Handler}, and prevents any parent Handlers from being
     * notified to avoid duplicated log messages.
     */
    private DrawerLayout drawer_layout;
    private NavigationView navigationView;
    private TextView console;

    private static void addMessage(String message) {

    }

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
        LOG_HANDLER.setLevel(Level.FINE);
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


        console = findViewById(R.id.console);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        drawer_layout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer_layout.addDrawerListener(toggle);
        toggle.syncState();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity, menu);


        MenuItem action_daemon = menu.findItem(R.id.action_daemon);
        Drawable drawable = action_daemon.getIcon();
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


            case R.id.action_daemon: {
                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                Intent intent = new Intent(MainActivity.this, DaemonService.class);
                intent.setAction(DaemonService.ACTION_START_DAEMON_SERVICE);
                startService(intent);

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
            if (isLoggable(logRecord)) {
                final int priority;
                if (LEVEL_TO_LOG.containsKey(logRecord.getLevel())) {
                    priority = LEVEL_TO_LOG.get(logRecord.getLevel());
                } else {
                    priority = Log.VERBOSE;
                }

                String message = logRecord.getMessage() + "\n";

                final Throwable error = logRecord.getThrown();
                if (error != null) {
                    message += Log.getStackTraceString(error);
                }

                if (console != null) {
                    final String consoleOutput = message;
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            console.append(consoleOutput);
                        }
                    });
                }

            }
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

    }

}