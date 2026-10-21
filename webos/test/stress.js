/*
 * Stress test for Clear TV Islamic on webOS. Open index.html#stress (in a browser, or the simulator): it presses
 * the remote thousands of times the way a restless thumb would, and checks after every phase that
 * nothing broke, leaked or lost an app. Black-box: it reads only what is on the page.
 *
 * Result: written into <pre id="stress-result"> as JSON, and to the console.
 */
(function () {
  'use strict';
  var K = { LEFT: 37, UP: 38, RIGHT: 39, DOWN: 40, OK: 13, BACK: 461 };
  var errors = [], report = { phases: [] };
  window.addEventListener('error', function (e) { errors.push(String(e.message)); });
  window.addEventListener('unhandledrejection', function (e) { errors.push('promise: ' + e.reason); });

  var seed = 12345;
  function rand(n) { seed = (seed * 1103515245 + 12345) & 0x7fffffff; return seed % n; }
  function wait(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }
  function key(type, code, repeat) {
    var t0 = performance.now();
    document.dispatchEvent(new KeyboardEvent(type, { keyCode: code, which: code, repeat: !!repeat, bubbles: true }));
    return performance.now() - t0;
  }
  function press(code) { var t = key('keydown', code); t += key('keyup', code); return t; }
  async function holdOk(ms) { key('keydown', 13); for (var i = 0; i < ms / 100; i++) { await wait(100); key('keydown', 13, true); } key('keyup', 13); }

  function tiles() { return Array.prototype.slice.call(document.querySelectorAll('.tile')); }
  function order() {
    // Apps in their on-screen order, by where each tile is placed.
    return tiles().map(function (t) {
      return { name: t.textContent || t.style.backgroundImage, x: parseFloat(t.style.left), y: parseFloat(t.style.top) };
    }).sort(function (a, b) { return a.y - b.y || a.x - b.x; }).map(function (t) { return t.name; });
  }
  function state() {
    return {
      nodes: document.getElementsByTagName('*').length,
      tiles: tiles().length,
      focused: document.querySelectorAll('.tile.focus').length,
      wiggling: document.querySelectorAll('.tile.wiggle').length,
      gliders: document.querySelectorAll('#glider').length,
      inputsMenuOpen: !!document.querySelector('#menuItems.open'),
      holdMenuOpen: !!document.querySelector('#holdItems.open'),
      selectedPills: document.querySelectorAll('.pill.selected').length,
      heapMB: performance.memory ? +(performance.memory.usedJSHeapSize / 1048576).toFixed(1) : null
    };
  }
  /** Things that must be true whenever no key is held. */
  function check(label, expectApps) {
    var s = state(), problems = [];
    if (s.gliders !== 1) problems.push('gliders ' + s.gliders);
    if (s.tiles !== expectApps.length) problems.push('tile count ' + s.tiles + ' != ' + expectApps.length);
    var now = order().slice().sort().join('|'), was = expectApps.slice().sort().join('|');
    if (now !== was) problems.push('apps changed: lost or duplicated');
    var spots = {};
    tiles().forEach(function (t) { var k = t.style.left + ',' + t.style.top; spots[k] = (spots[k] || 0) + 1; });
    Object.keys(spots).forEach(function (k) { if (spots[k] > 1) problems.push('two tiles at ' + k); });
    if (s.focused > 1) problems.push('focused tiles ' + s.focused);
    if (s.wiggling > 1) problems.push('wiggling tiles ' + s.wiggling);
    if (s.selectedPills > 1) problems.push('selected pills ' + s.selectedPills);
    report.phases.push({ phase: label, state: s, problems: problems });
    return s;
  }

  async function run() {
    await wait(1500);                                   // apps, font and first place in
    // The moves below rearrange the apps; whatever order was saved before goes back at the end.
    var savedBefore = localStorage.getItem('glasstv.order');
    var apps = order();
    var start = check('start', apps), slow = 0, worst = 0, total = 0, n = 0;
    function timed(t) { total += t; n++; if (t > worst) worst = t; if (t > 16) slow++; }

    // 1. A restless thumb: arrows everywhere, taps on apps, Back, now and then.
    for (var i = 0; i < 4000; i++) {
      var r = rand(100), code = r < 22 ? K.LEFT : r < 44 ? K.RIGHT : r < 70 ? K.UP : r < 94 ? K.DOWN : r < 97 ? K.BACK : K.OK;
      if (code === K.OK && document.querySelector('.tile.focus')) continue;   // taps on apps would open them
      timed(press(code));
      if (document.querySelector('#menuItems.open') && rand(3) === 0) timed(press(K.BACK));
      if (i % 400 === 0) await wait(50);
    }
    timed(press(K.BACK));
    if (document.querySelector('#menuItems.open')) press(K.BACK);
    await wait(500);
    check('4000 random presses', apps);

    // 2. The Inputs menu, opened, walked and closed 300 times.
    press(K.BACK);
    for (var m = 0; m < 300; m++) {
      timed(press(K.UP));                               // dock -> top bar
      timed(press(K.LEFT));                             // Inputs
      timed(press(K.OK));                               // open
      for (var j = 0; j < 1 + rand(5); j++) timed(press(K.DOWN));
      for (var u = 0; u < 6; u++) timed(press(K.UP)); // up past the top row closes it
      timed(press(K.DOWN));                             // back to the dock
    }
    await wait(500);
    check('Inputs menu x300', apps);

    // 3. Hold OK, Move, drag the app all over, drop it; 20 times. The order must really change.
    var orderBefore = order().join('|');
    for (var h = 0; h < 20; h++) {
      press(K.BACK);
      for (var w = 0; w < rand(6); w++) press(K.RIGHT);
      for (var dn = 0; dn < rand(3); dn++) press(K.DOWN);
      await holdOk(800);
      await wait(50);
      if (!document.querySelector('#holdItems.open')) { report.phases.push({ phase: 'hold ' + h, problems: ['hold menu did not open'] }); continue; }
      timed(press(K.OK));                               // Move
      for (var mv = 0; mv < 25; mv++) timed(press([K.LEFT, K.RIGHT, K.UP, K.DOWN][rand(4)]));
      timed(press(K.OK));                               // drop
      await wait(300);
    }
    check('hold + move x20 (500 moves)', apps);
    report.orderChangedByMoves = order().join('|') !== orderBefore;
    var saved = null;
    try { saved = JSON.parse(localStorage.getItem('glasstv.order')); } catch (e) {}
    report.savedOrderLength = saved ? saved.length : null;

    // 4. Hold menu opened and backed out of, and a hold released on Open.
    for (var b = 0; b < 30; b++) { await holdOk(700); await wait(20); timed(press(K.BACK)); }
    check('hold menu x30', apps);

    // 5. Half an hour of pictures changing (virtual time in the test browser). #stress-quick skips it,
    //    for a run in real time, where the key timings mean something.
    var wallBefore = document.getElementById('wallA').style.backgroundImage + document.getElementById('wallB').style.backgroundImage;
    if (!/quick/.test(location.hash)) await wait(30 * 60 * 1000);
    var after = check('30 minutes of places', apps);
    report.placesChanged = wallBefore !== document.getElementById('wallA').style.backgroundImage + document.getElementById('wallB').style.backgroundImage;

    report.keys = { pressed: n, averageMs: +(total / n).toFixed(3), worstMs: +worst.toFixed(2), over16ms: slow };
    report.growth = { nodes: after.nodes - start.nodes, heapMB: after.heapMB !== null ? +(after.heapMB - start.heapMB).toFixed(1) : null };
    report.errors = errors;
    report.ok = !errors.length && report.phases.every(function (p) { return !p.problems.length; }) && report.growth.nodes === 0;
    try {
      if (savedBefore === null) localStorage.removeItem('glasstv.order');
      else localStorage.setItem('glasstv.order', savedBefore);
    } catch (e) {}
    var pre = document.createElement('pre');
    pre.id = 'stress-result';
    pre.textContent = JSON.stringify({ ok: report.ok, keys: report.keys, growth: report.growth, errors: report.errors,
      orderChangedByMoves: report.orderChangedByMoves, problems: report.phases.filter(function (p) { return p.problems.length; }) }, null, 1);
    pre.style.cssText = 'position:fixed;left:40px;top:40px;z-index:99;background:rgba(0,0,0,.85);color:#7CFC9A;' +
      'font:26px/1.35 monospace;padding:24px 32px;border-radius:16px;white-space:pre-wrap;max-width:1200px';
    document.body.appendChild(pre);
    console.log('[GlassTV stress] ' + JSON.stringify(report));
  }
  run();
})();
