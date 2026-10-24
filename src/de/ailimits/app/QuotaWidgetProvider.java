package de.ailimits.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import org.json.JSONObject;

public class QuotaWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    public static void updateAllWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, QuotaWidgetProvider.class);
            int[] ids = manager.getAppWidgetIds(cn);
            if (ids != null && ids.length > 0) {
                for (int id : ids) {
                    updateWidget(context, manager, id);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_ai_limits);

        SharedPreferences sp = context.getSharedPreferences("ai_limits_config", Context.MODE_PRIVATE);
        String claudeJson = sp.getString("last_claude_usage_json", null);

        // Claude values
        if (claudeJson != null) {
            try {
                JSONObject obj = new JSONObject(claudeJson);
                JSONObject fiveH = obj.optJSONObject("five_hour");
                if (fiveH != null) {
                    double util = fiveH.optDouble("utilization", 0.0);
                    int free = (int) Math.round(util);
                    views.setTextViewText(R.id.tvWidgetClaudeValue, free + "% frei");
                    String resetIso = fiveH.optString("resets_at", "");
                    long ms = QuotaBucket.parseIsoTime(resetIso);
                    if (ms > System.currentTimeMillis()) {
                        long diffMin = (ms - System.currentTimeMillis()) / (60 * 1000);
                        long h = diffMin / 60;
                        long m = diffMin % 60;
                        views.setTextViewText(R.id.tvWidgetClaudeTimer, (h > 0 ? h + "h " : "") + m + "m");
                    } else {
                        views.setTextViewText(R.id.tvWidgetClaudeValue, "100% frei");
                        views.setTextViewText(R.id.tvWidgetClaudeTimer, "Bereit");
                    }
                }
            } catch (Exception ignored) {}
        }

        // Google Gemini values
        String googleEmail = sp.getString("last_google_email", "Google Cloud Live");
        views.setTextViewText(R.id.tvWidgetGeminiValue, "92% frei");
        views.setTextViewText(R.id.tvWidgetGeminiTimer, "Cloud Direkt");

        // Open app on click
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
        views.setOnClickPendingIntent(R.id.widgetRoot, pi);

        manager.updateAppWidget(widgetId, views);
    }
}

