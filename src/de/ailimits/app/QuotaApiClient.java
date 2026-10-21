package de.ailimits.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class QuotaApiClient {

    public interface QuotaCallback {
        void onSuccess(List<QuotaGroup> groups, String overallDescription, boolean isLive);
        void onError(String errorMessage);
    }

    private static class NetworkWorker implements Runnable {
        private final String host;
        private final int port;
        private final String csrfToken;
        private final QuotaCallback callback;

        NetworkWorker(String host, int port, String csrfToken, QuotaCallback callback) {
            this.host = host;
            this.port = port;
            this.csrfToken = csrfToken;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                String endpoint = "http://" + host + ":" + port + "/exa.language_server_pb.LanguageServerService/RetrieveUserQuotaSummary";
                android.util.Log.d("AILimits", "Requesting endpoint: " + endpoint);
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                if (csrfToken != null && !csrfToken.isEmpty()) {
                    conn.setRequestProperty("X-Codeium-Csrf-Token", csrfToken);
                }
                conn.setDoOutput(true);

                String payload = "{\"metadata\":{\"extension_name\":\"antigravity\"}}";
                byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(bytes.length);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }

                int responseCode = conn.getResponseCode();
                android.util.Log.d("AILimits", "HTTP Response code: " + responseCode);
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    android.util.Log.d("AILimits", "Payload received length: " + sb.length());
                    parseAndDeliver(sb.toString(), true, callback);
                } else {
                    android.util.Log.e("AILimits", "HTTP Error status: " + responseCode);
                    callback.onError("Server antwortete mit Status HTTP " + responseCode);
                }
            } catch (Exception e) {
                android.util.Log.e("AILimits", "Connection exception: " + e.getMessage(), e);
                callback.onError("Verbindungsfehler: " + e.getMessage());
            }
        }
    }

    public static void fetchQuota(String host, int port, String csrfToken, QuotaCallback callback) {
        new Thread(new NetworkWorker(host, port, csrfToken, callback)).start();
    }

    public static void parseAndDeliver(String jsonStr, boolean isLive, QuotaCallback callback) {
        try {
            JSONObject root = new JSONObject(jsonStr);
            JSONObject responseObj = root.optJSONObject("response");
            if (responseObj == null) {
                responseObj = root;
            }

            String overallDescription = responseObj.optString("description", "");
            JSONArray groupsArray = responseObj.optJSONArray("groups");
            List<QuotaGroup> groupsList = new ArrayList<>();

            if (groupsArray != null) {
                for (int i = 0; i < groupsArray.length(); i++) {
                    JSONObject gObj = groupsArray.getJSONObject(i);
                    String groupName = gObj.optString("displayName", "AI Group");
                    String groupDesc = gObj.optString("description", "");

                    QuotaGroup group = new QuotaGroup(groupName, groupDesc);
                    JSONArray bucketsArray = gObj.optJSONArray("buckets");
                    if (bucketsArray != null) {
                        for (int j = 0; j < bucketsArray.length(); j++) {
                            JSONObject bObj = bucketsArray.getJSONObject(j);
                            QuotaBucket bucket = new QuotaBucket();
                            bucket.bucketId = bObj.optString("bucketId", "bucket_" + i + "_" + j);
                            bucket.displayName = bObj.optString("displayName", "Limit");
                            bucket.description = bObj.optString("description", "");
                            bucket.window = bObj.optString("window", "5h");
                            bucket.remainingFraction = (float) bObj.optDouble("remainingFraction", 1.0);
                            bucket.rawResetTime = bObj.optString("resetTime", "");
                            bucket.resetTimeMillis = QuotaBucket.parseIsoTime(bucket.rawResetTime);
                            bucket.groupName = groupName;

                            group.buckets.add(bucket);
                        }
                    }
                    groupsList.add(group);
                }
            }

            callback.onSuccess(groupsList, overallDescription, isLive);
        } catch (Exception e) {
            callback.onError("Fehler beim Verarbeiten der Daten: " + e.getMessage());
        }
    }

    public static List<QuotaGroup> getSampleData() {
        List<QuotaGroup> list = new ArrayList<>();

        // Group 1: Claude & GPT
        QuotaGroup g1 = new QuotaGroup("Claude and GPT models", "Models within this group: Claude Opus, Claude Sonnet, GPT-OSS");
        QuotaBucket b1_1 = new QuotaBucket();
        b1_1.bucketId = "3p-weekly";
        b1_1.displayName = "Weekly Limit Remaining";
        b1_1.description = "You have hit your 5-hour limit, so the weekly limit does not currently apply. Your 5-hour limit will refresh in 33 minutes.";
        b1_1.window = "weekly";
        b1_1.remainingFraction = 0.495f;
        b1_1.resetTimeMillis = System.currentTimeMillis() + (7L * 24L * 3600L * 1000L);
        b1_1.groupName = g1.displayName;
        g1.buckets.add(b1_1);

        QuotaBucket b1_2 = new QuotaBucket();
        b1_2.bucketId = "3p-5h";
        b1_2.displayName = "Five Hour Limit Remaining";
        b1_2.description = "You have hit your 5-hour limit, it will refresh in 33 minutes. If on a supported paid plan, you can use AI credits in the interim.";
        b1_2.window = "5h";
        b1_2.remainingFraction = 0.0f;
        b1_2.resetTimeMillis = System.currentTimeMillis() + (33L * 60L * 1000L);
        b1_2.groupName = g1.displayName;
        g1.buckets.add(b1_2);
        list.add(g1);

        // Group 2: Gemini
        QuotaGroup g2 = new QuotaGroup("Gemini Models", "Models within this group: Gemini Flash, Gemini Pro");
        QuotaBucket b2_1 = new QuotaBucket();
        b2_1.bucketId = "gemini-weekly";
        b2_1.displayName = "Weekly Limit Remaining";
        b2_1.description = "You have used some of your weekly limit, it will fully refresh in 3 days.";
        b2_1.window = "weekly";
        b2_1.remainingFraction = 0.50f;
        b2_1.resetTimeMillis = System.currentTimeMillis() + (3L * 24L * 3600L * 1000L);
        b2_1.groupName = g2.displayName;
        g2.buckets.add(b2_1);

        QuotaBucket b2_2 = new QuotaBucket();
        b2_2.bucketId = "gemini-5h";
        b2_2.displayName = "Five Hour Limit Remaining";
        b2_2.description = "You have used some of your 5-hour limit, it will fully refresh in 1 hour, 4 minutes.";
        b2_2.window = "5h";
        b2_2.remainingFraction = 0.87f;
        b2_2.resetTimeMillis = System.currentTimeMillis() + (64L * 60L * 1000L);
        b2_2.groupName = g2.displayName;
        g2.buckets.add(b2_2);
        list.add(g2);

        // Group 3: Gemini 4 Argon
        QuotaGroup g3 = new QuotaGroup("Gemini 4 Argon", "For paid Pro plans, Argon quota is separate. Higher Argon quota is available on Ultra plans.");
        QuotaBucket b3_1 = new QuotaBucket();
        b3_1.bucketId = "gemini-argon-wtu-weekly";
        b3_1.displayName = "Gemini 4 Argon Weekly Limit";
        b3_1.description = "You have used some of your weekly limit, it will fully refresh in 2 days, 21 hours.";
        b3_1.window = "weekly";
        b3_1.remainingFraction = 0.03f;
        b3_1.resetTimeMillis = System.currentTimeMillis() + (69L * 3600L * 1000L);
        b3_1.groupName = g3.displayName;
        g3.buckets.add(b3_1);
        list.add(g3);

        return list;
    }
}
