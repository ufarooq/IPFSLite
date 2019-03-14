package threads.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import threads.core.Preferences;

import static com.google.common.base.Preconditions.checkNotNull;

class NotificationSender {
    private static final String CHANNEL_ID = "CHANNEL_ID";
    private static final String GROUP_ID = "GROUP_ID";
    private static final int GROUP_IDX = 999;

    static void createChannel(@NonNull Context context) {
        checkNotNull(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                CharSequence name = context.getString(R.string.channel_name);
                String description = context.getString(R.string.channel_description);
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name,
                        NotificationManager.IMPORTANCE_LOW);
                mChannel.setDescription(description);

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(mChannel);
                }

            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        }
    }

    static void showNotification(@NonNull Context context, @NonNull String message, int code) {
        checkNotNull(context);
        checkNotNull(message);
        try {
            buildGroupNotification(context);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            Notification notification = createNotification(context, message);
            notificationManager.notify(code, notification);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }


    private static void buildGroupNotification(@NonNull Context context) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_ID);

        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setSmallIcon(R.drawable.server_network);
        builder.setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE);
        builder.setGroup(GROUP_ID);
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
            notificationManager.notify(GROUP_IDX, notification);
        }

    }

    static NotificationCompat.Builder createDownloadProgressNotification(@NonNull Context context,
                                                                         @NonNull String content) {
        checkNotNull(context);
        checkNotNull(content);

        buildGroupNotification(context);


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
        builder.setAutoCancel(true);
        builder.setContentText(content);
        builder.setPriority(NotificationManager.IMPORTANCE_DEFAULT);
        builder.setSmallIcon(R.drawable.server_network);
        return builder;
    }


    private static Notification createNotification(@NonNull Context context,
                                                   @NonNull String message) {
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
                        .setContentText(message)
                        .setPriority(NotificationManager.IMPORTANCE_MAX)
                        .setSmallIcon(R.drawable.server_network);
        return notificationBuilder.build();
    }


}
