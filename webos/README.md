# Clear TV Islamic for LG webOS (preview)

LG TVs run webOS, not Android, so the Android launcher cannot run on them. This folder is Clear TV Islamic's Home
rebuilt as a webOS web app: the clock row, a new place to visit behind your apps every week, the glass dock
and buttons, the selection that glides between them, the room taking the selected app's colour, and the
glass Inputs menu.

**It is a preview.** It has been built and checked in LG's webOS TV 26 Simulator only, not yet on a real LG TV.

## What it can and cannot do on webOS

| | On webOS |
|---|---|
| Glass look, places with names, clock, gliding selection, Inputs menu | Yes |
| Arranging your apps: hold OK on an app, **Move**, carry it with the arrows, OK to put it down | Yes, remembered on the TV |
| Listing and opening the TV's apps and HDMI / AV inputs | Yes, through webOS's application manager (to be confirmed on a real TV) |
| Being the **home screen** | **No.** webOS lets no app replace its home screen: Clear TV Islamic is an app you open |
| Debloating, cache cleaning, the clock screensaver | **No.** webOS gives apps no access to the system |
| Hiding pictures with people in them | By their caption only: webOS has no face detector |

For the whole Clear TV Islamic on an LG TV, plug in a Google TV or Android TV box (Chromecast with Google TV, Mi Box,
NVIDIA Shield) and set it up as in the main [README](../README.md).

## Running it

You need Node.js and LG's tools: the [webOS TV Simulator](https://webostv.developer.lge.com/develop/tools/simulator-installation)
and the command-line tools (`npm install -g @webos-tools/cli`).

```bash
ares-launch -s 26 -sp <path to webOS_TV_26_Simulator> webos      # in the simulator
```

Developer Mode differs between LG models and webOS versions; look up the steps for the exact model first.
On a real LG TV: install **Developer Mode** from the LG Content Store, sign in with a free LG developer
account, switch Dev Mode on, then:

```bash
ares-setup-device                         # add the TV (its IP and the passphrase Dev Mode shows)
ares-package -e samples -e test webos     # makes com.cocombee.glasstv_0.1.0_all.ipk
ares-install -d <tv> com.cocombee.glasstv_0.1.0_all.ipk
ares-launch -d <tv> com.cocombee.glasstv
```

Developer Mode switches itself off after 50 hours unless you extend it in the Dev Mode app.

## Previewing in a browser

`index.html#focus=-1,0` starts with Inputs selected, `#menu` with the Inputs menu open, `#hold` with an
app's hold menu open, `#move` with an app being moved. Sample tiles show
when there is no webOS application manager. Real app banners can be dropped into `webos/samples/`
(`youtube.png`, `netflix.png`, ...): that folder is kept out of git, since they are other companies' logos.

## Stress test

`index.html#stress` (or `ares-launch ... -p "stress=quick"` in the simulator) presses the remote over 8,000
times: random arrows and Back, the Inputs menu opened and walked 300 times, apps held, moved 500 times and
dropped, and, in the full run, half an hour of places changing. It checks after each phase that no app was
lost or doubled, nothing is left open or selected twice, and the page has not grown, then shows the result.
The last run in the webOS TV 26 Simulator: no errors, no growth, 0.12 ms per key press on average and 3.7 ms
at worst.
