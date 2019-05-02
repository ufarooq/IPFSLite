package threads.server;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Preferences;

public class LoginActivity extends AppCompatActivity {


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
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            Service.getInstance(getApplicationContext());

            runOnUiThread(() -> progress_bar.setVisibility(View.GONE));
            runOnUiThread(this::login);
        });

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

