package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.util.Calendar;

/**
 * Wakes the launcher at the next prayer time, so the azan comes even while another app (YouTube,
 * Netflix, Live TV) is in front: then {@link AzanOverlay} shows it over that app. While Home or the
 * screensaver is in front they give the azan themselves, and the alarm only sets the next one.
 *
 * <p>Set again whenever Home starts, after every alarm, and when the TV boots.
 */
final class AzanAlarm {
    private static final String TAG = "TvHome.Azan";
    static final String ACTION = "com.gathin.tvhome.AZAN";

    private AzanAlarm() {
    }

    /** Sets the alarm for the next prayer time after now; clears it when the azan is off. */
    static void scheduleNext(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, new Intent(ACTION).setClass(ctx, Receiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Config cfg = Config.load(ctx);
        long at = cfg.azan && cfg.azanOverlay ? next(ctx, cfg.prayerZone) : -1;
        if (at < 0) {
            am.cancel(pi);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Without the exact-alarm permission (newer Android), within a minute of the time.
            am.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        Log.i(TAG, "next azan alarm at " + new java.util.Date(at));
    }

    /** The next prayer time after now, today or tomorrow, a second past the minute; -1 when unknown. */
    private static long next(Context ctx, String zone) {
        if (zone == null || zone.isEmpty()) return -1;
        Calendar now = Calendar.getInstance();
        for (int day = 0; day < 2; day++) {
            Calendar d = (Calendar) now.clone();
            d.add(Calendar.DAY_OF_MONTH, day);
            PrayerTimes.Day times = PrayerTimes.day(ctx, zone, PrayerTimes.key(d));
            if (times == null) continue;
            for (int m : times.minutes) {
                Calendar t = (Calendar) d.clone();
                t.set(Calendar.HOUR_OF_DAY, m / 60);
                t.set(Calendar.MINUTE, m % 60);
                t.set(Calendar.SECOND, 1);
                t.set(Calendar.MILLISECOND, 0);
                if (t.after(now)) return t.getTimeInMillis();
            }
        }
        return -1;
    }

    /** The alarm itself, and the TV starting up or the launcher being updated. */
    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (ACTION.equals(intent.getAction())) {
                PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                boolean screenOn = pm == null || pm.isInteractive();
                // Home and the screensaver give it themselves; with the screen off, nobody hears it.
                if (!Azan.homeInFront() && screenOn) {
                    Intent svc = new Intent(ctx, AzanOverlay.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(svc);
                    else ctx.startService(svc);
                }
            }
            scheduleNext(ctx);
        }
    }
}
