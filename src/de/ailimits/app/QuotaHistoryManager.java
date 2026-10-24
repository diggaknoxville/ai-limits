package de.ailimits.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class QuotaHistoryManager extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ai_limits_history.db";
    // Version 2 cleanly wipes legacy synthetic mock data
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_HISTORY = "quota_history";
    public static final String COL_ID = "_id";
    public static final String COL_BUCKET_ID = "bucket_id";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_REMAINING = "remaining_fraction";
    public static final String COL_USED_PCT = "used_pct";
    public static final String COL_SESSION_INFO = "session_info";

    private static QuotaHistoryManager instance;

    public static synchronized QuotaHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new QuotaHistoryManager(context.getApplicationContext());
        }
        return instance;
    }

    public static class HistoryEntry {
        public final long timestamp;
        public final float remainingFraction;
        public final float usedPercent;
        public final String sessionInfo;

        public HistoryEntry(long timestamp, float remainingFraction, float usedPercent, String sessionInfo) {
            this.timestamp = timestamp;
            this.remainingFraction = remainingFraction;
            this.usedPercent = usedPercent;
            this.sessionInfo = sessionInfo;
        }

        public String formatDateTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM. HH:mm", AppI18n.getLocale());
            return sdf.format(new Date(timestamp));
        }

        public String formatTimeOnly() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", AppI18n.getLocale());
            return sdf.format(new Date(timestamp));
        }
    }

    public QuotaHistoryManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_HISTORY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_BUCKET_ID + " TEXT NOT NULL, " +
                COL_TIMESTAMP + " INTEGER NOT NULL, " +
                COL_REMAINING + " REAL NOT NULL, " +
                COL_USED_PCT + " REAL NOT NULL, " +
                COL_SESSION_INFO + " TEXT" +
                ");");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bucket_time ON " + TABLE_HISTORY + " (" + COL_BUCKET_ID + ", " + COL_TIMESTAMP + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    /**
     * Records an authentic, genuine snapshot measured from the real API response.
     * No synthetic or interpolated values are ever inserted.
     */
    public synchronized void recordSnapshot(QuotaBucket bucket) {
        if (bucket == null || bucket.bucketId == null) return;

        long now = System.currentTimeMillis();
        float remaining = bucket.getRemainingFraction();
        float usedPct = (1.0f - remaining) * 100f;
        String info = bucket.groupName != null ? bucket.groupName : "";

        SQLiteDatabase db = getWritableDatabase();

        // Avoid identical duplicate snapshots within 30 seconds
        Cursor c = db.rawQuery("SELECT " + COL_TIMESTAMP + ", " + COL_REMAINING + " FROM " + TABLE_HISTORY +
                " WHERE " + COL_BUCKET_ID + " = ? ORDER BY " + COL_TIMESTAMP + " DESC LIMIT 1",
                new String[]{bucket.bucketId});

        if (c.moveToFirst()) {
            long lastTime = c.getLong(0);
            float lastRem = c.getFloat(1);
            c.close();
            if (now - lastTime < 30000L && Math.abs(remaining - lastRem) < 0.005f) {
                return; // Duplicate within 30 seconds with no change
            }
        } else {
            c.close();
        }

        ContentValues cv = new ContentValues();
        cv.put(COL_BUCKET_ID, bucket.bucketId);
        cv.put(COL_TIMESTAMP, now);
        cv.put(COL_REMAINING, remaining);
        cv.put(COL_USED_PCT, usedPct);
        cv.put(COL_SESSION_INFO, info);

        db.insert(TABLE_HISTORY, null, cv);

        // Keep maximum 500 real snapshots per bucket to prevent database bloat
        db.execSQL("DELETE FROM " + TABLE_HISTORY + " WHERE " + COL_BUCKET_ID + " = ? AND " +
                COL_ID + " NOT IN (SELECT " + COL_ID + " FROM " + TABLE_HISTORY +
                " WHERE " + COL_BUCKET_ID + " = ? ORDER BY " + COL_TIMESTAMP + " DESC LIMIT 500)",
                new Object[]{bucket.bucketId, bucket.bucketId});
    }

    /**
     * Retrieves actual recorded history entries since the specified timestamp.
     */
    public synchronized List<HistoryEntry> getHistory(String bucketId, long sinceMillis) {
        List<HistoryEntry> list = new ArrayList<>();
        if (bucketId == null) return list;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COL_TIMESTAMP + ", " + COL_REMAINING + ", " + COL_USED_PCT + ", " + COL_SESSION_INFO +
                " FROM " + TABLE_HISTORY + " WHERE " + COL_BUCKET_ID + " = ? AND " + COL_TIMESTAMP + " >= ? ORDER BY " + COL_TIMESTAMP + " ASC",
                new String[]{bucketId, String.valueOf(sinceMillis)});

        while (c.moveToNext()) {
            list.add(new HistoryEntry(c.getLong(0), c.getFloat(1), c.getFloat(2), c.getString(3)));
        }
        c.close();

        return list;
    }
}
