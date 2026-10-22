# The prompt

> **For Clear TV Islamic, don't use this prompt:** it builds a plain launcher from scratch, without the prayer times,
> azan or mosques. Ask your assistant to follow [AGENTS.md](AGENTS.md) instead, for example: *"Set up Clear TV
> Islamic on my TV following AGENTS.md. My TV is <model>, its IP is <ip>, I live in <district, state>."*
> This page is kept for reference.

Paste this into Claude Code (or another coding assistant with shell access) on a computer that is
on the same Wi-Fi as the TV. Works on Android TV / Google TV sets (Xiaomi, TCL, Hisense, Sony,
OnePlus, Realme, Nvidia Shield and most others). It will not work on Samsung Tizen, LG webOS or
Fire TV. If you cloned this repo, mention that: the assistant can reuse `tools/` and `launcher/`
instead of writing them from scratch, and only needs to edit `launcher/.../values/config.xml`.

Before you start, on the TV: Settings → Device Preferences → About → press OK on "Build" 7 times,
then go back → Developer options → turn on USB debugging (and Network/ADB debugging if your TV
shows it). Keep the TV on. Some boxes cut power to their USB port while USB debugging is on, so a
USB speaker or drive plugged into it goes dead until you switch debugging off again.

---

I want to debloat my Android TV and replace its launcher with a clean Apple TV style home screen.
My TV model is: <MODEL NAME HERE>. Work on the real device, step by step, and verify everything you
do with screenshots pulled from the TV. Ask me before anything that depends on my preferences.

Check the model first
- Before connecting or downloading anything, check my model: does it run Android TV or Google TV, Android 8
  or newer? If it runs Samsung Tizen, LG webOS, Hisense VIDAA, Roku, Vizio, Fire OS or anything else that
  isn't Android TV, tell me first, plainly, that the launcher can't be installed on it (Fire TV: debloat
  only), suggest a Google TV stick in its HDMI port instead, and stop until I decide.
- Look up where developer settings are on my exact model before telling me what to press.

Connect
- Find the TV on my network (hostname, ARP table, or ask me for the IP) and connect over ADB on port 5555.
- If the stock adb binary fails with "No route to host" on macOS even though the port is open, that is
  macOS Local Network privacy blocking adb. Don't debug it: use the Python `adb-shell` package in a venv
  as the ADB client instead. If its file push hangs on larger files, transfer files as base64 chunks
  through the shell and verify the md5.
- Tell me when the "Allow USB debugging?" prompt will appear on the TV so I can accept it.
- If `adb install` hangs on Android 14 it is waiting for an on-screen confirmation; push the APK and run
  `pm install -r` over the shell instead.

Back up first
- Save the full package list, disabled-package list, getprop output and all settings to a backup folder
  before changing anything, and write a restore script that re-enables everything you disable.
- Ask me what is plugged into the TV's USB ports. Some devices cut their power while USB debugging
  is on, so warn me, and remind me to switch debugging off once you're done.

Debloat
- List installed packages and sort them into: manufacturer telemetry/analytics, manufacturer content
  and ad services (PatchWall, recommendation rows, content providers), background services I probably
  don't need (share/cast duplicates, cleaners), preloaded apps, and streaming apps.
- Disable the first categories with `pm disable-user --user 0`, never uninstall system packages.
- Never disable what the TV needs to work: the TV app and input services that play HDMI and the
  tuner, the input/source menu, remote control and Bluetooth pairing services, the on-screen
  keyboard, Google Play Store and Play Services. Manufacturer package names often don't say what a
  package does: check its activities and services with `dumpsys package` first, and ask me if it's
  still unclear.
- Before touching streaming apps, voice assistant, Chromecast, Miracast or the update service, ask me
  which ones I use with a checklist question and disable only what I don't tick.
- Run `pm` commands one per shell call, chained commands may silently stop after the first.
- Disable in small batches and screenshot between them: a batch can take something load-bearing with
  it and you cannot tell which.
- Offer me ad-blocking DNS as an optional extra: on Android 9 and newer, Private DNS set to an
  ad-blocking resolver (`settings put global private_dns_specifier dns.adguard-dns.com`, then
  `settings put global private_dns_mode hostname`). Tell me it doesn't block YouTube ads, check an app
  still plays afterwards, and note the undo (`private_dns_mode opportunistic`) in the README.

Launcher
- Check that the TV accepts APKs before building anything. Some China-market sets answer every
  sideload with `INSTALL_FAILED_VERIFICATION_FAILURE` — `adb install`, `pm install`, a hand-started
  installer and even the manufacturer's own APKs all fail — and others refuse any package that
  declares `android.intent.category.HOME` unless it carries the manufacturer's signature. If you
  hit either, do the debloat half, say plainly that the launcher half is impossible here, and stop
  retrying installs. The matching `examples/` header says which case your TV is.
- Don't install a third-party launcher that charges for basics like wallpapers. Build a small native
  Android TV launcher instead (Java, AndroidX RecyclerView, minSdk matching the TV, debug build is fine),
  install it over ADB, set it as the default home and disable the stock launcher. If several apps claim
  the HOME intent, list them with `cmd package query-activities -a android.intent.action.MAIN -c
  android.intent.category.HOME` and set the default with `cmd package set-home-activity`.
- Before disabling the stock launcher, check which other apps claim HOME: on Google TV the setup wizard
  (`com.google.android.tungsten.setupwraith`) takes over even after the stock launcher is disabled, and
  the matching `examples/` header says what to do.
- On Android 11 and newer the launcher must declare `<queries>` for `android.intent.category.LEANBACK_LAUNCHER`,
  or package visibility silently hides every app and the grid comes up empty; target a recent SDK, some
  Google TV 14 sets refuse older targets at install.
- Design, Apple TV 4K style: pitch black background; a dock row at the bottom of the screen inside a
  slightly lighter rounded band holding the four apps I pick (ask me which, and the order); the rest of
  the apps in a 4-column grid below that scrolls up when I press Down; one translucent "glass" highlight
  that springs from tile to tile instead of per-tile borders; the focused tile scales up slightly and
  shows its name underneath; above the dock, the focused dock app's name in large light type with an
  ambient glow in a colour sampled from its banner; frosted pill buttons top right for Inputs, Wi-Fi
  (showing the network name) and Settings; a greeting, a large thin clock and the date top left in a
  downloaded open-licence typeface such as Outfit. Inputs opens a compact glass dropdown under the pill
  listing the HDMI ports, AV and Live TV, switching via the TV input framework.
- Make D-pad navigation deterministic: Up/Down move exactly one row, Up from the dock goes to the top
  bar, the ends of the top bar don't fall through to tiles. Remember the last focused tile.
- Keep it light: the TV has about 1 GB of RAM. No blur, no video, no network polling.

Verify
- After each change, inject remote key presses over ADB, take a screenshot from the TV and look at it.
  Fix what you see before moving on.
- If a remote button relies on an accessibility helper, check that the service is enabled and bound
  for the current user, then ask me to press the real button; an injected key is not proof (see
  `docs/TROUBLESHOOTING.md`).
- When done, give me a restore script, a build-and-install script, and a README that explains how to
  re-enable any app and how to change the dock order or the wallpaper.

---

Expect to answer three or four checklist questions (which apps you use, which four go in the dock,
whether to keep the voice assistant, Chromecast and firmware updates). Everything is reversible.
