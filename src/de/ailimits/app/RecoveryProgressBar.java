package de.ailimits.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class RecoveryProgressBar extends View {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bgRect = new RectF();
    private final RectF progressRect = new RectF();

    private float progress = 0.0f; // 0.0 (noch lange, rot) bis 1.0 (voll erholt/kurz, grün)

    public RecoveryProgressBar(Context context) {
        super(context);
        init();
    }

    public RecoveryProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecoveryProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint.setColor(0xFF1E2430);
        bgPaint.setStyle(Paint.Style.FILL);

        progressPaint.setStyle(Paint.Style.FILL);
    }

    public void setProgress(float fraction) {
        this.progress = Math.max(0.0f, Math.min(1.0f, fraction));
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public static int interpolateColor(int colorStart, int colorEnd, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        int a = (int) (Color.alpha(colorStart) + ratio * (Color.alpha(colorEnd) - Color.alpha(colorStart)));
        int r = (int) (Color.red(colorStart) + ratio * (Color.red(colorEnd) - Color.red(colorStart)));
        int g = (int) (Color.green(colorStart) + ratio * (Color.green(colorEnd) - Color.green(colorStart)));
        int b = (int) (Color.blue(colorStart) + ratio * (Color.blue(colorEnd) - Color.blue(colorStart)));
        return Color.argb(a, r, g, b);
    }

    public static int getRecoveryColor(float progress) {
        // Rot (noch lange, 0.0) -> Gelb (halb, 0.5) -> Fließend Grün (noch kurz, 1.0)
        if (progress < 0.5f) {
            return interpolateColor(0xFFEF4444, 0xFFF59E0B, progress * 2.0f);
        } else {
            return interpolateColor(0xFFF59E0B, 0xFF10B981, (progress - 0.5f) * 2.0f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float radius = h / 2f;

        // Background track
        bgRect.set(0, 0, w, h);
        canvas.drawRoundRect(bgRect, radius, radius, bgPaint);

        // Progress bar with dynamic color interpolation
        float progressWidth = w * progress;
        if (progressWidth > 0) {
            progressPaint.setColor(getRecoveryColor(progress));
            progressRect.set(0, 0, progressWidth, h);
            canvas.drawRoundRect(progressRect, radius, radius, progressPaint);
        }
    }
}
