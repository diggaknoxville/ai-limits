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
            // Handle +00:00 or -00:00 or Z
            if (clean.contains("+")) {
                clean = clean.substring(0, clean.indexOf("+"));
            } else if (clean.endsWith("Z")) {
                clean = clean.substring(0, clean.length() - 1);
            }

            // Truncate fractional seconds to 3 digits (milliseconds)
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
