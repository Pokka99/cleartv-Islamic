package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * The screensaver's Islamic face (dream_islamic.xml), used when a theme sets prayer_zone: the time,
 * the Malay and Hijri dates, today's prayer times with the next one in gold, and a verse of the Quran
 * with its translation (res/raw/ayat.json), changing every few minutes. ClockDream moves it about the
 * screen; this class fills it in and sizes it.
 */
final class IslamicDream {
    private static final String TAG = "TvHome.Dream";
    private static final long VERSE_MS = 3 * 60_000L;
    private static final String[] DAYS = {"Ahad", "Isnin", "Selasa", "Rabu", "Khamis", "Jumaat", "Sabtu"};
    private static final String[] MONTHS = {"Januari", "Februari", "Mac", "April", "Mei", "Jun", "Julai", "Ogos",
            "September", "Oktober", "November", "Disember"};

    final View content;
    private final Context ctx;
    private final String zone;
    private final IslamicPattern pattern;
    private final TextView time, day, date, hijri, next, arabic, meaning, ref;
    /** The glass card of prayer times: one cell each, its name above its time, gold rules between. */
    private final LinearLayout prayers;
    private final LinearLayout[] cells = new LinearLayout[PrayerTimes.NAMES.length];
    private final TextView[] cellNames = new TextView[cells.length], cellTimes = new TextView[cells.length];
    private final View[] rules = new View[cells.length - 1];
    private int nextShown = -2;
    private final View verse;
    private final List<String[]> verses = new ArrayList<>();
    private final Random random = new Random();
    private int verseIndex;
    private long verseShownAt;
    /** Mosque photos from the theme's masjid/ folder (tools/masjid-photos.py), cross-faded behind it all. */
    private final List<java.io.File> photos = new ArrayList<>();
    private JSONObject photoCaptions = new JSONObject();
    /** Two photo layers, lower and upper, so one can fade over or off the other. */
    private android.widget.ImageView photoLower, photoUpper;
    private android.graphics.Bitmap lowerBitmap, upperBitmap;
    private boolean upperShown;
    private TextView photoCaption, photoCredit;
    private int photoIndex = -1, screenW, screenH;
    private boolean decoding;

