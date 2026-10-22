# Instructions for AI agents

You are helping someone set up **Clear TV Islamic** on their own TV: on Android TV and Google TV, debloat it and install
this launcher as the home screen, with **JAKIM prayer times under the clock, the azan at every prayer, an Islamic
screensaver with verses of the Quran, and mosque photos behind Home** (Mecca, Medina and more); on LG webOS, install
the webOS app. The Islamic features are the reason people install this version: they only turn on with the Islamic
theme of step 6, so **never finish without step 6**. This file is the playbook. It is written for any brand and for any
AI model, large or small. Follow it in order, and read [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)
before you improvise a fix.

## First: check the model, before anything else

As soon as you know the TV's or box's brand and model (the person names it, or it is on a photo or sticker),
check it **before** connecting, asking for an IP address, downloading tools or touching any setting:

1. Look it up in [docs/DEVICES.md](docs/DEVICES.md) and the table below.
2. If it is **not supported** (Samsung Tizen, Hisense or Toshiba VIDAA, Roku, Vizio, Panasonic My Home Screen,
   Apple TV, an older unifi IPTV box), or only partly (Fire TV and the debloat-only models: no Clear TV Islamic Home;
   LG webOS: only an app you open), say so **first**, in plain words, before any setup step. For example:
   "Your Samsung QN90B runs Tizen, so Clear TV Islamic can't run on it. A Google TV stick in its HDMI port gives you
   all of it." Then stop, unless they choose the box or the partial option.
3. If you don't know whether the model runs Android TV, find out first (the About screen, the maker's spec
   page). Never start a setup "to see if it works".

## Which Clear TV Islamic to install

Ask the person for the TV's brand and model, and what the Settings → About screen says. It is the operating
system that matters, not the brand: Hisense, Panasonic, Toshiba, TCL and Philips sell both kinds. Then pick
**one** row, and follow only that route.

| The TV or box runs | Install | Follow |
|---|---|---|
| **Android TV or Google TV, Android 8 or newer** (Sony, TCL, Xiaomi, Philips, many Hisense, Sharp, OnePlus, NVIDIA Shield, Chromecast with Google TV, Mi Box, Android projectors…) | **The Android launcher**: debloat + Clear TV Islamic as the home screen + self-cleaning | This file, steps 1 to 7 |
| Android TV 5, 6 or 7 | Debloat only: the launcher needs Android 8 | Steps 1 to 3, then step 7 |
| Fire TV (Amazon sticks, Fire TV Edition TVs) | Debloat only: Fire OS takes Home back from any other launcher | Steps 1 to 3, then step 7 |
| **LG (webOS)** | **The webOS app** (preview): an app you open, not the home screen; no debloat, no cache cleaning | [webos/README.md](webos/README.md) only. Not these steps: they are for Android |
| Samsung (Tizen), Roku, VIDAA, Vizio SmartCast, Panasonic My Home Screen, Apple TV | Nothing: these cannot run any version | Stop |

[docs/DEVICES.md](docs/DEVICES.md) says how far each model has been tested; tell the person which section their
model is in, and don't promise more than that.

How to tell them apart:
- **Android**: About shows "Android TV OS" or an Android version, or the setup connects with `tools/tv.py
  --probe` (step 1). Confirm with `ro.build.version.sdk`: **26 or higher** for the launcher.
- **webOS**: an LG TV; About shows "webOS TV" and a version. `tools/tv.py` cannot connect to it, and that is
  expected: webOS has no Android debugging. Only LG's tools (`ares-*`) and Developer Mode reach it.
- Not sure: ask for a photo of the About screen. Install only the version for that system; none runs on the
  others.

For a TV that can only get "Nothing" or a partial install, tell the person plainly, and mention the option
that gives them everything: a Google TV or Android TV box (Chromecast with Google TV, Xiaomi Mi Box, NVIDIA
Shield) in an HDMI port, set up with the Android route. Don't look for workarounds such as rooting the TV.

The launcher adapts by itself to everything else: 720p, 1080p and 4K screens, TVs with or without HDMI inputs
or a tuner (sticks and boxes just show fewer inputs), any language. On a weak TV (1 GB of RAM, or the selection
stutters) set `"reduced_motion": true` in the theme (step 6).

