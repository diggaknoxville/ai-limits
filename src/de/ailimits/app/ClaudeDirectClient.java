package de.ailimits.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClaudeDirectClient {

    private static final String TAG = "ClaudeDirect";
    public static final String CLIENT_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e";
    public static final String USAGE_URL = "https://api.anthropic.com/api/oauth/usage";
    public static final String TOKEN_URL = "https://console.anthropic.com/v1/oauth/token";

    private static long rateLimitedUntil = 0L;

    public interface ClaudeUsageCallback {
        void onClaudeSuccess(QuotaGroup claudeGroup);
        void onClaudeError(String errorMsg);
    }

    private static class ClaudeWorker implements Runnable {
        private final Context context;
        private final ClaudeUsageCallback callback;

        ClaudeWorker(Context context, ClaudeUsageCallback callback) {
            this.context = context;
            this.callback = callback;
        }

        @Override
        public void run() {
            SharedPreferences sp = context.getSharedPreferences("ai_limits_config", Context.MODE_PRIVATE);
            String accessToken = sp.getString("claude_access_token", "");
            if (accessToken.isEmpty()) accessToken = MainActivity.DEFAULT_CLAUDE_ACCESS;
            String refreshToken = sp.getString("claude_refresh_token", "");
            if (refreshToken.isEmpty()) refreshToken = MainActivity.DEFAULT_CLAUDE_REFRESH;

            if (accessToken.isEmpty() && refreshToken.isEmpty()) {
                callback.onClaudeError("Kein Claude Token hinterlegt");
                return;
            }

            // Check rate-limit cooldown
            if (System.currentTimeMillis() < rateLimitedUntil) {
                Log.d(TAG, "Cooling down after 429 rate limit, using cached usage");
                String cached = sp.getString("last_claude_usage_json", null);
                if (cached != null) {
                    try {
                        callback.onClaudeSuccess(parseClaudeUsage(new JSONObject(cached)));
                        return;
                    } catch (Exception ignored) {}
                }
            }

            try {
                FetchResult result = fetchUsage(context, accessToken);

                if (result.code == 401 && !refreshToken.isEmpty()) {
                    Log.d(TAG, "401 Unauthorized: Refreshing Claude access token...");
                    String newAccess = doRefreshToken(context, refreshToken);
                    if (newAccess != null) {
                        accessToken = newAccess;
                        result = fetchUsage(context, accessToken);
                    }
                }

                if (result.data != null) {
                    QuotaGroup group = parseClaudeUsage(result.data);
                    callback.onClaudeSuccess(group);
                } else {
                    // Fall back to cached data if available
                    String cached = sp.getString("last_claude_usage_json", null);
                    if (cached != null) {
                        callback.onClaudeSuccess(parseClaudeUsage(new JSONObject(cached)));
                    } else {
                        callback.onClaudeError("Claude API: Status " + result.code);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Claude fetch error: " + e.getMessage(), e);
                // Try cache
                String cached = sp.getString("last_claude_usage_json", null);
                if (cached != null) {
                    try {
                        callback.onClaudeSuccess(parseClaudeUsage(new JSONObject(cached)));
                        return;
                    } catch (Exception ignored) {}
                }
                callback.onClaudeError("Claude Fehler: " + e.getMessage());
            }
        }
    }

    private static class FetchResult {
        int code;
        JSONObject data;

        FetchResult(int code, JSONObject data) {
            this.code = code;
            this.data = data;
        }
    }

    public static void fetchClaudeGroup(Context context, ClaudeUsageCallback callback) {
        new Thread(new ClaudeWorker(context, callback)).start();
    }

    private static FetchResult fetchUsage(Context context, String accessToken) {
        try {
            URL url = new URL(USAGE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("anthropic-beta", "oauth-2025-04-20");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "claude-code");

            int code = conn.getResponseCode();
            Log.d(TAG, "Claude usage HTTP code: " + code);

            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();

                SharedPreferences sp = context.getSharedPreferences("ai_limits_config", Context.MODE_PRIVATE);
                sp.edit().putString("last_claude_usage_json", sb.toString()).apply();
                return new FetchResult(code, new JSONObject(sb.toString()));
            } else if (code == 429) {
                rateLimitedUntil = System.currentTimeMillis() + (300 * 1000); // 5m cooldown
                Log.w(TAG, "429 Rate limited. Cooling down 5m.");
                return new FetchResult(code, null);
            } else {
                return new FetchResult(code, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchUsage exception: " + e.getMessage());
            return new FetchResult(0, null);
        }
    }

    private static String doRefreshToken(Context context, String refreshToken) {
        try {
            URL url = new URL(TOKEN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "claude-code");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("refresh_token", refreshToken);
            body.put("grant_type", "refresh_token");
            body.put("client_id", CLIENT_ID);

            byte[] b = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(b.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(b);
            }

            int code = conn.getResponseCode();
            Log.d(TAG, "Refresh HTTP code: " + code);
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                JSONObject resp = new JSONObject(sb.toString());
                String newAccess = resp.optString("access_token", null);
                if (newAccess != null && !newAccess.isEmpty()) {
                    SharedPreferences sp = context.getSharedPreferences("ai_limits_config", Context.MODE_PRIVATE);
                    sp.edit().putString("claude_access_token", newAccess).apply();
                    return newAccess;
                }
            } else {
                if (conn.getErrorStream() != null) {
                    BufferedReader errBr = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errSb = new StringBuilder();
                    String errLine;
                    while ((errLine = errBr.readLine()) != null) {
                        errSb.append(errLine);
                    }
                    errBr.close();
                    Log.e(TAG, "Refresh Error Body: " + errSb.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "RefreshToken exception: " + e.getMessage());
        }
        return null;
    }

    public static QuotaGroup parseClaudeUsage(JSONObject data) {
        QuotaGroup group = new QuotaGroup("Claude Pro (Anthropic)", "Official Claude Pro Account • Direct Cloud API");

        // 5-Hour Limit
        JSONObject fiveH = data.optJSONObject("five_hour");
        if (fiveH != null) {
            double util = fiveH.optDouble("utilization", 0.0);
            String resetsAt = fiveH.optString("resets_at", "");

            QuotaBucket b5h = new QuotaBucket();
            b5h.bucketId = "claude_pro_5h";
            b5h.displayName = "Claude Pro 5-Hour Limit";
            b5h.window = "5h";
            b5h.remainingFraction = (float) Math.max(0.0, Math.min(1.0, util / 100.0));
            b5h.rawResetTime = resetsAt;
            b5h.resetTimeMillis = QuotaBucket.parseIsoTime(resetsAt);
            if (b5h.resetTimeMillis > 0 && System.currentTimeMillis() >= b5h.resetTimeMillis) {
                b5h.remainingFraction = 1.0f;
            }
            b5h.groupName = group.displayName;
            b5h.description = String.format(java.util.Locale.US, "%.0f%% remaining", util);

            group.buckets.add(b5h);
        }

        // 7-Day Limit
        JSONObject sevenD = data.optJSONObject("seven_day");
        if (sevenD != null) {
            double util = sevenD.optDouble("utilization", 0.0);
            String resetsAt = sevenD.optString("resets_at", "");

            QuotaBucket b7d = new QuotaBucket();
            b7d.bucketId = "claude_pro_7d";
            b7d.displayName = "Claude Pro Weekly Limit (7-Day)";
            b7d.window = "weekly";
            b7d.remainingFraction = (float) Math.max(0.0, Math.min(1.0, util / 100.0));
            b7d.rawResetTime = resetsAt;
            b7d.resetTimeMillis = QuotaBucket.parseIsoTime(resetsAt);
            if (b7d.resetTimeMillis > 0 && System.currentTimeMillis() >= b7d.resetTimeMillis) {
                b7d.remainingFraction = 1.0f;
            }
            b7d.groupName = group.displayName;
            b7d.description = String.format(java.util.Locale.US, "%.0f%% remaining", util);

            group.buckets.add(b7d);
        }

        // Breakdown row
        JSONObject breakdown = data.optJSONObject("seven_day_breakdown");
        if (breakdown != null) {
            JSONArray rows = breakdown.optJSONArray("rows");
            if (rows != null) {
                StringBuilder sb = new StringBuilder("Aufteilung: ");
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject r = rows.optJSONObject(i);
                    if (r != null) {
                        String name = r.optString("display_name", "");
                        int pct = r.optInt("percent", 0);
                        if (pct > 0 || i == 0) {
                            if (i > 0) sb.append(" • ");
                            sb.append(name).append(": ").append(pct).append("%");
                        }
                    }
                }
                group.description = group.description + "\n" + sb.toString();
            }
        }

        return group;
    }

    public static String getInitialCachedJson() {
        return "{\n" +
                "  \"five_hour\": {\n" +
                "    \"utilization\": 43.0,\n" +
                "    \"resets_at\": \"2026-10-21T19:20:00.193377+00:00\"\n" +
                "  },\n" +
                "  \"seven_day\": {\n" +
                "    \"utilization\": 100.0,\n" +
                "    \"resets_at\": \"2026-10-21T23:00:00.193394+00:00\"\n" +
                "  },\n" +
                "  \"seven_day_breakdown\": {\n" +
                "    \"rows\": [\n" +
                "      { \"display_name\": \"Claude Code\", \"percent\": 97 },\n" +
                "      { \"display_name\": \"Chats\", \"percent\": 0 },\n" +
                "      { \"display_name\": \"Cowork\", \"percent\": 3 }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
    }
}

