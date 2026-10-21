# Keeping the cache down

Android TVs quietly accumulate cache, and the Play Store is the worst offender:
on a 43" Mi TV with 11 GB of storage it had hoarded roughly 6.6 GB of leftover
download fragments, filling `/data` to 87%.

That is enough to break things in ways that do not look like a disk problem.
YouTube launched, reached its main activity, played its UI sounds — and drew
nothing at all. Clearing the caches freed about 7 GB and fixed it on the spot,
with no reinstall and no sign-out.

Clear TV Islamic stops that from building up again, on the TV itself. A computer script does the same for several
TVs at once (below).

## On the TV itself (Clear TV Islamic)

Every 15 days, once Home has sat idle on screen for 20 seconds, Clear TV Islamic runs `pm trim-caches` on its own
TV. It also cleans early, at most every 3 days, when storage passes 80 %. No computer is involved, so it
works the same whether the TV was set up from a Mac, Windows or Linux.

Android lets no installed app clear other apps' caches, so Clear TV Islamic asks the TV's own debugging service,
the one the setup used over Wi-Fi, to run that one command. The first time, 20 seconds after Home comes up, the TV
shows **Allow USB debugging?** for up to a minute: tick **Always allow** and press OK. Watch for it: with
"Always allow" already ticked, a single OK press on the remote answers it, so it can come and go unseen. That means network debugging has to stay on; the TV
only accepts connections it has allowed this way.

The TV forgets an allowed key that has not connected for 7 days, so between cleans Clear TV Islamic checks in every
3 days: it connects and disconnects, running nothing, and the TV keeps trusting it without asking again.

It is built not to get in the way: it only ever sends that one command, never while an app is in front;
when debugging is off it does nothing; it tries at most once a day; and after three unanswered or declined
prompts it stops asking.

- Change how often with `"cache_clean_days"` in the theme's `config.json` (1 to 90); `0` turns it off.
- Clean right now, and show the prompt again: `tools/tv.py 'touch /sdcard/Android/data/com.gathin.tvhome/files/clean-now'`,
  then press Home and wait 20 seconds.
- See the prompt again (to test it): also `touch .../files/new-key` before `clean-now`. Clear TV Islamic makes a new key,
  which the TV has not allowed yet.
- Its log: `logcat -s TvHome.Clean`.

## The computer script

[`tools/cache-clean.sh`](../tools/cache-clean.sh) cleans every TV on the network from a Mac or Linux
computer, Clear TV Islamic or not.

## What it does

1. Scans the local network for devices with ADB listening on port 5555.
2. Confirms each one is a television — `ro.build.characteristics` contains `tv`,
   or the device reports the leanback feature. A phone or tablet with ADB left
   on is reported and skipped, never touched.
3. Runs `pm trim-caches` on each confirmed TV, logging usage before and after.

Confirmed TVs are remembered under `~/.cache/clean-tv/`, so later runs find
them without a full scan. Nothing is written inside the repository.

## What it never touches

`pm trim-caches` can only reach cache directories. App data lives elsewhere, so
this cannot affect sign-ins, accounts, app settings, watch history or installed
apps. Cache is disposable by design — the TV re-downloads what it still needs.

## Usage

```sh
tools/cache-clean.sh                     # clean every TV found
tools/cache-clean.sh --dry-run           # report, change nothing
tools/cache-clean.sh --scan-only         # just list what is out there
tools/cache-clean.sh --device 10.0.0.5   # target one device
tools/cache-clean.sh --subnet 192.168.1  # scan a specific /24
```

A full sweep of a /24 takes about ten seconds. `adb` is found via `$ADB`, then
`PATH`, then `./platform-tools/adb`.

## Running it on a schedule (macOS)

Save this as `~/Library/LaunchAgents/com.example.tv-cache-clean.plist`, with
`REPO` replaced by the path to your checkout. `StartInterval` is in seconds —
1296000 is every 15 days.

Clearing caches has a cost: every app re-downloads its pictures the first time it
opens afterwards, so profile photos, thumbnails and artwork load slowly or blank
for a moment. Every 15 days keeps the disk in check without making that a
regular thing. Clearing every day or two would mean living with it constantly.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>Label</key>
	<string>com.example.tv-cache-clean</string>
	<key>ProgramArguments</key>
	<array>
		<string>REPO/tools/cache-clean.sh</string>
	</array>
	<key>StartInterval</key>
	<integer>1296000</integer>
	<key>RunAtLoad</key>
	<false/>
</dict>
</plist>
```

Then:

```sh
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/com.example.tv-cache-clean.plist
```

It only runs while the computer is awake and on the same network as the TV.
When the TV is off, the run logs that nothing was found and exits. To stop it:

```sh
launchctl bootout gui/$(id -u)/com.example.tv-cache-clean
```

## Compatibility

Android TV, Google TV, Fire TV and other Android-based sets. Samsung (Tizen)
and LG (webOS) have no equivalent interface and cannot be cleared this way.

## Notes from the field

**A blank app is not always a broken app.** When YouTube drew nothing, the
things that looked guilty were innocent: the clock was correct, DNS resolved,
the Play Store rendered normally, and the system volume overlay drew fine on
top of the blank app. A full disk and a corrupt cache were the whole story.

**`dumpsys media_session` settles arguments.** It reports playback state and
the current title, which tells you whether an app is genuinely playing when the
screen suggests otherwise.

**Screenshots can lie.** On some devices `adb exec-out screencap -p` prepends
vendor noise before the PNG header — strip everything before the first
`\x89PNG` or the file will not open. Video surfaces also capture as flat grey,
which looks exactly like a broken app and is not.

**Useful one-liners.**

```sh
adb connect <tv-ip>:5555
adb -s <tv-ip>:5555 shell df -h /data                 # free space
adb -s <tv-ip>:5555 shell dumpsys media_session       # is it really playing?
adb -s <tv-ip>:5555 shell pm trim-caches 9999999999   # clear caches
```
