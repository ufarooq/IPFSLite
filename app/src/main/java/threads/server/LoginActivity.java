package threads.server;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login);


        ProgressBar progress_bar = findViewById(R.id.progress_bar);

        progress_bar.setVisibility(View.VISIBLE);

        TextView app_version = findViewById(R.id.app_version);
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            app_version.setText(getString(R.string.version_number, version));
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            Service.getInstance(getApplicationContext());
            NotificationService.notifications(getApplicationContext());
            NotificationService.periodic(getApplicationContext());
            CleanupService.cleanup(getApplicationContext());

            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if ("text/plain".equals(type)) {
                    handleSendText(intent);
                } else {
                    handleSend(intent);
                }
            }
            runOnUiThread(() -> progress_bar.setVisibility(View.GONE));
            runOnUiThread(this::login);
        });


    }

    void handleSendText(Intent intent) {
        try {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                Service.getInstance(this).storeData(getApplicationContext(), text);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    void handleSend(Intent intent) {
        try {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                Service.getInstance(this).storeData(getApplicationContext(), uri);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private void login() {
        Intent intent = new Intent(getApplicationContext(),
                MainActivity.class);
        startActivity(intent);
        finish();

    }


    @Override
    public void onResume() {
        super.onResume();

    }
}

