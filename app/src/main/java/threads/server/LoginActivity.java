package threads.server;

import android.content.ClipData;
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
    public void onCreate(Bundle savedInstanceState) {
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

        onLoad();
    }


    private void handleSendText(Intent intent) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            try {
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text != null) {
                    Service.getInstance(this).storeData(getApplicationContext(), text);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    private void handleSend(Intent intent, boolean multi) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (multi) {
                    ClipData mClipData = intent.getClipData();
                    if (mClipData != null) {
                        for (int i = 0; i < mClipData.getItemCount(); i++) {
                            ClipData.Item item = mClipData.getItemAt(i);
                            Service.getInstance(this).storeData(getApplicationContext(), item.getUri());
                        }
                    } else if (uri != null) {
                        Service.getInstance(this).storeData(getApplicationContext(), uri);
                    }

                } else if (uri != null) {
                    Service.getInstance(this).storeData(getApplicationContext(), uri);
                }


            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    private void onLoad() {

        Intent intent = getIntent();
        final String action = intent.getAction();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Service.getInstance(getApplicationContext());

                // jobs
                JobServiceLoadNotifications.notifications(getApplicationContext());
                JobServiceDownloader.downloader(getApplicationContext());
                JobServicePublisher.publish(getApplicationContext());
                JobServicePeers.peers(getApplicationContext());
                JobServiceFindPeers.findPeers(getApplicationContext());
                JobServiceAutoConnect.autoConnect(getApplicationContext());
                JobServiceCleanup.cleanup(getApplicationContext());
                ContentsService.contents(getApplicationContext());


                if (Intent.ACTION_SEND.equals(action)) {
                    String type = intent.getType();
                    if ("text/plain".equals(type)) {
                        handleSendText(intent);
                    } else {
                        handleSend(intent, false);
                    }
                } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                        handleSend(intent, true);
                    } else {
                        String type = intent.getType();
                        if ("text/plain".equals(type)) {
                            handleSendText(intent);
                        } else {
                            handleSend(intent, true);
                        }
                    }
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage());
            }

            Intent main = new Intent(getApplicationContext(), MainActivity.class);
            main.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(main);
            finish();

        });
    }

}

