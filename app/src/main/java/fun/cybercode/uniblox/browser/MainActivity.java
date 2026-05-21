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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {

    private FrameLayout webViewContainer;
    private EditText editUrl;
    private ProgressBar progressBar;
    private ImageButton btnBookmark;
    private DrawerLayout drawerLayout;
    
    private BookmarkViewModel bookmarkViewModel;
    private TabViewModel tabViewModel;
    
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
        drawerLayout = findViewById(R.id.drawer_layout);

        bookmarkViewModel = new ViewModelProvider(this).get(BookmarkViewModel.class);
        tabViewModel = new ViewModelProvider(this).get(TabViewModel.class);

        setupAddressBar();
        setupButtons();
        setupDrawers();
        observeTabs();
        startWatchdog();
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
        editUrl.setText(currentUrl);
        updateBookmarkIcon(currentUrl);
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

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
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
                }
                tabViewModel.updateActiveTabInfo(view.getTitle() != null ? view.getTitle() : "UNIBLOX Browser", url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
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
        findViewById(R.id.btn_menu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        
        View btnTabs = findViewById(R.id.btn_tabs);
        if (btnTabs != null) {
            btnTabs.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
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
        
        findViewById(R.id.btn_share).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, currentUrl);
            startActivity(Intent.createChooser(intent, "Share via"));
        });

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
        recyclerTabs.setLayoutManager(new LinearLayoutManager(this));
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
