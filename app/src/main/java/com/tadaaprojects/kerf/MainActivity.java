package com.tadaaprojects.kerf;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Kerf vector viewer (DXF, DWG, SVG, G-code).
 *
 * The whole viewer is a web page (app/src/main/assets/www/index.html) shown in a WebView.
 * This class only does the Android jobs the page cannot do on its own:
 *  - serve the page and its files from inside the app (no internet needed)
 *  - show the phone's file picker when the page asks for a file
 *  - accept drawings sent from a file manager ("Open with") or shared from another app
 *  - pass the back button to the page so it can close its panels first
 */
public class MainActivity extends Activity {

    private static final String HOST = "appassets.androidplatform.net";
    private static final String BASE = "https://" + HOST;
    private static final int PICK_FILE = 41;

    private WebView web;
    private ValueCallback<Uri[]> pendingPick;
    private boolean pageReady = false;

    // A file handed to us by another app, waiting to be shown.
    private Uri incomingUri;
    private String incomingName;
    // The file the page is currently allowed to read through /incoming/.
    private volatile Uri servingUri;
    private int incomingCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createWebView();
        readIntent(getIntent());
        web.loadUrl(BASE + "/www/index.html");
    }

    private void createWebView() {
        pageReady = false;
        web = new WebView(this);
        web.setBackgroundColor(getResources().getColor(R.color.bg));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);      // lets the page remember settings
        s.setAllowFileAccess(false);
        s.setSupportZoom(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return serve(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (ActivityNotFoundException ignored) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                deliverIncoming();
            }

            // A very large or damaged drawing can make the viewer run out of memory.
            // Instead of closing the whole app, start the viewer again.
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (view != web) return true;
                ViewGroupHelper.remove(view);
                view.destroy();
                createWebView();
                web.loadUrl(BASE + "/www/index.html#reload");
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingPick != null) pendingPick.onReceiveValue(null);
                pendingPick = callback;
                // DXF, DWG and G-code files have no reliable file type on Android, so show all files.
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("*/*");
                try {
                    startActivityForResult(pick, PICK_FILE);
                } catch (ActivityNotFoundException e) {
                    pendingPick = null;
                    return false;
                }
                return true;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readIntent(intent);
        deliverIncoming();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || pendingPick == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            result = new Uri[]{data.getData()};
        }
        pendingPick.onReceiveValue(result);
        pendingPick = null;
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.appHandleBack ? window.appHandleBack() : false", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String handled) {
                if (!"true".equals(handled)) finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }

    /* ---------- Files from other apps ---------- */

    private void readIntent(Intent intent) {
        if (intent == null) return;
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            uri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (uri == null) return;
        incomingUri = uri;
        incomingName = displayName(uri);
    }

    private void deliverIncoming() {
        if (!pageReady || incomingUri == null) return;
        servingUri = incomingUri;
        incomingCount++;
        String url = BASE + "/incoming/" + incomingCount;
        String js = "window.appOpenUrl && window.appOpenUrl(" + JSONObject.quote(url) + "," + JSONObject.quote(incomingName) + ")";
        incomingUri = null;
        web.evaluateJavascript(js, null);
    }

    private String displayName(Uri uri) {
        String name = null;
        if ("content".equals(uri.getScheme())) {
            Cursor c = null;
            try {
                c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
                if (c != null && c.moveToFirst()) name = c.getString(0);
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.close();
            }
        }
        if (name == null) name = uri.getLastPathSegment();
        return name == null ? "Drawing" : name;
    }

    /* ---------- Serving the page ---------- */

    private WebResourceResponse serve(Uri url) {
        if (!HOST.equals(url.getHost())) return null;
        String path = url.getPath() == null ? "" : url.getPath();
        try {
            if (path.startsWith("/www/")) {
                String asset = path.substring(1);
                InputStream in = getAssets().open(asset);
                return new WebResourceResponse(mimeFor(asset), mimeFor(asset).startsWith("text/") ? "utf-8" : null, in);
            }
            if (path.startsWith("/incoming/") && servingUri != null) {
                InputStream in = getContentResolver().openInputStream(servingUri);
                Map<String, String> headers = new HashMap<>();
                headers.put("Cache-Control", "no-store");
                return new WebResourceResponse("application/octet-stream", null, 200, "OK", headers, in);
            }
        } catch (Exception ignored) {
        }
        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
    }

    /** Small helper so the crashed WebView can be taken off the screen. */
    private static final class ViewGroupHelper {
        static void remove(android.view.View v) {
            android.view.ViewParent p = v.getParent();
            if (p instanceof android.view.ViewGroup) ((android.view.ViewGroup) p).removeView(v);
        }
    }

    private static String mimeFor(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".html")) return "text/html";
        if (n.endsWith(".js")) return "text/javascript";
        if (n.endsWith(".css")) return "text/css";
        if (n.endsWith(".json")) return "application/json";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".woff2")) return "font/woff2";
        if (n.endsWith(".wasm")) return "application/wasm";
        if (n.endsWith(".mjs")) return "text/javascript";
        return "application/octet-stream";
    }
}
