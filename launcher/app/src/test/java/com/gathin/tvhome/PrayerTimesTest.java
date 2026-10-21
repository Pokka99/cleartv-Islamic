package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** e-Solat's dates and times, read into what the row under the clock shows. */
public class PrayerTimesTest {

    @Test
    public void readsMalayAndEnglishDates() {
        assertEquals("21-10-2026", PrayerTimes.dateKey("21-Okt-2026"));
        assertEquals("01-08-2026", PrayerTimes.dateKey("01-Ogos-2026"));
        assertEquals("05-03-2026", PrayerTimes.dateKey("05-Mac-2026"));
        assertEquals("31-12-2026", PrayerTimes.dateKey("31-Dec-2026"));
        assertEquals("", PrayerTimes.dateKey("not a date"));
    }

    @Test
    public void readsTimesAndHijri() {
        assertEquals(781, PrayerTimes.minutes("13:01:00"));
        assertEquals(-1, PrayerTimes.minutes("25:00:00"));
        assertEquals("10 Jamadilawal 1448", PrayerTimes.hijri("1448-05-10"));
        assertEquals("1 Ramadan 1448", PrayerTimes.hijri("1448-09-01"));
    }

    @Test
    public void findsTheNextPrayerAndCountsDown() {
        int[] day = {349, 781, 978, 1140, 1210};
        assertEquals(0, PrayerTimes.next(day, 300));
        assertEquals(2, PrayerTimes.next(day, 781));
        assertEquals(-1, PrayerTimes.next(day, 1215));
        assertEquals("dalam 12 min", PrayerTimes.countdown(12));
        assertEquals("dalam 1 j 5 min", PrayerTimes.countdown(65));
        assertEquals("dalam 2 j", PrayerTimes.countdown(120));
        assertEquals("05:49", PrayerTimes.format(349, true));
        assertEquals("1:01", PrayerTimes.format(781, false));
    }
}
