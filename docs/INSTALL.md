# Installing Glass TV Islamic, step by step

<p><b>English</b> · <a href="INSTALL.ms.md">Bahasa Melayu</a> · <a href="../README.md">Back to the README</a></p>

This guide is for anyone. You will not need to code, and you will not need Java or the Android SDK: you will copy
a few commands from this page into your computer. It takes about 30 minutes, and everything can be undone.

**You need**

- An **Android TV or Google TV**, Android 8 or newer (Sony, TCL, Sharp, Philips, Xiaomi, Hisense Android models,
  Chromecast with Google TV, Mi Box...). Samsung, LG and Roku TVs cannot run it.
- A **computer** (Windows or Mac) on the **same Wi-Fi** as the TV.
- Optional: your favourite **azan recordings** as MP3 files.

In the commands below, replace `192.168.1.50` with your TV's own address (step 3). Mac commands and Windows
commands are shown separately: use the ones for your computer.

---

## Part 1: on the TV

### 1. Check that your TV runs Android TV

On the TV: **Settings → Device Preferences → About**. Look for **Android TV OS version**: it must be **8 or
higher**. If you don't see "Android TV" anywhere, your TV can't run Glass TV; a Google TV stick in its HDMI port
can.

### 2. Turn on developer options and USB debugging

1. Still on **About**, scroll down to **Build**.
2. Press **OK on Build seven times**. The TV says *"You are now a developer"*. (If it asks for a PIN, type it.)
3. Press **Back** once. A new menu, **Developer options**, is now in Device Preferences.
4. Open **Developer options** and turn on **USB debugging**. If you also see **Network debugging** or
   **ADB debugging**, turn that on too.

No cable is needed: it works over Wi-Fi. Some TVs cut the power to their USB ports while USB debugging is on, so
a USB speaker or drive may go quiet until you turn it off.

### 3. Find the TV's address on your network

**Settings → Device Preferences → About → Status → IP address** (or **Settings → Network & Internet →** your
Wi-Fi). Write down the number that looks like `192.168.1.50` or `10.0.0.23`. Ignore the long ones with colons.

---

## Part 2: on your computer

### 4. Install Python (once)

