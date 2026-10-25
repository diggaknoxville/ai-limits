package de.ailimits.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OtaUpdateManager {
    private static final String TAG = "OtaUpdateManager";
    public static final String APP_REPO = "diggaknoxville/ai-limits";
    public static final String WATCH_REPO = "diggaknoxville/WatchOS-Ultra";
    public static final String CURRENT_APP_VERSION = "1.2.0";

    private static OtaUpdateManager instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface WatchUpdateCallback {
        void onWatchUpdateResult(boolean updateAvailable, String latestTag, String downloadUrl);
    }

    public static synchronized OtaUpdateManager getInstance() {
        if (instance == null) {
            instance = new OtaUpdateManager();
        }
        return instance;
    }

    private OtaUpdateManager() {}

    public void checkAppUpdate(final Activity activity, final boolean userInitiated) {
        if (activity == null || activity.isFinishing()) return;

        executor.execute(() -> {
            try {
                String apiUrl = "https://api.github.com/repos/" + APP_REPO + "/releases/latest";
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "AILimits-Android/" + CURRENT_APP_VERSION);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    if (userInitiated) {
                        mainHandler.post(() -> Toast.makeText(activity, "Keine Releases auf GitHub gefunden (App aktuell)", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    if (userInitiated) {
                        mainHandler.post(() -> Toast.makeText(activity, "Update-Prüfung fehlgeschlagen (HTTP " + code + ")", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject release = new JSONObject(sb.toString());
                String tagName = release.optString("tag_name", "");
                String releaseName = release.optString("name", tagName);
                String body = release.optString("body", "Neue Verbesserungen & Bugfixes.");

                String apkDownloadUrl = null;
                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset == null) continue;
                        String name = asset.optString("name", "");
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", null);
                            break;
                        }
                    }
                }

                if (apkDownloadUrl == null) {
                    if (userInitiated) {
                        mainHandler.post(() -> Toast.makeText(activity, "Keine APK-Datei im neuesten GitHub Release gefunden.", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                String cleanRemote = tagName.replaceAll("[^0-9.]", "");
                String cleanLocal = CURRENT_APP_VERSION.replaceAll("[^0-9.]", "");

                if (isNewerVersion(cleanRemote, cleanLocal)) {
                    final String downloadUrl = apkDownloadUrl;
                    mainHandler.post(() -> showUpdateDialog(activity, releaseName, body, downloadUrl));
                } else if (userInitiated) {
                    mainHandler.post(() -> Toast.makeText(activity, "Die AI-Limits App ist bereits auf dem neuesten Stand (v" + CURRENT_APP_VERSION + ").", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Prüfen auf GitHub Releases: " + e.getMessage(), e);
                if (userInitiated) {
                    mainHandler.post(() -> Toast.makeText(activity, "Update-Fehler: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    public void checkWatchFirmware(final WatchUpdateCallback callback) {
        executor.execute(() -> {
            try {
                String apiUrl = "https://api.github.com/repos/" + WATCH_REPO + "/releases/latest";
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "AILimits-Android/" + CURRENT_APP_VERSION);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject release = new JSONObject(sb.toString());
                    String tagName = release.optString("tag_name", "");
                    String binUrl = null;
                    JSONArray assets = release.optJSONArray("assets");
                    if (assets != null) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject a = assets.optJSONObject(i);
                            if (a != null && a.optString("name", "").endsWith(".bin")) {
                                binUrl = a.optString("browser_download_url", null);
                                break;
                            }
                        }
                    }
                    final String finalBinUrl = binUrl;
                    mainHandler.post(() -> callback.onWatchUpdateResult(true, tagName, finalBinUrl));
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Watch update check error: " + e.getMessage());
            }
            mainHandler.post(() -> callback.onWatchUpdateResult(false, "", null));
        });
    }

    private boolean isNewerVersion(String remote, String local) {
        if (remote.isEmpty()) return false;
        String[] remParts = remote.split("\\.");
        String[] locParts = local.split("\\.");
        int len = Math.max(remParts.length, locParts.length);

        for (int i = 0; i < len; i++) {
            int r = i < remParts.length ? parseSafeInt(remParts[i]) : 0;
            int l = i < locParts.length ? parseSafeInt(locParts[i]) : 0;
            if (r > l) return true;
            if (r < l) return false;
        }
        return false;
    }

    private int parseSafeInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void showUpdateDialog(Activity activity, String title, String notes, String downloadUrl) {
        new AlertDialog.Builder(activity)
                .setTitle("⚡ Neues AI-Limits Update verfügbar!")
                .setMessage("Version: " + title + "\n\nÄnderungen:\n" + notes + "\n\nMöchtest du das Update jetzt herunterladen und installieren?")
                .setPositiveButton("Aktualisieren", (dialog, which) -> downloadAndInstallApk(activity, downloadUrl))
                .setNegativeButton("Später", null)
                .show();
    }

    private void downloadAndInstallApk(Activity activity, String downloadUrl) {
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setTitle("Update wird geladen...");
        progress.setMessage("Bitte warten...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setCancelable(false);
        progress.setMax(100);
        progress.show();

        executor.execute(() -> {
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.connect();

                int fileLength = conn.getContentLength();
                File cacheDir = activity.getExternalCacheDir();
                if (cacheDir == null) cacheDir = activity.getCacheDir();

                File apkFile = new File(cacheDir, "ai-limits-update.apk");
                if (apkFile.exists()) apkFile.delete();

                InputStream input = conn.getInputStream();
                FileOutputStream output = new FileOutputStream(apkFile);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        int pct = (int) (total * 100 / fileLength);
                        mainHandler.post(() -> progress.setProgress(pct));
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                mainHandler.post(() -> {
                    progress.dismiss();
                    installApk(activity, apkFile);
                });
            } catch (Exception e) {
                Log.e(TAG, "Download error: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    progress.dismiss();
                    Toast.makeText(activity, "Download fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void installApk(Activity activity, File apkFile) {
        try {
            Uri contentUri = AppFileProvider.getUriForFile(activity, apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Install error: " + e.getMessage(), e);
            Toast.makeText(activity, "Installationsfehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

