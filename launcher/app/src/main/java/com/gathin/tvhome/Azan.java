package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;

import java.io.File;
import java.util.Calendar;

/**
 * The call to prayer at each of the day's prayer times (PrayerTimes): {@link AzanView} over the
 * screen and, when the theme has one, azan.mp3 (azan-subuh.mp3 for Subuh) from the theme folder.
 * Shown once per prayer, by the screensaver or by Home, whichever is in front when the time comes;
 * over any other app, AzanOverlay shows it as a band across the bottom of the screen.
 */
final class Azan {
    private static final String TAG = "TvHome.Azan";
    /** Minutes after the prayer time in which the azan still starts, so a late tick does not miss it. */
    private static final int WINDOW_MIN = 3;
    /** How long the animation stays without a sound file, and after the sound has ended. */
    private static final long SILENT_MS = 90_000L, AFTER_SOUND_MS = 8_000L;

    private final ViewGroup parent;
    private final Runnable onEnd;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finish = this::stop;
    private AzanView view;
    private MediaPlayer player;
    /**
     * True while Home or the screensaver is on screen: they give the azan themselves, full screen.
     * Otherwise another app is in front, and AzanOverlay gives it over that app.
     */
    static volatile boolean homeResumed, dreaming;

    static boolean homeInFront() {
        return homeResumed || dreaming;
    }

    Azan(ViewGroup parent, Runnable onEnd) {
        this.parent = parent;
        this.onEnd = onEnd;
    }

    /**
     * The prayer whose azan is due now and has not been given yet, or -1. Marks it given, so the
     * screensaver and Home never both start it.
     */
    static int takeDue(Context ctx, String zone) {
        if (zone == null || zone.isEmpty()) return -1;
        Calendar now = Calendar.getInstance();
        PrayerTimes.Day today = PrayerTimes.day(ctx, zone, PrayerTimes.key(now));
        if (today == null) return -1;
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        // To try it out: touch azan-now in the theme folder, and the next prayer's azan starts
        // at the next check.
        File dir = Config.dir(ctx);
        File test = dir != null ? new File(dir, "azan-now") : null;
        if (test != null && test.delete()) {
            int next = PrayerTimes.next(today.minutes, nowMin);
            return next >= 0 ? next : 0;
        }
        for (int i = 0; i < today.minutes.length; i++) {
            if (nowMin < today.minutes[i] || nowMin >= today.minutes[i] + WINDOW_MIN) continue;
            String key = PrayerTimes.key(now) + "#" + i;
            SharedPreferences p = ctx.getSharedPreferences("azan", Context.MODE_PRIVATE);
            if (key.equals(p.getString("given", ""))) return -1;
            p.edit().putString("given", key).commit();
            return i;
        }
        return -1;
    }

    boolean showing() {
        return view != null;
    }

    /** Shows the azan for prayer {@code index} and plays its sound, if the theme has one. */
    void start(Context ctx, String zone, int index, boolean reducedMotion) {
        stop();
        Calendar now = Calendar.getInstance();
        PrayerTimes.Day today = PrayerTimes.day(ctx, zone, PrayerTimes.key(now));
        String time = today != null ? PrayerTimes.format(today.minutes[index],
                android.text.format.DateFormat.is24HourFormat(ctx)) : "";
        if (today != null && !android.text.format.DateFormat.is24HourFormat(ctx)) {
            time += today.minutes[index] < 12 * 60 ? " AM" : " PM";
        }
        view = new AzanView(ctx, PrayerTimes.NAMES[index], time, today != null ? today.hijri : "", reducedMotion);
        parent.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        view.start();
        Log.i(TAG, "azan " + PrayerTimes.NAMES[index]);

        File sound = sound(ctx, index);
        if (sound == null) {
            handler.postDelayed(finish, SILENT_MS);
            return;
        }
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
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
            releasePlayer();
            handler.postDelayed(finish, SILENT_MS);
        }
    }

    /** Stops the sound and fades the animation away. Safe to call when nothing is showing. */
    void stop() {
        handler.removeCallbacks(finish);
        releasePlayer();
        if (view == null) return;
        final AzanView v = view;
        view = null;
        v.animate().alpha(0f).setDuration(800).withEndAction(() -> {
            v.stop();
            parent.removeView(v);
        }).start();
        if (onEnd != null) onEnd.run();
    }

    private void releasePlayer() {
        if (player == null) return;
        try {
            player.stop();
        } catch (IllegalStateException ignored) {
            // not prepared yet
        }
        player.release();
        player = null;
    }

    /** azan-subuh.mp3 for Subuh when the theme has it, otherwise azan.mp3, otherwise null. */
    static File sound(Context ctx, int index) {
        File dir = Config.dir(ctx);
        if (dir == null) return null;
        if (index == 0) {
            File subuh = new File(dir, "azan-subuh.mp3");
            if (subuh.isFile() && subuh.length() > 0) return subuh;
        }
        File azan = new File(dir, "azan.mp3");
        return azan.isFile() && azan.length() > 0 ? azan : null;
    }
}
