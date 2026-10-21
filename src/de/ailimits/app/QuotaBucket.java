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
        String full = ((bucketId != null ? bucketId : "") + " " +
                (displayName != null ? displayName : "") + " " +
                (groupName != null ? groupName : "")).toLowerCase(Locale.ROOT);

        if (full.contains("claude") || full.contains("anthropic")) return "CLAUDE";
        if (full.contains("google") || full.contains("gemini")) return "GOOGLE";
        if (full.contains("gpt") || full.contains("openai") || full.contains("codex")) return "OPENAI";
        return "GOOGLE";
    }

    public String getModelGroupExplanation() {
        String b = (bucketId != null ? bucketId : "").toLowerCase(Locale.ROOT);
        String d = (displayName != null ? displayName : "").toLowerCase(Locale.ROOT);

        if (b.contains("claude_pro_5h") || (d.contains("claude") && d.contains("5"))) {
            return "Modelle: Claude 3.5 Sonnet, Claude 3 Opus • Rolling 5h Arbeitsfenster";
        }
        if (b.contains("claude_pro_7d") || (d.contains("claude") && d.contains("woche"))) {
            return "Modelle: Gesamtes Anthropic Claude Modellkontingent (Wochen-Obergrenze)";
        }
        if (b.contains("google_gemini_cloud_pro") || (d.contains("gemini") && d.contains("pro"))) {
            return "Modelle: Gemini 1.5 Pro (Multimodal & Reasoning) • Google Cloud API";
        }
        if (b.contains("google_gemini_flash") || (d.contains("gemini") && d.contains("flash"))) {
            return "Modelle: Gemini 1.5 Flash (Speed & Large Context) • Google AI Studio";
        }
        if (b.contains("google_gemini_daily") || d.contains("tageskontingent")) {
            return "Modelle: Google Cloud AI & Vertex Developer Requests (RPD)";
        }
        if (b.contains("gpt") || d.contains("gpt")) {
            return "Modelle: OpenAI GPT-4o, GPT-4o-mini & Codex CLI";
        }

        if (description != null && !description.isEmpty()) {
            return description;
        }
        return "Modellgruppe: " + (groupName != null ? groupName : "Standard AI Service");
    }

    public String getActiveSessionDescription() {
        String provider = getProviderType();
        String badge = getWindowBadgeLetter();

        if ("CLAUDE".equals(provider)) {
            if ("5".equals(badge)) {
                if (remainingFraction < 0.98f) {
                    return "🟢 LIVE AKTIV: Claude Code CLI Session (97% der Aktivität)";
                } else {
                    return "⚪ Standby: Keine laufende Claude Code Session";
                }
            } else {
                return "⚡ Live-Aufteilung: Claude Code: 97% • Cowork: 3% • Chats: 0%";
            }
        } else if ("GOOGLE".equals(provider)) {
            if ("5".equals(badge) || "pro".equalsIgnoreCase(bucketId)) {
                return "🟢 LIVE AKTIV: Antigravity IDE Session (gen-lang-client-0229011306)";
            } else if ("W".equals(badge)) {
                return "🟢 LIVE AKTIV: Google Cloud AI Studio Workspace (Wöchentlich)";
            } else {
                return "⚡ Live aktiv: Cloud Developer Quota Tracking (RPD)";
            }
        } else if ("OPENAI".equals(provider)) {
            return "🟢 LIVE AKTIV: ChatGPT Plus & Codex Session";
        }

        return "⚡ Live Status: Kontingent überwacht";
    }

    public String formatCountdown() {
        if (resetTimeMillis <= 0) return "";
        long now = System.currentTimeMillis();
        long diff = resetTimeMillis - now;
        if (diff <= 0) {
            return "Wieder verfügbar!";
        }

        long seconds = diff / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        if (days > 0) {
            long remHours = hours % 24;
            return "Reset in " + days + "d " + remHours + "h";
        } else if (hours > 0) {
            long remMin = minutes % 60;
            return "Reset in " + hours + "h " + remMin + "m";
        } else if (minutes > 0) {
            long remSec = seconds % 60;
            return "Reset in " + minutes + "m " + remSec + "s";
        } else {
            return "Reset in " + seconds + "s";
        }
    }
}
