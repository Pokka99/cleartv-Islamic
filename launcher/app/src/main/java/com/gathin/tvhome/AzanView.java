package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

/**
 * The azan screen: a gold crescent and star breathing in a soft glow, rings of light spreading out
 * from it like the call going out over the town, and the prayer's name beneath. Drawn on a canvas,
 * one frame at a time while it is on screen, so it costs nothing once it is gone.
 *
 * <p>Sized from the screen's height, like the screensaver's clock.
 */
final class AzanView extends View {
    private static final long RING_MS = 5200L;
    private static final int RINGS = 3;
    private static final int GOLD = 0xFFE9CB7E, GOLD_DEEP = 0xFFC9973F;

    private final String name, line;
    private final boolean reducedMotion;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint info = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path crescent = new Path(), star = new Path();
    private final Paint backgroundPaint = new Paint();
    private long startedAt;
    private boolean running;
    private int sizedFor;
    private float cx, cy, moonR;

    AzanView(Context ctx, String name, String time, String hijri, boolean reducedMotion) {
        super(ctx);
        this.name = name;
        this.line = hijri.isEmpty() ? time : time + "   ·   " + hijri;
        this.reducedMotion = reducedMotion;
        Typeface outfit = ctx.getResources().getFont(R.font.outfit);
        for (Paint p : new Paint[]{small, big, info}) {
            p.setTypeface(outfit);
            p.setTextAlign(Paint.Align.CENTER);
        }
        small.setColor(0xCCF3E3B5);
        small.setLetterSpacing(0.32f);
        big.setColor(Color.WHITE);
        big.setLetterSpacing(0.02f);
        info.setColor(0xB3FFFFFF);
        info.setLetterSpacing(0.06f);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(GOLD);
        setAlpha(0f);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription("Telah masuk waktu " + name);
    }

    void start() {
        startedAt = SystemClock.uptimeMillis();
        running = true;
        animate().alpha(1f).setDuration(1500).start();
        postInvalidateOnAnimation();
    }

    void stop() {
        running = false;
    }

    private void size(int w, int h) {
        sizedFor = w * 31 + h;
        float u = h / 380f;
        cx = w / 2f;
        cy = h * 0.36f;
        moonR = 42f * u;
        // Night teal, lighter behind the moon and falling to near black at the edges.
        backgroundPaint.setShader(new RadialGradient(cx, cy, Math.max(w, h) * 0.75f,
                new int[]{0xFF154A3E, 0xFF0A2620, 0xFF040E0C}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        glow.setShader(new RadialGradient(cx, cy, moonR * 3.2f,
                new int[]{0x66E9CB7E, 0x22E9CB7E, 0x00E9CB7E}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        fill.setShader(new LinearGradient(cx - moonR, cy - moonR, cx + moonR, cy + moonR,
                GOLD, GOLD_DEEP, Shader.TileMode.CLAMP));

        // The crescent: the moon's disc less a disc shifted up and to the right.
        crescent.reset();
        Path cut = new Path();
        crescent.addCircle(cx, cy, moonR, Path.Direction.CW);
        cut.addCircle(cx + moonR * 0.42f, cy - moonR * 0.18f, moonR * 0.84f, Path.Direction.CW);
        crescent.op(cut, Path.Op.DIFFERENCE);
        // A five-pointed star inside the crescent's opening.
        star.reset();
        float sx = cx + moonR * 0.52f, sy = cy - moonR * 0.06f, outer = moonR * 0.28f, inner = outer * 0.42f;
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 5 * i - Math.PI / 2;
            float r = i % 2 == 0 ? outer : inner;
            float x = sx + (float) (Math.cos(a) * r), y = sy + (float) (Math.sin(a) * r);
            if (i == 0) star.moveTo(x, y);
            else star.lineTo(x, y);
        }
        star.close();

        small.setTextSize(13f * u);
        big.setTextSize(58f * u);
        info.setTextSize(14f * u);
        ring.setStrokeWidth(1.6f * u);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        if (sizedFor != w * 31 + h) size(w, h);
        long t = SystemClock.uptimeMillis() - startedAt;
        canvas.drawRect(0, 0, w, h, backgroundPaint);

        // Rings spreading out from the moon, each fading as it widens.
        float maxR = Math.max(w, h) * 0.62f;
        for (int i = 0; i < RINGS; i++) {
            float phase = ((t + i * RING_MS / RINGS) % RING_MS) / (float) RING_MS;
            float r = moonR * 1.25f + (maxR - moonR * 1.25f) * ease(phase);
            ring.setAlpha((int) (150 * (1f - phase) * (1f - phase)));
            canvas.drawCircle(cx, cy, r, ring);
        }

        // The moon breathes: a slow swell of its glow and a slight change of size.
        float breath = reducedMotion ? 0f : (float) Math.sin(t / 1600.0 * Math.PI) * 0.5f + 0.5f;
        glow.setAlpha((int) (170 + 85 * breath));
        canvas.drawCircle(cx, cy, moonR * 3.2f, glow);
        float s = 1f + 0.035f * breath;
        canvas.save();
        canvas.scale(s, s, cx, cy);
        canvas.drawPath(crescent, fill);
        canvas.drawPath(star, fill);
        canvas.restore();

        float u = h / 380f;
        float y = cy + moonR + 50f * u;
        canvas.drawText("TELAH MASUK WAKTU", cx, y, small);
        canvas.drawText(name, cx, y + 62f * u, big);
        canvas.drawText(line, cx, y + 92f * u, info);

        if (running) postInvalidateOnAnimation();
    }

    /** Fast at first, slowing as it spreads. */
    private static float ease(float x) {
        return 1f - (1f - x) * (1f - x) * (1f - x);
    }
}
