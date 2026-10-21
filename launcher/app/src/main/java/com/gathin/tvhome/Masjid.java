package com.gathin.tvhome;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mosque photos from the theme's masjid/ folder (tools/masjid-photos.py), with their captions.json:
 * behind the Islamic screensaver, and, unless the theme says "masjid_home": false, behind Home in
 * place of the week's places.
 */
final class Masjid {
    static final String DIR = "masjid";

    private Masjid() {
    }

    static File dir(Context ctx) {
        File base = Config.dir(ctx);
        return base != null ? new File(base, DIR) : null;
    }

    /** The photos, in file order; empty when the theme has none. */
    static List<File> list(Context ctx) {
        List<File> out = new ArrayList<>();
        File d = dir(ctx);
        File[] files = d != null ? d.listFiles() : null;
        if (files == null) return out;
        for (File f : files) {
            String n = f.getName().toLowerCase(java.util.Locale.ROOT);
            if (f.isFile() && f.length() > 0 && (n.endsWith(".jpg") || n.endsWith(".jpeg"))) out.add(f);
        }
        Collections.sort(out);
        return out;
    }

    /** captions.json from the photos' folder, or an empty object. */
    static JSONObject captions(File dir) {
        File f = dir != null ? new File(dir, "captions.json") : null;
        if (f == null || !f.isFile() || f.length() > 64 * 1024) return new JSONObject();
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) > 0) n += r;
            return new JSONObject(new String(buf, 0, n, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    /**
     * What Home's shelf says about a photo, in PlaceWallpapers.describe's shape: the mosque, the
     * photo's credit, the city. "Masjid Nabawi, Madinah" gives "Masjid Nabawi" and "Madinah".
     */
    static String[] describe(File photo, JSONObject captions) {
        if (photo == null) return null;
        JSONObject c = captions.optJSONObject(photo.getName());
        if (c == null) return null;
        String caption = c.optString("caption").trim();
        if (caption.isEmpty()) return null;
        int comma = caption.lastIndexOf(',');
        String name = comma > 0 ? caption.substring(0, comma).trim() : caption;
        String place = comma > 0 ? caption.substring(comma + 1).trim() : "";
        return new String[]{name, c.optString("credit").trim(), place};
    }
}
