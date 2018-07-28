package threads.server;

import android.arch.persistence.room.Room;
import android.util.Log;

public class Application extends android.app.Application {

    private static final String TAG = "Application";
    private static final String THREADS_TANGLE_DATABASE = "THREADS_TANGLE_DATABASE";

    private static ThreadsTangleDatabase database;

    public static ThreadsTangleDatabase getThreadsTangleDatabase() {
        return database;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e(TAG, "...... end application");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        database = Room.databaseBuilder(this,
                ThreadsTangleDatabase.class, THREADS_TANGLE_DATABASE).fallbackToDestructiveMigration().build();

        Log.e(TAG, "...... start application");

    }


}