    IslamicDream(android.service.dreams.DreamService dream, String zone) {
        this.ctx = dream;
        this.zone = zone;
        ViewGroup root = (ViewGroup) dream.findViewById(R.id.islamicRoot);
        pattern = new IslamicPattern(dream);
        root.addView(pattern, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        content = dream.findViewById(R.id.islamicContent);
        time = dream.findViewById(R.id.islamicTime);
        day = dream.findViewById(R.id.islamicDay);
        date = dream.findViewById(R.id.islamicDate);
        hijri = dream.findViewById(R.id.islamicHijri);
        prayers = dream.findViewById(R.id.islamicPrayers);
        Typeface outfit = dream.getResources().getFont(R.font.outfit);
        Typeface serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                rules[i - 1] = new View(dream);
                rules[i - 1].setBackgroundColor(0x80E9CB7E);
                prayers.addView(rules[i - 1]);
            }
            LinearLayout cell = new LinearLayout(dream);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            TextView name = new TextView(dream), at = new TextView(dream);
            name.setTypeface(outfit);
            name.setIncludeFontPadding(false);
            name.setText(PrayerTimes.NAMES[i]);
            name.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            at.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
            at.setTypeface(serifBold);
            at.setIncludeFontPadding(false);
            cell.addView(name);
            cell.addView(at);
            prayers.addView(cell);
            cells[i] = cell;
            cellNames[i] = name;
            cellTimes[i] = at;
        }
        next = dream.findViewById(R.id.islamicNext);
        verse = dream.findViewById(R.id.islamicVerse);
        arabic = dream.findViewById(R.id.islamicArabic);
        meaning = dream.findViewById(R.id.islamicMeaning);
        ref = dream.findViewById(R.id.islamicRef);
        loadPhotos(dream, root);
        loadVerses();
        verseIndex = verses.isEmpty() ? 0 : random.nextInt(verses.size());
        showVerse();
        update();
    }

    /** Lays the face out for a screen {@code w} by {@code h} pixels, in units of a screen 380 tall. */
    void size(int w, int h) {
        float u = h / 380f;
        boolean first = screenW == 0;
        screenW = w;
        screenH = h;
        if (photoCaption != null) {
            photoCaption.setTextSize(TypedValue.COMPLEX_UNIT_PX, 8.5f * u);
            photoCredit.setTextSize(TypedValue.COMPLEX_UNIT_PX, 5.5f * u);
            View box = (View) photoCaption.getParent();
            FrameLayout.LayoutParams bl = (FrameLayout.LayoutParams) box.getLayoutParams();
            bl.rightMargin = bl.bottomMargin = Math.round(14f * u);
            if (first) showNextPhoto();
        }
        px(R.id.islamicTime, 76f * u);
        // The clock in gold, lighter at the top of the digits, with a soft glow around it.
        float fs = time.getTextSize();
        time.getPaint().setShader(new android.graphics.LinearGradient(0, 0, 0, fs * 0.8f,
                new int[]{0xFFFBE7B0, 0xFFE9C676, 0xFFC4943F}, new float[]{0f, 0.5f, 1f},
                android.graphics.Shader.TileMode.CLAMP));
        time.setShadowLayer(14f * u, 0, 0, 0x55E9CB7E);
        // Room for the glow inside the view, or it is cut off square at the view's edges.
        int glow = Math.round(16f * u);
        time.setPadding(glow, glow, glow, glow);
        time.invalidate();
        px(R.id.islamicDay, 8.5f * u);
        px(R.id.islamicDate, 12.5f * u);
        px(R.id.islamicHijri, 12.5f * u);
        // The gold rule between the time and the dates, as tall as the digits.
        View rule = content.findViewById(R.id.islamicRule);
        LinearLayout.LayoutParams rl = (LinearLayout.LayoutParams) rule.getLayoutParams();
        rl.width = Math.max(1, Math.round(0.8f * u));
        rl.height = Math.round(44f * u);
        rl.leftMargin = 0;
        rl.rightMargin = Math.round(16f * u);
        px(R.id.islamicOrnament, 8f * u);
        int cellW = Math.round(62f * u), pad = Math.round(4f * u);
        prayers.setPadding(pad, pad, pad, pad);
        for (int i = 0; i < cells.length; i++) {
            cellNames[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 8.5f * u);
            cellTimes[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * u);
            ((LinearLayout.LayoutParams) cellTimes[i].getLayoutParams()).topMargin = Math.round(4f * u);
            cells[i].setPadding(0, Math.round(5f * u), 0, Math.round(6f * u));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) cells[i].getLayoutParams();
            lp.width = cellW;
            lp.leftMargin = lp.rightMargin = Math.round(3f * u);
            if (i > 0) {
                LinearLayout.LayoutParams rp = (LinearLayout.LayoutParams) rules[i - 1].getLayoutParams();
                rp.width = Math.max(1, Math.round(0.6f * u));
                rp.height = Math.round(24f * u);
            }
        }
        px(R.id.islamicNext, 10f * u);
        arabic.setTextSize(TypedValue.COMPLEX_UNIT_PX, 12.5f * u);
        meaning.setTextSize(TypedValue.COMPLEX_UNIT_PX, 7.5f * u);
        ref.setTextSize(TypedValue.COMPLEX_UNIT_PX, 6.5f * u);
        int wide = Math.round(w * 0.55f);
        arabic.setMaxWidth(wide);
        meaning.setMaxWidth(wide);
        top(R.id.islamicDate, 6f * u);
        top(R.id.islamicHijri, 5f * u);
        top(R.id.islamicOrnament, 16f * u);
        top(R.id.islamicPrayers, 13f * u);
        top(R.id.islamicNext, 7f * u);
        top(R.id.islamicVerse, 14f * u);
        top(R.id.islamicMeaning, 6f * u);
        top(R.id.islamicRef, 7f * u);
        content.requestLayout();
    }

    /** Once a minute: the dates and prayer times, the lattice drifted a little, a new verse when due. */
    void minute() {
        update();
        pattern.drift(random.nextFloat() * 6f - 3f, random.nextFloat() * 6f - 3f);
        if (android.os.SystemClock.elapsedRealtime() - verseShownAt >= VERSE_MS && verses.size() > 1) {
            verseIndex = (verseIndex + 1) % verses.size();
            showVerse();
            showNextPhoto();
        }
    }

    private void update() {
        Calendar now = Calendar.getInstance();
        day.setText(DAYS[now.get(Calendar.DAY_OF_WEEK) - 1]);
        date.setText(now.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[now.get(Calendar.MONTH)] + " " + now.get(Calendar.YEAR));
        PrayerTimes.Day today = PrayerTimes.day(ctx, zone, PrayerTimes.key(now));
        if (today == null) {
            hijri.setVisibility(View.GONE);
            prayers.setVisibility(View.GONE);
            next.setVisibility(View.GONE);
            return;
        }
        hijri.setText(today.hijri);
        hijri.setVisibility(today.hijri.isEmpty() ? View.GONE : View.VISIBLE);
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int n = PrayerTimes.next(today.minutes, nowMin);
        boolean is24 = android.text.format.DateFormat.is24HourFormat(ctx);
        // After Isyak, tomorrow's Subuh is the one framed.
        int framed = n >= 0 ? n : 0;
        for (int i = 0; i < cells.length; i++) {
            cellTimes[i].setText(PrayerTimes.format(today.minutes[i], is24));
            if (framed == nextShown) continue;
            boolean on = i == framed;
            cells[i].setBackgroundResource(on ? R.drawable.prayer_cell_next : 0);
            cellNames[i].setTextColor(on ? 0xFFE9CB7E : 0xB3F4EEDC);
            cellTimes[i].setTextColor(on ? 0xFFF2CF7A : 0xFFF4EEDC);
            cellTimes[i].setShadowLayer(on ? 8f : 0f, 0, 0, 0x66E9CB7E);
        }
        nextShown = framed;
        // The rules either side of the framed cell give way to its frame.
        for (int i = 0; i < rules.length; i++) {
            rules[i].setVisibility(i == framed - 1 || i == framed ? View.INVISIBLE : View.VISIBLE);
        }
        prayers.setVisibility(View.VISIBLE);
        int left;
        if (n >= 0) {
            left = today.minutes[n] - nowMin;
        } else {
            Calendar t = (Calendar) now.clone();
            t.add(Calendar.DAY_OF_MONTH, 1);
            PrayerTimes.Day tomorrow = PrayerTimes.day(ctx, zone, PrayerTimes.key(t));
            left = 24 * 60 - nowMin + (tomorrow != null ? tomorrow : today).minutes[0];
        }
        next.setText(PrayerTimes.NAMES[n >= 0 ? n : 0] + " " + PrayerTimes.countdown(left));
        next.setVisibility(View.VISIBLE);
    }

    private void showVerse() {
        verseShownAt = android.os.SystemClock.elapsedRealtime();
        if (verses.isEmpty()) {
            verse.setVisibility(View.GONE);
            return;
        }
        String[] v = verses.get(verseIndex);
        arabic.setText(v[0]);
        meaning.setText(v[1]);
        ref.setText(v[2]);
        verse.setVisibility(View.VISIBLE);
    }

    /** Frees the photo in memory; the screensaver is going away. */
    void release() {
        if (photoLower == null) return;
        photoLower.animate().cancel();
        photoUpper.animate().cancel();
        photoLower.setImageDrawable(null);
        photoUpper.setImageDrawable(null);
        if (lowerBitmap != null) lowerBitmap.recycle();
        if (upperBitmap != null) upperBitmap.recycle();
        lowerBitmap = upperBitmap = null;
    }

    private void loadPhotos(android.service.dreams.DreamService dream, ViewGroup root) {
        photos.addAll(Masjid.list(dream));
        if (photos.isEmpty()) return;
        photoCaptions = Masjid.captions(Masjid.dir(dream));
        photoIndex = random.nextInt(photos.size()) - 1;
        // The photos take the lattice's place: a photo, darkened so the gold text stays readable.
        pattern.setVisibility(View.GONE);
        FrameLayout.LayoutParams full = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        photoLower = new android.widget.ImageView(dream);
        photoUpper = new android.widget.ImageView(dream);
        for (android.widget.ImageView v : new android.widget.ImageView[]{photoLower, photoUpper}) {
            v.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            v.setAlpha(0f);
        }
        root.addView(photoLower, 1, full);
        root.addView(photoUpper, 2, new FrameLayout.LayoutParams(full));
        View dim = new View(dream);
        dim.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB8020806, 0x99020806, 0xA6020806, 0xE6020806}));
        root.addView(dim, 3, new FrameLayout.LayoutParams(full));
        // Where the photo is, and who took it, in the bottom right corner.
        LinearLayout box = new LinearLayout(dream);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.END);
        Typeface outfit = dream.getResources().getFont(R.font.outfit);
        photoCaption = new TextView(dream);
        photoCaption.setTypeface(outfit);
        photoCaption.setTextColor(0xE6E9CB7E);
        photoCaption.setLetterSpacing(0.04f);
        photoCredit = new TextView(dream);
        photoCredit.setTypeface(outfit);
        photoCredit.setTextColor(0x80F4EEDC);
        box.addView(photoCaption);
        box.addView(photoCredit);
        root.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.BOTTOM | android.view.Gravity.END));
    }

    /** Decodes the next photo off the main thread, at the screen's size, and fades it in over the last. */
    private void showNextPhoto() {
        if (photos.isEmpty() || decoding || screenW == 0) return;
        decoding = true;
        photoIndex = (photoIndex + 1) % photos.size();
        final java.io.File f = photos.get(photoIndex);
        final int w = screenW, h = screenH;
        new Thread(() -> {
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(f.getPath(), o);
            int sample = 1;
            while (o.outWidth / (sample * 2) >= w && o.outHeight / (sample * 2) >= h) sample *= 2;
            o = new android.graphics.BitmapFactory.Options();
            o.inSampleSize = sample;
            o.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;
            final android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeFile(f.getPath(), o);
            content.post(() -> {
                decoding = false;
                if (b == null || photoLower == null) return;
                if (upperShown) {
                    // The new photo goes on the lower layer, and the upper one fades off it.
                    photoLower.setImageBitmap(b);
                    photoLower.setAlpha(1f);
                    if (lowerBitmap != null) lowerBitmap.recycle();
                    lowerBitmap = b;
                    photoUpper.animate().alpha(0f).setDuration(1800).withEndAction(() -> {
                        photoUpper.setImageDrawable(null);
                        if (upperBitmap != null) upperBitmap.recycle();
                        upperBitmap = null;
                    }).start();
                } else {
                    // The new photo fades in on the upper layer, over the lower one.
                    photoUpper.setImageBitmap(b);
                    photoUpper.setAlpha(0f);
                    if (upperBitmap != null) upperBitmap.recycle();
                    upperBitmap = b;
                    photoUpper.animate().alpha(1f).setDuration(1800).withEndAction(() -> {
                        photoLower.setImageDrawable(null);
                        photoLower.setAlpha(0f);
                        if (lowerBitmap != null) lowerBitmap.recycle();
                        lowerBitmap = null;
                    }).start();
                }
                upperShown = !upperShown;
                JSONObject c = photoCaptions.optJSONObject(f.getName());
                photoCaption.setText(c != null ? c.optString("caption") : "");
                photoCredit.setText(c != null ? c.optString("credit") : "");
            });
        }, "masjid-photo").start();
    }

    private void loadVerses() {
        try (InputStream in = ctx.getResources().openRawResource(R.raw.ayat)) {
            byte[] buf = new byte[64 * 1024];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
            JSONArray a = new JSONObject(new String(buf, 0, n, StandardCharsets.UTF_8)).getJSONArray("ayat");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                verses.add(new String[]{o.getString("ar"), o.getString("tr"), o.getString("ref")});
            }
        } catch (Exception e) {
            Log.w(TAG, "no verses: " + e.getMessage());
        }
    }

    private void px(int id, float size) {
        ((TextView) content.findViewById(id)).setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    private void top(int id, float margin) {
        View v = content.findViewById(id);
        ((LinearLayout.LayoutParams) v.getLayoutParams()).topMargin = Math.round(margin);
    }
}
