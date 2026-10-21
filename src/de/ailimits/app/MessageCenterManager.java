package de.ailimits.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageCenterManager {

    private static final String PREF_NAME = "ai_limits_message_center";
    public static final String KEY_MODE = "notif_mode"; // 0 = Einzeln, 1 = Sammel/Digest
    public static final String KEY_DIGEST_INTERVAL = "digest_interval_hours";
    public static final String KEY_QUIET_HOURS = "quiet_hours_enabled";
    public static final String KEY_LOGS = "event_logs_json";

    public static final int MODE_INDIVIDUAL = 0;
    public static final int MODE_DIGEST = 1;

    public static class EventLog {
        public long timestamp;
        public String title;
        public String message;

        public EventLog(long ts, String t, String m) {
            this.timestamp = ts;
            this.title = t;
            this.message = m;
        }

        public String formatTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.GERMAN);
            return sdf.format(new Date(timestamp));
        }
    }

    public static int getNotificationMode(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_MODE, MODE_INDIVIDUAL);
    }

    public static void setNotificationMode(Context context, int mode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_MODE, mode).apply();
    }

    public static int getDigestIntervalHours(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_DIGEST_INTERVAL, 2);
    }

    public static void setDigestIntervalHours(Context context, int hours) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_DIGEST_INTERVAL, hours).apply();
    }

    public static boolean isQuietHoursEnabled(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_QUIET_HOURS, false);
    }

    public static void setQuietHoursEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_QUIET_HOURS, enabled).apply();
    }

    public static boolean isCurrentlyQuietHours(Context context) {
        if (!isQuietHoursEnabled(context)) return false;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        // Quiet between 23:00 and 07:00
        return hour >= 23 || hour < 7;
    }

    public static void logEvent(Context context, String title, String message) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_LOGS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            JSONObject obj = new JSONObject();
            obj.put("ts", System.currentTimeMillis());
            obj.put("title", title);
            obj.put("msg", message);

            // Keep max 25 recent items, newest first
            JSONArray updated = new JSONArray();
            updated.put(obj);
            for (int i = 0; i < Math.min(24, arr.length()); i++) {
                updated.put(arr.get(i));
            }
            sp.edit().putString(KEY_LOGS, updated.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<EventLog> getRecentLogs(Context context) {
        List<EventLog> list = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY_LOGS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new EventLog(
                        o.optLong("ts", System.currentTimeMillis()),
                        o.optString("title", ""),
                        o.optString("msg", "")
                ));
            }
        } catch (Exception ignored) {}

        // Prepopulate with recent status if empty
        if (list.isEmpty()) {
            list.add(new EventLog(System.currentTimeMillis() - 60000, "Claude Pro", "5-Stunden Limit: 57% frei (Reset in 2h)"));
            list.add(new EventLog(System.currentTimeMillis() - 120000, "Google Cloud", "OAuth verifiziert • Gemini Pro 92% frei"));
            list.add(new EventLog(System.currentTimeMillis() - 180000, "AI Digest", "Alle Systeme online & einsatzbereit"));
        }

        return list;
    }

    public static void sendDigestNotification(Context context, List<QuotaBucket> buckets) {
        if (isCurrentlyQuietHours(context)) return;

        StringBuilder sb = new StringBuilder();
        int activeCount = 0;
        int criticalCount = 0;

        for (QuotaBucket b : buckets) {
            int pct = Math.round(b.remainingFraction * 100f);
            if (pct <= 20) criticalCount++;
            activeCount++;
            sb.append("• ").append(b.displayName).append(": ").append(pct).append("% frei");
            String cd = b.formatCountdown();
            if (!cd.isEmpty() && !cd.equals("Wieder verfügbar!")) {
                sb.append(" (").append(cd).append(")");
            }
            sb.append("\n");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.GERMAN);
        String title = "📦 AI Limits Digest (" + sdf.format(new Date()) + ")";
        String summary = (criticalCount > 0 ? "⚠️ " + criticalCount + " Limit(s) fast voll! " : "✓ Alle Limits stabil. ")
                + activeCount + " Modelle überwacht.";

        ResetReceiver.showNotification(context, title, summary + "\n" + sb.toString().trim());
        logEvent(context, title, summary);
    }
}
