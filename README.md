# Kerf Vector Viewer

A simple Android app for phones and tablets to look at 2D drawings and CNC files.

## What it opens

- **DXF**: AutoCAD and most CAD/CAM software.
- **DWG**: AutoCAD drawings, read by LibreDWG (an open-source DWG reader built into the app).
- **SVG**: Inkscape, Illustrator, Figma and others. Sizes in mm, cm, inch or px are all understood.
- **G-code**: .nc, .gcode, .tap, .ngc, .cnc and similar. Shows the toolpath from above, with fast travel moves dashed.

Files that Kerf can't open (CorelDRAW .cdr, Illustrator .ai, PDF, EPS) get a message suggesting you export them as DXF or SVG.

## What it does

- **Open file** button, or tap a drawing in your file manager and pick **Open with Kerf**, or **Share** a drawing to Kerf from WhatsApp, Drive or email.
- Drag with one finger to move, pinch to zoom, and double-tap to zoom in. **Fit** shows the whole drawing again.
- **Measure**: tap two points to get the distance, the X and Y parts, and the angle. Taps snap onto corners, line ends and circle centres.
- **Light / Dark** switches the background in one tap.
- **Info** shows:
  - DXF, DWG and SVG: size, total cut length, and the number of shapes and how many are open (not joined up).
  - G-code: cutting length, estimated time, travel distance, depth range and drill holes.
- A scale bar in the corner always shows a real length.
- **Settings** (top right): background, line colours (from the file or all one colour), show text, show travel moves, snap, mm or inch, and which unit to use for files that don't say.
- Made for phones and tablets, upright or sideways. On tablets and sideways phones the details and settings open as side panels, so the drawing stays visible.
- Works fully offline. Needs Android 7.0 or newer.

## Getting the APK (the installable app file)

### Way 1: GitHub builds it for you (already set up)

Every time code in this repository changes, GitHub builds a fresh APK by itself (takes about 3 minutes).

1. Open the **Releases** page of this repository (right side of the main page, or `/releases`).
2. Under the newest **Kerf build N**, tap **Kerf-build-N.apk** to download it.
3. Open the APK on your phone or tablet. Android will ask to allow installing apps from that source; allow it and tap **Install**.

To build again without changing anything: **Actions** tab → **Build APK** → **Run workflow**.

The app's signing key is kept out of this repository. It is stored as a private GitHub secret named `KERF_KEYSTORE` (Settings → Secrets and variables → Actions). With that secret set, every build is signed the same way, so a new APK installs over the old one as an update. Without it, Android may ask you to uninstall the old version first.

### Way 2: Android Studio on your laptop

1. Install Android Studio from developer.android.com/studio.
2. **File → Open** and choose this folder. Wait for "Gradle sync" to finish (10–20 minutes the first time).
3. Run `python3 tools/build_web.py` once, then **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. The file is `app/build/outputs/apk/debug/app-debug.apk`.

## Changing things later

- **The look and the tools** live in one file: `web/index.html`. After editing it, run `python3 tools/build_web.py`. GitHub does this step for you.
- **App name**: `app/src/main/res/values/strings.xml`.
- **App icon**: `app/src/main/res/drawable/ic_launcher_foreground.xml` and the `mipmap-*` folders.
- **Version number**: goes up by itself with every GitHub build (see `app/build.gradle`).

## Licence

Kerf is open source under the **GNU General Public License v3.0** (see [LICENSE](LICENSE)). You may use, change and share it; anyone who shares a changed version must also share its source code under the same licence.

- DWG reading uses **LibreDWG** (via libredwg-web 0.7.17, GPL-3.0), which is why Kerf uses the GPL too.
- DXF reading uses **dxf-parser** (MIT licence).
- The fonts are Barlow and JetBrains Mono (SIL Open Font Licence).

## Folder map

```
web/index.html            the viewer: screen, tools, settings and all file readers (edit this)
web/lib/dxf-parser.js     DXF reader
web/lib/libredwg/         DWG reader (JavaScript + WebAssembly, about 11 MB)
web/fonts/                fonts, so the app works offline
app/src/main/...          the Android wrapper around the viewer
tools/build_web.py        copies the viewer into the Android app
.github/workflows/        instructions GitHub follows to build the APK
```
