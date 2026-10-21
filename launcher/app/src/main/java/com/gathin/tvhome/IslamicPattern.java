package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * The Islamic screensaver's background: a lattice of eight-pointed stars (two squares turned 45°
 * against each other, linked by their points) in faint gold on night green, darker at the edges.
 * One small tile is drawn once and repeated by a shader; drifting it a few pixels each minute keeps
 * the lines from burning in and costs nothing between frames.
 */
final class IslamicPattern extends View {
    private final Paint pattern = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint shade = new Paint();
    private final Matrix shift = new Matrix();
    private Bitmap tile;
    private int sizedFor;
    private float dx, dy;

    IslamicPattern(Context ctx) {
        super(ctx);
    }

    /** Moves the lattice by a little; the shader repeats, so any offset is seamless. */
    void drift(float x, float y) {
        dx += x;
        dy += y;
        shift.setTranslate(dx, dy);
        if (pattern.getShader() != null) pattern.getShader().setLocalMatrix(shift);
        invalidate();
    }

    private void size(int w, int h) {
        sizedFor = w * 31 + h;
        int s = Math.max(48, Math.round(h / 7.5f));
        if (tile != null) tile.recycle();
        tile = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(tile);
        c.drawColor(0xFF0A2620);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1f, s / 110f));
        line.setColor(0x38E9CB7E);
        float m = s / 2f, r = s * 0.30f;
        // The star in the middle of the tile: two squares, one turned 45 degrees.
        c.drawPath(square(m, m, r, 0), line);
        c.drawPath(square(m, m, r, 45), line);
        // Its points reach out to the stars of the neighbouring tiles.
        float tip = r * 1.4142f;
        c.drawLine(m + tip, m, s, m, line);
        c.drawLine(m - tip, m, 0, m, line);
        c.drawLine(m, m + tip, m, s, line);
        c.drawLine(m, m - tip, m, 0, line);
        // A small rosette where four tiles meet.
        float q = s * 0.11f;
        for (float[] p : new float[][]{{0, 0}, {s, 0}, {0, s}, {s, s}}) {
            c.drawPath(square(p[0], p[1], q, 45), line);
        }
        line.setColor(0x24E9CB7E);
        c.drawCircle(m, m, r * 0.55f, line);
        BitmapShader shader = new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        shader.setLocalMatrix(shift);
        pattern.setShader(shader);
        // Lighter behind the middle, where the clock and the verse are; near black at the corners.
        shade.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.7f,
                new int[]{0x00000000, 0x55000000, 0xE6020806}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
    }

    private static Path square(float cx, float cy, float r, float degrees) {
        Path p = new Path();
        double a0 = Math.toRadians(degrees + 45);
        for (int i = 0; i < 4; i++) {
            double a = a0 + Math.PI / 2 * i;
            float x = cx + (float) (Math.cos(a) * r * 1.4142f), y = cy + (float) (Math.sin(a) * r * 1.4142f);
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        if (sizedFor != w * 31 + h) size(w, h);
        canvas.drawRect(0, 0, w, h, pattern);
        canvas.drawRect(0, 0, w, h, shade);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (tile != null) {
            pattern.setShader(null);
            tile.recycle();
            tile = null;
            sizedFor = 0;
        }
    }
}
