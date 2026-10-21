package de.ailimits.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsageHistoryChartView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path chartPath = new Path();
    private final Path fillPath = new Path();

    private List<QuotaHistoryManager.HistoryEntry> entries = new ArrayList<>();
    private int themeColor = 0xFF38BDF8; // Default Neon Cyan

    public UsageHistoryChartView(Context context) {
        super(context);
        init();
    }

    public UsageHistoryChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UsageHistoryChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(0xFF1E2430);
        gridPaint.setStrokeWidth(dpToPx(1));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(4), dpToPx(4)}, 0));

        linePaint.setColor(themeColor);
        linePaint.setStrokeWidth(dpToPx(2.5f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint.setColor(Color.WHITE);
        pointPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFF94A3B8);
        textPaint.setTextSize(spToPx(10));
        textPaint.setTextAlign(Paint.Align.RIGHT);

        labelPaint.setColor(0xFF64748B);
        labelPaint.setTextSize(spToPx(9));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setThemeColor(int color) {
        this.themeColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    public void setData(List<QuotaHistoryManager.HistoryEntry> data) {
        this.entries = data != null ? data : new ArrayList<>();
        invalidate();
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float padLeft = dpToPx(38);
        float padRight = dpToPx(16);
        float padTop = dpToPx(16);
        float padBottom = dpToPx(24);

        float plotW = w - padLeft - padRight;
        float plotH = h - padTop - padBottom;
        if (plotW <= 0 || plotH <= 0) return;

        // 1. Draw Grid Lines (100%, 75%, 50%, 25%, 0%)
        float[] gridLevels = new float[]{1.0f, 0.75f, 0.5f, 0.25f, 0.0f};
        for (float level : gridLevels) {
            float y = padTop + (1.0f - level) * plotH;
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);

            int pct = Math.round(level * 100f);
            canvas.drawText(pct + "%", padLeft - dpToPx(6), y + dpToPx(3.5f), textPaint);
        }

        if (entries == null || entries.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Keine Verlaufsdaten vorhanden", padLeft + plotW / 2f, padTop + plotH / 2f, textPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            return;
        }

        long minTime = entries.get(0).timestamp;
        long maxTime = entries.get(entries.size() - 1).timestamp;
        long timeSpan = Math.max(1000L, maxTime - minTime);

        // 2. Build Path for Line and Gradient Fill
        chartPath.reset();
        fillPath.reset();

        float firstX = padLeft;
        float firstY = padTop + (1.0f - entries.get(0).remainingFraction) * plotH;

        chartPath.moveTo(firstX, firstY);
        fillPath.moveTo(firstX, padTop + plotH);
        fillPath.lineTo(firstX, firstY);

        float lastX = firstX;
        float lastY = firstY;

        for (int i = 0; i < entries.size(); i++) {
            QuotaHistoryManager.HistoryEntry entry = entries.get(i);
            float timeRatio = (float) (entry.timestamp - minTime) / (float) timeSpan;
            float x = padLeft + timeRatio * plotW;
            float y = padTop + (1.0f - entry.remainingFraction) * plotH;

            chartPath.lineTo(x, y);
            fillPath.lineTo(x, y);

            lastX = x;
            lastY = y;
        }

        fillPath.lineTo(lastX, padTop + plotH);
        fillPath.close();

        // 3. Draw Gradient Fill Under Curve
        int topColor = (themeColor & 0x00FFFFFF) | 0x44000000;
        int botColor = (themeColor & 0x00FFFFFF) | 0x00000000;
        fillPaint.setShader(new LinearGradient(0, padTop, 0, padTop + plotH, topColor, botColor, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // 4. Draw Line Plot
        canvas.drawPath(chartPath, linePaint);

        // 5. Draw Data Points and Latest Value Callout
        for (int i = 0; i < entries.size(); i++) {
            QuotaHistoryManager.HistoryEntry entry = entries.get(i);
            float timeRatio = (float) (entry.timestamp - minTime) / (float) timeSpan;
            float x = padLeft + timeRatio * plotW;
            float y = padTop + (1.0f - entry.remainingFraction) * plotH;

            // Point outer ring
            pointPaint.setColor(themeColor);
            canvas.drawCircle(x, y, dpToPx(4), pointPaint);
            // Point inner dot
            pointPaint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, dpToPx(2), pointPaint);
        }

        // Draw Latest Point Callout
        if (!entries.isEmpty()) {
            QuotaHistoryManager.HistoryEntry latest = entries.get(entries.size() - 1);
            int remPct = Math.round(latest.remainingFraction * 100f);
            String callout = remPct + "% frei";

            pointPaint.setColor(0xFF0F172A);
            pointPaint.setStyle(Paint.Style.FILL);
            float calloutW = dpToPx(48);
            float calloutH = dpToPx(18);
            float calloutX = Math.min(w - padRight - calloutW, Math.max(padLeft, lastX - calloutW / 2f));
            float calloutY = Math.max(padTop - dpToPx(12), lastY - dpToPx(24));

            canvas.drawRoundRect(calloutX, calloutY, calloutX + calloutW, calloutY + calloutH, dpToPx(4), dpToPx(4), pointPaint);
            pointPaint.setColor(themeColor);
            pointPaint.setStyle(Paint.Style.STROKE);
            pointPaint.setStrokeWidth(dpToPx(1));
            canvas.drawRoundRect(calloutX, calloutY, calloutX + calloutW, calloutY + calloutH, dpToPx(4), dpToPx(4), pointPaint);
            pointPaint.setStyle(Paint.Style.FILL);

            textPaint.setColor(0xFFF8FAFC);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(callout, calloutX + calloutW / 2f, calloutY + dpToPx(12), textPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
        }

        // 6. Draw X-Axis Time Labels
        if (!entries.isEmpty()) {
            String startTimeStr = entries.get(0).formatTimeOnly();
            String endTimeStr = "Jetzt (" + entries.get(entries.size() - 1).formatTimeOnly() + ")";
            canvas.drawText(startTimeStr, padLeft + dpToPx(10), h - dpToPx(6), labelPaint);

            if (entries.size() > 2) {
                int midIdx = entries.size() / 2;
                String midTimeStr = entries.get(midIdx).formatTimeOnly();
                float midRatio = (float) (entries.get(midIdx).timestamp - minTime) / (float) timeSpan;
                canvas.drawText(midTimeStr, padLeft + midRatio * plotW, h - dpToPx(6), labelPaint);
            }

            labelPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(endTimeStr, w - padRight, h - dpToPx(6), labelPaint);
            labelPaint.setTextAlign(Paint.Align.CENTER);
        }
    }
}
