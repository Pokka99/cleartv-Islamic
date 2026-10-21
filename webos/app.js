/*
 * Clear TV Islamic for webOS. The Android launcher's Home, rebuilt as a webOS web app: the clock row, the place
 * shelf over a dimmed picture of a place, a glass dock of apps, and one selection that glides between
 * them. webOS lets no app replace its home screen, so this is an app you open; it lists and opens the
 * TV's apps through webOS's own application manager, and shows sample tiles where that is missing
 * (the webOS TV Simulator).
 */
(function () {
  'use strict';
  var COLUMNS = 4, TILE_W = 360, TILE_H = 202, GAP = 64, LEFT = 144;
  var DOCK_TILE_TOP = 720, GRID_TOP = 1036, ROW_PITCH = 292;
  /** How far the rows move up when a grid row is selected: the dock rises to 388, as on Android. */
  var GRID_SHIFT = -304;
  /** The pane is exactly the zoomed tile, behind it: hidden at rest, seen only while it glides. */
  var FOCUS_SCALE = 1.07;
  var ROTATE_MS = 60 * 1000;
  var KEY = { LEFT: 37, UP: 38, RIGHT: 39, DOWN: 40, OK: 13, BACK: 461 };

  var $ = function (id) { return document.getElementById(id); };
  var rowsEl = $('rows'), glider = $('glider'), hero = $('hero'), glow = $('glow');
  var pills = [$('inputs'), $('settings')];
  var apps = [], tiles = [], inputs = [];
  /** Hold OK on an app: its menu (Move, Open), and whether it is being moved. */
  var hold = { open: false, index: 0, rows: [], timer: 0, fired: false };
  var moving = false;
  var HOLD_MS = 600, ORDER_KEY = 'glasstv.order';
  var MOVE_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M12,3V21M3,12H21M9.5,5.5L12,3L14.5,5.5M9.5,18.5L12,21L14.5,18.5M5.5,9.5L3,12L5.5,14.5M18.5,9.5L21,12L18.5,14.5"/></svg>';
  var OPEN_ICON = '<svg viewBox="0 0 24 24" fill="#fff"><path d="M8,5.5v13l10.5,-6.5z"/></svg>';
  /** The Inputs menu: open or not, and which row is selected. */
  var menu = { open: false, index: 0, rows: [] };
  var HDMI_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.6"><path d="M3,7.5H21V12.5L18.5,16.5H5.5L3,12.5Z"/><path d="M7,11H17"/></svg>';
  var AV_ICON = '<svg viewBox="0 0 24 24" fill="#fff"><path d="M5,2c0,-0.55 -0.45,-1 -1,-1S3,1.45 3,2v4H1v6c0,1.3 0.84,2.4 2,2.82V23h2v-8.18C6.16,14.4 7,13.3 7,12V6H5V2zM13,2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v4H9v6c0,1.3 0.84,2.4 2,2.82V23h2v-8.18c1.16,-0.41 2,-1.51 2,-2.82V6h-2V2zM21,2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v4h-2v6c0,1.3 0.84,2.4 2,2.82V23h2v-8.18c1.16,-0.41 2,-1.51 2,-2.82V6h-2V2z"/></svg>';
  /** webOS has an app per input: com.webos.app.hdmi1, ..., com.webos.app.externalinput.av1. */
  var INPUT_ID = /^com\.webos\.app\.(hdmi\d|externalinput\.(av|component)\d)$/;
  var SAMPLE_INPUTS = [{ id: 'com.webos.app.hdmi1', title: 'HDMI 1' }, { id: 'com.webos.app.hdmi2', title: 'HDMI 2' },
    { id: 'com.webos.app.hdmi3', title: 'HDMI 3' }, { id: 'com.webos.app.externalinput.av1', title: 'AV' }];
  /** Where the selection is: row -1 is the top bar, row 0 the dock, 1 and on the grid. */
  var focus = { row: 0, col: 0 };

  // ---- webOS services -------------------------------------------------------------------------

  var bridges = [];
  function luna(uri, params, callback) {
    if (typeof PalmServiceBridge === 'undefined') { if (callback) callback(null); return; }
    var b = new PalmServiceBridge();
    bridges.push(b);   // kept, or the answer can be collected before it arrives
    b.onservicecallback = function (msg) {
      bridges.splice(bridges.indexOf(b), 1);
      var r = null;
      try { r = JSON.parse(msg); } catch (e) {}
      if (callback) callback(r);
    };
    b.call(uri, JSON.stringify(params || {}));
  }

  function launch(id) {
    if (!id) return;
    if (typeof PalmServiceBridge === 'undefined') { console.log('[GlassTV] would open ' + id); return; }
    luna('luna://com.webos.applicationManager/launch', { id: id });
  }

  /**
   * Sample tiles for the simulator, which has no application manager. A banner in samples/ (kept out of
   * the repository: they are other companies' logos) shows when present; otherwise the name, in words.
   */
  var SAMPLES = [
    ['YouTube', '#e62117', 'youtube'], ['Netflix', '#141414', 'netflix'], ['Disney+', '#0b2f5c', 'disney'],
    ['Apple TV', '#1c1c1e', 'appletv'], ['Viu', '#f5b800', 'viu'], ['Durioo+', '#f08a24', 'durioo'],
    ['Crunchyroll', '#f47521', 'crunchyroll'], ['Prime Video', '#1a98ff', 'prime'], ['Spotify', '#1db954'],
    ['LG Channels', '#c3195d'], ['Live TV', '#0f7b8a'], ['Web Browser', '#3b5bdb'], ['Media Player', '#6a5cff'],
    ['Gallery', '#e8590c'], ['Music', '#d6336c'], ['Settings', '#495057']
  ].map(function (s) {
    return { id: '', title: s[0], color: s[1], banner: s[2] ? 'samples/' + s[2] + '.png' : '', sample: true };
  });

  function loadApps(done) {
    luna('luna://com.webos.applicationManager/listLaunchPoints', {}, function (r) {
      var list = r && r.launchPoints;
      if (!list || !list.length) { inputs = SAMPLE_INPUTS; return done(SAMPLES); }
      // The inputs go in the Inputs menu, not among the apps.
      inputs = list.filter(function (lp) { return INPUT_ID.test(lp.id); }).map(function (lp) {
        return { id: lp.id, title: lp.title };
      });
      done(list.filter(function (lp) { return lp.id !== 'com.cocombee.glasstv' && !INPUT_ID.test(lp.id); }).map(function (lp) {
        return { id: lp.id, title: lp.title, icon: lp.largeIcon || lp.icon, banner: lp.bgImage,
          color: lp.bgColor || lp.iconColor || '#2a2a30' };
      }));
    });
  }

  // ---- tiles -------------------------------------------------------------------------------------

  function rowTop(row) { return row === 0 ? DOCK_TILE_TOP : GRID_TOP + (row - 1) * ROW_PITCH; }

  function build() {
    rowsEl.innerHTML = '';
    var tray = document.createElement('div');
    tray.className = 'tray glass';
    tray.style.top = (DOCK_TILE_TOP - 28) + 'px';
    rowsEl.appendChild(tray);
    buildMenu();
    // The selection lives in the rows too, above the tray's glass and under the tiles.
    rowsEl.appendChild(glider);
    buildHold();
    tiles = [];
    for (var i = 0; i < apps.length; i++) {
      var a = apps[i], t = document.createElement('div');
      t.className = 'tile';
      t.style.backgroundColor = a.color;
      if (a.banner) {
        t.style.backgroundImage = 'url("' + a.banner + '")';
        if (a.sample) wordIfMissing(t, a);
      } else if (a.icon) {
        var img = document.createElement('img');
        img.src = a.icon;
        t.appendChild(img);
      } else {
        var w = document.createElement('span');
        w.className = 'word';
        w.textContent = a.title;
        t.appendChild(w);
      }
      rowsEl.appendChild(t);
      // The name shows under a grid tile when it is selected; the dock has none.
      var n = document.createElement('div');
      n.className = 'name';
      n.textContent = a.title;
      rowsEl.appendChild(n);
      tiles.push({ el: t, name: n, app: a });
    }
    layoutTiles();
  }

  /** Puts every tile in its slot; a tile whose slot changed slides there. */
  function layoutTiles() {
    tiles.forEach(function (t, i) {
      var row = Math.floor(i / COLUMNS), col = i % COLUMNS, x = LEFT + col * (TILE_W + GAP);
      t.el.style.left = x + 'px';
      t.el.style.top = rowTop(row) + 'px';
      t.name.style.left = x + 'px';
      t.name.style.top = (rowTop(row) + TILE_H + 14) + 'px';
    });
  }

  // ---- arranging the apps --------------------------------------------------------------------------

  function key(a) { return a.id || a.title; }

  /** The order someone arranged, then any app installed since, in the TV's own order. */
  function ordered(list) {
    var saved = [];
    try { saved = JSON.parse(localStorage.getItem(ORDER_KEY)) || []; } catch (e) {}
    var pos = {};
    saved.forEach(function (k, i) { pos[k] = i; });
    return list.map(function (a, i) { return { a: a, i: i }; }).sort(function (x, y) {
      var px = pos[key(x.a)], py = pos[key(y.a)];
      if (px !== undefined && py !== undefined) return px - py;
      if (px !== undefined) return -1;
      if (py !== undefined) return 1;
      return x.i - y.i;
    }).map(function (x) { return x.a; });
  }

  function saveOrder() {
    try { localStorage.setItem(ORDER_KEY, JSON.stringify(apps.map(key))); } catch (e) {}
  }

  /** The menu held OK brings up, above the app. */
  function buildHold() {
    var glass = document.createElement('div'), items = document.createElement('div');
    glass.id = 'holdGlass';
    glass.className = 'glass';
    items.id = 'holdItems';
    hold.rows = [];
    [['Move', MOVE_ICON, 'move'], ['Open', OPEN_ICON, 'open']].forEach(function (r) {
      var row = document.createElement('div');
      row.className = 'mrow';
      row.innerHTML = r[1] + '<span></span>';
      row.lastChild.textContent = r[0];
      items.appendChild(row);
      hold.rows.push({ el: row, action: r[2] });
    });
    rowsEl.appendChild(glass);
    rowsEl.appendChild(items);
  }

  function openHold(open) {
    hold.open = open;
    hold.index = 0;
    var glass = $('holdGlass'), items = $('holdItems');
    if (open) {
      var x = LEFT + focus.col * (TILE_W + GAP), width = 300;
      items.style.width = glass.style.width = width + 'px';
      hold.left = x + (TILE_W - width) / 2;
      hold.top = rowTop(focus.row) - items.offsetHeight - 28;
      [glass, items].forEach(function (el) { el.style.left = hold.left + 'px'; el.style.top = hold.top + 'px'; });
      glass.style.height = items.offsetHeight + 'px';
    }
    glass.classList.toggle('open', open);
    items.classList.toggle('open', open);
    show();
  }

  function holdKey(code) {
    if (code === KEY.UP) hold.index = Math.max(0, hold.index - 1);
    else if (code === KEY.DOWN) hold.index = Math.min(hold.rows.length - 1, hold.index + 1);
    else if (code === KEY.BACK) return openHold(false);
    else if (code === KEY.OK) {
      var action = hold.rows[hold.index].action;
      openHold(false);
      if (action === 'open') launch(tiles[index(focus)].app.id);
      else startMove();
      return;
    } else return;
    show();
  }

  /** Picks the app up: it wiggles, the arrows carry it and the others slide aside; OK puts it down. */
  function startMove() {
    moving = true;
    tiles[index(focus)].el.classList.add('wiggle');
    show();
  }

  function moveKey(code) {
    var from = index(focus), to = from;
    if (code === KEY.LEFT) to = from - 1;
    else if (code === KEY.RIGHT) to = from + 1;
    else if (code === KEY.UP) to = from - COLUMNS;
    else if (code === KEY.DOWN) to = from + COLUMNS;
    else if (code === KEY.OK || code === KEY.BACK) {
      moving = false;
      tiles[from].el.classList.remove('wiggle');
      saveOrder();
      return show();
    } else return;
    to = Math.max(0, Math.min(apps.length - 1, to));
    if (to === from) return;
    tiles.splice(to, 0, tiles.splice(from, 1)[0]);
    apps.splice(to, 0, apps.splice(from, 1)[0]);
    focus = { row: Math.floor(to / COLUMNS), col: to % COLUMNS };
    layoutTiles();
    show();
  }

  function wordIfMissing(tile, a) {
    var probe = new Image();
    probe.onerror = function () {
      tile.style.backgroundImage = 'none';
      var w = document.createElement('span');
      w.className = 'word';
      w.textContent = a.title;
      tile.appendChild(w);
    };
    probe.src = a.banner;
  }

  /** The Inputs menu, under the Inputs and Settings buttons, as on Android. */
  function buildMenu() {
    var glass = document.createElement('div'), items = document.createElement('div');
    glass.id = 'menuGlass';
    glass.className = 'glass';
    items.id = 'menuItems';
    menu.rows = [];
    var sawHdmi = false;
    inputs.forEach(function (input) {
      var hdmi = /hdmi/.test(input.id);
      if (!hdmi && sawHdmi && !menu.rows.some(function (r) { return !r.hdmi; })) {
        var sep = document.createElement('div');
        sep.className = 'msep';
        items.appendChild(sep);
      }
      if (hdmi) sawHdmi = true;
      var row = document.createElement('div');
      row.className = 'mrow';
      row.innerHTML = (hdmi ? HDMI_ICON : AV_ICON) + '<span></span>';
      row.lastChild.textContent = input.title;
      items.appendChild(row);
      menu.rows.push({ input: input, hdmi: hdmi, el: row });
    });
    rowsEl.appendChild(glass);
    rowsEl.appendChild(items);
  }

  /**
   * Puts the menu under the Inputs and Settings buttons, measured when it opens: the buttons change
   * width once the font has loaded, and a menu placed earlier drifted from them.
   */
  function placeMenu() {
    var a = layoutRect(pills[0]), b = layoutRect(pills[1]), items = $('menuItems');
    menu.left = a.left;
    menu.top = a.top + a.height + 19;
    menu.width = b.left + b.width - a.left;
    [$('menuGlass'), items].forEach(function (el) {
      el.style.left = menu.left + 'px';
      el.style.top = menu.top + 'px';
      el.style.width = menu.width + 'px';
      el.style.height = 'auto';
    });
    $('menuGlass').style.height = items.offsetHeight + 'px';
  }

  function openMenu(open) {
    if (open && !inputs.length) { launch('com.webos.app.inputpicker'); return; }
    menu.open = open;
    menu.index = 0;
    if (open) placeMenu();
    $('menuGlass').classList.toggle('open', open);
    $('menuItems').classList.toggle('open', open);
    show();
  }

  function rows() { return Math.ceil(apps.length / COLUMNS); }
  function index(f) { return f.row * COLUMNS + f.col; }

  // ---- the selection ----------------------------------------------------------------------------

  var landTimer = 0;
  /** Moves the selection. {@code landStyle}: the look it takes once it has arrived, if not the same. */
  function placeGlider(x, y, w, h, radius, style, landStyle) {
    clearTimeout(landTimer);
    glider.className = style;
    if (landStyle && landStyle !== style) {
      landTimer = setTimeout(function () { glider.className = landStyle; }, 320);
    }
    glider.style.width = w + 'px';
    glider.style.height = h + 'px';
    glider.style.borderRadius = radius + 'px';
    glider.style.transform = 'translate(' + x + 'px,' + y + 'px)';
    glider.style.opacity = 1;
    // Over the hold menu's glass when that is open (it sits above the tiles); otherwise under the tiles.
    glider.style.zIndex = hold.open ? 4 : 1;
  }

  function show() {
    tiles.forEach(function (t) { t.el.classList.remove('focus'); t.name.classList.remove('on'); });
    pills.forEach(function (p) { p.classList.remove('selected'); });
    if (hold.open) {
      var hr = hold.rows[hold.index].el;
      tiles[index(focus)].el.classList.add('focus');
      placeGlider(hold.left + hr.offsetLeft, hold.top + hr.offsetTop, hr.offsetWidth, hr.offsetHeight, 22, 'dark-style');
      return;
    }
    if (menu.open) {
      // A dark row inside the menu's glass.
      rowsEl.style.transform = 'translateY(0)';
      var mr = menu.rows[menu.index].el;
      placeGlider(menu.left + mr.offsetLeft, menu.top + mr.offsetTop, mr.offsetWidth, mr.offsetHeight, 22, 'dark-style');
      hero.style.opacity = 1;
      glow.style.opacity = 0;
      return;
    }
    if (focus.row < 0) {
      pills[focus.col].classList.add('selected');
      // The top bar: a dark pill, and the rows go back to the dock.
      rowsEl.style.transform = 'translateY(0)';
      var p = layoutRect(pills[focus.col]);
      placeGlider(p.left, p.top, p.width, p.height, p.height / 2, 'dark-style');
      hero.style.opacity = 1;
      glow.style.opacity = 0;
      return;
    }
    var shift = focus.row === 0 ? 0 : GRID_SHIFT - (focus.row - 1) * ROW_PITCH;
    rowsEl.style.transform = 'translateY(' + shift + 'px)';
    var t = tiles[index(focus)];
    t.el.classList.add('focus');
    if (moving) {
      // The moving app hides the selection behind it.
      glider.style.opacity = 0;
      hero.style.opacity = focus.row === 0 ? 1 : 0;
      return;
    }
    if (focus.row > 0) t.name.classList.add('on');
    // Behind the zoomed tile, worked out from the layout so it lands where the rows are going.
    var w = TILE_W * FOCUS_SCALE, h = TILE_H * FOCUS_SCALE;
    var cx = LEFT + focus.col * (TILE_W + GAP) + TILE_W / 2, cy = rowTop(focus.row) + TILE_H / 2;
    // Down from the top bar it travels dark and turns to glass once it lands, unseen behind the app.
    var fromTop = glider.className === 'dark-style';
    placeGlider(cx - w / 2, cy - h / 2, w, h, 26 * FOCUS_SCALE, fromTop ? 'dark-style' : 'glass-style', 'glass-style');
    // The place shelf belongs to the dock; the room takes the app's colour.
    hero.style.opacity = focus.row === 0 ? 1 : 0;
    glow.style.background = 'radial-gradient(70% 60% at 50% 85%, ' + hexA(t.app.color, 0.45) + ', transparent 70%)';
    glow.style.opacity = 1;
  }

  /**
   * Where an element sits in the page's own 1920 x 1080 layout. Not getBoundingClientRect: the simulator
   * scales the page down to fit its window, and that answers in the scaled size.
   */
  function layoutRect(el) {
    var x = 0, y = 0, w = el.offsetWidth, h = el.offsetHeight;
    for (; el; el = el.offsetParent) { x += el.offsetLeft; y += el.offsetTop; }
    return { left: x, top: y, width: w, height: h };
  }

  function hexA(hex, a) {
    var m = /^#?([0-9a-f]{6})$/i.exec(hex || '');
    if (!m) return 'rgba(255,255,255,' + a / 3 + ')';
    var n = parseInt(m[1], 16);
    return 'rgba(' + (n >> 16 & 255) + ',' + (n >> 8 & 255) + ',' + (n & 255) + ',' + a + ')';
  }

  function move(dRow, dCol) {
    var f = { row: focus.row, col: focus.col };
    if (dRow < 0 && f.row === 0) {
      f = { row: -1, col: f.col >= COLUMNS / 2 ? 1 : 0 };
    } else if (dRow > 0 && f.row === -1) {
      f = { row: 0, col: f.col === 1 ? COLUMNS - 1 : 0 };
    } else if (f.row === -1) {
      f.col = Math.max(0, Math.min(pills.length - 1, f.col + dCol));
    } else {
      f.row = Math.max(0, Math.min(rows() - 1, f.row + dRow));
      f.col = Math.max(0, f.col + dCol);
      var last = apps.length - 1;
      if (index(f) > last) { f.row = Math.floor(last / COLUMNS); f.col = Math.min(f.col, last % COLUMNS); }
      f.col = Math.min(f.col, COLUMNS - 1);
    }
    focus = f;
    show();
  }

  function menuKey(code) {
    if (code === KEY.UP) {
      if (menu.index === 0) return openMenu(false);   // Up from the top row glides back to Inputs
      menu.index--;
    } else if (code === KEY.DOWN) {
      menu.index = Math.min(menu.rows.length - 1, menu.index + 1);
    } else if (code === KEY.OK) {
      launch(menu.rows[menu.index].input.id);
      return openMenu(false);
    } else if (code === KEY.BACK) {
      return openMenu(false);
    } else {
      return;
    }
    show();
  }

  document.addEventListener('keydown', function (e) {
    if (hold.open || moving) {
      if ([KEY.UP, KEY.DOWN, KEY.OK, KEY.BACK, KEY.LEFT, KEY.RIGHT].indexOf(e.keyCode) >= 0) {
        // The OK that brought the menu up is still held: its repeats and release choose nothing.
        if (e.keyCode === KEY.OK && (e.repeat || hold.fired)) { e.preventDefault(); return; }
        if (hold.open) holdKey(e.keyCode); else moveKey(e.keyCode);
        e.preventDefault();
      }
      return;
    }
    if (menu.open) {
      if ([KEY.UP, KEY.DOWN, KEY.OK, KEY.BACK, KEY.LEFT, KEY.RIGHT].indexOf(e.keyCode) >= 0) {
        menuKey(e.keyCode);
        e.preventDefault();
      }
      return;
    }
    switch (e.keyCode) {
      case KEY.LEFT: move(0, -1); break;
      case KEY.RIGHT: move(0, 1); break;
      case KEY.UP: move(-1, 0); break;
      case KEY.DOWN: move(1, 0); break;
      case KEY.OK:
        if (focus.row < 0 && pills[focus.col].id === 'inputs') openMenu(true);
        else if (focus.row < 0) launch(pills[focus.col].getAttribute('data-launch'));
        else if (!e.repeat && !hold.timer) {
          // On an app, OK opens it when let go; held, it brings up the app's menu instead.
          hold.fired = false;
          hold.timer = setTimeout(function () {
            hold.timer = 0;
            hold.fired = true;
            openHold(true);
          }, HOLD_MS);
        }
        break;
      case KEY.BACK:
        // As Home on Home: back to the first app.
        focus = { row: 0, col: 0 };
        show();
        break;
      default: return;
    }
    e.preventDefault();
  });

  document.addEventListener('keyup', function (e) {
    if (e.keyCode !== KEY.OK) return;
    if (hold.timer) {
      clearTimeout(hold.timer);
      hold.timer = 0;
      launch(tiles[index(focus)].app.id);
    }
    hold.fired = false;
  });

  // ---- clock -------------------------------------------------------------------------------------

  function tick() {
    var now = new Date();
    var parts = now.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }).split(' ');
    $('time').textContent = parts[0];
    $('ampm').textContent = parts[1] || '';
    $('date').textContent = now.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).toUpperCase();
  }

  // ---- places ------------------------------------------------------------------------------------

  var places = [], placeIndex = -1, front = $('wallA'), back = $('wallB');

  function nextPlace() {
    if (!places.length) return;
    placeIndex = (placeIndex + 1) % places.length;
    var p = places[placeIndex], img = new Image();
    img.onload = function () {
      // The words fade out with the old picture and back in with the new one.
      hero.classList.add('out');
      setTimeout(function () {
        $('placeName').textContent = p.name;
        $('placeCountry').textContent = p.country;
        $('placeText').textContent = p.text;
        hero.classList.remove('out');
      }, 250);
      back.style.backgroundImage = 'url("' + p.url + '")';
      back.classList.add('on');
      front.classList.remove('on');
      var t = front; front = back; back = t;
    };
    img.src = p.url;
  }

  // ---- start -------------------------------------------------------------------------------------

  tick();
  setInterval(tick, 10 * 1000);
  // index.html#stress, or the launch parameter {"stress": "quick"}, runs the stress test
  // (test/stress.js, left out of the package).
  var launchParams = '';
  try { launchParams = window.PalmSystem ? String(window.PalmSystem.launchParams || '') : ''; } catch (e) {}
  if (/stress/.test(location.hash + launchParams)) {
    if (/quick/.test(launchParams) && !/quick/.test(location.hash)) location.hash = 'stress-quick';
    var st = document.createElement('script');
    st.src = 'test/stress.js';
    document.body.appendChild(st);
  }
  loadApps(function (list) {
    apps = ordered(list);
    build();
    // "#focus=row,col" starts with that selected, for previews (row -1 is the top bar).
    var m = /focus=(-?\d+),(\d+)/.exec(location.hash);
    if (m) focus = { row: +m[1], col: +m[2] };
    if (/menu/.test(location.hash)) { focus = { row: -1, col: 0 }; openMenu(true); }
    if (/hold/.test(location.hash)) { focus = { row: 0, col: 1 }; openHold(true); }
    if (/move/.test(location.hash)) { focus = { row: 0, col: 1 }; startMove(); moveKey(KEY.RIGHT); }
    show();
    // The buttons take their final size once the font is in: measure the selection and menu again.
    if (document.fonts && document.fonts.ready) {
      document.fonts.ready.then(function () { if (menu.open) placeMenu(); show(); });
    }
  });
  Places.load(function (list) {
    places = list;
    nextPlace();
    if (places.length > 1) setInterval(nextPlace, ROTATE_MS);
  });
})();