## How to follow this playbook

Written so that any model can follow it to the letter:

- Do the steps in order, one at a time. Don't skip ahead or merge steps.
- Run **one command per shell call** and read its output before the next one.
- Every step ends with a **Check**. If the check fails, stop. Look the message up in
  [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md); if it isn't there, tell the person exactly what you saw.
  Never invent a fix that disables, uninstalls, resets or restarts something.
- Replace every `<ip>`, `<pkg>`, `<folder>` with a real value. Never run a line that still contains `<` `>`.
- Ask the person questions they can answer with yes/no or by reading the TV screen. Never ask them to type
  commands.
- Write every command that changes the TV into `backup/log.txt`, so all of it can be undone.
- Commands here are for macOS and Linux. On Windows use the `.ps1` script of the same name (`tools\disable.ps1`,
  `tools\theme.ps1`, `launcher\build-and-install.ps1`); see [docs/CLEANYOURTV-README.md](docs/CLEANYOURTV-README.md).
- When unsure, the answer is always: leave it as it is and ask.

## Ground rules

1. **The TV belongs to a family that is using it.** Ask before every change to the TV, say what it does and
   how to undo it, and wait for a yes. Ask even before read-only actions like screenshots or key presses if the
   person may be watching. If they say stop, stop touching the TV completely.
2. **Never uninstall a system package.** Disable it for user 0 (`pm disable-user --user 0 <pkg>`) so it can be
   restored. Keep a list of everything you disable and give the person a restore script.
3. **Back up before changing anything**: full package list, disabled list, `getprop`, `settings list system`,
   `secure` and `global`, and which app owns Home. Keep it outside the repository (`backup/` is gitignored).
4. **Disable in small batches** (about 10) and check the TV after each batch: Home still works, and the
   person confirms HDMI, the remote and Wi-Fi still work.
5. **Never disable what the TV needs**: the TV/tuner and HDMI input services, the input or source menu, the
   remote and Bluetooth pairing, the on-screen keyboard, Settings, System UI, Google Play Store and Play
   Services, and the current launcher until the new one is installed. Manufacturer package names often don't
   say what they do: check `dumpsys package <pkg>` for its activities and services before you touch one.
6. **Ask before touching anything the person might use**: streaming apps, voice search, Chromecast, screen
   mirroring (Miracast), casting, the USB media player, firmware updates. Use a checklist question.
7. **Don't guess what an app is from its name.** Example: on Xiaomi, `com.xiaomi.mitv.smartshare` looks like
   bloat but is the Miracast screen. When unsure, leave it on and ask.
8. **Credentials stay with the person.** They type passwords, PINs and pairing codes themselves. Never write an
   API key or token into the repository.
9. **Family-friendly by default.** The places pictures skip any picture with people in it; keep it that way.
   Ask about the person's preferences for any images you show them or put in docs.
10. **Downloads need a yes.** Say the file name, source and size before downloading tools (Android platform
    tools, JDK 17, Android SDK, GitHub CLI).

## Step by step

### 1. Connect

- **First, find where developer settings are on this exact model.** Every brand, and often every model and
  firmware year, hides them somewhere else, and some need a different number of presses or a hidden code.
  Before telling the person what to press:
  1. Get the exact model number (on the About screen or the sticker on the back) and the Android version.
  2. Look in [examples/](examples/) for that model: its notes say how it was done there.
  3. Otherwise search the web for "<brand> <model> developer options" and read the maker's own page or a
     recent forum answer for that model.
  4. Tell the person the steps for their model, one at a time, and ask what they see after each. If the
     screen does not match, stop and look again rather than guessing.
  Never send someone through menus written for a different model.
- The usual path on Android TV: Settings → Device Preferences → About → press OK on **Build** seven times, then Developer
  options → **USB debugging** on (also "Network/ADB debugging" if shown). No cable is needed; it works over the
  home Wi-Fi. Some TVs show **Wireless debugging** with a pairing code instead; ask the person for the code
  and the `ip:port`, then `adb pair`.
