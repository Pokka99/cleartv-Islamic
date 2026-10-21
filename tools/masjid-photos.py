#!/usr/bin/env python3
"""Download the mosque photos for the Islamic screensaver into a theme folder.

Usage: python3 tools/masjid-photos.py <theme folder> [list.json]

Reads themes/islamik/masjid.json (Wikimedia Commons file names, captions and credits), saves each
photo at 1920 px wide as <theme>/masjid/NN.jpg, and writes <theme>/masjid/captions.json, which the
screensaver shows under each photo with its photographer and licence. Push with tools/theme.sh.
"""
import json
import os
import sys
import time
import urllib.parse
import urllib.request

UA = {"User-Agent": "clear-tv-islamic/1.0 (TV screensaver; https://github.com/Pokka99/cleartv-Islamic)"}


def fetch(url):
    for attempt in range(5):
        try:
            return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60).read()
        except urllib.error.HTTPError as e:
            if e.code != 429:
                raise
            time.sleep(10 * (attempt + 1))
    raise SystemExit("Wikimedia keeps answering 429 (too many requests); try again later")


def main():
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    theme = sys.argv[1]
    here = os.path.dirname(os.path.abspath(__file__))
    listing = sys.argv[2] if len(sys.argv) > 2 else os.path.join(here, "..", "themes", "islamik", "masjid.json")
    photos = json.load(open(listing, encoding="utf-8"))["photos"]
    out = os.path.join(theme, "masjid")
    os.makedirs(out, exist_ok=True)
    captions = {}
    for n, p in enumerate(photos):
        api = "https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode({
            "action": "query", "titles": "File:" + p["commons"], "prop": "imageinfo",
            "iiprop": "url", "iiurlwidth": 1920, "format": "json"})
        page = next(iter(json.loads(fetch(api))["query"]["pages"].values()))
        if "imageinfo" not in page:
            print("not found on Commons, skipped:", p["commons"])
            continue
        name = "%02d.jpg" % n
        with open(os.path.join(out, name), "wb") as f:
            f.write(fetch(page["imageinfo"][0]["thumburl"]))
        captions[name] = {"caption": p["caption"], "credit": "Foto: %s, %s, Wikimedia Commons" % (p["author"], p["license"])}
        print(name, p["caption"])
        time.sleep(2)
    with open(os.path.join(out, "captions.json"), "w", encoding="utf-8") as f:
        json.dump(captions, f, ensure_ascii=False, indent=1)
    print("%d photos in %s" % (len(captions), out))


if __name__ == "__main__":
    main()
