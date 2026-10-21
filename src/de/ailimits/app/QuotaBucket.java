package de.ailimits.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class QuotaBucket {
    public String bucketId;
    public String displayName;
    public String description;
    public String window;
    public float remainingFraction;
    public String rawResetTime;
    public long resetTimeMillis;
    public String groupName;

    public QuotaBucket() {}

    public static long parseIsoTime(String isoString) {
        if (isoString == null || isoString.isEmpty()) return 0L;
        try {
            String clean = isoString.trim();
            if (clean.contains("+")) {
                clean = clean.substring(0, clean.indexOf("+"));
            } else if (clean.endsWith("Z")) {
                clean = clean.substring(0, clean.length() - 1);
            }

            if (clean.contains(".")) {
                int dotIdx = clean.indexOf(".");
                String mainPart = clean.substring(0, dotIdx);
                String nanoPart = clean.substring(dotIdx + 1);
                if (nanoPart.length() > 3) {
                    nanoPart = nanoPart.substring(0, 3);
                }
                clean = mainPart + "." + nanoPart;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(clean);
                return date != null ? date.getTime() : 0L;
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(clean);
                return date != null ? date.getTime() : 0L;
            }
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getTotalWindowMillis() {
        if (window != null) {
            String w = window.toLowerCase(Locale.ROOT);
            if (w.contains("5h") || w.contains("5")) return 5L * 3600L * 1000L;
            if (w.contains("week") || w.contains("7d") || w.contains("woche")) return 7L * 86400L * 1000L;
            if (w.contains("daily") || w.contains("day") || w.contains("tag")) return 24L * 3600L * 1000L;
        }
        if (bucketId != null) {
            String b = bucketId.toLowerCase(Locale.ROOT);
            if (b.contains("7d") || b.contains("week") || b.contains("flash")) return 7L * 86400L * 1000L;
            if (b.contains("daily") || b.contains("tag")) return 24L * 3600L * 1000L;
        }
        return 5L * 3600L * 1000L;
    }

    public float getRecoveryFraction() {
        if (resetTimeMillis <= 0) return 1.0f;
        long now = System.currentTimeMillis();
        long diff = resetTimeMillis - now;
        if (diff <= 0 || remainingFraction >= 0.999f) return 1.0f;

        long total = getTotalWindowMillis();
        float elapsedRatio = 1.0f - ((float) diff / (float) total);
        return Math.max(0.0f, Math.min(1.0f, elapsedRatio));
    }

    public String getWindowBadgeLetter() {
        if (window != null) {
            String w = window.toLowerCase(Locale.ROOT);
            if (w.contains("5h") || w.contains("5")) return "5";
            if (w.contains("week") || w.contains("7d") || w.contains("woche")) return "W";
            if (w.contains("daily") || w.contains("day") || w.contains("tag")) return "T";
        }
        if (bucketId != null) {
            String b = bucketId.toLowerCase(Locale.ROOT);
            if (b.contains("7d") || b.contains("week") || b.contains("flash")) return "W";
            if (b.contains("daily")) return "T";
        }
        if (displayName != null) {
            String d = displayName.toLowerCase(Locale.ROOT);
            if (d.contains("woche") || d.contains("7 tage")) return "W";
            if (d.contains("tage") || d.contains("tages")) return "T";
        }
        return "5";
    }

    public String getProviderType() {
        if (bucketId != null && bucketId.startsWith("claude_pro_")) {
            return "CLAUDE";
        }
        if (groupName != null && groupName.toLowerCase(Locale.ROOT).contains("claude pro")) {
            return "CLAUDE";
        }
        if (bucketId != null && (bucketId.startsWith("google_gemini_") || bucketId.contains("cloud_pro"))) {
            return "GOOGLE_CLOUD";
        }
        if (groupName != null && groupName.toLowerCase(Locale.ROOT).contains("cloud direkt")) {
            return "GOOGLE_CLOUD";
        }
        if (bucketId != null && (bucketId.startsWith("3p-") || bucketId.startsWith("gemini-") || bucketId.contains("argon"))) {
            return "GOOGLE_IDE";
        }
        if (groupName != null && groupName.toLowerCase(Locale.ROOT).contains("ide")) {
            return "GOOGLE_IDE";
        }
        return "GOOGLE_CLOUD";
    }

    public String getModelGroupExplanation() {
        return AppI18n.getModelGroupExplanation(bucketId, displayName);
    }

    public String getFormattedResetDateTime() {
        return AppI18n.formatResetDateTime(resetTimeMillis);
    }

    public String getDetailedModelList() {
        return AppI18n.getDetailedModelList(bucketId, displayName);
    }

    public String getTokenWeightingExplanation() {
        return AppI18n.getTokenWeightingExplanation();
    }

    public String formatCountdown() {
        if (resetTimeMillis <= 0) return "";
        long now = System.currentTimeMillis();
        long diff = resetTimeMillis - now;
        return AppI18n.getCountdownText(diff);
    }
}
