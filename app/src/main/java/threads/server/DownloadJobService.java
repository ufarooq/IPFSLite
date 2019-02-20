package threads.server;

import android.app.DownloadManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.PersistableBundle;
import android.util.Log;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.ipfs.IPFS;
import threads.ipfs.api.Link;

import static com.google.common.base.Preconditions.checkNotNull;

public class DownloadJobService extends JobService {

    private static final String TAG = DownloadJobService.class.getSimpleName();
    private static final String CID = "CID";

    public static void download(@NonNull Context context, @NonNull String cid) {
        checkNotNull(context);
        checkNotNull(cid);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);

        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, DownloadJobService.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(CID, cid);

            JobInfo jobInfo = new JobInfo.Builder(new Random().nextInt(), componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(bundle)
                    .build();
            int resultCode = jobScheduler.schedule(jobInfo);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Log.e(TAG, "Job scheduled!");
            } else {
                Log.e(TAG, "Job not scheduled");
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        PersistableBundle bundle = jobParameters.getExtras();
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        checkNotNull(downloadManager);
        final String cid = bundle.getString(DownloadJobService.CID);
        checkNotNull(cid);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    threads.ipfs.api.CID ccid = threads.ipfs.api.CID.create(cid);
                    List<Link> links = ipfs.ls(ccid);
                    Link link = links.get(0);
                    String path = link.getPath();

                    Uri uri = Uri.parse(
                            Preferences.getGateway(this) +
                                    cid + "/" + path);

                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                            DownloadManager.Request.NETWORK_MOBILE);
                    request.setAllowedOverRoaming(false);
                    request.setTitle(path);
                    request.setDescription(getString(R.string.content_downloaded, path));
                    request.setVisibleInDownloadsUi(true);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, path);


                    downloadManager.enqueue(request);


                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}