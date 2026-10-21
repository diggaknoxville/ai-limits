package de.ailimits.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.Notification;

public class ResetReceiver extends BroadcastReceiver {

    public static final String ACTION_QUOTA_RESET = "de.ailimits.app.ACTION_QUOTA_RESET";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MESSAGE = "extra_message";
    public static final String EXTRA_BUCKET_ID = "extra_bucket_id";
    public static final String CHANNEL_ID = "ai_limits_notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);

        if (title == null || title.isEmpty()) {
            title = "AI Limit wieder verfügbar!";
        }
        if (message == null || message.isEmpty()) {
            message = "Dein AI-Limit (Claude / Gemini / GPT) wurde zurückgesetzt und steht wieder bereit.";
        }

        showNotification(context, title, message);
    }

    public static void showNotification(Context context, String title, String message) {
        if (MessageCenterManager.isCurrentlyQuietHours(context)) {
            // Log event without vibrating/waking
            MessageCenterManager.logEvent(context, title + " [Ruhezeit]", message);
            return;
        }

        MessageCenterManager.logEvent(context, title, message);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AI Limits Benachrichtigungen",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Erinnerungen bei Kontingent-Reset von KI-Modellen");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 150, 250});
            nm.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        builder.setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setVibrate(new long[]{0, 250, 150, 250});

        int notifId = (int) (System.currentTimeMillis() % 100000);
        nm.notify(notifId, builder.build());
    }
}

