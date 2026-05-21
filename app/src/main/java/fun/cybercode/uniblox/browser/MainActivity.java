package fun.cybercode.uniblox.browser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import fun.cybercode.uniblox.browser.util.AdBlocker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import fun.cybercode.uniblox.browser.data.AppDatabase;
import fun.cybercode.uniblox.browser.data.Bookmark;
import fun.cybercode.uniblox.browser.data.BookmarkAdapter;
import fun.cybercode.uniblox.browser.data.Tab;
import fun.cybercode.uniblox.browser.ui.BookmarkViewModel;
import fun.cybercode.uniblox.browser.ui.TabAdapter;
import fun.cybercode.uniblox.browser.ui.TabViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Build;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity {

    private FrameLayout webViewContainer;
    private EditText editUrl;
    private ProgressBar progressBar;
    private ImageButton btnBookmark;
    private ImageView imgSslLock;
    private android.widget.TextView txtTabCount;
    private com.google.android.material.switchmaterial.SwitchMaterial switchDesktopMode;
    private DrawerLayout drawerLayout;
    
    private BookmarkViewModel bookmarkViewModel;
    private TabViewModel tabViewModel;
    
    private android.webkit.ValueCallback<android.net.Uri[]> filePathCallback;
    private final androidx.activity.result.ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (filePathCallback != null) {
                    android.net.Uri[] results = null;
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        String dataString = result.getData().getDataString();
                        if (dataString != null) {
                            results = new android.net.Uri[]{android.net.Uri.parse(dataString)};
                        }
                    }
                    filePathCallback.onReceiveValue(results);
                    filePathCallback = null;
                }
            }
    );
    
    private final Map<String, WebView> webViewMap = new HashMap<>();
    private WebView currentWebView;
    private String currentUrl = "file:///android_asset/home.html";
    private String currentTitle = "UNIBLOX Browser";

    private final Handler watchdogHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isMainThreadResponsive = true;
    private Thread watchdogThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!CertificateValidator.validateCertificate(this)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        webViewContainer = findViewById(R.id.webview_container);
        editUrl = findViewById(R.id.edit_url);
        progressBar = findViewById(R.id.progress_bar);
        btnBookmark = findViewById(R.id.btn_bookmark);
        imgSslLock = findViewById(R.id.img_ssl_lock);
        txtTabCount = findViewById(R.id.txt_tab_count);
        switchDesktopMode = findViewById(R.id.switch_desktop_mode);
        drawerLayout = findViewById(R.id.drawer_layout);

        bookmarkViewModel = new ViewModelProvider(this).get(BookmarkViewModel.class);
        tabViewModel = new ViewModelProvider(this).get(TabViewModel.class);

        setupAddressBar();
        setupButtons();
        setupDrawers();
        observeTabs();
        startWatchdog();
        checkEmulatorStatus();
    }

    private void checkEmulatorStatus() {
        if (isEmulator()) {
            View installBtn = findViewById(R.id.btn_install_app);
            if (installBtn != null) {
                installBtn.setVisibility(View.VISIBLE);
                installBtn.setOnClickListener(v -> showInstallDialog());
            }
        }
    }

    private boolean isEmulator() {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT));
    }

    private void showInstallDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Install UNIBLOX Browser")
            .setMessage("To install this app on your physical device:\n\n" +
                        "1. Open the 'Settings' menu (gear icon) in the top-right of the AI Studio Build interface.\n" +
                        "2. Select 'Generate APK' or 'Download Project as ZIP'.\n" +
                        "3. If you generated an APK, scan the QR code or follow the link to download it to your phone.\n" +
                        "4. Enable 'Install from Unknown Sources' in your phone's settings if prompted.")
            .setPositiveButton("Got it", null)
            .show();
    }

    private void startWatchdog() {
        watchdogThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                isMainThreadResponsive = false;
                watchdogHandler.post(() -> isMainThreadResponsive = true);

                try {
                    // Check every 20 seconds if the main thread has responded
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    break;
                }

                if (!isMainThreadResponsive) {
                    Log.e("UNIBLOX Browser", "Main thread frozen for 20s. Force closing.");
                    Process.killProcess(Process.myPid());
                    System.exit(10);
                }
            }
        }, "WatchdogThread");
        watchdogThread.start();
    }

    private void observeTabs() {
        tabViewModel.getActiveTab().observe(this, tab -> {
            if (tab != null) {
                switchTab(tab);
            }
        });

        tabViewModel.getTabs().observe(this, tabs -> {
            if (txtTabCount != null) {
                txtTabCount.setText(String.valueOf(tabs.size()));
            }
            // Cleanup orphaned WebViews
            Map<String, WebView> toRemove = new HashMap<>();
            for (String id : webViewMap.keySet()) {
                boolean found = false;
                for (Tab t : tabs) {
                    if (t.getId().equals(id)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    toRemove.put(id, webViewMap.get(id));
                }
            }
            for (Map.Entry<String, WebView> entry : toRemove.entrySet()) {
                WebView wv = entry.getValue();
                webViewContainer.removeView(wv);
                wv.destroy();
                webViewMap.remove(entry.getKey());
            }
        });
    }

    private void switchTab(Tab tab) {
        // Hide current
        if (currentWebView != null) {
            currentWebView.setVisibility(View.GONE);
        }

        // Get or create new
        WebView targetWebView = webViewMap.get(tab.getId());
        if (targetWebView == null) {
            targetWebView = createWebView(tab);
            webViewMap.put(tab.getId(), targetWebView);
            webViewContainer.addView(targetWebView);
            targetWebView.loadUrl(tab.getUrl());
        }

        currentWebView = targetWebView;
        currentWebView.setVisibility(View.VISIBLE);
        
        // Update UI
        currentUrl = currentWebView.getUrl() != null ? currentWebView.getUrl() : tab.getUrl();
        editUrl.setText(currentUrl.equals("file:///android_asset/home.html") ? "" : currentUrl);
        updateBookmarkIcon(currentUrl);
        updateSslIcon(currentUrl);
    }

    private void updateSslIcon(String url) {
        if (imgSslLock == null) return;
        
        runOnUiThread(() -> {
            if (url != null && url.startsWith("https://")) {
                imgSslLock.setVisibility(View.VISIBLE);
                imgSslLock.setImageResource(R.drawable.ic_lock);
                imgSslLock.setColorFilter(ContextCompat.getColor(this, R.color.chrome_blue));
            } else if (url != null && url.startsWith("file:///")) {
                imgSslLock.setVisibility(View.VISIBLE);
                imgSslLock.setImageResource(R.drawable.ic_lock);
                imgSslLock.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray));
            } else {
                imgSslLock.setVisibility(View.GONE);
            }
        });
    }

    private WebView createWebView(Tab tab) {
        WebView webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        
        // Ensure hardware acceleration is enabled for better WebGL performance
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(android.net.Uri.parse(url));
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open download link", Toast.LENGTH_SHORT).show();
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (AdBlocker.isAd(request.getUrl().toString())) {
                    return AdBlocker.createEmptyResource();
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (AdBlocker.isAd(url)) {
                    return true; // Block ad URLs from loading in the main frame
                }
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (view == currentWebView) {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    if (url.equals("file:///android_asset/home.html")) {
                        editUrl.setText("");
                        editUrl.setHint(R.string.address_bar_placeholder);
                    } else {
                        editUrl.setText(url);
                    }
                    currentUrl = url;
                    updateBookmarkIcon(url);
                    updateSslIcon(url);
                }
                tabViewModel.updateActiveTabInfo(view.getTitle() != null ? view.getTitle() : "UNIBLOX Browser", url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // AdBlocker JS Injection to hide common ad elements
                view.evaluateJavascript(
                    "(function() {" +
                    "   var css = 'iframe[src*=\"doubleclick.net\"], iframe[src*=\"googlesyndication.com\"], iframe[id*=\"google_ads\"], .ad-container, .adsbygoogle { display: none !important; }';" +
                    "   var style = document.createElement('style');" +
                    "   style.type = 'text/css';" +
                    "   style.appendChild(document.createTextNode(css));" +
                    "   document.head.appendChild(style);" +
                    "   " +
                    "   // Also remove gpt script tags if any were loaded before blocking" +
                    "   var scripts = document.getElementsByTagName('script');" +
                    "   for (var i = scripts.length - 1; i >= 0; i--) {" +
                    "       if (scripts[i].src && (scripts[i].src.indexOf('gpt.js') !== -1 || scripts[i].src.indexOf('googletagservices.com') !== -1)) {" +
                    "           scripts[i].parentNode.removeChild(scripts[i]);" +
                    "       }" +
                    "   }" +
                    "   " +
                    "   // Hide any remaining ad iframes" +
                    "   var iframes = document.getElementsByTagName('iframe');" +
                    "   for (var i = iframes.length - 1; i >= 0; i--) {" +
                    "       if (iframes[i].src && (iframes[i].src.indexOf('ads') !== -1 || iframes[i].src.indexOf('doubleclick') !== -1)) {" +
                    "           iframes[i].style.display = 'none';" +
                    "       }" +
                    "   }" +
                    "})();", null);

                if (view == currentWebView) {
                    progressBar.setVisibility(ProgressBar.GONE);
                    currentUrl = url;
                    currentTitle = (view.getTitle() == null || view.getTitle().isEmpty()) ? "UNIBLOX Browser" : view.getTitle();
                    if (url.equals("file:///android_asset/home.html")) {
                        editUrl.setText("");
                    }
                }
                tabViewModel.updateActiveTabInfo(currentTitle, url);
            }

            @Override
            public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                // This prevents the "Aw Snap" crash by handling the renderer failure
                if (view == currentWebView) {
                    currentWebView = null;
                }
                
                // Find which tab this WebView belonged to and remove it from our tracking
                String tabIdToRemove = null;
                for (Map.Entry<String, WebView> entry : webViewMap.entrySet()) {
                    if (entry.getValue() == view) {
                        tabIdToRemove = entry.getKey();
                        break;
                    }
                }
                
                if (tabIdToRemove != null) {
                    webViewMap.remove(tabIdToRemove);
                }
                
                webViewContainer.removeView(view);
                view.destroy();
                
                // If the crashed tab was the active one, refresh it to recover
                Tab active = tabViewModel.getActiveTab().getValue();
                if (active != null && active.getId().equals(tabIdToRemove)) {
                    switchTab(active);
                }
                
                return true; // Return true to indicate we've handled the crash
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            private View customView;
            private WebChromeClient.CustomViewCallback customViewCallback;

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                if (customView != null) {
                    onHideCustomView();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                webViewContainer.addView(customView);
                webViewContainer.setVisibility(View.VISIBLE);
                // Hide other UI elements if necessary
            }

            @Override
            public void onHideCustomView() {
                webViewContainer.removeView(customView);
                customView = null;
                customViewCallback.onCustomViewHidden();
            }

            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
                request.grant(request.getResources());
            }

            @Override
            public boolean onShowFileChooser(WebView webView, android.webkit.ValueCallback<android.net.Uri[]> filePathCallback, android.webkit.WebChromeClient.FileChooserParams fileChooserParams) {
                if (MainActivity.this.filePathCallback != null) {
                    MainActivity.this.filePathCallback.onReceiveValue(null);
                }
                MainActivity.this.filePathCallback = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    fileChooserLauncher.launch(intent);
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                tabViewModel.addNewTab("New Tab", "about:blank");
                
                // We'll let the ViewModel observer handle the actual WebView creation and attachment.
                // For now, we return true to indicate we've handled the window creation.
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view == currentWebView) {
                    progressBar.setProgress(newProgress);
                    if (newProgress == 100) {
                        progressBar.setVisibility(ProgressBar.GONE);
                    } else {
                        progressBar.setVisibility(ProgressBar.VISIBLE);
                    }
                }
            }
        });

        return webView;
    }

    private void setupAddressBar() {
        editUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadFormattedUrl(editUrl.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void loadFormattedUrl(String input) {
        String url;
        if (input == null || input.trim().isEmpty() || input.equalsIgnoreCase("home")) {
            url = "file:///android_asset/home.html";
        } else if (input.startsWith("http://") || input.startsWith("https://")) {
            url = input;
        } else if (input.contains(".")) {
            url = "https://" + input;
        } else {
            url = "https://www.google.com/search?q=" + input;
        }
        if (currentWebView != null) {
            currentWebView.loadUrl(url);
        }
    }

    private void setupButtons() {
        ImageButton btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }
        
        View btnTabs = findViewById(R.id.btn_tabs);
        if (btnTabs != null) {
            btnTabs.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    drawerLayout.openDrawer(GravityCompat.END);
                }
            });
        }
        
        findViewById(R.id.btn_home).setOnClickListener(v -> {
            if (currentWebView != null) currentWebView.loadUrl("file:///android_asset/home.html");
        });
        
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (currentWebView != null && currentWebView.canGoBack()) currentWebView.goBack();
        });
        
        findViewById(R.id.btn_forward).setOnClickListener(v -> {
            if (currentWebView != null && currentWebView.canGoForward()) currentWebView.goForward();
        });
        
        findViewById(R.id.btn_refresh).setOnClickListener(v -> {
            if (currentWebView != null) currentWebView.reload();
        });
        
        // Share functionality can be moved to a menu later

        if (switchDesktopMode != null) {
            switchDesktopMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (currentWebView != null) {
                    setDesktopMode(currentWebView, isChecked);
                    currentWebView.reload();
                }
            });
        }

        btnBookmark.setOnClickListener(v -> {
            bookmarkViewModel.toggleBookmark(currentTitle, currentUrl);
        });

        findViewById(R.id.btn_add_tab).setOnClickListener(v -> {
            tabViewModel.addNewTab("UNIBLOX Browser", "file:///android_asset/home.html");
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            }
        });
    }

    private void setupDrawers() {
        // Bookmarks
        RecyclerView recyclerBookmarks = findViewById(R.id.recycler_bookmarks);
        recyclerBookmarks.setLayoutManager(new LinearLayoutManager(this));
        BookmarkAdapter bookmarkAdapter = new BookmarkAdapter(bookmark -> {
            if (currentWebView != null) currentWebView.loadUrl(bookmark.getUrl());
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        recyclerBookmarks.setAdapter(bookmarkAdapter);

        bookmarkViewModel.getAllBookmarks().observe(this, bookmarks -> {
            bookmarkAdapter.submitList(bookmarks);
            updateBookmarkIcon(currentUrl);
        });

        // Tabs
        RecyclerView recyclerTabs = findViewById(R.id.recycler_tabs);
        boolean isHorizontalTabs = findViewById(R.id.nav_view_tabs) == null;
        
        if (isHorizontalTabs) {
            recyclerTabs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        } else {
            recyclerTabs.setLayoutManager(new LinearLayoutManager(this));
        }
        
        recyclerTabs.setHasFixedSize(true);
        
        TabAdapter tabAdapter = new TabAdapter(new TabAdapter.OnTabInteractionListener() {
            @Override
            public void onTabClick(Tab tab) {
                tabViewModel.setActiveTab(tab);
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }
            }

            @Override
            public void onTabClose(Tab tab) {
                tabViewModel.closeTab(tab);
            }
        });
        recyclerTabs.setAdapter(tabAdapter);

        tabViewModel.getTabs().observe(this, tabAdapter::submitList);
    }

    private void setDesktopMode(WebView webView, boolean enabled) {
        String newUserAgent = enabled ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" : null;
        webView.getSettings().setUserAgentString(newUserAgent);
        webView.getSettings().setUseWideViewPort(enabled);
        webView.getSettings().setLoadWithOverviewMode(enabled);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
    }

    private void updateBookmarkIcon(String url) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Bookmark b = AppDatabase.getDatabase(this).bookmarkDao().getBookmarkByUrl(url);
            runOnUiThread(() -> {
                if (b != null) {
                    btnBookmark.setImageResource(R.drawable.ic_star_filled_primary);
                } else {
                    btnBookmark.setImageResource(R.drawable.ic_star_outline_grey);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (watchdogThread != null) {
            watchdogThread.interrupt();
        }
        for (WebView wv : webViewMap.values()) {
            webViewContainer.removeView(wv);
            wv.destroy();
        }
        webViewMap.clear();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (findViewById(R.id.nav_view_tabs) != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
