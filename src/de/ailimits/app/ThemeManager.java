package de.ailimits.app;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {

    public static final String THEME_CYBERPUNK = "cyberpunk";
    public static final String THEME_OLED = "oled";
    public static final String THEME_OCEAN = "ocean";
    public static final String THEME_EMERALD = "emerald";

    public static final String PREF_THEME = "app_theme_key";

    public static class ThemeColors {
        public final int bg;
        public final int cardBg;
        public final int cardBorder;
        public final int cardInner;
        public final int accentPrimary;
        public final int accentSecondary;
        public final int textPrimary;
        public final int textSecondary;
        public final int textMuted;

        public ThemeColors(int bg, int cardBg, int cardBorder, int cardInner,
                           int accentPrimary, int accentSecondary,
                           int textPrimary, int textSecondary, int textMuted) {
            this.bg = bg;
            this.cardBg = cardBg;
            this.cardBorder = cardBorder;
            this.cardInner = cardInner;
            this.accentPrimary = accentPrimary;
            this.accentSecondary = accentSecondary;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.textMuted = textMuted;
        }
    }

    private static String currentTheme = THEME_CYBERPUNK;

    public static void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ai_limits_prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("pref_oled_mode", false)) {
            currentTheme = THEME_OLED;
        } else {
            currentTheme = prefs.getString(PREF_THEME, THEME_CYBERPUNK);
        }
    }

    public static String getThemeKey() {
        return currentTheme != null ? currentTheme : THEME_CYBERPUNK;
    }

    public static void setThemeKey(Context context, String themeKey) {
        currentTheme = themeKey;
        context.getSharedPreferences("ai_limits_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_THEME, themeKey)
                .putBoolean("pref_oled_mode", THEME_OLED.equals(themeKey))
                .apply();
    }

    public static ThemeColors getColors() {
        if (THEME_OLED.equals(currentTheme)) {
            return new ThemeColors(
                    0xFF000000, // pure black bg
                    0xFF121212, // card bg
                    0xFF262626, // border
                    0xFF0A0A0A, // inner card
                    0xFF60A5FA, // accent primary (blue)
                    0xFFC084FC, // accent secondary (purple)
                    0xFFFFFFFF, // text primary
                    0xFFA3A3A3, // text secondary
                    0xFF737373  // text muted
            );
        } else if (THEME_OCEAN.equals(currentTheme)) {
            return new ThemeColors(
                    0xFF0A192F, // midnight navy bg
                    0xFF112240, // card bg
                    0xFF233554, // border
                    0xFF0D1B2A, // inner card
                    0xFF64FFDA, // accent primary (seafoam cyan)
                    0xFF38BDF8, // accent secondary (sky blue)
                    0xFFE6F1FF, // text primary
                    0xFF8892B0, // text secondary
                    0xFF495670  // text muted
            );
        } else if (THEME_EMERALD.equals(currentTheme)) {
            return new ThemeColors(
                    0xFF0A1A14, // dark forest bg
                    0xFF132B20, // card bg
                    0xFF1F4D36, // border
                    0xFF0E2018, // inner card
                    0xFF34D399, // accent primary (emerald mint)
                    0xFF10B981, // accent secondary (jade green)
                    0xFFECFDF5, // text primary
                    0xFFA7F3D0, // text secondary
                    0xFF065F46  // text muted
            );
        } else {
            // Default Cyberpunk Dark
            return new ThemeColors(
                    0xFF0B0F19, // dark slate bg
                    0xFF111827, // card bg
                    0xFF1F2937, // border
                    0xFF0E131F, // inner card
                    0xFF38BDF8, // accent primary (neon cyan)
                    0xFFA855F7, // accent secondary (neon purple)
                    0xFFF9FAFB, // text primary
                    0xFF9CA3AF, // text secondary
                    0xFF6B7280  // text muted
            );
        }
    }
}
