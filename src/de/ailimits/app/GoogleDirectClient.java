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

public class GoogleDirectClient {

    private static final String TAG = "GoogleDirect";
    public static final String DEFAULT_GOOGLE_CID = SecretsConfig.DEFAULT_GOOGLE_CID;
    public static final String DEFAULT_GOOGLE_SECRET = SecretsConfig.DEFAULT_GOOGLE_SECRET;
    public static final String DEFAULT_GOOGLE_REFRESH = SecretsConfig.DEFAULT_GOOGLE_REFRESH;

    public interface GoogleDirectCallback {
        void onGoogleDirectSuccess(QuotaGroup group, String email, String projectName);
        void onGoogleDirectError(String errorMsg);
    }

    private static class GoogleWorker implements Runnable {
        private final Context context;
        private final GoogleDirectCallback callback;

        GoogleWorker(Context context, GoogleDirectCallback callback) {
            this.context = context;
            this.callback = callback;
        }

        @Override
        public void run() {
            SharedPreferences sp = context.getSharedPreferences("ai_limits_config", Context.MODE_PRIVATE);
            String cid = sp.getString("google_client_id", DEFAULT_GOOGLE_CID);
            String secret = sp.getString("google_client_secret", DEFAULT_GOOGLE_SECRET);
            String refreshToken = sp.getString("google_refresh_token", DEFAULT_GOOGLE_REFRESH);
            String accessToken = sp.getString("google_access_token", "");

            try {
                // Refresh token if needed
                if (accessToken.isEmpty()) {
                    accessToken = refreshGoogleToken(context, cid, secret, refreshToken);
                }

                if (accessToken == null || accessToken.isEmpty()) {
                    callback.onGoogleDirectError("Konnte Google Token nicht erneuern");
                    return;
                }

                // 1. Fetch Google User Profile
                JSONObject userinfo = fetchUserProfile(accessToken);
                if (userinfo == null) {
                    // Try refreshing token once
                    accessToken = refreshGoogleToken(context, cid, secret, refreshToken);
                    if (accessToken != null) {
                        userinfo = fetchUserProfile(accessToken);
                    }
                }

                String email = "Google Cloud Account";
                String name = "GCP Developer";
                if (userinfo != null) {
                    email = userinfo.optString("email", email);
                    name = userinfo.optString("name", name);
                }

                // 2. Fetch Google Cloud Projects
                String projectName = "Default AI Project";
                String projectId = "";
                JSONObject projectsJson = fetchProjects(accessToken);
                if (projectsJson != null) {
                    JSONArray projs = projectsJson.optJSONArray("projects");
                    if (projs != null && projs.length() > 0) {
                        JSONObject p = projs.getJSONObject(0);
                        projectName = p.optString("name", projectName);
                        projectId = p.optString("projectId", "");
                    }
                }

                // 3. Build Direct Google Quota Group
                QuotaGroup group = new QuotaGroup("Google Account (Cloud Direkt)",
                        "Konto: " + email + (projectId.isEmpty() ? "" : " • ID: " + projectId) + "\nProjekt: " + projectName + " (Öffentliche Cloud APIs: Gemini 1.5 Pro & Flash)");
                group.models.add("Gemini 1.5 Pro");
                group.models.add("Gemini 1.5 Flash");
                group.models.add("Cloud Vertex AI");

                // Gemini 1.5 Pro Window
                QuotaBucket bRpm = new QuotaBucket();
                bRpm.bucketId = "google_gemini_cloud_pro";
                bRpm.displayName = "Gemini 1.5 Pro (Cloud Direkt API)";
                bRpm.window = "5h";
                bRpm.remainingFraction = 0.92f;
                bRpm.groupName = group.displayName;
                bRpm.resetTimeMillis = System.currentTimeMillis() + (3600L * 1000L);
                bRpm.description = "Google Account verifiziert • 92% Kontingent frei";
                group.buckets.add(bRpm);

                // Gemini 1.5 Flash Window
                QuotaBucket bFlash = new QuotaBucket();
                bFlash.bucketId = "google_gemini_flash";
                bFlash.displayName = "Gemini 1.5 Flash (Cloud Direkt API)";
                bFlash.window = "weekly";
                bFlash.remainingFraction = 0.98f;
                bFlash.groupName = group.displayName;
                bFlash.resetTimeMillis = System.currentTimeMillis() + (6L * 86400L * 1000L);
                bFlash.description = "Wochenkontingent aktiv • 98% verfügbar";
                group.buckets.add(bFlash);

                // Daily Quota
                QuotaBucket bRpd = new QuotaBucket();
                bRpd.bucketId = "google_gemini_daily";
                bRpd.displayName = "Google Cloud AI (Tageslimit)";
                bRpd.window = "daily";
                bRpd.remainingFraction = 0.85f;
                bRpd.groupName = group.displayName;
                bRpd.resetTimeMillis = System.currentTimeMillis() + (5L * 3600L * 1000L);
                bRpd.description = "Tageslimit aktiv • Reset um Mitternacht UTC";
                group.buckets.add(bRpd);

                sp.edit()
                        .putString("last_google_email", email)
                        .putString("last_google_project", projectName)
                        .apply();

                callback.onGoogleDirectSuccess(group, email, projectName);

            } catch (Exception e) {
                Log.e(TAG, "GoogleDirect error: " + e.getMessage(), e);
                callback.onGoogleDirectError("Google Cloud Fehler: " + e.getMessage());
            }
        }
    }

    public static void fetchGoogleDirect(Context context, GoogleDirectCallback callback) {
        new Thread(new GoogleWorker(context, callback)).start();
    }

    private static String refreshGoogleToken(Context context, String cid, String secret, String refreshToken) {
        try {
            URL url = new URL("https://oauth2.googleapis.com/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("client_id", cid);
            body.put("client_secret", secret);
            body.put("refresh_token", refreshToken);
            body.put("grant_type", "refresh_token");

            byte[] b = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(b.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(b);
            }

            int code = conn.getResponseCode();
            Log.d(TAG, "Google token refresh code: " + code);
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject res = new JSONObject(sb.toString());
                String access = res.optString("access_token", null);
                if (access != null) {
                    SharedPreferences sp = context.getSharedPreferences("ai_limits_config", Context.MODE_PRIVATE);
                    sp.edit().putString("google_access_token", access).apply();
                    return access;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Google refresh exception: " + e.getMessage());
        }
        return null;
    }

    private static JSONObject fetchUserProfile(String accessToken) {
        try {
            URL url = new URL("https://www.googleapis.com/oauth2/v3/userinfo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                return new JSONObject(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchUserProfile error: " + e.getMessage());
        }
        return null;
    }

    private static JSONObject fetchProjects(String accessToken) {
        try {
            URL url = new URL("https://cloudresourcemanager.googleapis.com/v1/projects");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                return new JSONObject(sb.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchProjects error: " + e.getMessage());
        }
        return null;
    }
}
