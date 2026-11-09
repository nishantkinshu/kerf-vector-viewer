#!/usr/bin/env python3
"""Copy the viewer page from web/ into the Android app.

Edit web/index.html, then run:   python3 tools/build_web.py
The GitHub build runs this for you automatically.

What it does:
  - wraps the page in a full HTML document (charset, viewport)
  - swaps the online Google Fonts link for the font files bundled in web/fonts,
    so the app works with no internet
  - copies the libraries (web/lib) and fonts into app/src/main/assets/www
"""
import os
import re
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WEB = os.path.join(ROOT, "web")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "www")

FONT_FACES = [
    ("Barlow", 400, "barlow-latin-400-normal.woff2"),
    ("Barlow", 500, "barlow-latin-500-normal.woff2"),
    ("Barlow", 600, "barlow-latin-600-normal.woff2"),
    ("Barlow Condensed", 500, "barlow-condensed-latin-500-normal.woff2"),
    ("Barlow Condensed", 600, "barlow-condensed-latin-600-normal.woff2"),
    ("JetBrains Mono", 400, "jetbrains-mono-latin-400-normal.woff2"),
    ("JetBrains Mono", 500, "jetbrains-mono-latin-500-normal.woff2"),
]


def main():
    with open(os.path.join(WEB, "index.html"), encoding="utf-8") as fh:
        page = fh.read()

    # Remove the online font links and the artifact-only meta tag.
    page = re.sub(r'<link rel="(preconnect|stylesheet)" href="https://fonts\.g[^"]*"[^>]*>\n?', "", page)
    page = page.replace('<meta name="claude:viewport-fit-sides" content="cover">\n', "")

    faces = "\n".join(
        '@font-face { font-family: "%s"; font-weight: %d; font-style: normal; font-display: swap; src: url("fonts/%s") format("woff2"); }'
        % f for f in FONT_FACES
    )
    head = (
        "<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n"
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover\">\n"
        "<style>\n" + faces + "\n</style>\n"
    )
    # Everything up to the first <div id="app"> belongs in <head>.
    split = page.index('<div id="app">')
    doc = head + page[:split] + "</head>\n<body>\n" + page[split:] + "\n</body>\n</html>\n"

    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)
    with open(os.path.join(OUT, "index.html"), "w", encoding="utf-8") as fh:
        fh.write(doc)
    shutil.copytree(os.path.join(WEB, "lib"), os.path.join(OUT, "lib"))
    shutil.copytree(os.path.join(WEB, "fonts"), os.path.join(OUT, "fonts"))
    print("Viewer copied into", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
