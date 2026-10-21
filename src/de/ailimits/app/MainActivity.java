package de.ailimits.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements View.OnClickListener, Runnable,
        QuotaApiClient.QuotaCallback, ClaudeDirectClient.ClaudeUsageCallback, GoogleDirectClient.GoogleDirectCallback {

    private static final String TAG = "MainActivity";
    private static final String PREF_CONFIG = "ai_limits_config";

    public static final String KEY_HOST = "host";
    public static final String KEY_PORT = "port";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_CLAUDE_ACCESS_TOKEN = "claude_access_token";
    public static final String KEY_CLAUDE_REFRESH_TOKEN = "claude_refresh_token";
    public static final String KEY_GOOGLE_REFRESH_TOKEN = "google_refresh_token";
    public static final String KEY_OLED_MODE = "oled_mode";
    public static final String KEY_NOTIFY_LOW = "notify_low";
    public static final String KEY_NOTIFY_FULL = "notify_full";
    public static final String KEY_VIEW_MODE = "view_mode"; // "HORIZON" or "PROVIDER"

    // Defaults loaded from SecretsConfig (gitignored) or entered in Settings
    public static final String DEFAULT_CLAUDE_ACCESS = SecretsConfig.DEFAULT_CLAUDE_ACCESS;
    public static final String DEFAULT_CLAUDE_REFRESH = SecretsConfig.DEFAULT_CLAUDE_REFRESH;
    public static final String DEFAULT_GOOGLE_CID = SecretsConfig.DEFAULT_GOOGLE_CID;
    public static final String DEFAULT_GOOGLE_SECRET = SecretsConfig.DEFAULT_GOOGLE_SECRET;
    public static final String DEFAULT_GOOGLE_REFRESH = SecretsConfig.DEFAULT_GOOGLE_REFRESH;
    public static final String DEFAULT_GOOGLE_HOST = SecretsConfig.DEFAULT_GOOGLE_HOST;
    public static final int DEFAULT_GOOGLE_PORT = SecretsConfig.DEFAULT_GOOGLE_PORT;
    public static final String DEFAULT_GOOGLE_CSRF = SecretsConfig.DEFAULT_GOOGLE_CSRF;

    private View svRoot;
    private TextView tvStatus;
    private TextView tvFooter;
    private TextView tvPaceIndicator;
    private TextView tvSavingsText;
    private LinearLayout llGroupsContainer;
    private Button btnMessageCenter;
    private Button btnShare;
    private ImageButton btnRefresh;
    private ImageButton btnSettings;
    private Button btnQuickTimer;

    // View Mode Switcher
    private Button btnViewHorizon;
    private Button btnViewProvider;
    private String viewMode = "HORIZON"; // Default to Horizon / Time-window first

    // Freie Filter Chips
    private Button chipFilterAll;
    private Button chipFilterWeek;
    private Button chipFilter5h;
    private Button chipFilterDay;
    private Button chipFilterClaude;
    private Button chipFilterGoogle;
    private Button chipFilterCritical;
    private String selectedFilter = "ALL";

    private SharedPreferences prefs;
    private Handler mainHandler;
    private final List<TickerItem> tickerItems = new ArrayList<>();
    private int tickCount = 0;
    private long quickTimerEndTime = 0L;

    // Cache of fetched data
    private QuotaGroup cachedClaudeGroup = null;
    private QuotaGroup cachedGoogleDirectGroup = null;
    private List<QuotaGroup> cachedLocalGroups = new ArrayList<>();
    private boolean claudeIsLive = false;
    private boolean googleDirectIsLive = false;
    private boolean localIsLive = false;
    private String googleOverallDescription = "";

    public static class TickerItem {
        public QuotaBucket bucket;
        public TextView tvCountdown;
        public CircularProgressView progressView;
        public RecoveryProgressBar recoveryBar;
        public TextView tvRecoveryRatio;
        public Button btnAlarm;

        public TickerItem(QuotaBucket b, TextView tv, CircularProgressView cp, RecoveryProgressBar rBar, TextView tvRec, Button btn) {
            this.bucket = b;
            this.tvCountdown = tv;
            this.progressView = cp;
            this.recoveryBar = rBar;
            this.tvRecoveryRatio = tvRec;
            this.btnAlarm = btn;
        }
    }

    private static class ToastRunnable implements Runnable {
        private final Context context;
        private final String message;

        ToastRunnable(Context context, String message) {
            this.context = context;
            this.message = message;
        }

        @Override
        public void run() {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    private static class AlarmClickListener implements View.OnClickListener {
        private final MainActivity activity;
        private final QuotaBucket bucket;
        private final Button btnAlarm;

        AlarmClickListener(MainActivity activity, QuotaBucket bucket, Button btnAlarm) {
            this.activity = activity;
            this.bucket = bucket;
            this.btnAlarm = btnAlarm;
        }

        @Override
        public void onClick(View v) {
            boolean isArmed = NotificationScheduler.isAlarmScheduled(activity, bucket.bucketId);
            if (isArmed) {
                NotificationScheduler.cancelResetNotification(activity, bucket.bucketId);
                activity.updateAlarmButtonState(btnAlarm, false);
                Toast.makeText(activity, "Erinnerung entfernt", Toast.LENGTH_SHORT).show();
            } else {
                if (bucket.resetTimeMillis <= System.currentTimeMillis()) {
                    Toast.makeText(activity, "Limit ist bereits wieder verfügbar!", Toast.LENGTH_SHORT).show();
                    return;
                }
                NotificationScheduler.scheduleResetNotification(activity, bucket);
                activity.updateAlarmButtonState(btnAlarm, true);
                Toast.makeText(activity, "🔔 Erinnerung aktiviert für " + bucket.displayName, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static class SettingsDialogHolder implements View.OnClickListener,
            QuotaApiClient.QuotaCallback, ClaudeDirectClient.ClaudeUsageCallback, GoogleDirectClient.GoogleDirectCallback {
        final MainActivity activity;
        final AlertDialog dialog;
        final EditText etClaudeRefresh;
        final EditText etGoogleRefresh;
        final EditText etHost;
        final EditText etPort;
        final EditText etToken;
        final CheckBox cbOledMode;
        final CheckBox cbNotifyLow;
        final CheckBox cbNotifyFull;

        SettingsDialogHolder(MainActivity activity, AlertDialog dialog,
                             EditText claudeRef, EditText googleRef,
                             EditText host, EditText port, EditText token,
                             CheckBox oled, CheckBox notifyLow, CheckBox notifyFull) {
            this.activity = activity;
            this.dialog = dialog;
            this.etClaudeRefresh = claudeRef;
            this.etGoogleRefresh = googleRef;
            this.etHost = host;
            this.etPort = port;
            this.etToken = token;
            this.cbOledMode = oled;
            this.cbNotifyLow = notifyLow;
            this.cbNotifyFull = notifyFull;
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btnCancelSettings) {
                dialog.dismiss();
            } else if (id == R.id.btnSaveSettings) {
                String cRef = etClaudeRefresh.getText().toString().trim();
                String gRef = etGoogleRefresh.getText().toString().trim();
                String host = etHost.getText().toString().trim();
                int port = 41359;
                try {
                    port = Integer.parseInt(etPort.getText().toString().trim());
                } catch (Exception ignored) {}
                String token = etToken.getText().toString().trim();

                activity.prefs.edit()
                        .putString(KEY_CLAUDE_REFRESH_TOKEN, cRef)
                        .putString(KEY_GOOGLE_REFRESH_TOKEN, gRef)
                        .putString(KEY_HOST, host)
                        .putInt(KEY_PORT, port)
                        .putString(KEY_TOKEN, token)
                        .putBoolean(KEY_OLED_MODE, cbOledMode.isChecked())
                        .putBoolean(KEY_NOTIFY_LOW, cbNotifyLow.isChecked())
                        .putBoolean(KEY_NOTIFY_FULL, cbNotifyFull.isChecked())
                        .apply();

                activity.applyOledMode();
                Toast.makeText(activity, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                activity.loadQuotaData();
            } else if (id == R.id.btnTestClaude) {
                Toast.makeText(activity, "Prüfe Claude Cloud API (anthropic.com)…", Toast.LENGTH_SHORT).show();
                ClaudeDirectClient.fetchClaudeGroup(activity, this);
            } else if (id == R.id.btnTestGoogleDirect) {
                Toast.makeText(activity, "Prüfe Google Cloud Account Direkt…", Toast.LENGTH_SHORT).show();
                GoogleDirectClient.fetchGoogleDirect(activity, this);
            } else if (id == R.id.btnTestConnection) {
                String host = etHost.getText().toString().trim();
                int port = 41359;
                try {
                    port = Integer.parseInt(etPort.getText().toString().trim());
                } catch (Exception ignored) {}
                String token = etToken.getText().toString().trim();

                Toast.makeText(activity, "Prüfe Desktop-Bridge (" + host + ":" + port + ")…", Toast.LENGTH_SHORT).show();
                QuotaApiClient.fetchQuota(host, port, token, this);
            } else if (id == R.id.btnTestNotification) {
                ResetReceiver.showNotification(
                        activity,
                        "AI Limits: Claude & Google",
                        "Dein 5-Stunden-Limit ist wieder verfügbar (100% frei)!"
                );
                Toast.makeText(activity, "🔔 Test-Benachrichtigung gesendet!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onClaudeSuccess(QuotaGroup claudeGroup) {
            activity.mainHandler.post(new ToastRunnable(activity, "✓ Claude Cloud erfolgreich verbunden!"));
        }

        @Override
        public void onClaudeError(String errorMsg) {
            activity.mainHandler.post(new ToastRunnable(activity, "✗ Claude Fehler: " + errorMsg));
        }

        @Override
        public void onGoogleDirectSuccess(QuotaGroup group, String email, String projectName) {
            activity.mainHandler.post(new ToastRunnable(activity, "✓ Google Cloud erfolgreich!\nKonto: " + email));
        }

        @Override
        public void onGoogleDirectError(String errorMsg) {
            activity.mainHandler.post(new ToastRunnable(activity, "✗ Google Direct Fehler: " + errorMsg));
        }

        @Override
        public void onSuccess(List<QuotaGroup> groups, String overallDescription, boolean isLive) {
            activity.mainHandler.post(new ToastRunnable(activity, "✓ Desktop-Bridge erfolgreich!"));
        }

        @Override
        public void onError(String errorMessage) {
            activity.mainHandler.post(new ToastRunnable(activity, "✗ Bridge Fehler: " + errorMessage));
        }
    }

    private static class MessageCenterDialogHolder implements View.OnClickListener {
        final MainActivity activity;
        final AlertDialog dialog;
        final RadioGroup rgNotifMode;
        final RadioButton rbModeIndividual;
        final RadioButton rbModeDigest;
        final CheckBox cbQuietHours;
        final CheckBox cbEarlyWarning;
        final Button btnInt1h;
        final Button btnInt2h;
        final Button btnInt4h;
        final Button btnIntDaily;
        int selectedInterval = 2;

        MessageCenterDialogHolder(MainActivity activity, AlertDialog dialog,
                                  RadioGroup rg, RadioButton rbIndiv, RadioButton rbDig,
                                  CheckBox cbQuiet, CheckBox cbEarly,
                                  Button b1, Button b2, Button b4, Button bDaily) {
            this.activity = activity;
            this.dialog = dialog;
            this.rgNotifMode = rg;
            this.rbModeIndividual = rbIndiv;
            this.rbModeDigest = rbDig;
            this.cbQuietHours = cbQuiet;
            this.cbEarlyWarning = cbEarly;
            this.btnInt1h = b1;
            this.btnInt2h = b2;
            this.btnInt4h = b4;
            this.btnIntDaily = bDaily;
            this.selectedInterval = MessageCenterManager.getDigestIntervalHours(activity);
            updateIntervalButtons();
        }

        void updateIntervalButtons() {
            btnInt1h.setBackgroundResource(selectedInterval == 1 ? R.drawable.button_background : R.drawable.button_secondary_background);
            btnInt1h.setTextColor(selectedInterval == 1 ? 0xFFFFFFFF : 0xFF94A3B8);

            btnInt2h.setBackgroundResource(selectedInterval == 2 ? R.drawable.button_background : R.drawable.button_secondary_background);
            btnInt2h.setTextColor(selectedInterval == 2 ? 0xFFFFFFFF : 0xFF94A3B8);

            btnInt4h.setBackgroundResource(selectedInterval == 4 ? R.drawable.button_background : R.drawable.button_secondary_background);
            btnInt4h.setTextColor(selectedInterval == 4 ? 0xFFFFFFFF : 0xFF94A3B8);

            btnIntDaily.setBackgroundResource(selectedInterval == 24 ? R.drawable.button_background : R.drawable.button_secondary_background);
            btnIntDaily.setTextColor(selectedInterval == 24 ? 0xFFFFFFFF : 0xFF94A3B8);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btnCancelMessageCenter) {
                dialog.dismiss();
            } else if (id == R.id.btnSaveMessageCenter) {
                int mode = rbModeDigest.isChecked() ? MessageCenterManager.MODE_DIGEST : MessageCenterManager.MODE_INDIVIDUAL;
                MessageCenterManager.setNotificationMode(activity, mode);
                MessageCenterManager.setDigestIntervalHours(activity, selectedInterval);
                MessageCenterManager.setQuietHoursEnabled(activity, cbQuietHours.isChecked());
                activity.prefs.edit().putBoolean(KEY_NOTIFY_LOW, cbEarlyWarning.isChecked()).apply();

                Toast.makeText(activity, "Message Center Einstellungen gespeichert", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else if (id == R.id.btnSendTestDigest) {
                List<QuotaBucket> allBuckets = activity.collectAllBuckets();
                MessageCenterManager.sendDigestNotification(activity, allBuckets);
                Toast.makeText(activity, "📦 Test-Sammelbenachrichtigung gesendet!", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.btnInt1h) {
                selectedInterval = 1;
                updateIntervalButtons();
            } else if (id == R.id.btnInt2h) {
                selectedInterval = 2;
                updateIntervalButtons();
            } else if (id == R.id.btnInt4h) {
                selectedInterval = 4;
                updateIntervalButtons();
            } else if (id == R.id.btnIntDaily) {
                selectedInterval = 24;
                updateIntervalButtons();
            }
        }
    }

    private static class RebuildUiRunnable implements Runnable {
        private final MainActivity activity;

        RebuildUiRunnable(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void run() {
            activity.rebuildCombinedUi();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences(PREF_CONFIG, MODE_PRIVATE);

        // Pre-seed default tokens if not already saved or empty
        if (prefs.getString(KEY_CLAUDE_ACCESS_TOKEN, "").isEmpty()) {
            prefs.edit()
                    .putString(KEY_CLAUDE_ACCESS_TOKEN, DEFAULT_CLAUDE_ACCESS)
                    .putString(KEY_CLAUDE_REFRESH_TOKEN, DEFAULT_CLAUDE_REFRESH)
                    .putString(KEY_GOOGLE_REFRESH_TOKEN, DEFAULT_GOOGLE_REFRESH)
                    .putString(KEY_HOST, DEFAULT_GOOGLE_HOST)
                    .putInt(KEY_PORT, DEFAULT_GOOGLE_PORT)
                    .putString(KEY_TOKEN, DEFAULT_GOOGLE_CSRF)
                    .putBoolean(KEY_NOTIFY_LOW, true)
                    .putBoolean(KEY_NOTIFY_FULL, true)
                    .putString(KEY_VIEW_MODE, "HORIZON")
                    .putString("last_claude_usage_json", ClaudeDirectClient.getInitialCachedJson())
                    .apply();
        }

        viewMode = prefs.getString(KEY_VIEW_MODE, "HORIZON");

        svRoot = findViewById(R.id.svRoot);
        tvStatus = findViewById(R.id.tvStatus);
        tvFooter = findViewById(R.id.tvFooter);
        tvPaceIndicator = findViewById(R.id.tvPaceIndicator);
        tvSavingsText = findViewById(R.id.tvSavingsText);
        llGroupsContainer = findViewById(R.id.llGroupsContainer);
        btnMessageCenter = findViewById(R.id.btnMessageCenter);
        btnShare = findViewById(R.id.btnShare);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnSettings = findViewById(R.id.btnSettings);
        btnQuickTimer = findViewById(R.id.btnQuickTimer);

        btnViewHorizon = findViewById(R.id.btnViewHorizon);
        btnViewProvider = findViewById(R.id.btnViewProvider);

        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterWeek = findViewById(R.id.chipFilterWeek);
        chipFilter5h = findViewById(R.id.chipFilter5h);
        chipFilterDay = findViewById(R.id.chipFilterDay);
        chipFilterClaude = findViewById(R.id.chipFilterClaude);
        chipFilterGoogle = findViewById(R.id.chipFilterGoogle);
        chipFilterCritical = findViewById(R.id.chipFilterCritical);

        btnMessageCenter.setOnClickListener(this);
        btnShare.setOnClickListener(this);
        btnRefresh.setOnClickListener(this);
        btnSettings.setOnClickListener(this);
        btnQuickTimer.setOnClickListener(this);

        btnViewHorizon.setOnClickListener(this);
        btnViewProvider.setOnClickListener(this);

        chipFilterAll.setOnClickListener(this);
        chipFilterWeek.setOnClickListener(this);
        chipFilter5h.setOnClickListener(this);
        chipFilterDay.setOnClickListener(this);
        chipFilterClaude.setOnClickListener(this);
        chipFilterGoogle.setOnClickListener(this);
        chipFilterCritical.setOnClickListener(this);

        quickTimerEndTime = prefs.getLong("quick_timer_end", 0L);

        applyOledMode();
        updateViewModeButtons();
        updateFilterChipStyles();

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }

        startTicker();

        // Load initial Claude state from cache immediately
        String cachedClaude = prefs.getString("last_claude_usage_json", null);
        if (cachedClaude != null) {
            try {
                cachedClaudeGroup = ClaudeDirectClient.parseClaudeUsage(new JSONObject(cachedClaude));
                claudeIsLive = true;
            } catch (Exception ignored) {}
        }

        loadQuotaData();
    }

    private void applyOledMode() {
        boolean oled = prefs.getBoolean(KEY_OLED_MODE, false);
        if (svRoot != null) {
            svRoot.setBackgroundColor(oled ? 0xFF000000 : 0xFF0E1015);
        }
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(oled ? 0xFF000000 : 0xFF0E1015));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnRefresh) {
            loadQuotaData();
        } else if (id == R.id.btnSettings) {
            showSettingsDialog();
        } else if (id == R.id.btnMessageCenter) {
            showMessageCenterDialog();
        } else if (id == R.id.btnShare) {
            shareQuotaStatus();
        } else if (id == R.id.btnQuickTimer) {
            toggleQuickTimer();
        } else if (id == R.id.btnViewHorizon) {
            viewMode = "HORIZON";
            prefs.edit().putString(KEY_VIEW_MODE, viewMode).apply();
            updateViewModeButtons();
            rebuildCombinedUi();
        } else if (id == R.id.btnViewProvider) {
            viewMode = "PROVIDER";
            prefs.edit().putString(KEY_VIEW_MODE, viewMode).apply();
            updateViewModeButtons();
            rebuildCombinedUi();
        } else if (id == R.id.chipFilterAll) {
            selectedFilter = "ALL";
            updateFilterChipStyles();
            rebuildCombinedUi();
        } else if (id == R.id.chipFilterWeek) {
            selectedFilter = "WEEK";
            updateFilterChipStyles();
            rebuildCombinedUi();
        } else if (id == R.id.chipFilter5h) {
            selectedFilter = "5H";
            updateFilterChipStyles();
            rebuildCombinedUi();
        } else if (id == R.id.chipFilterDay) {
            selectedFilter = "DAY";
            updateFilterChipStyles();
            rebuildCombinedUi();
        } else if (id == R.id.chipFilterClaude) {
            selectedFilter = "CLAUDE";
            updateFilterChipStyles();
            rebuildCombinedUi();
        } else if (id == R.id.chipFilterGoogle) {
            selectedFilter = "GOOGLE";
            updateFilterChipStyles();
            rebuildCombinedUi();
        } else if (id == R.id.chipFilterCritical) {
            selectedFilter = "CRITICAL";
            updateFilterChipStyles();
            rebuildCombinedUi();
        }
    }

    private void updateViewModeButtons() {
        boolean isHorizon = "HORIZON".equals(viewMode);
        btnViewHorizon.setBackgroundResource(isHorizon ? R.drawable.button_background : R.drawable.button_secondary_background);
        btnViewHorizon.setTextColor(isHorizon ? 0xFFFFFFFF : 0xFF94A3B8);

        btnViewProvider.setBackgroundResource(!isHorizon ? R.drawable.button_background : R.drawable.button_secondary_background);
        btnViewProvider.setTextColor(!isHorizon ? 0xFFFFFFFF : 0xFF94A3B8);
    }

    private void updateFilterChipStyles() {
        updateSingleChip(chipFilterAll, "ALL".equals(selectedFilter));
        updateSingleChip(chipFilterWeek, "WEEK".equals(selectedFilter));
        updateSingleChip(chipFilter5h, "5H".equals(selectedFilter));
        updateSingleChip(chipFilterDay, "DAY".equals(selectedFilter));
        updateSingleChip(chipFilterClaude, "CLAUDE".equals(selectedFilter));
        updateSingleChip(chipFilterGoogle, "GOOGLE".equals(selectedFilter));
        updateSingleChip(chipFilterCritical, "CRITICAL".equals(selectedFilter));
    }

    private void updateSingleChip(Button chip, boolean isSelected) {
        if (chip == null) return;
        chip.setBackgroundResource(isSelected ? R.drawable.button_background : R.drawable.button_secondary_background);
        chip.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF94A3B8);
    }

    private void toggleQuickTimer() {
        long now = System.currentTimeMillis();
        if (quickTimerEndTime > now) {
            quickTimerEndTime = 0L;
            prefs.edit().remove("quick_timer_end").apply();
            NotificationScheduler.cancelResetNotification(this, "quick_5h_timer");
            btnQuickTimer.setText("⏱️ 5h-Timer starten");
            btnQuickTimer.setTextColor(0xFF38BDF8);
            Toast.makeText(this, "⏱️ 5-Stunden-Timer abgebrochen", Toast.LENGTH_SHORT).show();
        } else {
            quickTimerEndTime = now + (5L * 3600L * 1000L);
            prefs.edit().putLong("quick_timer_end", quickTimerEndTime).apply();

            QuotaBucket quickBucket = new QuotaBucket();
            quickBucket.bucketId = "quick_5h_timer";
            quickBucket.displayName = "5-Stunden AI Limit";
            quickBucket.resetTimeMillis = quickTimerEndTime;
            quickBucket.groupName = "Manuell gesetzter Timer";
            NotificationScheduler.scheduleResetNotification(this, quickBucket);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.GERMAN);
            Toast.makeText(this, "⏱️ 5h-Timer aktiviert! Alarm um " + sdf.format(new Date(quickTimerEndTime)) + " Uhr", Toast.LENGTH_LONG).show();
        }
    }

    private void shareQuotaStatus() {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN);
        sb.append("📊 AI Limits Status (").append(sdf.format(new Date())).append(")\n");
        sb.append("-----------------------------\n");
        List<QuotaBucket> all = collectAllBuckets();
        for (QuotaBucket b : all) {
            int pct = Math.round(b.remainingFraction * 100f);
            sb.append("• ").append(b.displayName).append(": ").append(pct).append("% frei");
            String cd = b.formatCountdown();
            if (!cd.isEmpty() && !cd.equals("Wieder verfügbar!")) {
                sb.append(" (").append(cd).append(")");
            }
            sb.append("\n");
        }
        sb.append("-----------------------------\n");
        sb.append("Ermittelt direkt via AI Limits Mobile App");

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("AI Limits Status", sb.toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "📋 Status in Zwischenablage kopiert!", Toast.LENGTH_SHORT).show();
    }

    public List<QuotaBucket> collectAllBuckets() {
        List<QuotaBucket> list = new ArrayList<>();
        if (cachedClaudeGroup != null) list.addAll(cachedClaudeGroup.buckets);
        if (cachedGoogleDirectGroup != null) list.addAll(cachedGoogleDirectGroup.buckets);
        if (cachedLocalGroups != null) {
            for (QuotaGroup g : cachedLocalGroups) list.addAll(g.buckets);
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTicker();
    }

    private void startTicker() {
        mainHandler.post(this);
    }

    private void stopTicker() {
        mainHandler.removeCallbacks(this);
    }

    @Override
    public void run() {
        tickCount++;
        updateTickers();

        if (tickCount % 30 == 0) {
            loadQuotaData();
        }
        mainHandler.postDelayed(this, 1000);
    }

    private void updateTickers() {
        for (TickerItem item : tickerItems) {
            if (item.tvCountdown != null && item.bucket != null) {
                item.tvCountdown.setText(item.bucket.formatCountdown());
            }
            if (item.recoveryBar != null && item.bucket != null) {
                float recFraction = item.bucket.getRecoveryFraction();
                item.recoveryBar.setProgress(recFraction);
                if (item.tvRecoveryRatio != null) {
                    if (recFraction >= 0.999f) {
                        item.tvRecoveryRatio.setText("✅ 100% Bereit");
                        item.tvRecoveryRatio.setTextColor(0xFF10B981);
                    } else {
                        int pct = Math.round(recFraction * 100f);
                        item.tvRecoveryRatio.setText(String.format(Locale.US, "Erholung: %d%%", pct));
                        item.tvRecoveryRatio.setTextColor(RecoveryProgressBar.getRecoveryColor(recFraction));
                    }
                }
            }
        }

        long now = System.currentTimeMillis();
        if (quickTimerEndTime > now) {
            long diff = quickTimerEndTime - now;
            long h = diff / (3600L * 1000L);
            long m = (diff % (3600L * 1000L)) / (60L * 1000L);
            long s = (diff % (60L * 1000L)) / 1000L;
            btnQuickTimer.setText(String.format(Locale.US, "⏱️ 5h (%02d:%02d:%02d)", h, m, s));
            btnQuickTimer.setTextColor(0xFF10B981);
        } else if (quickTimerEndTime > 0) {
            quickTimerEndTime = 0L;
            prefs.edit().remove("quick_timer_end").apply();
            btnQuickTimer.setText("⏱️ 5h-Timer starten");
            btnQuickTimer.setTextColor(0xFF38BDF8);
        }
    }

    public void loadQuotaData() {
        tvStatus.setText("● Lade Claude Pro & Google Cloud…");
        tvStatus.setTextColor(0xFFF59E0B);

        String host = prefs.getString(KEY_HOST, DEFAULT_GOOGLE_HOST);
        int port = prefs.getInt(KEY_PORT, DEFAULT_GOOGLE_PORT);
        String token = prefs.getString(KEY_TOKEN, DEFAULT_GOOGLE_CSRF);

        ClaudeDirectClient.fetchClaudeGroup(this, this);
        GoogleDirectClient.fetchGoogleDirect(this, this);
        QuotaApiClient.fetchQuota(host, port, token, this);
    }

    @Override
    public void onClaudeSuccess(QuotaGroup claudeGroup) {
        this.cachedClaudeGroup = claudeGroup;
        this.claudeIsLive = true;
        Log.d(TAG, "Claude Pro successfully fetched directly from cloud");
        checkNotificationThresholds(claudeGroup);
        mainHandler.post(new RebuildUiRunnable(this));
    }

    @Override
    public void onClaudeError(String errorMsg) {
        Log.e(TAG, "Claude Pro direct fetch error: " + errorMsg);
        this.claudeIsLive = false;
        mainHandler.post(new RebuildUiRunnable(this));
    }

    @Override
    public void onGoogleDirectSuccess(QuotaGroup group, String email, String projectName) {
        this.cachedGoogleDirectGroup = group;
        this.googleDirectIsLive = true;
        Log.d(TAG, "Google Direct Cloud connected: " + email);
        mainHandler.post(new RebuildUiRunnable(this));
    }

    @Override
    public void onGoogleDirectError(String errorMsg) {
        Log.e(TAG, "Google Direct fetch error: " + errorMsg);
        this.googleDirectIsLive = false;
        mainHandler.post(new RebuildUiRunnable(this));
    }

    @Override
    public void onSuccess(List<QuotaGroup> groups, String overallDescription, boolean isLive) {
        this.cachedLocalGroups = groups;
        this.localIsLive = isLive;
        this.googleOverallDescription = overallDescription;
        Log.d(TAG, "Desktop Bridge groups fetched: " + groups.size());
        mainHandler.post(new RebuildUiRunnable(this));
    }

    @Override
    public void onError(String errorMessage) {
        Log.d(TAG, "Desktop Bridge offline (normal when unterwegs): " + errorMessage);
        this.localIsLive = false;
        mainHandler.post(new RebuildUiRunnable(this));
    }

    private void checkNotificationThresholds(QuotaGroup group) {
        if (!prefs.getBoolean(KEY_NOTIFY_LOW, true) || group == null) return;
        for (QuotaBucket b : group.buckets) {
            if ("claude_pro_5h".equals(b.bucketId)) {
                if (b.remainingFraction <= 0.15f && b.remainingFraction > 0.0f) {
                    boolean alreadyWarned = prefs.getBoolean("warned_low_5h", false);
                    if (!alreadyWarned) {
                        ResetReceiver.showNotification(
                                this,
                                "⚠️ AI Limits Frühwarnung",
                                "Dein Claude Pro 5-Stunden-Limit ist fast aufgebraucht (< 15% übrig)!"
                        );
                        prefs.edit().putBoolean("warned_low_5h", true).apply();
                    }
                } else if (b.remainingFraction > 0.40f) {
                    prefs.edit().putBoolean("warned_low_5h", false).apply();
                }
            }
        }
    }

    public void rebuildCombinedUi() {
        List<QuotaGroup> combined = new ArrayList<>();

        if ("HORIZON".equals(viewMode)) {
            // BUILD TIME-HORIZON VIEW: Weekly limits first, then 5-hour limits, then daily
            combined = buildHorizonView();
        } else {
            // BUILD PROVIDER VIEW: Grouped by Claude, Google, etc.
            if (cachedClaudeGroup != null && shouldIncludeInFilter(cachedClaudeGroup, selectedFilter)) {
                combined.add(cachedClaudeGroup);
            }
            if (cachedGoogleDirectGroup != null && shouldIncludeInFilter(cachedGoogleDirectGroup, selectedFilter)) {
                combined.add(cachedGoogleDirectGroup);
            }
            if (cachedLocalGroups != null) {
                for (QuotaGroup lg : cachedLocalGroups) {
                    if (shouldIncludeInFilter(lg, selectedFilter)) {
                        combined.add(lg);
                    }
                }
            }
        }

        // Status Bar Update
        if (claudeIsLive && googleDirectIsLive) {
            tvStatus.setText("● Claude Cloud & Google Cloud Live");
            tvStatus.setTextColor(0xFF10B981);
        } else if (claudeIsLive) {
            tvStatus.setText("● Claude Cloud Live • Google Standby");
            tvStatus.setTextColor(0xFF38BDF8);
        } else if (googleDirectIsLive) {
            tvStatus.setText("● Google Cloud Live • Claude Standby");
            tvStatus.setTextColor(0xFF38BDF8);
        } else {
            tvStatus.setText("● Offline / Demo-Modus");
            tvStatus.setTextColor(0xFFEF4444);
        }

        updatePaceAndSavings();
        QuotaWidgetProvider.updateAllWidgets(this);

        renderQuotaGroups(combined, googleOverallDescription);
    }

    // Build Time-Horizon View (Wochenlimits zuerst, dann 5h, dann Tag)
    private List<QuotaGroup> buildHorizonView() {
        List<QuotaGroup> result = new ArrayList<>();
        List<QuotaBucket> all = collectAllBuckets();

        // 1. Weekly Limits Group (7 Tage & Langzeit-Kapazität)
        QuotaGroup weeklyGroup = new QuotaGroup("🗓️ Wochenlimits (7 Tage Langzeit-Kapazität)",
                "Gesamte verfügbare Wochenkapazität über alle Anbieter & Modelle");
        weeklyGroup.models.add("Claude 3.5 Sonnet");
        weeklyGroup.models.add("Gemini 3.8 Pro");
        weeklyGroup.models.add("Gemini 3.8 Flash");
        weeklyGroup.models.add("Gemini 1.5 Flash");
        weeklyGroup.models.add("Gemini 4 Argon");

        // 2. 5-Hour Limits Group (Rolling Windows & Sofort-Puffer)
        QuotaGroup fiveHourGroup = new QuotaGroup("⏱️ 5-Stunden-Limits (Sofort verfügbar)",
                "Rolling Windows für intensive Programmier- & Agenten-Sessions");
        fiveHourGroup.models.add("Claude Code (Anthropic)");
        fiveHourGroup.models.add("Gemini 3.8 (Antigravity IDE)");
        fiveHourGroup.models.add("Claude & GPT (Google IDE)");
        fiveHourGroup.models.add("Gemini 1.5 Pro (Cloud Direkt)");

        // 3. Daily Limits Group (24h / Midnight Reset)
        QuotaGroup dailyGroup = new QuotaGroup("☀️ Tageslimits (Reset um Mitternacht UTC)",
                "Tägliche Kontingente & Request-Limits (RPD)");
        dailyGroup.models.add("Google Cloud Developer API");

        for (QuotaBucket b : all) {
            if (matchesBucketFilter(b, selectedFilter)) {
                String wLetter = b.getWindowBadgeLetter();
                if ("W".equals(wLetter)) {
                    weeklyGroup.buckets.add(b);
                } else if ("T".equals(wLetter)) {
                    dailyGroup.buckets.add(b);
                } else {
                    fiveHourGroup.buckets.add(b);
                }
            }
        }

        // Add to result if buckets exist
        if (!weeklyGroup.buckets.isEmpty()) {
            float avgWeeklyFree = 0f;
            for (QuotaBucket b : weeklyGroup.buckets) avgWeeklyFree += b.remainingFraction;
            avgWeeklyFree = avgWeeklyFree / weeklyGroup.buckets.size();
            weeklyGroup.description = String.format(Locale.GERMAN,
                    "Verfügbare Gesamtkapazität: %.0f%% frei • Claude Pro, Google IDE & Cloud Direkt",
                    avgWeeklyFree * 100f);
            result.add(weeklyGroup);
        }

        if (!fiveHourGroup.buckets.isEmpty()) {
            result.add(fiveHourGroup);
        }

        if (!dailyGroup.buckets.isEmpty()) {
            result.add(dailyGroup);
        }

        return result;
    }

    private boolean shouldIncludeInFilter(QuotaGroup group, String filter) {
        if ("ALL".equals(filter)) return true;
        if ("CLAUDE".equals(filter)) {
            String dn = group.displayName.toLowerCase(Locale.ROOT);
            return dn.contains("claude pro") || (dn.contains("anthropic") && !dn.contains("ide"));
        }
        if ("GOOGLE".equals(filter)) {
            String dn = group.displayName.toLowerCase(Locale.ROOT);
            return dn.contains("google") || dn.contains("gemini") || dn.contains("ide");
        }
        for (QuotaBucket b : group.buckets) {
            if (matchesBucketFilter(b, filter)) return true;
        }
        return false;
    }

    private boolean matchesBucketFilter(QuotaBucket b, String filter) {
        if ("ALL".equals(filter)) return true;
        if ("WEEK".equals(filter)) return "W".equals(b.getWindowBadgeLetter());
        if ("5H".equals(filter)) return "5".equals(b.getWindowBadgeLetter());
        if ("DAY".equals(filter)) return "T".equals(b.getWindowBadgeLetter());
        if ("CLAUDE".equals(filter)) return "CLAUDE".equals(b.getProviderType());
        if ("GOOGLE".equals(filter)) return "GOOGLE_CLOUD".equals(b.getProviderType()) || "GOOGLE_IDE".equals(b.getProviderType());
        if ("CRITICAL".equals(filter)) return b.remainingFraction <= 0.25f;
        return true;
    }

    private void updatePaceAndSavings() {
        if (cachedClaudeGroup != null && !cachedClaudeGroup.buckets.isEmpty()) {
            QuotaBucket b5h = null;
            QuotaBucket b7d = null;
            for (QuotaBucket b : cachedClaudeGroup.buckets) {
                if ("claude_pro_5h".equals(b.bucketId)) b5h = b;
                if ("claude_pro_7d".equals(b.bucketId)) b7d = b;
            }

            if (b5h != null) {
                float util = (1.0f - b5h.remainingFraction) * 100f;
                if (util > 80f) {
                    tvPaceIndicator.setText("🔥 Hoher Verbrauch: Pause empfohlen");
                    tvPaceIndicator.setTextColor(0xFFEF4444);
                } else if (util > 30f) {
                    tvPaceIndicator.setText("⚡ Tempo: Optimal & Ausgewogen");
                    tvPaceIndicator.setTextColor(0xFF10B981);
                } else {
                    tvPaceIndicator.setText("🟢 Geringer Verbrauch: Volle Power");
                    tvPaceIndicator.setTextColor(0xFF38BDF8);
                }
            }

            if (b7d != null && b7d.remainingFraction <= 0.05f) {
                tvSavingsText.setText("Woche: ~38,50 € API-Gegenwert (Claude Code 97%) • Über 16,50 € Ersparnis mit Flatrate!");
            } else {
                tvSavingsText.setText("Diesen Monat: ~44,50 € API-Tokenwert verbraucht • Über 22,50 € Ersparnis mit Flatrate!");
            }
        }
    }

    public void renderQuotaGroups(List<QuotaGroup> groups, String overallDescription) {
        tickerItems.clear();
        llGroupsContainer.removeAllViews();

        if (overallDescription != null && !overallDescription.isEmpty()) {
            tvFooter.setText(overallDescription);
        }

        for (final QuotaGroup group : groups) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_background);
            int p = dp(16);
            card.setPadding(p, p, p, p);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = dp(16);
            card.setLayoutParams(lp);

            // Group Title
            TextView tvGroupTitle = new TextView(this);
            tvGroupTitle.setText(group.displayName);
            tvGroupTitle.setTextColor(0xFFF8FAFC);
            tvGroupTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvGroupTitle.setTypeface(null, Typeface.BOLD);
            card.addView(tvGroupTitle);

            // Model chips row
            List<String> modelNames = group.extractModelNames();
            if (!modelNames.isEmpty()) {
                LinearLayout chipsRow = new LinearLayout(this);
                chipsRow.setOrientation(LinearLayout.HORIZONTAL);
                chipsRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                ((LinearLayout.LayoutParams) chipsRow.getLayoutParams()).topMargin = dp(6);

                for (String modelName : modelNames) {
                    TextView chip = new TextView(this);
                    chip.setText(modelName);
                    chip.setTextColor(0xFF94A3B8);
                    chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                    chip.setBackgroundResource(R.drawable.badge_background);
                    chip.setPadding(dp(8), dp(3), dp(8), dp(3));

                    LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    chipLp.rightMargin = dp(6);
                    chipsRow.addView(chip, chipLp);
                }
                card.addView(chipsRow);
            }

            // Description
            if (group.description != null && !group.description.isEmpty()) {
                TextView tvDesc = new TextView(this);
                tvDesc.setText(group.description);
                tvDesc.setTextColor(0xFF64748B);
                tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                descLp.topMargin = dp(8);
                card.addView(tvDesc, descLp);
            }

            // Buckets
            for (final QuotaBucket bucket : group.buckets) {
                LinearLayout bucketCard = new LinearLayout(this);
                bucketCard.setOrientation(LinearLayout.VERTICAL);
                bucketCard.setBackgroundColor(0xFF0F1218);
                bucketCard.setPadding(dp(12), dp(12), dp(12), dp(12));

                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                bLp.topMargin = dp(10);
                bucketCard.setLayoutParams(bLp);

                // Top section: Left Usage Ring, Right info column
                LinearLayout topRow = new LinearLayout(this);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);
                topRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                // 1. LINKS: IMMER USAGE RING
                final CircularProgressView cp = new CircularProgressView(this);
                int size = dp(76);
                LinearLayout.LayoutParams cpLp = new LinearLayout.LayoutParams(size, size);
                cpLp.rightMargin = dp(12);
                cp.setLayoutParams(cpLp);
                cp.setProgress(bucket.remainingFraction);
                topRow.addView(cp);

                // 2. RECHTE SPALTE
                LinearLayout detailsCol = new LinearLayout(this);
                detailsCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams detLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                detailsCol.setLayoutParams(detLp);

                // Header line: Provider Badge + Window Badge (5 / W / T) + Title + Alarm Button
                LinearLayout headerLine = new LinearLayout(this);
                headerLine.setOrientation(LinearLayout.HORIZONTAL);
                headerLine.setGravity(Gravity.CENTER_VERTICAL);
                headerLine.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                // Provider-Symbol
                TextView badgeProvider = new TextView(this);
                String provider = bucket.getProviderType();
                if ("CLAUDE".equals(provider)) {
                    badgeProvider.setText("🟣 Claude Pro");
                    badgeProvider.setTextColor(0xFFD8B4FE);
                    badgeProvider.setBackgroundResource(R.drawable.badge_claude);
                } else if ("GOOGLE_IDE".equals(provider)) {
                    badgeProvider.setText("🔵 Google IDE");
                    badgeProvider.setTextColor(0xFF7DD3FC);
                    badgeProvider.setBackgroundResource(R.drawable.badge_google);
                } else {
                    badgeProvider.setText("🔵 Cloud Direkt");
                    badgeProvider.setTextColor(0xFF38BDF8);
                    badgeProvider.setBackgroundResource(R.drawable.badge_google);
                }
                badgeProvider.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                badgeProvider.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams bpLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                bpLp.rightMargin = dp(6);
                headerLine.addView(badgeProvider, bpLp);

                // Offensichtliche 5 bzw. W (bzw. T) Kontingentart-Badge
                TextView badgeWindow = new TextView(this);
                String badgeLetter = bucket.getWindowBadgeLetter();
                if ("5".equals(badgeLetter)) {
                    badgeWindow.setText(" 5h ");
                    badgeWindow.setTextColor(0xFF000000);
                    badgeWindow.setBackgroundResource(R.drawable.badge_window_5h);
                } else if ("W".equals(badgeLetter)) {
                    badgeWindow.setText(" W ");
                    badgeWindow.setTextColor(0xFFFFFFFF);
                    badgeWindow.setBackgroundResource(R.drawable.badge_window_week);
                } else {
                    badgeWindow.setText(" T ");
                    badgeWindow.setTextColor(0xFF000000);
                    badgeWindow.setBackgroundResource(R.drawable.badge_window_day);
                }
                badgeWindow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                badgeWindow.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams bwLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                bwLp.rightMargin = dp(6);
                headerLine.addView(badgeWindow, bwLp);

                // Title
                TextView tvBTitle = new TextView(this);
                tvBTitle.setText(bucket.displayName);
                tvBTitle.setTextColor(0xFFF8FAFC);
                tvBTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                tvBTitle.setTypeface(null, Typeface.BOLD);
                tvBTitle.setSingleLine(true);
                tvBTitle.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                tvBTitle.setLayoutParams(titleLp);
                headerLine.addView(tvBTitle);

                // Alarm Button
                final Button btnAlarm = new Button(this);
                boolean isArmed = NotificationScheduler.isAlarmScheduled(this, bucket.bucketId);
                updateAlarmButtonState(btnAlarm, isArmed);
                btnAlarm.setOnClickListener(new AlarmClickListener(this, bucket, btnAlarm));
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(26)
                );
                btnLp.leftMargin = dp(6);
                headerLine.addView(btnAlarm, btnLp);

                detailsCol.addView(headerLine);

                // Erklärungstext welches Model bzw. welche Modellgruppe
                TextView tvModelExp = new TextView(this);
                tvModelExp.setText(bucket.getModelGroupExplanation());
                tvModelExp.setTextColor(0xFF94A3B8);
                tvModelExp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                LinearLayout.LayoutParams expLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                expLp.topMargin = dp(4);
                detailsCol.addView(tvModelExp, expLp);

                // Live-Nutzung & Session-Anzeige
                TextView tvSession = new TextView(this);
                String sessionDesc = bucket.getActiveSessionDescription();
                tvSession.setText(sessionDesc);
                if (sessionDesc.contains("LIVE AKTIV")) {
                    tvSession.setTextColor(0xFF34D399); // Crisp Emerald
                } else {
                    tvSession.setTextColor(0xFF64748B); // Slate Muted
                }
                tvSession.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                tvSession.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams sessLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                sessLp.topMargin = dp(3);
                detailsCol.addView(tvSession, sessLp);

                topRow.addView(detailsCol);
                bucketCard.addView(topRow);

                // 3. UNTEN: FORTSCHRITTSBALKEN BIS ES WIEDER VERFÜGBAR IST
                // Fließend von Rot (noch lange) -> Gelb (halb) -> Grün (noch kurz)
                final RecoveryProgressBar recoveryBar = new RecoveryProgressBar(this);
                float recoveryFraction = bucket.getRecoveryFraction();
                recoveryBar.setProgress(recoveryFraction);
                LinearLayout.LayoutParams recLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(7)
                );
                recLp.topMargin = dp(10);
                bucketCard.addView(recoveryBar, recLp);

                // Statuszeile unter dem Recovery-Balken
                LinearLayout statusRow = new LinearLayout(this);
                statusRow.setOrientation(LinearLayout.HORIZONTAL);
                statusRow.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams statLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                statLp.topMargin = dp(4);
                statusRow.setLayoutParams(statLp);

                final TextView tvCountdown = new TextView(this);
                tvCountdown.setText(bucket.formatCountdown());
                tvCountdown.setTextColor(bucket.remainingFraction > 0.5f ? 0xFF10B981 : (bucket.remainingFraction > 0.15f ? 0xFFF59E0B : 0xFFEF4444));
                tvCountdown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                tvCountdown.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams cdLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                statusRow.addView(tvCountdown, cdLp);

                final TextView tvRecoveryRatio = new TextView(this);
                if (recoveryFraction >= 0.999f) {
                    tvRecoveryRatio.setText("✅ 100% Bereit");
                    tvRecoveryRatio.setTextColor(0xFF10B981);
                } else {
                    int pct = Math.round(recoveryFraction * 100f);
                    tvRecoveryRatio.setText(String.format(Locale.US, "Erholung: %d%%", pct));
                    tvRecoveryRatio.setTextColor(RecoveryProgressBar.getRecoveryColor(recoveryFraction));
                }
                tvRecoveryRatio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                tvRecoveryRatio.setTypeface(null, Typeface.BOLD);
                statusRow.addView(tvRecoveryRatio);

                bucketCard.addView(statusRow);

                card.addView(bucketCard);

                tickerItems.add(new TickerItem(bucket, tvCountdown, cp, recoveryBar, tvRecoveryRatio, btnAlarm));
            }

            llGroupsContainer.addView(card);
        }
    }

    public void updateAlarmButtonState(Button btn, boolean isArmed) {
        btn.setText(isArmed ? "🔔 An" : "🔔 Aus");
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btn.setTextColor(isArmed ? 0xFF10B981 : 0xFF94A3B8);
        btn.setBackgroundResource(isArmed ? R.drawable.badge_background : R.drawable.button_secondary_background);
        btn.setPadding(dp(8), dp(2), dp(8), dp(2));
    }

    private void showMessageCenterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_message_center, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        RadioGroup rg = dialogView.findViewById(R.id.rgNotifMode);
        RadioButton rbIndiv = dialogView.findViewById(R.id.rbModeIndividual);
        RadioButton rbDig = dialogView.findViewById(R.id.rbModeDigest);

        int currentMode = MessageCenterManager.getNotificationMode(this);
        if (currentMode == MessageCenterManager.MODE_DIGEST) {
            rbDig.setChecked(true);
        } else {
            rbIndiv.setChecked(true);
        }

        Button b1 = dialogView.findViewById(R.id.btnInt1h);
        Button b2 = dialogView.findViewById(R.id.btnInt2h);
        Button b4 = dialogView.findViewById(R.id.btnInt4h);
        Button bDaily = dialogView.findViewById(R.id.btnIntDaily);

        CheckBox cbQuiet = dialogView.findViewById(R.id.cbQuietHours);
        cbQuiet.setChecked(MessageCenterManager.isQuietHoursEnabled(this));

        CheckBox cbEarly = dialogView.findViewById(R.id.cbEarlyWarning);
        cbEarly.setChecked(prefs.getBoolean(KEY_NOTIFY_LOW, true));

        // Render recent event logs
        LinearLayout llFeed = dialogView.findViewById(R.id.llEventLogContainer);
        List<MessageCenterManager.EventLog> logs = MessageCenterManager.getRecentLogs(this);
        for (MessageCenterManager.EventLog log : logs) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(6), dp(4), dp(6), dp(4));

            TextView tvHeader = new TextView(this);
            tvHeader.setText(log.formatTime() + " • " + log.title);
            tvHeader.setTextColor(0xFF38BDF8);
            tvHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvHeader.setTypeface(null, Typeface.BOLD);
            row.addView(tvHeader);

            TextView tvBody = new TextView(this);
            tvBody.setText(log.message);
            tvBody.setTextColor(0xFF94A3B8);
            tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            row.addView(tvBody);

            llFeed.addView(row);
        }

        MessageCenterDialogHolder holder = new MessageCenterDialogHolder(
                this, dialog, rg, rbIndiv, rbDig, cbQuiet, cbEarly, b1, b2, b4, bDaily
        );

        b1.setOnClickListener(holder);
        b2.setOnClickListener(holder);
        b4.setOnClickListener(holder);
        bDaily.setOnClickListener(holder);

        Button btnTestDigest = dialogView.findViewById(R.id.btnSendTestDigest);
        Button btnSave = dialogView.findViewById(R.id.btnSaveMessageCenter);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelMessageCenter);

        btnTestDigest.setOnClickListener(holder);
        btnSave.setOnClickListener(holder);
        btnCancel.setOnClickListener(holder);

        dialog.show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText etClaudeRefresh = dialogView.findViewById(R.id.etClaudeRefresh);
        EditText etGoogleRefresh = dialogView.findViewById(R.id.etGoogleRefresh);
        EditText etHost = dialogView.findViewById(R.id.etHost);
        EditText etPort = dialogView.findViewById(R.id.etPort);
        EditText etToken = dialogView.findViewById(R.id.etToken);

        CheckBox cbOled = dialogView.findViewById(R.id.cbOledMode);
        CheckBox cbNotifyLow = dialogView.findViewById(R.id.cbNotifyLow);
        CheckBox cbNotifyFull = dialogView.findViewById(R.id.cbNotifyFull);

        Button btnTestClaude = dialogView.findViewById(R.id.btnTestClaude);
        Button btnTestGoogleDirect = dialogView.findViewById(R.id.btnTestGoogleDirect);
        Button btnTestConn = dialogView.findViewById(R.id.btnTestConnection);
        Button btnTestNotif = dialogView.findViewById(R.id.btnTestNotification);
        Button btnSave = dialogView.findViewById(R.id.btnSaveSettings);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelSettings);

        etClaudeRefresh.setText(prefs.getString(KEY_CLAUDE_REFRESH_TOKEN, DEFAULT_CLAUDE_REFRESH));
        etGoogleRefresh.setText(prefs.getString(KEY_GOOGLE_REFRESH_TOKEN, DEFAULT_GOOGLE_REFRESH));
        etHost.setText(prefs.getString(KEY_HOST, DEFAULT_GOOGLE_HOST));
        etPort.setText(String.valueOf(prefs.getInt(KEY_PORT, DEFAULT_GOOGLE_PORT)));
        etToken.setText(prefs.getString(KEY_TOKEN, DEFAULT_GOOGLE_CSRF));

        cbOled.setChecked(prefs.getBoolean(KEY_OLED_MODE, false));
        cbNotifyLow.setChecked(prefs.getBoolean(KEY_NOTIFY_LOW, true));
        cbNotifyFull.setChecked(prefs.getBoolean(KEY_NOTIFY_FULL, true));

        SettingsDialogHolder holder = new SettingsDialogHolder(
                this, dialog,
                etClaudeRefresh, etGoogleRefresh,
                etHost, etPort, etToken,
                cbOled, cbNotifyLow, cbNotifyFull
        );

        btnTestClaude.setOnClickListener(holder);
        btnTestGoogleDirect.setOnClickListener(holder);
        btnTestConn.setOnClickListener(holder);
        btnTestNotif.setOnClickListener(holder);
        btnSave.setOnClickListener(holder);
        btnCancel.setOnClickListener(holder);

        dialog.show();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
