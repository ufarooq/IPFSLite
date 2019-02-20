package threads.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import threads.ipfs.api.Link;

import static com.google.common.base.Preconditions.checkNotNull;

class NotificationSender {
    static final AtomicInteger NOTIFICATIONS_COUNTER = new AtomicInteger(1000);
    private static final String CHANNEL_ID = "CHANNEL_ID";
    private static final String GROUP_ID = "GROUP_ID";
    private static final String TAG = NotificationSender.class.getSimpleName();


    public static void createChannel(@NonNull Context context) {
        checkNotNull(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                CharSequence name = context.getString(R.string.channel_name);
                String description = context.getString(R.string.channel_description);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                mChannel.setDescription(description);

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(mChannel);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
    }

    static void showLinkNotification(@NonNull Context context,
                                     @NonNull Link link) {
        checkNotNull(context);
        checkNotNull(link);
        try {
            buildGroupNotification(context);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            Notification notification = createLinkNotification(context, link);
            notificationManager.notify(NOTIFICATIONS_COUNTER.incrementAndGet(), notification);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    private static void buildGroupNotification(@NonNull Context context) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                NotificationSender.CHANNEL_ID);

        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setSmallIcon(R.drawable.server_network);
        builder.setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE);
        builder.setGroup(NotificationSender.GROUP_ID);
        builder.setAutoCancel(true);
        builder.setGroupSummary(true);
        builder.setPriority(NotificationManager.IMPORTANCE_DEFAULT);

        Intent defaultIntent = new Intent(context, MainActivity.class);
        int requestID = (int) System.currentTimeMillis();
        defaultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent defaultPendingIntent = PendingIntent.getActivity(
                context, requestID, defaultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(defaultPendingIntent);


        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(CHANNEL_ID.hashCode(), notification);
        }

    }

    static NotificationCompat.Builder createDownloadProgressNotification(@NonNull Context context,
                                                                         @NonNull String content) {
        checkNotNull(context);
        checkNotNull(content);


        Intent notifyIntent = new Intent(context, MainActivity.class);

        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int requestID = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestID,
                notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setContentIntent(pendingIntent);
        builder.setGroup(GROUP_ID);
        builder.setProgress(0, 0, true);
        builder.setAutoCancel(false);
        builder.setContentText(content);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setSmallIcon(R.drawable.server_network);
        return builder;
    }


    private static Notification createLinkNotification(@NonNull Context context,
                                                       @NonNull Link link) {
        checkNotNull(context);
        checkNotNull(link);

        return createNotification(context, link.getPath(), NotificationCompat.PRIORITY_MAX);
    }


    private static Notification createNotification(@NonNull Context context,
                                                   @NonNull String message,
                                                   int priority) {
        checkNotNull(context);
        checkNotNull(message);

        Intent notifyIntent = new Intent(context, MainActivity.class);

        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int requestID = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestID,
                notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentIntent(pendingIntent)
                        .setGroup(GROUP_ID)
                        .setAutoCancel(true)
                        .setPriority(priority)
                        .setContentText(message)
                        .setSmallIcon(R.drawable.server_network);
        return notificationBuilder.build();
    }


}
