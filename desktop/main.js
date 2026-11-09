// Kerf for Windows.
// Shows the same viewer page as the Android app (web/index.html) in a desktop window.
// Electron gives the page its own copy of Chrome, so it works the same on every PC.

const { app, BrowserWindow, Menu, protocol, shell, nativeTheme } = require('electron');
const path = require('path');
const fs = require('fs');

// ---- The only part that differs between Facet and Kerf ----
const CONFIG = {
  openFunction: 'appOpenUrl',                    // page function that opens a file handed over by Windows
  background: { dark: '#0e1013', light: '#fbfaf7' },
};
// -----------------------------------------------------------

const WWW = path.join(__dirname, 'www');
const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json', '.wasm': 'application/wasm', '.woff2': 'font/woff2',
  '.png': 'image/png', '.svg': 'image/svg+xml', '.txt': 'text/plain; charset=utf-8',
};

// The page is served from app://local/ so that fonts, scripts and the DWG reader's .wasm file load like on a website.
protocol.registerSchemesAsPrivileged([
  { scheme: 'app', privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true } },
]);

let win = null;
const opened = new Map();   // files passed in by Windows ("Open with", double-click), served at app://local/opened/N
let openedCount = 0;
let pendingFile = null;

// A file path given on the command line, e.g. when a drawing is opened from File Explorer.
function fileFromArgs(argv) {
  for (const a of argv.slice(1)) {
    if (!a || a.startsWith('-')) continue;
    try {
      const p = path.resolve(a);
      if (fs.statSync(p).isFile() && p !== process.execPath) return p;
    } catch (e) { /* not a file */ }
  }
  return null;
}

function openInPage(file) {
  if (!file || !win) return;
  const id = String(++openedCount);
  opened.set(id, file);
  const url = 'app://local/opened/' + id;
  const fn = CONFIG.openFunction;
  win.webContents.executeJavaScript(`window.${fn} && window.${fn}(${JSON.stringify(url)}, ${JSON.stringify(path.basename(file))})`).catch(() => {});
}

function createWindow() {
  win = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 360,
    minHeight: 480,
    show: false,
    autoHideMenuBar: true,
    backgroundColor: nativeTheme.shouldUseDarkColors ? CONFIG.background.dark : CONFIG.background.light,
    webPreferences: { contextIsolation: true, sandbox: true, nodeIntegration: false, spellcheck: false },
  });
  win.once('ready-to-show', () => win.show());

  // Links to websites open in the normal browser, never inside the app.
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
  win.webContents.on('will-navigate', (event, url) => {
    if (!url.startsWith('app://')) {
      event.preventDefault();
      if (/^https?:/i.test(url)) shell.openExternal(url);
    }
  });

  win.webContents.on('did-finish-load', () => {
    if (pendingFile) { openInPage(pendingFile); pendingFile = null; }
  });

  // If a huge file makes the page run out of memory, start it again instead of showing a blank window.
  win.webContents.on('render-process-gone', (event, details) => {
    if (details.reason !== 'clean-exit') win.loadURL('app://local/index.html#reload');
  });

  // Keyboard shortcuts: Ctrl+O opens a file, F11 full screen, Esc closes panels (handled by the page).
  win.webContents.on('before-input-event', (event, input) => {
    if (input.type !== 'keyDown') return;
    if (input.control && !input.alt && input.key.toLowerCase() === 'o') {
      event.preventDefault();
      win.webContents.executeJavaScript("document.getElementById('openBtn').click()", true).catch(() => {});
    } else if (input.key === 'F11') {
      event.preventDefault();
      win.setFullScreen(!win.isFullScreen());
    }
  });

  win.loadURL('app://local/index.html');
}

if (!app.requestSingleInstanceLock()) {
  // Already running: the first window receives the file instead.
  app.quit();
} else {
  pendingFile = fileFromArgs(process.argv);

  app.on('second-instance', (event, argv) => {
    if (!win) return;
    if (win.isMinimized()) win.restore();
    win.focus();
    openInPage(fileFromArgs(argv));
  });

  app.whenReady().then(() => {
    Menu.setApplicationMenu(null);

    protocol.handle('app', async (request) => {
      const url = new URL(request.url);
      let rel = decodeURIComponent(url.pathname);
      if (rel === '/' || rel === '') rel = '/index.html';
      let file;
      let type;
      const m = /^\/opened\/(\d+)$/.exec(rel);
      if (m) {
        file = opened.get(m[1]);
        type = 'application/octet-stream';
      } else {
        file = path.normalize(path.join(WWW, rel));
        if (!file.startsWith(WWW + path.sep)) return new Response('Not found', { status: 404 });
        type = MIME[path.extname(file).toLowerCase()] || 'application/octet-stream';
      }
      try {
        const data = await fs.promises.readFile(file);
        return new Response(data, { headers: { 'content-type': type, 'cache-control': 'no-store' } });
      } catch (e) {
        return new Response('Not found', { status: 404 });
      }
    });

    createWindow();
  });

  app.on('window-all-closed', () => app.quit());
}
