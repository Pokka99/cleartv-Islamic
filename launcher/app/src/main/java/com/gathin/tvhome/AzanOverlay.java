package com.gathin.tvhome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.File;
import java.util.Calendar;

/**
 * The azan over whatever app is in front (YouTube, Netflix, Live TV): a band across the lower third
 * of the screen, a gold crescent breathing in it, the prayer's name and time, and the azan playing.
 * The app underneath is asked to pause (audio focus) and carries on afterwards. Any button on the
 * remote stops it; it also ends by itself once the azan has finished.
 *
 * <p>Started by AzanAlarm at the prayer time. Needs "display over other apps", which the install
 * scripts grant ({@code appops set com.gathin.tvhome SYSTEM_ALERT_WINDOW allow}). Runs only for the
 * length of the azan.
 */
public class AzanOverlay extends Service {
    private static final String TAG = "TvHome.Azan";
    private static final String CHANNEL = "azan";
    private static final long SILENT_MS = 60_000L, AFTER_SOUND_MS = 4_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finish = this::finish;
    private WindowManager wm;
    private Band band;
    private MediaPlayer player;
    private AudioManager audio;
    private AudioFocusRequest focus;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        goForeground();
        if (band != null) return START_NOT_STICKY;
        Config cfg = Config.load(this);
        int index = Azan.takeDue(this, cfg.prayerZone);
        if (index < 0 || !Settings.canDrawOverlays(this)) {
            if (index >= 0) Log.w(TAG, "no permission to show over other apps; run appops set "
                    + getPackageName() + " SYSTEM_ALERT_WINDOW allow");
            stopSelf();
            return START_NOT_STICKY;
        }
        show(cfg.prayerZone, index);
        play(index);
        Log.i(TAG, "azan " + PrayerTimes.NAMES[index] + " over another app");
        return START_NOT_STICKY;
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Azan", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        Notification n = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Azan").build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }
    }

    private void show(String zone, int index) {
        PrayerTimes.Day today = PrayerTimes.day(this, zone, PrayerTimes.key(Calendar.getInstance()));
        boolean is24 = android.text.format.DateFormat.is24HourFormat(this);
        String time = today != null ? PrayerTimes.format(today.minutes[index], is24)
                + (is24 ? "" : today.minutes[index] < 12 * 60 ? " AM" : " PM") : "";
        String line = today == null || today.hijri.isEmpty() ? time : time + "   ·   " + today.hijri;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        band = new Band(this, PrayerTimes.NAMES[index], line);
        int h = getResources().getDisplayMetrics().heightPixels;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, Math.round(h * 0.34f),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                // Focusable, so the remote's next press comes here and stops the azan.
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM;
        wm.addView(band, lp);
        band.enter();
    }

    private void play(int index) {
        File sound = Azan.sound(this, index);
        if (sound == null) {
            handler.postDelayed(finish, SILENT_MS);
            return;
        }
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
        // The video underneath pauses for the azan, and may carry on after it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attrs).build();
            audio.requestAudioFocus(focus);
        }
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(attrs);
            player.setDataSource(sound.getPath());
            player.setOnCompletionListener(mp -> handler.postDelayed(finish, AFTER_SOUND_MS));
            player.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "sound failed: " + what + "/" + extra);
                handler.postDelayed(finish, SILENT_MS);
                return true;
            });
            player.setOnPreparedListener(MediaPlayer::start);
            player.prepareAsync();
        } catch (Exception e) {
            Log.w(TAG, "cannot play " + sound + ": " + e.getMessage());
            handler.postDelayed(finish, SILENT_MS);
        }
    }

    /** Stops the sound, lets the app underneath have its sound back and slides the band away. */
    private void finish() {
        handler.removeCallbacks(finish);
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
                // not prepared yet
            }
            player.release();
            player = null;
        }
        if (focus != null && audio != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audio.abandonAudioFocusRequest(focus);
            focus = null;
        }
        if (band == null) {
            stopSelf();
            return;
        }
        final Band b = band;
        band = null;
        b.leave(() -> {
            try {
                wm.removeView(b);
            } catch (IllegalArgumentException ignored) {
                // already gone
            }
            stopSelf();
        });
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(finish);
        if (band != null) {
            try {
                wm.removeView(band);
            } catch (IllegalArgumentException ignored) {
                // already gone
            }
            band = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    /**
     * The band: night green fading up into the picture, a thin gold rule, a breathing crescent and
     * star, "TELAH MASUK WAKTU", the prayer's name, its time and the Hijri date, and how to stop it.
     */
    private final class Band extends FrameLayout {
        private final String name, line;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rule = new Paint(Paint.ANTI_ALIAS_FLAG), shade = new Paint();
        private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG), big = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint info = new Paint(Paint.ANTI_ALIAS_FLAG), hint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path crescent = new Path(), star = new Path();
        private final long startedAt = SystemClock.uptimeMillis();
        private int sizedFor;
        private float cx, cy, r, textX, u;
        private boolean running = true;

        Band(Context ctx, String name, String line) {
            super(ctx);
            this.name = name;
            this.line = line;
            setWillNotDraw(false);
            setFocusable(true);
            setFocusableInTouchMode(true);
            Typeface outfit = ctx.getResources().getFont(R.font.outfit);
            for (Paint p : new Paint[]{small, big, info, hint}) p.setTypeface(outfit);
            small.setColor(0xFFE9CB7E);
            small.setLetterSpacing(0.3f);
            big.setColor(0xFFFFFFFF);
            info.setColor(0xCCF4EEDC);
            info.setLetterSpacing(0.04f);
            hint.setColor(0x99F4EEDC);
            hint.setTextAlign(Paint.Align.RIGHT);
            rule.setColor(0x99E9CB7E);
            setAlpha(0f);
            setContentDescription("Telah masuk waktu " + name);
        }

        void enter() {
            setTranslationY(80f);
            animate().alpha(1f).translationY(0f).setDuration(700).start();
            post(this::requestFocus);
            postInvalidateOnAnimation();
        }

        void leave(Runnable done) {
            running = false;
            animate().alpha(0f).translationY(60f).setDuration(600).withEndAction(done).start();
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            // Any button stops the azan; that press does nothing else.
            if (event.getAction() == KeyEvent.ACTION_DOWN) finish();
            return true;
        }

        private void size(int w, int h) {
            sizedFor = w * 31 + h;
            u = h / 130f;
            r = 24f * u;
            cx = 70f * u;
            cy = h * 0.62f;
            textX = cx + r * 2.4f;
            shade.setShader(new LinearGradient(0, 0, 0, h, new int[]{0x00061813, 0xD9061813, 0xF7040E0C},
                    new float[]{0f, 0.38f, 1f}, Shader.TileMode.CLAMP));
            glow.setShader(new RadialGradient(cx, cy, r * 2.6f, new int[]{0x66E9CB7E, 0x00E9CB7E}, null,
                    Shader.TileMode.CLAMP));
            fill.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r, 0xFFE9CB7E, 0xFFC9973F,
                    Shader.TileMode.CLAMP));
            crescent.reset();
            Path cut = new Path();
            crescent.addCircle(cx, cy, r, Path.Direction.CW);
            cut.addCircle(cx + r * 0.42f, cy - r * 0.18f, r * 0.84f, Path.Direction.CW);
            crescent.op(cut, Path.Op.DIFFERENCE);
            star.reset();
            float sx = cx + r * 0.52f, sy = cy - r * 0.06f, outer = r * 0.28f, inner = outer * 0.42f;
            for (int i = 0; i < 10; i++) {
                double a = Math.PI / 5 * i - Math.PI / 2;
                float rr = i % 2 == 0 ? outer : inner;
                float x = sx + (float) (Math.cos(a) * rr), y = sy + (float) (Math.sin(a) * rr);
                if (i == 0) star.moveTo(x, y);
                else star.lineTo(x, y);
            }
            star.close();
            small.setTextSize(8f * u);
            big.setTextSize(30f * u);
            info.setTextSize(9f * u);
            hint.setTextSize(7.5f * u);
            rule.setStrokeWidth(Math.max(1f, 0.6f * u));
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;
            if (sizedFor != w * 31 + h) size(w, h);
            c.drawRect(0, 0, w, h, shade);
            float top = h * 0.30f;
            c.drawLine(cx - r, top, w - cx + r, top, rule);
            long t = SystemClock.uptimeMillis() - startedAt;
            float breath = (float) Math.sin(t / 1600.0 * Math.PI) * 0.5f + 0.5f;
            glow.setAlpha((int) (150 + 105 * breath));
            c.drawCircle(cx, cy, r * 2.6f, glow);
            float s = 1f + 0.04f * breath;
            c.save();
            c.scale(s, s, cx, cy);
            c.drawPath(crescent, fill);
            c.drawPath(star, fill);
            c.restore();
            float baseName = cy + 10f * u;
            c.drawText("TELAH MASUK WAKTU", textX, cy - 18f * u, small);
            c.drawText(name, textX, baseName, big);
            c.drawText(line, textX + big.measureText(name) + 14f * u, baseName, info);
            c.drawText("Tekan mana-mana butang untuk hentikan azan", w - cx + r, h - 12f * u, hint);
            if (running) postInvalidateOnAnimation();
        }
    }
}
