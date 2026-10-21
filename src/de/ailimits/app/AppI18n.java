package de.ailimits.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppI18n {

    public static final String LANG_EN = "en";
    public static final String LANG_DE = "de";
    public static final String LANG_FR = "fr";

    public static final String PREF_LANG = "app_language";

    private static String currentLang = LANG_EN;

    public static void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ai_limits_prefs", Context.MODE_PRIVATE);
        currentLang = prefs.getString(PREF_LANG, LANG_EN);
    }

    public static String getLang() {
        return currentLang != null ? currentLang : LANG_EN;
    }

    public static void setLang(Context context, String lang) {
        currentLang = lang;
        context.getSharedPreferences("ai_limits_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_LANG, lang)
                .apply();
    }

    public static Locale getLocale() {
        if (LANG_DE.equals(currentLang)) return Locale.GERMAN;
        if (LANG_FR.equals(currentLang)) return Locale.FRENCH;
        return Locale.ENGLISH;
    }

    // App Header
    public static String getAppTitle() {
        return "AI Limits";
    }

    public static String getAppSubtitle() {
        if (LANG_DE.equals(currentLang)) {
            return "Claude Pro & Google Cloud Kontingent-Tracker";
        } else if (LANG_FR.equals(currentLang)) {
            return "Suivi des quotas Claude Pro & Google Cloud";
        } else {
            return "Claude Pro & Google Cloud Quota Tracker";
        }
    }

    // Status Texts
    public static String getStatusLiveBoth() {
        if (LANG_DE.equals(currentLang)) return "● Claude Cloud & Google Cloud Live";
        if (LANG_FR.equals(currentLang)) return "● Claude Cloud & Google Cloud En direct";
        return "● Claude Cloud & Google Cloud Live";
    }

    public static String getStatusClaudeLive() {
        if (LANG_DE.equals(currentLang)) return "● Claude Cloud Live • Google Standby";
        if (LANG_FR.equals(currentLang)) return "● Claude Cloud En direct • Google En attente";
        return "● Claude Cloud Live • Google Standby";
    }

    public static String getStatusGoogleLive() {
        if (LANG_DE.equals(currentLang)) return "● Google Cloud Live • Claude Standby";
        if (LANG_FR.equals(currentLang)) return "● Google Cloud En direct • Claude En attente";
        return "● Google Cloud Live • Claude Standby";
    }

    public static String getStatusOffline() {
        if (LANG_DE.equals(currentLang)) return "● Offline / Demo-Modus";
        if (LANG_FR.equals(currentLang)) return "● Hors ligne / Mode Démo";
        return "● Offline / Demo Mode";
    }

    public static String getStatusConnecting() {
        if (LANG_DE.equals(currentLang)) return "Aktualisiere Daten…";
        if (LANG_FR.equals(currentLang)) return "Mise à jour des données…";
        return "Updating quota data…";
    }

    // View Modes
    public static String getViewHorizon() {
        if (LANG_DE.equals(currentLang)) return "⏳ Zeitfenster (Woche zuerst)";
        if (LANG_FR.equals(currentLang)) return "⏳ Horizon (Semaine d'abord)";
        return "⏳ Time Horizon (Weekly First)";
    }

    public static String getViewProvider() {
        if (LANG_DE.equals(currentLang)) return "🏢 Nach Anbieter";
        if (LANG_FR.equals(currentLang)) return "🏢 Par Fournisseur";
        return "🏢 By Provider";
    }

    // Filter Chips
    public static String getFilterAll() {
        if (LANG_DE.equals(currentLang)) return "Alle Limits";
        if (LANG_FR.equals(currentLang)) return "Tous les quotas";
        return "All Quotas";
    }

    public static String getFilterWeek() {
        if (LANG_DE.equals(currentLang)) return "🗓️ Wochenlimits";
        if (LANG_FR.equals(currentLang)) return "🗓️ Hebdomadaire";
        return "🗓️ Weekly Limits";
    }

    public static String getFilter5h() {
        if (LANG_DE.equals(currentLang)) return "⏱️ 5-Stunden";
        if (LANG_FR.equals(currentLang)) return "⏱️ 5-Heures";
        return "⏱️ 5-Hour Limits";
    }

    public static String getFilterDay() {
        if (LANG_DE.equals(currentLang)) return "☀️ Tageslimits";
        if (LANG_FR.equals(currentLang)) return "☀️ Journalier";
        return "☀️ Daily Limits";
    }

    public static String getFilterClaude() {
        return "🟣 Claude Pro";
    }

    public static String getFilterGoogle() {
        if (LANG_DE.equals(currentLang)) return "🔵 Google (Cloud & IDE)";
        if (LANG_FR.equals(currentLang)) return "🔵 Google (Cloud & IDE)";
        return "🔵 Google (Cloud & IDE)";
    }

    public static String getFilterCritical() {
        if (LANG_DE.equals(currentLang)) return "⚠️ Knapp (< 25%)";
        if (LANG_FR.equals(currentLang)) return "⚠️ Critique (< 25%)";
        return "⚠️ Low (< 25%)";
    }

    // Quick Timer
    public static String getQuickTimerStart() {
        if (LANG_DE.equals(currentLang)) return "⏱️ 5h-Timer starten";
        if (LANG_FR.equals(currentLang)) return "⏱️ Minuteur 5h";
        return "⏱️ Start 5h Timer";
    }

    // Value Card
    public static String getSavingsCardTitle() {
        if (LANG_DE.equals(currentLang)) return "💎 Flatrate Ersparnis & Pace";
        if (LANG_FR.equals(currentLang)) return "💎 Économies Flatrate & Rythme";
        return "💎 Subscription Savings & Pace";
    }

    public static String getPaceBalanced() {
        if (LANG_DE.equals(currentLang)) return "⚡ Tempo: Ausgewogen";
        if (LANG_FR.equals(currentLang)) return "⚡ Rythme : Équilibré";
        return "⚡ Pace: Balanced";
    }

    public static String getPaceHigh() {
        if (LANG_DE.equals(currentLang)) return "🔥 Tempo: Hoher Verbrauch";
        if (LANG_FR.equals(currentLang)) return "🔥 Rythme : Consommation élevée";
        return "🔥 Pace: High Consumption";
    }

    public static String getPaceLow() {
        if (LANG_DE.equals(currentLang)) return "🌱 Tempo: Sparsam";
        if (LANG_FR.equals(currentLang)) return "🌱 Rythme : Économique";
        return "🌱 Pace: Conservative";
    }

    public static String getSavingsText(float savedEur) {
        if (LANG_DE.equals(currentLang)) {
            return String.format(Locale.GERMAN, "Diesen Monat: ~%.2f € Tokenwert • Deutliche Ersparnis durch Flatrate!", savedEur);
        } else if (LANG_FR.equals(currentLang)) {
            return String.format(Locale.FRENCH, "Ce mois-ci : ~%.2f € de valeur en jetons • Économies substantielles avec l'abonnement !", savedEur);
        } else {
            return String.format(Locale.US, "This month: ~€%.2f token equivalent value • Significant savings with your plan!", savedEur);
        }
    }

    // Horizon Group Titles
    public static String getHorizonWeeklyTitle() {
        if (LANG_DE.equals(currentLang)) return "🗓️ Wochenlimits (7 Tage Langzeit-Kapazität)";
        if (LANG_FR.equals(currentLang)) return "🗓️ Quotas Hebdomadaires (7 jours de capacité)";
        return "🗓️ Weekly Quotas (7-Day Long-Term Capacity)";
    }

    public static String getHorizonWeeklyDesc(float avgFree) {
        if (LANG_DE.equals(currentLang)) {
            return String.format(Locale.GERMAN, "Durchschnittlich %.0f%% frei • Langzeit-Puffer über Claude & Google", avgFree * 100f);
        } else if (LANG_FR.equals(currentLang)) {
            return String.format(Locale.FRENCH, "Moyenne de %.0f%% disponible • Capacité long terme Claude & Google", avgFree * 100f);
        } else {
            return String.format(Locale.US, "Average %.0f%% available • Long-term buffer across Claude & Google", avgFree * 100f);
        }
    }

    public static String getHorizon5hTitle() {
        if (LANG_DE.equals(currentLang)) return "⏱️ 5-Stunden-Limits (Sofort verfügbar)";
        if (LANG_FR.equals(currentLang)) return "⏱️ Quotas 5 Heures (Disponible immédiatement)";
        return "⏱️ 5-Hour Limits (Immediate Session Buffer)";
    }

    public static String getHorizon5hDesc(float avgFree) {
        if (LANG_DE.equals(currentLang)) {
            return String.format(Locale.GERMAN, "Durchschnittlich %.0f%% frei • Rolling Windows für intensive Coding-Sessions", avgFree * 100f);
        } else if (LANG_FR.equals(currentLang)) {
            return String.format(Locale.FRENCH, "Moyenne de %.0f%% disponible • Fenêtres glissantes pour sessions de code", avgFree * 100f);
        } else {
            return String.format(Locale.US, "Average %.0f%% available • Rolling windows for intensive coding sessions", avgFree * 100f);
        }
    }

    public static String getHorizonDailyTitle() {
        if (LANG_DE.equals(currentLang)) return "☀️ Tageslimits (Reset um Mitternacht UTC)";
        if (LANG_FR.equals(currentLang)) return "☀️ Quotas Journaliers (Réinitialisation à minuit UTC)";
        return "☀️ Daily Limits (Midnight UTC Reset)";
    }

    public static String getHorizonDailyDesc(float avgFree) {
        if (LANG_DE.equals(currentLang)) {
            return String.format(Locale.GERMAN, "Tägliche Kontingente & Request-Limits (RPD) • %.0f%% frei", avgFree * 100f);
        } else if (LANG_FR.equals(currentLang)) {
            return String.format(Locale.FRENCH, "Quotas quotidiens & limites de requêtes (RPD) • %.0f%% disponible", avgFree * 100f);
        } else {
            return String.format(Locale.US, "Daily developer quotas & request limits (RPD) • %.0f%% available", avgFree * 100f);
        }
    }

    // Badges & Window Letters
    public static String getWindowLetter(String badgeLetter) {
        if ("W".equals(badgeLetter)) {
            if (LANG_DE.equals(currentLang)) return " W ";
            if (LANG_FR.equals(currentLang)) return " S ";
            return " W ";
        } else if ("T".equals(badgeLetter)) {
            if (LANG_DE.equals(currentLang)) return " T ";
            if (LANG_FR.equals(currentLang)) return " J ";
            return " D ";
        } else {
            return " 5h ";
        }
    }

    public static String getProviderBadgeText(String provider) {
        if ("CLAUDE".equals(provider)) {
            return "🟣 Claude Pro";
        } else if ("GOOGLE_IDE".equals(provider)) {
            return "🔵 Google IDE";
        } else {
            if (LANG_DE.equals(currentLang)) return "🔵 Cloud Direkt";
            if (LANG_FR.equals(currentLang)) return "🔵 Cloud Direct";
            return "🔵 Cloud Direct";
        }
    }

    // Recovery & Countdown
    public static String getFullReady() {
        if (LANG_DE.equals(currentLang)) return "✅ 100% Bereit";
        if (LANG_FR.equals(currentLang)) return "✅ 100% Prêt";
        return "✅ 100% Ready";
    }

    public static String getRecoveryPct(int pct) {
        if (LANG_DE.equals(currentLang)) return "Erholung: " + pct + "%";
        if (LANG_FR.equals(currentLang)) return "Rétablissement : " + pct + "%";
        return "Recovery: " + pct + "%";
    }

    public static String getCountdownText(long diffMillis) {
        if (diffMillis <= 0) {
            if (LANG_DE.equals(currentLang)) return "Jetzt verfügbar";
            if (LANG_FR.equals(currentLang)) return "Disponible maintenant";
            return "Available now";
        }
        long hours = diffMillis / (3600L * 1000L);
        long mins = (diffMillis % (3600L * 1000L)) / (60L * 1000L);
        long days = hours / 24;

        if (days > 0) {
            long remHours = hours % 24;
            if (LANG_DE.equals(currentLang)) return String.format(Locale.GERMAN, "Reset in %dd %dh", days, remHours);
            if (LANG_FR.equals(currentLang)) return String.format(Locale.FRENCH, "Réinitialisation dans %dj %dh", days, remHours);
            return String.format(Locale.US, "Resets in %dd %dh", days, remHours);
        } else {
            if (LANG_DE.equals(currentLang)) return String.format(Locale.GERMAN, "Reset in %dh %02dm", hours, mins);
            if (LANG_FR.equals(currentLang)) return String.format(Locale.FRENCH, "Réinitialisation dans %dh %02dm", hours, mins);
            return String.format(Locale.US, "Resets in %dh %02dm", hours, mins);
        }
    }

    // Detail Dialog Texts
    public static String getDetailCapacity(int remPct) {
        if (LANG_DE.equals(currentLang)) {
            return remPct + "% frei • " + (100 - remPct) + "% verbraucht";
        } else if (LANG_FR.equals(currentLang)) {
            return remPct + "% disponible • " + (100 - remPct) + "% utilisé";
        } else {
            return remPct + "% available • " + (100 - remPct) + "% used";
        }
    }

    public static String formatResetDateTime(long millis) {
        if (millis <= 0) {
            if (LANG_DE.equals(currentLang)) return "Kein Reset-Zeitpunkt bekannt";
            if (LANG_FR.equals(currentLang)) return "Heure de réinitialisation inconnue";
            return "No reset schedule available";
        }
        if (LANG_DE.equals(currentLang)) {
            SimpleDateFormat sdf = new SimpleDateFormat("E, d. MMM yyyy, HH:mm 'Uhr'", Locale.GERMAN);
            return sdf.format(new Date(millis));
        } else if (LANG_FR.equals(currentLang)) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE d MMM yyyy à HH:mm", Locale.FRENCH);
            return sdf.format(new Date(millis));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy, h:mm a", Locale.ENGLISH);
            return sdf.format(new Date(millis));
        }
    }

    public static String getTimelineHeader() {
        if (LANG_DE.equals(currentLang)) return "📈 Verbrauchs-Historie (Echte SQLite Messreihe)";
        if (LANG_FR.equals(currentLang)) return "📈 Historique d'utilisation (Mesures réelles SQLite)";
        return "📈 Usage History Timeline (Real SQLite Snapshots)";
    }

    public static String getTimelineEmpty() {
        if (LANG_DE.equals(currentLang)) return "Noch keine Messdaten aufgezeichnet.\nSnapshots werden bei jeder Aktualisierung automatisch gespeichert.";
        if (LANG_FR.equals(currentLang)) return "Aucun instantané enregistré.\nLes mesures réelles s'accumulent à chaque synchronisation.";
        return "No historical snapshots recorded yet.\nReal measurements are stored automatically on every refresh.";
    }

    public static String getTimelineOnePoint() {
        if (LANG_DE.equals(currentLang)) return "Erster Messpunkt erfasst • Verlauf wächst kontinuierlich mit jedem Abruf.";
        if (LANG_FR.equals(currentLang)) return "Première mesure enregistrée • L'historique s'accumule à chaque appel.";
        return "First measurement recorded • Timeline expands automatically with each sync.";
    }

    public static String getBtn24h() {
        if (LANG_DE.equals(currentLang)) return "24 Stunden";
        if (LANG_FR.equals(currentLang)) return "24 Heures";
        return "24 Hours";
    }

    public static String getBtn7d() {
        if (LANG_DE.equals(currentLang)) return "7 Tage";
        if (LANG_FR.equals(currentLang)) return "7 Jours";
        return "7 Days";
    }

    public static String getModelsHeader() {
        if (LANG_DE.equals(currentLang)) return "Enthaltene Modelle & Spezifikationen";
        if (LANG_FR.equals(currentLang)) return "Modèles inclus & spécifications";
        return "Included Models & Specifications";
    }

    public static String getMechanicsHeader() {
        if (LANG_DE.equals(currentLang)) return "Token-Gewichtungsmechanik";
        if (LANG_FR.equals(currentLang)) return "Mécanique de pondération des jetons";
        return "Token Weighting Mechanics";
    }

    public static String getBtnClose() {
        if (LANG_DE.equals(currentLang)) return "Schließen";
        if (LANG_FR.equals(currentLang)) return "Fermer";
        return "Close";
    }

    public static String getBtnCopy() {
        if (LANG_DE.equals(currentLang)) return "Zusammenfassung kopieren";
        if (LANG_FR.equals(currentLang)) return "Copier le résumé";
        return "Copy Summary";
    }

    // Model Group Explanation
    public static String getModelGroupExplanation(String bucketId, String displayName) {
        String b = (bucketId != null ? bucketId : "").toLowerCase(Locale.ROOT);
        String d = (displayName != null ? displayName : "").toLowerCase(Locale.ROOT);

        if (b.contains("claude_pro_5h") || (d.contains("claude pro") && d.contains("5"))) {
            if (LANG_DE.equals(currentLang)) return "Modelle: Claude 3.5 Sonnet, Claude 3 Opus & Claude 3.5 Haiku";
            if (LANG_FR.equals(currentLang)) return "Modèles : Claude 3.5 Sonnet, Claude 3 Opus & Claude 3.5 Haiku";
            return "Models: Claude 3.5 Sonnet, Claude 3 Opus & Claude 3.5 Haiku";
        }
        if (b.contains("claude_pro_7d") || (d.contains("claude pro") && d.contains("woche"))) {
            if (LANG_DE.equals(currentLang)) return "Modelle: Anthropic Gesamtkontingent (Claude Code CLI + Web)";
            if (LANG_FR.equals(currentLang)) return "Modèles : Quota Anthropic global (Claude Code CLI + Web)";
            return "Models: Anthropic Global Quota (Claude Code CLI + Web)";
        }
        if (b.contains("google_gemini_cloud_pro") || (d.contains("gemini") && d.contains("pro") && d.contains("direkt"))) {
            if (LANG_DE.equals(currentLang)) return "Modelle: Gemini 1.5 Pro (2M Kontext) • Google Cloud API Projekt";
            if (LANG_FR.equals(currentLang)) return "Modèles : Gemini 1.5 Pro (Contexte 2M) • Projet API Google Cloud";
            return "Models: Gemini 1.5 Pro (2M Context) • Google Cloud API Project";
        }
        if (b.contains("google_gemini_flash") || (d.contains("gemini") && d.contains("flash"))) {
            if (LANG_DE.equals(currentLang)) return "Modelle: Gemini 1.5 Flash (1M Kontext) • Google Cloud Developer API";
            if (LANG_FR.equals(currentLang)) return "Modèles : Gemini 1.5 Flash (Contexte 1M) • API Google Cloud Developer";
            return "Models: Gemini 1.5 Flash (1M Context) • Google Cloud Developer API";
        }
        if (b.contains("google_gemini_daily") || d.contains("tageskontingent")) {
            if (LANG_DE.equals(currentLang)) return "Modelle: Google Cloud AI Developer Requests (RPD, Reset 0:00 UTC)";
            if (LANG_FR.equals(currentLang)) return "Modèles : Requêtes Développeur Google Cloud AI (RPD, Reset 0h UTC)";
            return "Models: Google Cloud AI Developer Requests (RPD, Reset 0:00 UTC)";
        }
        if (b.startsWith("3p-") || d.contains("claude & gpt") || d.contains("claude and gpt")) {
            if (LANG_DE.equals(currentLang)) return "Modelle: Claude 3.5 Sonnet, Opus, GPT-4o • Google Antigravity IDE";
            if (LANG_FR.equals(currentLang)) return "Modèles : Claude 3.5 Sonnet, Opus, GPT-4o • Google Antigravity IDE";
            return "Models: Claude 3.5 Sonnet, Opus, GPT-4o • Google Antigravity IDE";
        }
        if (b.startsWith("gemini-argon") || d.contains("argon")) {
            if (LANG_DE.equals(currentLang)) return "Modelle: Gemini 4 Argon (Advanced Deep Reasoning) • Google IDE";
            if (LANG_FR.equals(currentLang)) return "Modèles : Gemini 4 Argon (Raisonnement approfondi) • Google IDE";
            return "Models: Gemini 4 Argon (Advanced Deep Reasoning) • Google IDE";
        }
        if (b.startsWith("gemini-") || d.contains("gemini models")) {
            if (LANG_DE.equals(currentLang)) return "Modelle: Gemini 3.8 Pro, 3.8 Pro Lite, 3.1 Flash & 3.8 Flash • IDE";
            if (LANG_FR.equals(currentLang)) return "Modèles : Gemini 3.8 Pro, 3.8 Pro Lite, 3.1 Flash & 3.8 Flash • IDE";
            return "Models: Gemini 3.8 Pro, 3.8 Pro Lite, 3.1 Flash & 3.8 Flash • IDE";
        }

        return displayName != null ? displayName : "AI Model Group";
    }

    public static String getDetailedModelList(String bucketId, String displayName) {
        String b = (bucketId != null ? bucketId : "").toLowerCase(Locale.ROOT);
        String d = (displayName != null ? displayName : "").toLowerCase(Locale.ROOT);

        if (b.contains("claude_pro_5h") || (d.contains("claude pro") && d.contains("5"))) {
            if (LANG_DE.equals(currentLang)) {
                return "• Claude 3.5 Sonnet (200k Kontext • Ultraschnell für Code-Generation)\n" +
                       "• Claude 3 Opus (Höchste Reasoning-Tiefe für Systemarchitektur)\n" +
                       "• Claude 3.5 Haiku (Leichtgewichtig & Schnell)";
            } else if (LANG_FR.equals(currentLang)) {
                return "• Claude 3.5 Sonnet (Contexte 200k • Ultra-rapide pour le code)\n" +
                       "• Claude 3 Opus (Raisonnement approfondi pour l'architecture)\n" +
                       "• Claude 3.5 Haiku (Léger et véloce)";
            } else {
                return "• Claude 3.5 Sonnet (200k context • High speed coding & logic)\n" +
                       "• Claude 3 Opus (Maximum reasoning depth for complex architecture)\n" +
                       "• Claude 3.5 Haiku (Lightweight & responsive)";
            }
        }
        if (b.contains("claude_pro_7d") || (d.contains("claude pro") && d.contains("woche"))) {
            if (LANG_DE.equals(currentLang)) {
                return "• Gesamtes Anthropic Claude Modellkontingent\n" +
                       "• Deckt Claude Code CLI und Desktop/Web-Chats ab\n" +
                       "• Wöchentliche Gesamtkapazitätsgrenze deines Pro-Plans";
            } else if (LANG_FR.equals(currentLang)) {
                return "• Quota global des modèles Anthropic Claude\n" +
                       "• Couvre Claude Code CLI et les discussions web/desktop\n" +
                       "• Plafond de capacité hebdomadaire officiel de votre plan Pro";
            } else {
                return "• Global Anthropic Claude model quota ceiling\n" +
                       "• Covers Claude Code CLI sessions and desktop/web chats\n" +
                       "• Official 7-day total usage capacity for your Pro plan";
            }
        }
        if (b.contains("google_gemini_cloud_pro") || (d.contains("gemini") && d.contains("pro") && d.contains("direkt"))) {
            if (LANG_DE.equals(currentLang)) {
                return "• Gemini 1.5 Pro (2 Millionen Token Kontextfenster)\n" +
                       "• Multimodal (Audio, Video, PDF & Large Codebase Analysen)\n" +
                       "• Google Cloud Developer API Projekt (gen-lang-client-0229011306)";
            } else if (LANG_FR.equals(currentLang)) {
                return "• Gemini 1.5 Pro (Fenêtre de contexte de 2 millions de jetons)\n" +
                       "• Multimodal (Audio, vidéo, PDF et analyse de bases de code)\n" +
                       "• Projet d'API Google Cloud Developer (gen-lang-client-0229011306)";
            } else {
                return "• Gemini 1.5 Pro (2 Million token context window)\n" +
                       "• Multimodal (Audio, video, documents & massive repository analysis)\n" +
                       "• Google Cloud Developer API project (gen-lang-client-0229011306)";
            }
        }
        if (b.contains("google_gemini_flash") || (d.contains("gemini") && d.contains("flash"))) {
            if (LANG_DE.equals(currentLang)) {
                return "• Gemini 1.5 Flash (1 Million Token Kontextfenster)\n" +
                       "• Optimiert für maximale Durchsatzrate & minimale Latenz\n" +
                       "• Google Cloud Developer API Projektanfragen";
            } else if (LANG_FR.equals(currentLang)) {
                return "• Gemini 1.5 Flash (Fenêtre de contexte d'un million de jetons)\n" +
                       "• Optimisé pour un débit maximal et une latence minimale\n" +
                       "• Requêtes du projet d'API Google Cloud Developer";
            } else {
                return "• Gemini 1.5 Flash (1 Million token context window)\n" +
                       "• Engineered for high throughput and ultra-low latency\n" +
                       "• Google Cloud Developer API project requests";
            }
        }
        if (b.startsWith("gemini-argon") || d.contains("argon")) {
            if (LANG_DE.equals(currentLang)) {
                return "• Gemini 4 Argon (Advanced Deep Reasoning)\n" +
                       "• Speziell für komplexe mathematische Beweise und Logikplanung\n" +
                       "• Separates, isoliertes Kontingent in Google Antigravity";
            } else if (LANG_FR.equals(currentLang)) {
                return "• Gemini 4 Argon (Raisonnement approfondi avancé)\n" +
                       "• Dédié aux preuves mathématiques complexes et planification logique\n" +
                       "• Quota isolé distinct dans Google Antigravity";
            } else {
                return "• Gemini 4 Argon (Advanced Deep Reasoning)\n" +
                       "• Designed for complex problem solving, math & architectural planning\n" +
                       "• Distinct, isolated quota bucket inside Google Antigravity";
            }
        }
        if (b.startsWith("3p-") || d.contains("claude & gpt") || d.contains("claude and gpt")) {
            if (LANG_DE.equals(currentLang)) {
                return "• Claude 3.5 Sonnet (Partner-Modell via Google IDE)\n" +
                       "• Claude 3 Opus (Partner-Modell via Google IDE)\n" +
                       "• GPT-4o (Partner-Modell via Google IDE)\n" +
                       "• Integriert im Antigravity Developer-Pool";
            } else if (LANG_FR.equals(currentLang)) {
                return "• Claude 3.5 Sonnet (Modèle partenaire via Google IDE)\n" +
                       "• Claude 3 Opus (Modèle partenaire via Google IDE)\n" +
                       "• GPT-4o (Modèle partenaire via Google IDE)\n" +
                       "• Intégré dans le pool de développement Antigravity";
            } else {
                return "• Claude 3.5 Sonnet (Partner model via Google IDE proxy)\n" +
                       "• Claude 3 Opus (Partner model via Google IDE proxy)\n" +
                       "• GPT-4o (Partner model via Google IDE proxy)\n" +
                       "• Included within the Antigravity developer quota pool";
            }
        }

        // Default Gemini IDE Models
        if (LANG_DE.equals(currentLang)) {
            return "• Gemini 3.8 Pro (Lead Coding & Autonomous Agent Model)\n" +
                   "• Gemini 3.8 Pro Lite (Effiziente Refactorings & schnelle Antworten)\n" +
                   "• Gemini 3.1 Flash (Schnelle Code-Vervollständigung & Inline-Edits)\n" +
                   "• Gemini 3.8 Flash (Multimodal & Agent Execution)";
        } else if (LANG_FR.equals(currentLang)) {
            return "• Gemini 3.8 Pro (Modèle de codage principal et agents autonomes)\n" +
                   "• Gemini 3.8 Pro Lite (Refactoring efficace et réponses rapides)\n" +
                   "• Gemini 3.1 Flash (Complétion rapide et modifications en ligne)\n" +
                   "• Gemini 3.8 Flash (Multimodal et exécution d'agents)";
        } else {
            return "• Gemini 3.8 Pro (Flagship coding & autonomous agent model)\n" +
                   "• Gemini 3.8 Pro Lite (Efficient refactoring & fast turnarounds)\n" +
                   "• Gemini 3.1 Flash (Ultra-fast completions & inline editing)\n" +
                   "• Gemini 3.8 Flash (Multimodal agent task execution)";
        }
    }

    public static String getTokenWeightingExplanation() {
        if (LANG_DE.equals(currentLang)) {
            return "Innerhalb dieser Gruppe teilen sich die Modelle das Kontingent proportional zum Token-Kostenaufwand:\n\n" +
                   "• Input-Tokens: Geringster Kontingent-Verbrauch (günstige Einlese-Phase).\n" +
                   "• Output-Tokens: Höchste Gewichtung (aufwändige Inferenz & Generierung).\n" +
                   "• Context-Caching: Bis zu 75% Rabatt bei wiederholtem Prompt-Kontext.";
        } else if (LANG_FR.equals(currentLang)) {
            return "Au sein de ce groupe, les modèles partagent le quota proportionnellement au coût de calcul des jetons :\n\n" +
                   "• Jetons d'entrée : Consommation minimale (phase de lecture du prompt).\n" +
                   "• Jetons de sortie : Pondération maximale (inférence et génération intensives).\n" +
                   "• Mise en cache de contexte : Jusqu'à 75% de réduction sur les contextes répétés.";
        } else {
            return "Models in this group draw from the quota proportionally to the compute cost of each token type:\n\n" +
                   "• Input Tokens: Lowest quota consumption (prompt reading & context ingestion).\n" +
                   "• Output Tokens: Highest weighting (intensive inference & generation).\n" +
                   "• Context Caching: Up to 75% discount on recurrent prompt context.";
        }
    }

    // Settings Strings
    public static String getSettingsTitle() {
        if (LANG_DE.equals(currentLang)) return "Einstellungen & AI Accounts";
        if (LANG_FR.equals(currentLang)) return "Paramètres & Comptes IA";
        return "Settings & AI Accounts";
    }

    public static String getSettingsSubtitle() {
        if (LANG_DE.equals(currentLang)) return "Verbindungen, Themes, Sprachen & Benachrichtigungen.";
        if (LANG_FR.equals(currentLang)) return "Connexions, thèmes, langues et notifications.";
        return "Connections, themes, languages & notifications.";
    }

    public static String getSectionLanguage() {
        if (LANG_DE.equals(currentLang)) return "1. Sprache / Language / Langue";
        if (LANG_FR.equals(currentLang)) return "1. Langue / Language / Sprache";
        return "1. Language / Sprache / Langue";
    }

    public static String getSectionTheme() {
        if (LANG_DE.equals(currentLang)) return "2. App Theme & Design";
        if (LANG_FR.equals(currentLang)) return "2. Thème & Style Visuel";
        return "2. App Theme & Design";
    }

    public static String getThemeNameCyberpunk() {
        if (LANG_DE.equals(currentLang)) return "🌌 Cyberpunk Dark (Standard)";
        if (LANG_FR.equals(currentLang)) return "🌌 Cyberpunk Sombre (Défaut)";
        return "🌌 Cyberpunk Dark (Default)";
    }

    public static String getThemeNameOled() {
        if (LANG_DE.equals(currentLang)) return "🖤 OLED Tiefschwarz (Akkusparend)";
        if (LANG_FR.equals(currentLang)) return "🖤 OLED Noir Absolu (Éco batterie)";
        return "🖤 OLED Pure Black (Battery Saver)";
    }

    public static String getThemeNameOcean() {
        if (LANG_DE.equals(currentLang)) return "🌊 Deep Ocean Navy (Tiefblau)";
        if (LANG_FR.equals(currentLang)) return "🌊 Océan Profond (Marine)";
        return "🌊 Deep Ocean Navy";
    }

    public static String getThemeNameEmerald() {
        if (LANG_DE.equals(currentLang)) return "🌲 Emerald Matrix (Smaragdgrün)";
        if (LANG_FR.equals(currentLang)) return "🌲 Matrice Émeraude";
        return "🌲 Emerald Matrix";
    }

    public static String getSectionClaude() {
        if (LANG_DE.equals(currentLang)) return "3. Claude Pro (Anthropic Direkt • Unterwegs)";
        if (LANG_FR.equals(currentLang)) return "3. Claude Pro (Anthropic Direct • Partout)";
        return "3. Claude Pro (Anthropic Direct • Anywhere)";
    }

    public static String getClaudeTokenHint() {
        return "Claude Refresh Token (sk-ant-ort01-...)";
    }

    public static String getBtnTestClaude() {
        if (LANG_DE.equals(currentLang)) return "Claude Cloud prüfen (api.anthropic.com)";
        if (LANG_FR.equals(currentLang)) return "Tester Claude Cloud (api.anthropic.com)";
        return "Verify Claude Cloud (api.anthropic.com)";
    }

    public static String getSectionGoogle() {
        if (LANG_DE.equals(currentLang)) return "4. Google Account (Cloud Direkt • Unterwegs)";
        if (LANG_FR.equals(currentLang)) return "4. Compte Google (Cloud Direct • Partout)";
        return "4. Google Account (Cloud Direct • Anywhere)";
    }

    public static String getGoogleTokenHint() {
        return "Google Refresh Token (1//033...)";
    }

    public static String getBtnTestGoogle() {
        if (LANG_DE.equals(currentLang)) return "Google Cloud prüfen (oauth2.googleapis.com)";
        if (LANG_FR.equals(currentLang)) return "Tester Google Cloud (oauth2.googleapis.com)";
        return "Verify Google Cloud (oauth2.googleapis.com)";
    }

    public static String getSectionBridge() {
        if (LANG_DE.equals(currentLang)) return "5. Antigravity Desktop-Bridge (Optional / Lokal)";
        if (LANG_FR.equals(currentLang)) return "5. Pont Desktop Antigravity (Optionnel / Local)";
        return "5. Antigravity Desktop-Bridge (Optional / Local)";
    }

    public static String getBtnTestBridge() {
        if (LANG_DE.equals(currentLang)) return "Bridge prüfen";
        if (LANG_FR.equals(currentLang)) return "Tester le pont";
        return "Test Bridge";
    }

    public static String getBtnTestAlert() {
        if (LANG_DE.equals(currentLang)) return "🔔 Test-Alarm";
        if (LANG_FR.equals(currentLang)) return "🔔 Tester l'alerte";
        return "🔔 Test Alert";
    }

    public static String getSectionNotifications() {
        if (LANG_DE.equals(currentLang)) return "6. Benachrichtigungs-Optionen";
        if (LANG_FR.equals(currentLang)) return "6. Options de notification";
        return "6. Notification Options";
    }

    public static String getBtnCancel() {
        if (LANG_DE.equals(currentLang)) return "Abbrechen";
        if (LANG_FR.equals(currentLang)) return "Annuler";
        return "Cancel";
    }

    public static String getBtnSave() {
        if (LANG_DE.equals(currentLang)) return "Speichern";
        if (LANG_FR.equals(currentLang)) return "Enregistrer";
        return "Save Settings";
    }

    public static String getSettingsSavedToast() {
        if (LANG_DE.equals(currentLang)) return "Einstellungen erfolgreich gespeichert";
        if (LANG_FR.equals(currentLang)) return "Paramètres enregistrés avec succès";
        return "Settings saved successfully";
    }
}
