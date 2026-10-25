package de.ailimits.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.KeyEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WatchBleSyncManager {
    private static final String TAG = "WatchBleSync";

    public static final String WATCH_NAME = "WatchOS Ultra";
    public static final UUID SERVICE_AI_LIMITS = UUID.fromString("0000a100-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_DATA        = UUID.fromString("0000a101-0000-1000-8000-00805f9b34fb");
    public static final UUID CHAR_STATUS      = UUID.fromString("0000a102-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD_UUID        = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static WatchBleSyncManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BluetoothGatt connectedGatt = null;
    private Context appContext = null;
    private boolean isScanning = false;
    private byte[] pendingPayload = null;
    private SyncCallback activeCallback = null;
    private BluetoothLeScanner activeScanner = null;
    private WatchScanCallback activeScanCallback = null;

    public interface SyncCallback {
        void onSyncSuccess();
        void onSyncError(String error);
    }

    public static synchronized WatchBleSyncManager getInstance() {
        if (instance == null) {
            instance = new WatchBleSyncManager();
        }
        return instance;
    }

    private WatchBleSyncManager() {}

    public void syncBucketsToWatch(Context context, List<QuotaBucket> buckets, SyncCallback callback) {
        this.appContext = context.getApplicationContext();
        this.activeCallback = callback;
        this.pendingPayload = buildBinaryPayload(buckets);

        BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bm == null) {
            notifyError("Bluetooth Manager nicht verfügbar");
            return;
        }

        BluetoothAdapter adapter = bm.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            notifyError("Bluetooth ist deaktiviert");
            return;
        }

        // If already connected to WatchOS Ultra GATT, transmit immediately
        if (connectedGatt != null) {
            BluetoothGattService service = connectedGatt.getService(SERVICE_AI_LIMITS);
            if (service != null) {
                BluetoothGattCharacteristic charData = service.getCharacteristic(CHAR_DATA);
                if (charData != null && pendingPayload != null) {
                    try {
                        charData.setValue(pendingPayload);
                        charData.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        connectedGatt.writeCharacteristic(charData);
                        Log.d(TAG, "Direkt ueber bestehende GATT Verbindung gesendet");
                        return;
                    } catch (SecurityException ignored) {}
                }
            }
        }

        // Check bonded/paired devices first
        try {
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                if (WATCH_NAME.equalsIgnoreCase(device.getName())) {
                    Log.d(TAG, "Gefunden unter gekoppelten Geräten: " + device.getAddress());
                    connectToDevice(context, device);
                    return;
                }
            }
        } catch (SecurityException ignored) {}

        // Otherwise scan via BLE
        startScan(context, adapter);
    }

    private void startScan(Context context, BluetoothAdapter adapter) {
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            notifyError("BLE Scanner nicht verfügbar");
            return;
        }

        if (isScanning) return;
        isScanning = true;
        activeScanner = scanner;

        ScanFilter nameFilter = new ScanFilter.Builder()
                .setDeviceName(WATCH_NAME)
                .build();
        ScanFilter uuidFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_AI_LIMITS))
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(nameFilter);
        filters.add(uuidFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        activeScanCallback = new WatchScanCallback(this, context);

        try {
            scanner.startScan(filters, settings, activeScanCallback);
            handler.postDelayed(new ScanTimeoutRunnable(this), 8000);
        } catch (SecurityException e) {
            isScanning = false;
            notifyError("Berechtigung fehlt: " + e.getMessage());
        }
    }

    private static class ScanTimeoutRunnable implements Runnable {
        private final WatchBleSyncManager manager;
        ScanTimeoutRunnable(WatchBleSyncManager manager) { this.manager = manager; }
        @Override
        public void run() {
            if (manager.isScanning) {
                manager.stopScan();
                manager.notifyError("WatchOS Ultra nicht in Reichweite gefunden");
            }
        }
    }

    private void stopScan() {
        if (!isScanning) return;
        isScanning = false;
        if (activeScanner != null && activeScanCallback != null) {
            try {
                activeScanner.stopScan(activeScanCallback);
            } catch (Exception ignored) {}
        }
        activeScanner = null;
        activeScanCallback = null;
    }

    private static class WatchScanCallback extends ScanCallback {
        private final WatchBleSyncManager manager;
        private final Context context;

        WatchScanCallback(WatchBleSyncManager manager, Context context) {
            this.manager = manager;
            this.context = context;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice dev = result.getDevice();
            try {
                String name = dev.getName();
                if (WATCH_NAME.equalsIgnoreCase(name) || (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null && result.getScanRecord().getServiceUuids().contains(new ParcelUuid(SERVICE_AI_LIMITS)))) {
                    manager.stopScan();
                    Log.d(TAG, "WatchOS Ultra per Scan entdeckt: " + dev.getAddress());
                    manager.connectToDevice(context, dev);
                }
            } catch (SecurityException ignored) {}
        }

        @Override
        public void onScanFailed(int errorCode) {
            manager.isScanning = false;
            Log.e(TAG, "BLE Scan fehlgeschlagen: " + errorCode);
            manager.notifyError("Scan-Fehler: " + errorCode);
        }
    }

    private void connectToDevice(Context context, BluetoothDevice device) {
        try {
            connectedGatt = device.connectGatt(context, false, new WatchGattCallback(this, context));
        } catch (SecurityException e) {
            notifyError("GATT Verbindung fehlgeschlagen: " + e.getMessage());
        }
    }

    private static class WatchGattCallback extends BluetoothGattCallback {
        private final WatchBleSyncManager manager;
        private final Context context;

        WatchGattCallback(WatchBleSyncManager manager, Context context) {
            this.manager = manager;
            this.context = context;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Mit WatchOS Ultra verbunden, starte Service-Discovery...");
                try {
                    gatt.discoverServices();
                } catch (SecurityException ignored) {}
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "WatchOS Ultra getrennt.");
                gatt.close();
                manager.connectedGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_AI_LIMITS);
                if (service != null) {
                    BluetoothGattCharacteristic charStatus = service.getCharacteristic(CHAR_STATUS);
                    BluetoothGattCharacteristic charData = service.getCharacteristic(CHAR_DATA);

                    // Subscribe to status notifications (receives CMD:MEDIA:* and CMD:CAL_SYNC from watch)
                    if (charStatus != null) {
                        try {
                            gatt.setCharacteristicNotification(charStatus, true);
                            BluetoothGattDescriptor desc = charStatus.getDescriptor(CCCD_UUID);
                            if (desc != null) {
                                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(desc);
                            }
                        } catch (SecurityException ignored) {}
                    }

                    // Write pending Quota payload
                    if (charData != null && manager.pendingPayload != null) {
                        try {
                            charData.setValue(manager.pendingPayload);
                            charData.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            boolean ok = gatt.writeCharacteristic(charData);
                            Log.d(TAG, "Datenübertragung gestartet: " + ok);
                        } catch (SecurityException ignored) {}
                    }

                    // Also immediately push upcoming Google Calendar events on connect!
                    manager.handler.postDelayed(() -> manager.syncCalendarToWatch(context, gatt), 600);
                    return;
                }
            }
            manager.disconnectGatt(gatt);
            manager.notifyError("AI Limits Service auf der Uhr nicht gefunden");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Daten erfolgreich an WatchOS Ultra übertragen!");
                manager.notifySuccess();
            } else {
                Log.e(TAG, "Schreibfehler auf Charakteristik: " + status);
                manager.notifyError("Schreibfehler: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] val = characteristic.getValue();
            if (val == null || val.length == 0) return;
            String msg = new String(val, StandardCharsets.UTF_8).trim();
            Log.d(TAG, "Kommando von WatchOS Ultra empfangen: " + msg);

            if (msg.startsWith("CMD:MEDIA:")) {
                manager.handleMediaCommand(context, msg);
            } else if ("CMD:CAL_SYNC".equals(msg)) {
                manager.syncCalendarToWatch(context, gatt);
            }
        }
    }

    private void handleMediaCommand(Context context, String cmd) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;

        if ("CMD:MEDIA:PLAY_PAUSE".equals(cmd)) {
            dispatchMediaKey(am, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        } else if ("CMD:MEDIA:NEXT".equals(cmd)) {
            dispatchMediaKey(am, KeyEvent.KEYCODE_MEDIA_NEXT);
        } else if ("CMD:MEDIA:PREV".equals(cmd)) {
            dispatchMediaKey(am, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        } else if ("CMD:MEDIA:VOL_UP".equals(cmd)) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        } else if ("CMD:MEDIA:VOL_DOWN".equals(cmd)) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        }
    }

    private static void dispatchMediaKey(AudioManager am, int keyCode) {
        try {
            am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Senden von MediaKeyEvent: " + e.getMessage());
        }
    }

    public void syncCalendarToWatch(Context context, BluetoothGatt gatt) {
        if (gatt == null) return;
        try {
            BluetoothGattService service = gatt.getService(SERVICE_AI_LIMITS);
            if (service == null) return;
            BluetoothGattCharacteristic charData = service.getCharacteristic(CHAR_DATA);
            if (charData == null) return;

            String calJson = queryUpcomingCalendarJson(context, 8);
            if (calJson != null && calJson.length() > 0) {
                charData.setValue(calJson.getBytes(StandardCharsets.UTF_8));
                charData.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                gatt.writeCharacteristic(charData);
                Log.d(TAG, "Google Kalender JSON an Uhr gesendet: " + calJson);
            }
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Kalender-Sync: " + e.getMessage());
        }
    }

    public static String queryUpcomingCalendarJson(Context context, int maxEvents) {
        try {
            long now = System.currentTimeMillis();
            long end = now + 7L * 86400000L; // Next 7 days

            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, now - 1800000L);
            ContentUris.appendId(builder, end);

            String[] projection = new String[]{
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.EVENT_LOCATION,
                    CalendarContract.Instances.ALL_DAY
            };

            Cursor cursor = context.getContentResolver().query(
                    builder.build(), projection, null, null, CalendarContract.Instances.BEGIN + " ASC"
            );

            if (cursor == null) return "{\"cal\":[]}";

            JSONArray arr = new JSONArray();
            int count = 0;
            while (cursor.moveToNext() && count < maxEvents) {
                String title = cursor.getString(0);
                long begin = cursor.getLong(1);
                long finish = cursor.getLong(2);
                String loc = cursor.getString(3);
                int allDay = cursor.getInt(4);

                JSONObject ev = new JSONObject();
                ev.put("t", title != null ? title : "Termin");
                ev.put("s", begin / 1000L);
                ev.put("e", finish / 1000L);
                ev.put("l", loc != null ? loc : "");
                ev.put("a", allDay);
                arr.put(ev);
                count++;
            }
            cursor.close();

            JSONObject root = new JSONObject();
            root.put("cal", arr);
            return root.toString();
        } catch (SecurityException se) {
            Log.w(TAG, "READ_CALENDAR Berechtigung noch nicht erteilt");
            return "{\"cal\":[]}";
        } catch (Exception e) {
            Log.e(TAG, "Kalenderabfrage Fehler: " + e.getMessage());
            return "{\"cal\":[]}";
        }
    }

    private void disconnectGatt(BluetoothGatt gatt) {
        try {
            gatt.disconnect();
            gatt.close();
        } catch (Exception ignored) {}
        connectedGatt = null;
    }

    private static class NotifySuccessRunnable implements Runnable {
        private final SyncCallback callback;
        NotifySuccessRunnable(SyncCallback cb) { this.callback = cb; }
        @Override
        public void run() { if (callback != null) callback.onSyncSuccess(); }
    }

    private static class NotifyErrorRunnable implements Runnable {
        private final SyncCallback callback;
        private final String error;
        NotifyErrorRunnable(SyncCallback cb, String err) { this.callback = cb; this.error = err; }
        @Override
        public void run() { if (callback != null) callback.onSyncError(error); }
    }

    private void notifySuccess() {
        if (activeCallback != null) {
            handler.post(new NotifySuccessRunnable(activeCallback));
        }
    }

    private void notifyError(final String error) {
        if (activeCallback != null) {
            handler.post(new NotifyErrorRunnable(activeCallback, error));
        }
    }

    private byte[] buildBinaryPayload(List<QuotaBucket> buckets) {
        // Matches struct AiLimitsBlePacket (56 bytes, little endian)
        ByteBuffer buf = ByteBuffer.allocate(56);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) 0xA1); // Magic
        buf.put((byte) 0x01); // Version

        int c5h_p = 100, cWk_p = 100;
        long c5h_r = 0, cWk_r = 0;
        int agG5h_p = 100, agGWk_p = 100;
        long agG5h_r = 0, agGWk_r = 0;
        int agC5h_p = 100, agCWk_p = 100;
        long agC5h_r = 0, agCWk_r = 0;
        int gc5h_p = 100, gcDay_p = 100, gcWk_p = 100;
        long gc5h_r = 0, gcDay_r = 0, gcWk_r = 0;
        int argon_p = 100;
        long argon_r = 0;

        if (buckets != null) {
            for (QuotaBucket b : buckets) {
                String bid = b.bucketId != null ? b.bucketId.toLowerCase(java.util.Locale.ROOT) : "";
                int pct = Math.round(b.getRemainingFraction() * 100f);
                long rSec = b.resetTimeMillis / 1000L;

                // 1. Claude Pro Direct API
                if ("claude_pro_5h".equals(bid) || (bid.startsWith("claude") && bid.contains("5h"))) {
                    c5h_p = pct; c5h_r = rSec;
                } else if ("claude_pro_7d".equals(bid) || (bid.startsWith("claude") && (bid.contains("7d") || bid.contains("week")))) {
                    cWk_p = pct; cWk_r = rSec;
                }
                // 2. Google Antigravity IDE Models
                else if ("gemini-argon-wtu-weekly".equals(bid) || bid.contains("argon")) {
                    argon_p = pct; argon_r = rSec;
                } else if ("gemini-5h".equals(bid)) {
                    agG5h_p = pct; agG5h_r = rSec;
                } else if ("gemini-weekly".equals(bid)) {
                    agGWk_p = pct; agGWk_r = rSec;
                } else if ("3p-5h".equals(bid)) {
                    agC5h_p = pct; agC5h_r = rSec;
                } else if ("3p-weekly".equals(bid)) {
                    agCWk_p = pct; agCWk_r = rSec;
                }
                // 3. Google Cloud Direct API
                else if ("google_gemini_cloud_pro".equals(bid) || bid.contains("cloud_pro")) {
                    gc5h_p = pct; gc5h_r = rSec;
                } else if ("google_gemini_daily".equals(bid) || bid.contains("daily")) {
                    gcDay_p = pct; gcDay_r = rSec;
                } else if ("google_gemini_flash".equals(bid) || bid.contains("flash")) {
                    gcWk_p = pct; gcWk_r = rSec;
                }
            }
        }

        buf.put((byte) c5h_p);
        buf.putInt((int) c5h_r);
        buf.put((byte) cWk_p);
        buf.putInt((int) cWk_r);

        buf.put((byte) agG5h_p);
        buf.putInt((int) agG5h_r);
        buf.put((byte) agGWk_p);
        buf.putInt((int) agGWk_r);

        buf.put((byte) agC5h_p);
        buf.putInt((int) agC5h_r);
        buf.put((byte) agCWk_p);
        buf.putInt((int) agCWk_r);

        buf.put((byte) gc5h_p);
        buf.putInt((int) gc5h_r);
        buf.put((byte) gcDay_p);
        buf.putInt((int) gcDay_r);
        buf.put((byte) gcWk_p);
        buf.putInt((int) gcWk_r);

        buf.put((byte) argon_p);
        buf.putInt((int) argon_r);

        buf.putInt((int) (System.currentTimeMillis() / 1000L)); // sync_timestamp

        return buf.array();
    }
}
