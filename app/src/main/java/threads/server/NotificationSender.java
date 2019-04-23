package threads.server;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import threads.core.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

class NotificationSender {
    private static final String CHANNEL_ID = "CHANNEL_ID";

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


    static NotificationCompat.Builder createProgressNotification(@NonNull Context context,
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
        builder.setProgress(0, 0, true);
        builder.setAutoCancel(true);
        builder.setContentText(content);
        builder.setPriority(NotificationManager.IMPORTANCE_DEFAULT);
        builder.setSmallIcon(R.drawable.server_network);
        return builder;
    }


    static NotificationCompat.Builder createCallNotification(@NonNull Context context,
                                                             @NonNull String pid,
                                                             @NonNull String name,
                                                             @Nullable String[] ices) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(name);
        Intent intent = RTCCallActivity.createIntent(context, pid, name, ices, false);
        intent.setAction(RTCCallActivity.ACTION_INCOMING_CALL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        int requestID = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestID,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(context.getString(R.string.incoming_call));
        builder.setContentText(context.getString(R.string.is_calling, name));
        builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        builder.setSmallIcon(R.drawable.phone_black);
        return builder;
    }

}
