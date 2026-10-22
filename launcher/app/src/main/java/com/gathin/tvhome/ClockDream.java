package com.gathin.tvhome;

import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Locale;
import java.util.Random;

/**
 * Screensaver: a grey analog clock (see {@link ClockFace}) with the time and a short date beneath.
 * It sits in the middle of the screen and every minute shifts a few pixels around the centre, so
 * nothing burns in while it still reads as centred. No network, no images: cheap enough for a 1 GB TV.
 *
 * <p>With a prayer_zone in the theme it shows {@link IslamicDream} instead: the time, prayer times and
 * a verse of the Quran over a lattice of eight-pointed stars.
 *
 * <p>Everything is sized from the screen's height, in the units the design was drawn in: a screen
 * 380 units tall.
 */
public class ClockDream extends DreamService {

    private static final long MOVE_MS = 60_000L;
    private static final float SCREEN_UNITS = 380f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private View content;
    private ClockFace face;
    /** The Islamic face (IslamicDream), shown instead of the analog clock when a theme sets prayer_zone. */
    private IslamicDream islamic;
    /** The azan at prayer times (Azan), or null when the theme has no prayer_zone or turns it off. */
    private Azan azan;
    private String prayerZone;
    private boolean reducedMotion;
    private static final long AZAN_CHECK_MS = 20_000L;
    private final Runnable azanCheck = new Runnable() {
        @Override
        public void run() {
            int due = Azan.takeDue(ClockDream.this, prayerZone);
            if (due >= 0) {
                // The clock steps aside and the screen comes up to full brightness for the azan.
                handler.removeCallbacks(tick);
                content.animate().cancel();
                content.animate().alpha(0f).setDuration(800).start();
                setScreenBright(true);
                azan.start(ClockDream.this, prayerZone, due, reducedMotion);
            }
            handler.postDelayed(this, AZAN_CHECK_MS);
        }
    };

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            moveClock();
            handler.postDelayed(this, MOVE_MS);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        Config cfg = Config.load(this);
        if (!cfg.prayerZone.isEmpty()) {
            setContentView(R.layout.dream_islamic);
            islamic = new IslamicDream(this, cfg.prayerZone);
            content = islamic.content;
        } else {
            setContentView(R.layout.dream_clock);
            content = findViewById(R.id.dreamContent);
        }
        if (cfg.azan && !cfg.prayerZone.isEmpty()) {
            prayerZone = cfg.prayerZone;
            reducedMotion = cfg.reducedMotion;
            azan = new Azan((ViewGroup) content.getParent(), () -> {
                setScreenBright(false);
                content.animate().alpha(1f).setDuration(1200).start();
                handler.removeCallbacks(tick);
                handler.postDelayed(tick, MOVE_MS);
            });
        }
        View screen = (View) content.getParent();
        if (islamic != null) {
            screen.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (r - l != or - ol || b - t != ob - ot) islamic.size(r - l, b - t);
            });
        } else {
            face = new ClockFace(this);
            ((ViewGroup) content).addView(face, 0, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextClock date = findViewById(R.id.dreamDate);
            // The month in short and the year (in capitals, see dream_clock.xml): OCT 21, 2026.
            String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdyyyy");
            date.setFormat12Hour(pattern);
            date.setFormat24Hour(pattern);
            screen.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (b - t != ob - ot) size(b - t);
            });
        }
        // Re-centre once the clock has its size for this screen.
        content.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol || b - t != ob - ot) placeClock();
        });
    }

    /** Lays the clock out for a screen {@code height} pixels tall. */
    private void size(int height) {
        float u = height / SCREEN_UNITS;
        face.setRimRadius(83.6f * u);
        TextView time = findViewById(R.id.dreamTime);
        TextView amPm = findViewById(R.id.dreamAmPm);
        TextView date = findViewById(R.id.dreamDate);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, 44f * u);
        amPm.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * u);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);

        // The tops of the digits sit 34 units under the rim, below the room the dial keeps for its shadow.
        LinearLayout.LayoutParams row = (LinearLayout.LayoutParams) findViewById(R.id.dreamTimeRow).getLayoutParams();
        row.topMargin = Math.round(34f * u - face.shadowRoom() - capInset(time, "0"));
        // AM/PM's capitals start where the digits do.
        LinearLayout.LayoutParams ampm = (LinearLayout.LayoutParams) amPm.getLayoutParams();
        ampm.leftMargin = Math.round(5f * u);
        amPm.setTranslationY(capInset(time, "0") - capInset(amPm, "PM"));
        // The date's capitals start 16.5 units under the digits' baseline.
        LinearLayout.LayoutParams dateRow = (LinearLayout.LayoutParams) date.getLayoutParams();
        dateRow.topMargin = Math.round(16.5f * u - time.getPaint().getFontMetrics().descent - capInset(date, "O"));
        content.requestLayout();
    }

    /** How far below the top of {@code view} the tops of {@code letters} are drawn. */
    static float capInset(TextView view, String letters) {
        Paint paint = view.getPaint();
        Rect bounds = new Rect();
        paint.getTextBounds(letters, 0, letters.length(), bounds);
        return bounds.top - paint.getFontMetrics().ascent;
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        content.setAlpha(0f);
        content.post(() -> {
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        });
        if (face != null) face.start();
        handler.postDelayed(tick, MOVE_MS);
        Azan.dreaming = true;
        if (azan != null) handler.post(azanCheck);
    }

    @Override
    public void onDreamingStopped() {
        handler.removeCallbacks(tick);
        handler.removeCallbacks(azanCheck);
        if (azan != null) azan.stop();
        Azan.dreaming = false;
        if (face != null) face.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        if (islamic != null) islamic.release();
        handler.removeCallbacks(azanCheck);
        if (azan != null) azan.stop();
        super.onDetachedFromWindow();
    }

    private void moveClock() {
        content.animate().alpha(0f).setDuration(900).withEndAction(() -> {
            if (islamic != null) islamic.minute();
            placeClock();
            content.animate().alpha(1f).setDuration(1200).start();
        }).start();
    }

    /** The middle of the screen, nudged up to 1.5 % either way. */
    private void placeClock() {
        View parent = (View) content.getParent();
        float nudgeX = parent.getWidth() * 0.015f * (2f * random.nextFloat() - 1f);
        float nudgeY = parent.getHeight() * 0.015f * (2f * random.nextFloat() - 1f);
        float x = (parent.getWidth() - content.getWidth()) / 2f + nudgeX;
        float y = (parent.getHeight() - content.getHeight()) / 2f + nudgeY;
        content.setX(x);
        content.setY(y);
    }
}
