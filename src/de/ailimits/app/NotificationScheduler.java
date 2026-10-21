package de.ailimits.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class NotificationScheduler {

    private static final String PREF_NAME = "ai_limits_alarms";

    public static boolean isAlarmScheduled(Context context, String bucketId) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean("alarm_" + bucketId, false);
    }

    public static void scheduleResetNotification(Context context, QuotaBucket bucket) {
        if (bucket == null || bucket.resetTimeMillis <= System.currentTimeMillis()) {
            return;
        }

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, ResetReceiver.class);
        intent.setAction(ResetReceiver.ACTION_QUOTA_RESET);
        intent.putExtra(ResetReceiver.EXTRA_BUCKET_ID, bucket.bucketId);
        intent.putExtra(ResetReceiver.EXTRA_TITLE, "AI Limit wieder frei: " + bucket.displayName);
        intent.putExtra(ResetReceiver.EXTRA_MESSAGE,
                "Das Kontingent für " + (bucket.groupName != null ? bucket.groupName : "deine AI-Modelle") +
                        " ist vollständig erneuert (100% verfügbar).");

        int requestCode = bucket.bucketId.hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, bucket.resetTimeMillis, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, bucket.resetTimeMillis, pi);
        }

        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean("alarm_" + bucket.bucketId, true).apply();
    }

    public static void cancelResetNotification(Context context, String bucketId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, ResetReceiver.class);
        intent.setAction(ResetReceiver.ACTION_QUOTA_RESET);
        int requestCode = bucketId.hashCode();

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }

        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove("alarm_" + bucketId).apply();
    }
}

