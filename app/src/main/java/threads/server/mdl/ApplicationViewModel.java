package threads.server.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import threads.server.ContentsService;
import threads.server.jobs.JobServiceAutoConnect;
import threads.server.jobs.JobServiceCleanup;
import threads.server.jobs.JobServiceDownloader;
import threads.server.jobs.JobServiceFindPeers;
import threads.server.jobs.JobServiceLoadNotifications;
import threads.server.jobs.JobServicePeers;
import threads.server.jobs.JobServicePublisher;

public class ApplicationViewModel extends AndroidViewModel {
    public ApplicationViewModel(@NonNull Application application) {
        super(application);


        JobServiceLoadNotifications.notifications(application.getApplicationContext());
        JobServiceDownloader.downloader(application.getApplicationContext());
        JobServicePublisher.publish(application.getApplicationContext());
        JobServicePeers.peers(application.getApplicationContext());
        JobServiceFindPeers.findPeers(application.getApplicationContext());
        JobServiceAutoConnect.autoConnect(application.getApplicationContext());
        JobServiceCleanup.cleanup(application.getApplicationContext());
        ContentsService.contents(application.getApplicationContext());
    }
}
