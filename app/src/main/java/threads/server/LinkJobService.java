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
import threads.iri.IThreadsServer;
import threads.iri.server.Server;
import threads.iri.server.ServerVisibility;
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

                    if (!Server.isReachable(Application.getDefaultServer(getApplicationContext()))) {
                        return;
                    }

                    IThreadsAPI travelTangleAPI = Application.getThreadsAPI();
                    ITangleServer tangleServer =
                            Application.getTangleServer(getApplicationContext());

                    Server serverConfig = null;
                    ILink link = travelTangleAPI.getLinkByAccountAddress(accountAddress);
                    String token = "";
                    IThreadsServer tangleDaemon = Application.getThreadsServer();
                    if (tangleDaemon.isRunning()) {
                        Pair<Server, ServerVisibility> pair = IThreadsServer.getServer(
                                getApplicationContext(), tangleDaemon);

                        if (pair.second != ServerVisibility.OFFLINE) {
                            serverConfig = pair.first;
                        }
                    } else {
                        if (tangleServer.getServerInfo().isSupportDataStorage()) {
                            serverConfig = Application.getDefaultServer(getApplicationContext());
                        }
                    }

                    if (link == null) {
                        String address = travelTangleAPI.generateSeed();
                        String nextLink = travelTangleAPI.generateSeed();

                        String protocol = "";
                        String host = "";
                        String port = "";
                        String cert = "";
                        String alias = "";
                        if (serverConfig != null) {
                            protocol = serverConfig.getProtocol();
                            host = serverConfig.getHost();
                            port = serverConfig.getPort();
                            cert = serverConfig.getCert();
                            alias = serverConfig.getAlias();
                        }

                        link = travelTangleAPI.createLink(
                                accountAddress, address, nextLink, token,
                                protocol, host, port, cert, alias);

                        travelTangleAPI.storeLink(link);
                        travelTangleAPI.insertLink(tangleServer, link, Application.getAesKey());
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
                                    serverConfig.getCert(),
                                    serverConfig.getAlias());
                            travelTangleAPI.updateLink(link);
                            travelTangleAPI.insertLink(tangleServer, link, Application.getAesKey());
                        } else {
                            travelTangleAPI.verifyLink(tangleServer, link, Application.getAesKey());
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