- Ask for the TV's IP address (Settings → Network & Internet → the Wi-Fi network).
- Set up the Python client once (all the scripts use it; no `adb` program is needed):
  `python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt`
- Connect: `TV_HOST=<ip> tools/venv/bin/python -I tools/tv.py --probe`. When the "Allow USB debugging?"
  prompt appears on the TV, tell the person to tick **Always allow** and press OK, then run it again.
- **Check**: it prints six lines, `ro.product.model = …` through `ro.product.manufacturer = …`. A timeout or
  "connection refused" means debugging is off or the IP is wrong: see TROUBLESHOOTING, "Connecting".
  Note `ro.build.version.sdk`: below 26, stop after step 3 (debloat only).
- To run any shell command on the TV: `TV_HOST=<ip> tools/venv/bin/python -I tools/tv.py '<command>'`.
  Using `adb` instead is fine where it works (`adb connect <ip>:5555`, then `adb shell '<command>'`).

### 2. Identify the TV and back up

- `tools/tv.py --probe` gives model, codename and Android version. Look for a matching folder in
  [examples/](examples/): its `debloat.txt` is a tested list, and its header names packages that must stay.
- Do the backup from rule 3. Also note `cmd package query-activities --brief -a android.intent.action.MAIN
  -c android.intent.category.HOME`: every app that can claim Home.
- **Check**: the backup files exist in `backup/` and none is empty.

### 3. Debloat

- With a matching example: copy its `debloat.txt`, show the person what it disables, comment out anything
  they use, then apply it in batches with `tools/disable.sh <list>`.
- Without one: list packages, sort them into telemetry, ads and content rows, background services, preloaded
  apps and streaming apps, and propose a list. Only the first three are candidates without asking.
- Run `pm` commands one per shell call; chained commands can silently stop after the first.
- Keep the stock launcher's packages until step 5.
- **Check** after every batch: the person presses Home and the TV's own home screen still opens, the remote
  works, Wi-Fi is connected, and an HDMI input still shows a picture. If anything broke, re-enable that batch's
  packages one by one (`pm enable <pkg>`) before going on.

### 4. Build the launcher (usually not needed)

- **Use the ready-made APK instead**: `dist/clear-tv-islamic.apk` is built from this repository. With it, skip the
  rest of this step: no JDK or Android SDK is needed. Build only when the person wants to change the code.
- Needs JDK 17 and Android SDK platform 34 + build-tools 34.0.0. On a Mac without Homebrew, download
  Temurin 17 and the Android command-line tools, then `sdkmanager --sdk_root=<dir> --licenses` (the person
  must agree to the licence) and `"platforms;android-34" "build-tools;34.0.0"`.
- **Paths with spaces** (e.g. macOS "Application Support") break `sdkmanager`. Keep the JDK and SDK in a
  path without spaces.
- Configure in `launcher/app/src/main/res/values/config.xml`, or better with a theme (step 6): ask which four
  apps go in the dock and in what order, using the package names from `pm list packages -3`.
- Build: `cd launcher && ./gradlew testReleaseUnitTest assembleRelease`. The APK must stay under 400 KB
  (CI checks it).
- **Check**: `launcher/app/build/outputs/apk/release/app-release.apk` exists and is about 300 KB.

### 5. Install and make it Home

- With the ready-made APK: `APK=dist/clear-tv-islamic.apk TV_HOST=<ip> launcher/build-and-install.sh` (Windows:
  `launcher\build-and-install.ps1 -TvHost <ip> -Apk dist\clear-tv-islamic.apk`). It does everything below without
  building.
- `TV_HOST=<ip> launcher/build-and-install.sh` (Windows: `launcher\build-and-install.ps1 -TvHost <ip>`). It has a
  safeguard that puts the TV back as it was if Clear TV Islamic does not work on it:
  - Before changing anything it refuses Android older than 8 and Fire TV, with a plain message.
  - It installs and starts Clear TV Islamic, and checks it is really in front and not restarting **before** it
    touches the TV's own launcher.
  - Only then it disables the stock launcher and makes Clear TV Islamic Home, presses Home and checks again.
  - If any check fails, or the connection drops part-way, it removes Clear TV Islamic, re-enables the original
    launcher, sets it back as Home and presses Home. The debloat list is not touched; `tools/restore.sh`
    undoes that.