- **Mac:** open **Terminal** (press Cmd + Space, type *Terminal*, press Enter) and type `python3 --version`.
  If it shows a version number, you already have it. If not, install it from
  [python.org/downloads](https://www.python.org/downloads/).
- **Windows:** install it from [python.org/downloads](https://www.python.org/downloads/). On the first screen of
  the installer, **tick "Add python.exe to PATH"**.

### 5. Download Glass TV Islamic

1. Open [github.com/Pokka99/glass-tv-Islamic](https://github.com/Pokka99/glass-tv-Islamic).
2. Press the green **Code** button, then **Download ZIP**.
3. Unzip it into your **Documents** folder. You now have a folder called `glass-tv-Islamic-main`.

### 6. Open a terminal in that folder

- **Mac:** in Terminal, type:
  ```bash
  cd ~/Documents/glass-tv-Islamic-main
  ```
- **Windows:** open the `glass-tv-Islamic-main` folder in File Explorer, click the address bar, type
  `powershell` and press Enter. Then allow the scripts to run in this window:
  ```powershell
  Set-ExecutionPolicy -Scope Process Bypass
  ```

Keep this window open for all the steps below.

### 7. Set up the tools (once)

- **Mac:**
  ```bash
  python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
  ```
- **Windows:**
  ```powershell
  py -m venv tools\venv; tools\venv\Scripts\pip install -r tools\requirements.txt
  ```

### 8. Connect to the TV

Tell the tools where your TV is, then connect:

- **Mac:**
  ```bash
  export TV_HOST=192.168.1.50
  tools/venv/bin/python -I tools/tv.py --probe
  ```
- **Windows:**
  ```powershell
  $env:TV_HOST = "192.168.1.50"
  tools\venv\Scripts\python -I tools\tv.py --probe
  ```

**Look at the TV:** a box asks **"Allow USB debugging?"**. Tick **Always allow from this computer** and press
**OK**. Then run the second command again.

It worked when you see six lines, from `ro.product.model = ...` to `ro.product.manufacturer = ...`.
If it says *timeout* or *connection refused*, USB debugging is off or the address is wrong (steps 2 and 3).

### 9. Keep a copy of the TV's app list (just in case)

- **Mac:** `tools/venv/bin/python -I tools/tv.py 'pm list packages' > my-tv-apps.txt`
- **Windows:** `tools\venv\Scripts\python -I tools\tv.py 'pm list packages' > my-tv-apps.txt`

---

## Part 3: install Glass TV Islamic

### 10. Install it and make it the home screen

- **Mac:**
  ```bash
  APK=dist/glass-tv-islamik.apk launcher/build-and-install.sh
  ```
- **Windows:**
  ```powershell
  launcher\build-and-install.ps1 -Apk dist\glass-tv-islamik.apk
  ```

The screen on the TV changes a few times. It has finished when you see
**`Installed. Press Home on the remote to see it.`** Press **Home** on the remote: Glass TV is your home screen.

If anything goes wrong, the script puts the TV's own home screen back by itself and tells you why.

### 11. Make your own settings (your "theme")

1. Copy the folder `themes/islamik` and name the copy `my-theme` (in the same `glass-tv-Islamic-main` folder).
2. Open `my-theme/config.json` in a text editor (TextEdit on Mac, Notepad on Windows). It looks like this:
   ```json
   {
     "prayer_zone": "TRG04",
     "azan": true,
     "reduced_motion": true
   }
   ```
3. Change **`TRG04`** to your **JAKIM zone**. Find it on [e-solat.gov.my](https://www.e-solat.gov.my): the zone
   list shows codes such as `WLY01` (Kuala Lumpur, Putrajaya), `SGR01` (Gombak, Petaling, Sepang, Hulu Langat,
   Hulu Selangor, Shah Alam), `JHR02` (Johor Bahru, Kota Tinggi, Mersing, Kulai) or `TRG04` (Kemaman, Dungun).
4. Optional, to choose the four apps in the dock, add a line with their package names, for example:
   `"dock_apps": ["com.google.android.youtube.tv", "com.netflix.ninja", "com.tm.unifitv", "com.amazon.amazonvideo.livingroom"],`
   You can also arrange the apps later with the remote: hold **OK** on an app, choose **Move**.

### 12. Add your azan (optional)

Copy your azan recording into `my-theme` and name it exactly **`azan.mp3`**. For a different azan at Subuh, add
**`azan-subuh.mp3`** too. Without them the azan screen still shows, silently.

### 13. Add the mosque photos (optional)

- **Mac:** `python3 tools/masjid-photos.py my-theme`
- **Windows:** `py tools\masjid-photos.py my-theme`

This downloads 13 photos (Masjid al-Haram, the Kaaba, the Prophet's Mosque, Quba, the Crystal Mosque, Putra
Mosque, Istanbul, Brunei and more, about 9 MB) from Wikimedia Commons into `my-theme/masjid`.

### 14. Send your settings to the TV

- **Mac:** `tools/theme.sh my-theme`
- **Windows:** `tools\theme.ps1 my-theme`

With the photos and azan files this can take a few minutes. Home restarts with your prayer times under the
clock. The year's prayer times arrive from JAKIM within a minute (the TV needs internet).

### 15. Turn on the Islamic screensaver

- **Mac:** `tools/venv/bin/python -I tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'`
- **Windows:** `tools\venv\Scripts\python -I tools\tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'`

Now, when the TV rests, the gold clock, the prayer times and a verse of the Quran appear over the mosques.

### 16. Let the TV keep itself clean (optional)

Every 15 days Glass TV clears the apps' leftover files. It asks the TV once for permission. With someone at the
TV, run:

- **Mac:** `tools/venv/bin/python -I tools/tv.py 'touch /sdcard/Android/data/com.gathin.tvhome/files/clean-now'`
- **Windows:** `tools\venv\Scripts\python -I tools\tv.py 'touch /sdcard/Android/data/com.gathin.tvhome/files/clean-now'`

Press **Home** and wait 20 seconds. When the TV asks **"Allow USB debugging?"**, tick **Always allow** and press
**OK**. For this to keep working, leave USB debugging on.

**Done.** Alhamdulillah.

---

## Removing the ads and bloat (optional, for the careful)

Each brand puts its own tracking and advert apps on the TV. The folder [examples](../examples) has tested lists
for many TV models. Turning apps off is safe (nothing is deleted, and one command turns them back on), but turn
off only what you understand, a few at a time, and check after each batch that Home, the remote, Wi-Fi and HDMI
still work. The full guide is in [CLEANYOURTV-README.md](CLEANYOURTV-README.md). If you would rather not do this
by hand, an AI assistant such as Claude Code can do it with you safely: see [AGENTS.md](../AGENTS.md).

## Undoing everything

- **Back to the TV's own home screen** (Mac; on Windows use `tools\venv\Scripts\python` instead):
  ```bash
  tools/venv/bin/python -I tools/tv.py 'pm enable com.google.android.tvlauncher'
  tools/venv/bin/python -I tools/tv.py 'pm uninstall com.gathin.tvhome'
  ```
  On newer Google TVs the first line is `pm enable com.google.android.apps.tv.launcherx`.
- **Turn apps you disabled back on:** `tools/restore.sh <the same list>` (Windows: `tools\restore.ps1`).
- **Remove your settings:** `tools/theme.sh --reset` (Windows: `tools\theme.ps1 -Reset`).
- Finally you can turn **USB debugging** off again in Developer options.

## When something goes wrong

| What you see | What to do |
|---|---|
| `timeout` or `connection refused` when connecting | USB debugging is off, the address is wrong, or the TV and computer are on different Wi-Fi networks. |
| The "Allow USB debugging?" box never appears | Turn USB debugging off and on again, then connect again. |
| Windows says scripts are disabled | Run `Set-ExecutionPolicy -Scope Process Bypass` in the same window (step 6). |
| No prayer times under the clock | Check `prayer_zone` in `config.json`, that the TV has internet, then wait a minute. |
| No sound at the azan | The file must be named exactly `azan.mp3` and be inside `my-theme`; send it again (step 14). |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | A different build of Glass TV is already installed: remove it first (`pm uninstall com.gathin.tvhome`), then step 10 again. |

More answers are in [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
