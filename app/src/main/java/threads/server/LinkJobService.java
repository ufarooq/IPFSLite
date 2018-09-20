package threads.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.util.Log;

import threads.core.IThreadsAPI;
import threads.core.api.ILink;
import threads.iri.ITangleDaemon;
import threads.iri.daemon.ServerVisibility;
import threads.iri.event.EventsDatabase;
import threads.iri.server.ServerConfig;
import threads.iri.tangle.ITangleServer;
import threads.iri.tangle.Pair;

import static com.google.android.gms.common.internal.Preconditions.checkNotNull;

public class LinkJobService extends JobService {
    private static final String TAG = "LinkJobService";

    public static void checkLink(@NonNull Context context, @NonNull String accountAddress) {
        checkNotNull(context);
        checkNotNull(accountAddress);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);

        ComponentName componentName = new ComponentName(context, LinkJobService.class);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("ACCOUNT", accountAddress);


        JobInfo jobInfo = new JobInfo.Builder(22, componentName)
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

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        PersistableBundle bundle = jobParameters.getExtras();
        final String accountAddress = bundle.getString("ACCOUNT");
        checkNotNull(accountAddress);
        new Thread(new Runnable() {
            public void run() {
                long start = System.currentTimeMillis();

                try {
                    Log.e(TAG, " still running ...");

                    EventsDatabase eventsDatabase = Application.getEventsDatabase();

                    if (!ITangleDaemon.isReachable(Application.getServerConfig(getApplicationContext()))) {
                        return;
                    }

                    IThreadsAPI travelTangleAPI = Application.getThreadsAPI();
                    ITangleServer tangleServer =
                            Application.getTangleServer(getApplicationContext());

                    ServerConfig serverConfig = null;
                    ILink link = travelTangleAPI.getLinkByAccountAddress(accountAddress);
                    String token = "";
                    ITangleDaemon tangleDaemon = Application.getTangleDaemon();
                    if (tangleDaemon.isDaemonRunning()) {
                        Pair<ServerConfig, ServerVisibility> pair = ITangleDaemon.getDaemonConfig(
                                getApplicationContext(), tangleDaemon);

                        if (pair.second != ServerVisibility.OFFLINE) {
                            serverConfig = pair.first;
                        }
                    } else {
                        if (tangleServer.getServerInfo().isSupportDataStorage()) {
                            serverConfig = Application.getServerConfig(getApplicationContext());
                        }
                    }

                    if (link == null) {
                        String address = travelTangleAPI.generateSeed();
                        String nextLink = travelTangleAPI.generateSeed();


                        link = travelTangleAPI.createLink(
                                accountAddress, address, nextLink, token,
                                serverConfig.getProtocol(),
                                serverConfig.getHost(),
                                serverConfig.getPort(),
                                serverConfig.getCert());
                        travelTangleAPI.storeLink(link);
                        travelTangleAPI.insertLink(tangleServer, link, new AesKey());
                    } else {
                        boolean updateRequired = false;

                        if (!link.getToken().equals(token)) {
                            updateRequired = true;
                        }
                        if (serverConfig != null) {
                            updateRequired = !serverConfig.getCert().equals(link.getCert()) ||
                                    !serverConfig.getPort().equals(link.getPort()) ||
                                    !serverConfig.getHost().equals(link.getHost()) ||
                                    !serverConfig.getProtocol().equals(link.getProtocol());
                        }

                        if (updateRequired) {
                            String nextLink = travelTangleAPI.generateSeed();
                            // should overwrite the old entry
                            link = travelTangleAPI.createLink(accountAddress,
                                    link.getLink(), nextLink, token,
                                    serverConfig.getProtocol(),
                                    serverConfig.getHost(),
                                    serverConfig.getPort(),
                                    serverConfig.getCert());
                            travelTangleAPI.updateLink(link);
                            travelTangleAPI.insertLink(tangleServer, link, new AesKey());
                        } else {
                            travelTangleAPI.verifyLink(tangleServer, link, new AesKey());
                        }
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                } finally {
                    Log.e(TAG, " finish running [" + (System.currentTimeMillis() - start) + "]...");
                    jobFinished(jobParameters, false);
                }
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