- Before that, look at the Home list from step 2. Disable any other app there that is neither the stock
  launcher nor this one, after asking (on Xiaomi the factory test app `com.duokan.factorytest`), or the Home
  button can land on it.
- On Google TV the setup wizard (`com.google.android.tungsten.setupwraith`) can take Home; see the
  troubleshooting guide.
- After the stock launcher is gone, its recommendation services (`tvrecommendations`, `leanbacklauncher*`)
  can be disabled too.
- **Check**: the script prints `Home now resolves to com.gathin.tvhome/…` and ends with `Installed. Press Home
  on the remote to see it.` A line starting with `ERROR:` means it stopped and already put the stock launcher
  back; the message says what to do next.

### 6. The Islamic theme: prayer times, azan, mosques, screensaver (never skip)

Without this step the TV shows the plain launcher (a grey clock and Bing's places), and the person will think the
install failed. Do all of it:

1. **Ask where they live** (state and district) and find their **JAKIM zone code** on
   [e-solat.gov.my](https://www.e-solat.gov.my): for example `WLY01` Kuala Lumpur and Putrajaya, `SGR01` Gombak,
   Petaling, Sepang, Hulu Langat, Hulu Selangor and Shah Alam, `JHR02` Johor Bahru, Kota Tinggi, Mersing and Kulai,
   `TRG04` Kemaman and Dungun. Never guess a zone: wrong prayer times are worse than none. Check it answers:
   `curl -s "https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=today&zone=<ZONE>"` must return a
   `prayerTime` list. If they are outside Malaysia, say plainly that the prayer times come from JAKIM and cover
   Malaysia only, and ask before going on.
2. **Make their theme folder** from the Islamic one, outside the tracked files: `cp -R themes/islamik backup/theme`
   (`backup/` is gitignored). In `backup/theme/config.json` set `"prayer_zone"` to their zone, and add
   `"dock_apps"` with the four apps they picked, in order (package names from `pm list packages -3`).
3. **The azan**: `themes/islamik` already has `azan.mp3` (Ustaz Asri Ibrahim, Rabbani) and `azan-subuh.mp3`. Ask
   whether they want these or their own recordings; theirs replace the files of the same name.
4. **Mosque photos**: `python3 tools/masjid-photos.py backup/theme` (13 photos, about 9 MB, from Wikimedia Commons;
   say so before downloading). They go behind Home and the screensaver.
5. **Push it**: `TV_HOST=<ip> tools/theme.sh backup/theme` (Windows: `tools\theme.ps1 -TvHost <ip> backup\theme`).
   With the photos and the azan this takes a few minutes.
6. **The screensaver**: `settings put secure screensaver_components com.gathin.tvhome/.ClockDream`.
7. **Check**, one at a time:
   - `logcat -d -s TvHome.Solat` shows `saved 365 days for <ZONE>` within a minute (the TV needs internet).
   - A screenshot of Home (ask first) shows the prayer row under the clock and a mosque behind the apps.
   - With the person's go-ahead (it plays the azan aloud): `touch /sdcard/Android/data/com.gathin.tvhome/files/azan-now`,
     press Home, and within a minute `logcat -d -s TvHome.Azan` shows `azan <prayer>` and the azan plays; any
     button on the remote stops it.
   - Over another app: `appops get com.gathin.tvhome SYSTEM_ALERT_WINDOW` says `allow` (the install script sets
     it), and `dumpsys alarm | grep com.gathin.tvhome.AZAN` shows the next prayer's alarm. With the person's
     go-ahead, open YouTube, `touch .../files/azan-now` and
     `am broadcast -a com.gathin.tvhome.AZAN -n 'com.gathin.tvhome/.AzanAlarm$Receiver'`: a band with the azan slides
     up over YouTube, and a button press stops it.
   - `am start -n com.android.systemui/.Somnambulator` starts the screensaver: the gold clock, the prayer card and
     a verse over a mosque photo. Any button ends it.

### 6b. Other settings and wallpapers (themes)

A theme is a folder with `config.json` (and optional `wallpaper.jpg` or `wallpapers/`). Push it with
`TV_HOST=<ip> tools/theme.sh <folder>`, no rebuild needed. All keys are in
[themes/README.md](themes/README.md). Useful ones:

```json
{
  "dock_apps": ["com.google.android.youtube.tv", "com.netflix.ninja"],
  "show_network_pill": false,
  "wallpaper_seconds": 15
}
```

- `dock_apps` beyond the dock's size continue as the first grid rows, in order, so the person can order the
  grid too.
- **Places**: on by default, nothing to set up. The first week's pictures arrive within a minute of Home first
  starting with internet; until then the background is plain. `"places": false` uses the theme's own pictures
  and keeps the launcher offline.
- **App order**: the person can arrange the apps themselves on the TV (hold OK on an app → Move); once they
  have, their order wins over `dock_apps`.
- **Android 10 quirk**: the first time the launcher gains the internet permission, sockets fail with `EPERM`
  until the TV restarts. Ask before restarting it.
- Point the screensaver at the launcher's clock before disabling the stock screensaver:
  `settings put secure screensaver_components com.gathin.tvhome/.ClockDream`.
- **Check**: Home restarts within a few seconds with the new settings.
- **Cache cleaning**: Clear TV Islamic clears the TV's app caches itself every 15 days ([docs/cache-clean.md](docs/cache-clean.md)).
  It needs the TV's permission once. With the person at the TV, run
  `touch /sdcard/Android/data/com.gathin.tvhome/files/clean-now`, press Home and wait 20 seconds: the TV asks
  **Allow USB debugging?**; they tick **Always allow** and press OK. **Check**: `logcat -d -s TvHome.Clean`
  shows `cleared cached files`. It needs network debugging left on; if they would rather switch it off,
  cleaning stops, nothing else changes.

### 7. Verify and hand over

- Ask before taking screenshots. Use `screencap -p /sdcard/s.png` and pull it; `exec-out screencap` gives
  broken PNGs on some MediaTek TVs. Screenshots of Live TV, HDMI or Netflix come out black.
- Check: Home is the new launcher (`cmd package resolve-activity --brief -c android.intent.category.HOME -a
  android.intent.action.MAIN`), System UI and the TV service are running, the dock and grid show the apps, and the
  Islamic theme of step 6 is on: prayer times under the clock, a mosque behind the apps.
- Ask the person to try with the real remote: Home button, Inputs → an HDMI device, playing a video, the mic
  button, screen mirroring, and holding OK on an app to move it.
- Give them: the restore script (re-enables everything and puts the stock launcher back), where the theme
  folder is, and the choice about debugging: left on, the TV keeps cleaning its own caches (it only accepts
  connections it has allowed); switched off, that cleaning stops and nothing else changes.
- Offer to switch off firmware updates only after explaining the trade-off: updates can bring the bloat back,
  but they also carry security fixes.

## Working on this repository

- Tests: `python3 -m unittest discover -s tests` (CI sets `CLEANYOURTV_STRICT=1` on every push, which also checks the guide's counts) and
  `cd launcher && ./gradlew testReleaseUnitTest assembleRelease`.
- The original Clean Your TV guide lives in [docs/CLEANYOURTV-README.md](docs/CLEANYOURTV-README.md); the tests
  read its TV table and counts. Keep its relative links pointing one level up (`../`).
- Keep the launcher light: no new background services (the one exception is AzanOverlay, which runs only for the
  length of an azan, started by an alarm at the prayer time), no network use but the weekly places pictures (none with
  `"places": false`), one wallpaper bitmap
  in memory, decoding off the main thread, nothing running while another app is in front.
- Commits carry only the human author's identity. Don't add AI attribution lines.
- Credit stays: this project is based on [Clean Your TV](https://github.com/Gathin23/cleanyourtv) by Gathin (MIT).
  Keep `LICENSE`, `launcher/FONT-LICENSE-OFL.txt` and the note in the README on where the places pictures
  come from.
- [PROMPT.md](PROMPT.md) is the original one-paste prompt; this file adds the safety rules and the lessons above.
