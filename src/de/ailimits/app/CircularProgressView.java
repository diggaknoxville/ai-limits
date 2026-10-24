package de.ailimits.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularProgressView extends View {

    private float progress = 0f; // 0.0 to 1.0
    private boolean showUsed = false;
    private Paint trackPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private RectF arcBounds = new RectF();

    private int trackColor = 0xFF1E293B;
    private int greenColor = 0xFF10B981;
    private int amberColor = 0xFFF59E0B;
    private int redColor = 0xFFEF4444;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(trackColor);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setProgress(float fraction) {
        if (fraction < 0f) fraction = 0f;
        if (fraction > 1f) fraction = 1f;
        this.progress = fraction;
        invalidate();
    }

    public void setShowUsed(boolean showUsed) {
        this.showUsed = showUsed;
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = (int) (80 * getResources().getDisplayMetrics().density);
        int width = resolveSize(defaultSize, widthMeasureSpec);
        int height = resolveSize(defaultSize, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float strokeWidth = width * 0.11f;
        trackPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeWidth(strokeWidth);

        float padding = strokeWidth / 2f + 4f;
        arcBounds.set(padding, padding, width - padding, height - padding);

        // Draw background circle track
        canvas.drawOval(arcBounds, trackPaint);

        // Select color based on quota percentage (remaining basis)
        if (progress > 0.50f) {
            progressPaint.setColor(greenColor);
        } else if (progress > 0.15f) {
            progressPaint.setColor(amberColor);
        } else {
            progressPaint.setColor(redColor);
        }

        // Draw active arc from -90 degrees (top)
        float drawFraction = showUsed ? (1.0f - progress) : progress;
        float sweepAngle = drawFraction * 360f;
        if (sweepAngle > 0.5f) {
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint);
        }

        // Draw percentage text in center
        int percent = Math.round(drawFraction * 100f);
        textPaint.setTextSize(width * 0.28f);
        float textY = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(percent + "%", width / 2f, textY, textPaint);
    }
}

