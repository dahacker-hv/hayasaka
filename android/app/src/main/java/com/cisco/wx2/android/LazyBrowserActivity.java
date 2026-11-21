package com.cisco.wx2.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;

public class LazyBrowserActivity extends Activity {
    private WebView web;
    private EditText address;
    private TextView status;
    private boolean musicOpen = false;
    private final int bg = Color.rgb(7, 7, 7);
    private final int panel = Color.rgb(20, 20, 20);
    private final int accent = Color.rgb(210, 210, 210);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildUi();
        if (state == null) web.loadUrl("https://www.google.com");
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private Button makeButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(8, 0, 8, 0);
        b.setBackground(rounded(panel, 22));
        return b;
    }

    private LinearLayout page;
    private LinearLayout sidebarItems;
    private boolean sidebarExpanded = true;
    private final java.util.ArrayList<String> bookmarks = new java.util.ArrayList<>();

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(bg);
        root.setPadding(0, 8, 10, 8);
        setContentView(root);

        LinearLayout sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setGravity(Gravity.CENTER_HORIZONTAL);
        sidebar.setPadding(5, 12, 5, 12);
        GradientDrawable dockBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.rgb(10, 11, 10), Color.rgb(18, 21, 17), Color.rgb(8, 9, 8)});
        dockBg.setCornerRadius(42);
        dockBg.setStroke(1, Color.rgb(72, 88, 53));
        sidebar.setBackground(dockBg);
        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(76, 470);
        dockParams.gravity = Gravity.CENTER_VERTICAL;
        dockParams.setMargins(0, 0, 4, 0);
        root.addView(sidebar, dockParams);

        TextView dockBrand = new TextView(this);
        dockBrand.setText("L");
        dockBrand.setTextColor(Color.rgb(217, 255, 98));
        dockBrand.setTextSize(24);
        dockBrand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        dockBrand.setGravity(Gravity.CENTER);
        sidebar.addView(dockBrand, new LinearLayout.LayoutParams(-1, 48));

        ScrollView dockScroll = new ScrollView(this);
        dockScroll.setFillViewport(false);
        sidebar.addView(dockScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        sidebarItems = new LinearLayout(this);
        sidebarItems.setOrientation(LinearLayout.VERTICAL);
        dockScroll.addView(sidebarItems);
        addDockItem("⌂", "Home", v -> { musicOpen = false; web.loadUrl("https://www.google.com"); });
        addDockItem("◷", "History", v -> showHistory());
        addDockItem("↓", "Downloads", v -> {
            try { startActivity(new android.content.Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)); }
            catch (Exception e) { Toast.makeText(this, "Downloads are not available on this device", Toast.LENGTH_SHORT).show(); }
        });
        addDockItem("☆", "Bookmark", v -> addBookmark());
        addDockItem("♫", "Music", v -> openMusic());
        addDockItem("⌘", "Extensions", v -> Toast.makeText(this, "Extensions support is coming soon", Toast.LENGTH_SHORT).show());
        addDockItem("⚙", "Settings", v -> showSettings());
        addDockItem("◐", "Theme", v -> Toast.makeText(this, "Matte black theme is active", Toast.LENGTH_SHORT).show());
        addDockItem("?", "About", v -> new android.app.AlertDialog.Builder(this)
            .setTitle("LAZY BROWSER").setMessage("Version 1.1\nA browser built for your space.")
            .setPositiveButton("OK", null).show());

        Button collapse = makeButton("‹");
        collapse.setTextColor(Color.rgb(217, 255, 98));
        sidebar.addView(collapse, new LinearLayout.LayoutParams(-1, 46));
        collapse.setOnClickListener(v -> {
            sidebarExpanded = !sidebarExpanded;
            android.view.ViewGroup.LayoutParams lp = sidebar.getLayoutParams();
            lp.width = sidebarExpanded ? 76 : 48;
            sidebar.setLayoutParams(lp);
            collapse.setText(sidebarExpanded ? "‹" : "›");
            for (int i = 0; i < sidebarItems.getChildCount(); i++) {
                View item = sidebarItems.getChildAt(i);
                if (item instanceof LinearLayout) {
                    LinearLayout row = (LinearLayout) item;
                    if (row.getChildCount() > 1) row.getChildAt(1).setVisibility(sidebarExpanded ? View.VISIBLE : View.GONE);
                }
            }
        });

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(12, 5, 0, 0);
        root.addView(page, new LinearLayout.LayoutParams(0, -1, 1));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = new TextView(this);
        brand.setText("LAZY");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(22);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView tag = new TextView(this);
        tag.setText("  /  BROWSER");
        tag.setTextColor(Color.rgb(217, 255, 98));
        tag.setTextSize(11);
        tag.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        Button menu = makeButton("⋮");
        menu.setTextSize(23);
        brandRow.addView(brand);
        brandRow.addView(tag, new LinearLayout.LayoutParams(0, 42, 1));
        brandRow.addView(menu, new LinearLayout.LayoutParams(48, 42));
        page.addView(brandRow, new LinearLayout.LayoutParams(-1, 42));
        menu.setOnClickListener(this::showBrowserMenu);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(14, 3, 6, 3);
        searchRow.setBackground(rounded(panel, 24));
        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.GRAY);
        address.setHint("Search or enter address…");
        address.setTextSize(15);
        address.setSelectAllOnFocus(true);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setBackgroundColor(Color.TRANSPARENT);
        searchRow.addView(address, new LinearLayout.LayoutParams(0, 48, 1));
        Button go = makeButton("GO");
        go.setTextColor(Color.BLACK);
        go.setBackground(rounded(Color.rgb(217, 255, 98), 18));
        searchRow.addView(go, new LinearLayout.LayoutParams(58, 42));
        page.addView(searchRow, new LinearLayout.LayoutParams(-1, 54));

        status = new TextView(this);
        status.setText("● READY");
        status.setTextColor(Color.rgb(145, 160, 125));
        status.setTextSize(9);
        status.setPadding(5, 7, 0, 6);
        page.addView(status);

        web = new WebView(this);
        web.setBackgroundColor(bg);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setLoadsImagesAutomatically(true);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int progress) {
                status.setText(progress >= 100 ? "● READY" : "● LOADING  " + progress + "%");
            }
            @Override public void onReceivedTitle(WebView view, String title) {
                if (title != null && !musicOpen) status.setText(title);
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return false; }
            @Override public void onPageFinished(WebView view, String url) {
                if (!musicOpen) address.setText(url);
            }
        });
        page.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

        go.setOnClickListener(v -> openAddress());
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                openAddress(); return true;
            }
            return false;
        });
    }

    private void addDockItem(String icon, String label, View.OnClickListener action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(2, 8, 2, 8);
        TextView glyph = new TextView(this);
        glyph.setText(icon);
        glyph.setTextColor(Color.WHITE);
        glyph.setTextSize(23);
        glyph.setGravity(Gravity.CENTER);
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextColor(Color.rgb(190, 195, 186));
        caption.setTextSize(9);
        caption.setGravity(Gravity.CENTER);
        item.addView(glyph, new LinearLayout.LayoutParams(-1, 30));
        item.addView(caption, new LinearLayout.LayoutParams(-1, 19));
        item.setOnClickListener(action);
        sidebarItems.addView(item, new LinearLayout.LayoutParams(-1, 58));
    }

    private void showHistory() {
        if (web == null || web.copyBackForwardList().getSize() == 0) {
            Toast.makeText(this, "No browsing history yet", Toast.LENGTH_SHORT).show();
            return;
        }
        android.webkit.WebBackForwardList list = web.copyBackForwardList();
        java.util.ArrayList<String> titles = new java.util.ArrayList<>();
        java.util.ArrayList<String> urls = new java.util.ArrayList<>();
        for (int i = list.getSize() - 1; i >= 0; i--) {
            android.webkit.WebHistoryItem item = list.getItemAtIndex(i);
            titles.add(item.getTitle() == null ? item.getUrl() : item.getTitle());
            urls.add(item.getUrl());
        }
        new android.app.AlertDialog.Builder(this).setTitle("History")
            .setItems(titles.toArray(new String[0]), (d, which) -> { musicOpen = false; web.loadUrl(urls.get(which)); })
            .setNegativeButton("Close", null).show();
    }

    private void addBookmark() {
        String url = web == null ? "" : web.getUrl();
        if (url == null || url.isEmpty()) return;
        if (!bookmarks.contains(url)) bookmarks.add(url);
        new android.app.AlertDialog.Builder(this).setTitle("Bookmarks")
            .setItems(bookmarks.toArray(new String[0]), (d, which) -> { musicOpen = false; web.loadUrl(bookmarks.get(which)); })
            .setPositiveButton("Done", null).show();
    }

    private void showSettings() {
        String[] options = {"JavaScript: enabled", "Desktop site: use browser menu", "Clear browsing data"};
        new android.app.AlertDialog.Builder(this).setTitle("Settings")
            .setItems(options, (d, which) -> {
                if (which == 2) new android.app.AlertDialog.Builder(this).setTitle("Clear browsing data?")
                    .setMessage("Clear cached web data and cookies?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear", (dialog, w) -> {
                        web.clearCache(true);
                        android.webkit.CookieManager.getInstance().removeAllCookies(null);
                        Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show();
                    }).show();
            }).setNegativeButton("Close", null).show();
    }

    private void showBrowserMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Music Player");
        popup.getMenu().add("Hayasaka");
        popup.getMenu().add("Home");
        popup.getMenu().add("Reload");
        popup.getMenu().add("Back");
        popup.getMenu().add("Forward");
        popup.getMenu().add("Downloads");
        popup.getMenu().add("Bookmarks");
        popup.getMenu().add("History");
        popup.getMenu().add("Settings");
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            switch (title) {
                case "Music Player": openMusic(); return true;
                case "Hayasaka": startActivity(new android.content.Intent(this, HayasakaActivity.class)); return true;
                case "Home": musicOpen = false; web.loadUrl("https://www.google.com"); return true;
                case "Reload": if (!musicOpen) web.reload(); return true;
                case "Back": onBackPressed(); return true;
                case "Forward": if (!musicOpen && web.canGoForward()) web.goForward(); return true;
                case "Downloads":
                    try { startActivity(new android.content.Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)); }
                    catch (Exception e) { Toast.makeText(this, "Downloads are not available on this device", Toast.LENGTH_SHORT).show(); }
                    return true;
                case "Bookmarks": addBookmark(); return true;
                case "History": showHistory(); return true;
                case "Settings": showSettings(); return true;
            }
            return false;
        });
        popup.show();
    }

    private void openMusic() {
        musicOpen = true;
        address.setText("LAZY Music • Audius");
        status.setText("MUSIC • CONNECTING TO AUDIUS");
        web.loadDataWithBaseURL("https://audius.co/", musicHtml(), "text/html", "UTF-8", null);
    }

    private String musicHtml() {
        return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><style>"
            + "*{box-sizing:border-box}body{margin:0;background:#080909;color:#f5f6f4;font:14px Arial,sans-serif;padding:22px 18px 150px}"
            + ".top{display:flex;justify-content:space-between;align-items:center;margin-bottom:26px}.brand{font-size:11px;letter-spacing:3px;font-weight:800;color:#d9ff62}.live{font-size:10px;color:#a4a99a;letter-spacing:1.5px}"
            + ".eyebrow{font-size:10px;letter-spacing:2.5px;color:#a4a99a;text-transform:uppercase}.headline{font-size:34px;line-height:1.08;letter-spacing:-1.5px;font-weight:800;margin:10px 0 8px}.headline span{color:#d9ff62}.sub{color:#888f85;font-size:13px;line-height:1.5;margin-bottom:22px}"
            + ".search{display:flex;gap:8px;background:#151816;border:1px solid #292f29;border-radius:16px;padding:6px;margin-bottom:25px}.search input{min-width:0;flex:1;background:transparent;border:0;outline:0;color:white;padding:11px;font-size:14px}.search button{background:#d9ff62;color:#111;border:0;border-radius:11px;padding:0 16px;font-weight:800}"
            + ".section{display:flex;justify-content:space-between;align-items:center;margin:25px 0 13px}.section b{font-size:17px}.section small{color:#a9c45d;font-size:10px;letter-spacing:1.5px}"
            + ".feature{position:relative;overflow:hidden;border:1px solid #343b2c;border-radius:22px;padding:18px;background:radial-gradient(ellipse at 85% 15%,#45532a 0,#20271a 34%,#141814 72%);min-height:150px}.feature:after{content:'♫';position:absolute;right:18px;top:2px;font-size:110px;font-weight:900;color:rgba(217,255,98,.10)}.feature .eyebrow{color:#d9ff62}.feature h2{font-size:23px;margin:12px 0 6px;max-width:230px}.feature p{margin:0;color:#aeb5a5;font-size:12px}.feature button{margin-top:17px;background:#d9ff62;color:#111;border:0;border-radius:12px;padding:11px 17px;font-weight:800}"
            + ".track{display:flex;align-items:center;gap:12px;padding:11px 0;border-bottom:1px solid #202420}.cover{width:54px;height:54px;flex:none;border-radius:12px;object-fit:cover;background:#20251e}.meta{min-width:0;flex:1}.name{font-size:14px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.artist{font-size:12px;color:#858d82;margin-top:5px}.play{width:38px;height:38px;border-radius:50%;border:1px solid #3b4434;background:#1b2118;color:#d9ff62;font-size:15px}"
            + ".empty{padding:22px 8px;color:#8e968b;text-align:center;line-height:1.5}.msg{font-size:11px;color:#9da694;margin:12px 2px;min-height:16px}"
            + ".dock{position:fixed;bottom:0;left:0;right:0;padding:12px 16px 18px;background:linear-gradient(180deg,rgba(8,9,9,.2),#080909 22%)}.now{display:flex;align-items:center;gap:11px;padding:11px 12px;border:1px solid #333b2d;border-radius:17px;background:#171b16;box-shadow:0 10px 35px #000}.now img{width:42px;height:42px;border-radius:9px;object-fit:cover;background:#252b21}.nowmeta{min-width:0;flex:1}.nowtitle{font-size:12px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.nowartist{font-size:10px;color:#929b8c;margin-top:4px}.now button{background:transparent;border:0;color:#d9ff62;font-size:22px;padding:4px 8px}audio{display:none}"
            + "</style></head><body><div class='top'><div class='brand'>LAZY / SOUND</div><div class='live'>● AUDIO DISCOVERY</div></div>"
            + "<div class='eyebrow'>Your sound. Your space.</div><div class='headline'>Find your<br><span>next favourite.</span></div>"
            + "<div class='sub'>A world of independent music, picked for the moment.</div>"
            + "<div class='search'><input id='q' placeholder='Tracks, artists, sounds…'><button onclick='search()'>SEARCH</button></div>"
            + "<div class='feature'><div class='eyebrow'>THE DAILY MIX</div><h2>Fresh sounds.<br>Zero limits.</h2><p>Explore what listeners are playing now.</p><button onclick='loadTrending()'>EXPLORE TRENDING ↗</button></div>"
            + "<div class='section'><b id='listTitle'>Trending now</b><small>AUDIUS PICKS</small></div><div id='list'></div><div id='msg' class='msg'>Connecting to Audius…</div>"
            + "<div class='dock'><div class='now'><img id='nowImg' src='' alt=''><div class='nowmeta'><div id='nowTitle' class='nowtitle'>Nothing playing yet</div><div id='nowArtist' class='nowartist'>Choose a track to begin</div></div><button id='toggle' onclick='togglePlay()'>▶</button></div><audio id='player' controls></audio></div>"
            + "<script>const api='https://api.audius.co',app='LAZY_BROWSER';let current=null;async function request(path){let r=await fetch(api+'/v1'+path+(path.includes('?')?'&':'?')+'app_name='+app);if(!r.ok)throw Error('Provider unavailable');return(await r.json()).data}"
            + "function esc(s){return String(s||'').replace(/[&<>\\\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\\\"':'&quot;',\\\"'\\\":'&#39;'}[c]))}"
            + "function draw(items){let l=document.getElementById('list');l.innerHTML='';if(!items||!items.length){l.innerHTML='<div class=\\\"empty\\\">No tracks found. Try another search.</div>';return}items.forEach(t=>{let d=document.createElement('div');d.className='track';let img=t.artwork?.['150x150']||'';d.innerHTML='<img class=\\\"cover\\\" src=\\\"'+esc(img)+'\\\"><div class=\\\"meta\\\"><div class=\\\"name\\\">'+esc(t.title)+'</div><div class=\\\"artist\\\">'+esc(t.user?.name)+'</div></div><button class=\\\"play\\\">▶</button>';d.querySelector('button').onclick=()=>play(t);l.appendChild(d)})}"
            + "function play(t){current=t;let p=document.getElementById('player');document.getElementById('nowTitle').textContent=t.title||'Unknown track';document.getElementById('nowArtist').textContent=t.user?.name||'Audius artist';document.getElementById('nowImg').src=t.artwork?.['150x150']||'';p.src=api+'/v1/tracks/'+encodeURIComponent(t.id)+'/stream?app_name='+app;p.play().then(()=>document.getElementById('toggle').textContent='Ⅱ').catch(()=>{document.getElementById('msg').textContent='Tap play to start this track';document.getElementById('toggle').textContent='▶'})}"
            + "function togglePlay(){let p=document.getElementById('player');if(!p.src)return;if(p.paused){p.play();document.getElementById('toggle').textContent='Ⅱ'}else{p.pause();document.getElementById('toggle').textContent='▶'}}"
            + "async function loadTrending(){document.getElementById('listTitle').textContent='Trending now';document.getElementById('msg').textContent='Loading fresh tracks…';try{draw(await request('/tracks/trending?limit=25'));document.getElementById('msg').textContent='Updated from Audius'}catch(e){document.getElementById('msg').textContent='Audius is unavailable right now. Please retry.'}}"
            + "async function search(){let q=document.getElementById('q').value.trim();if(!q)return;document.getElementById('listTitle').textContent='Search results';document.getElementById('msg').textContent='Searching Audius…';try{draw(await request('/tracks/search?query='+encodeURIComponent(q)+'&limit=25'));document.getElementById('msg').textContent='Results from Audius'}catch(e){document.getElementById('msg').textContent='Search unavailable. Check your connection and retry.'}}"
            + "document.getElementById('q').addEventListener('keydown',e=>{if(e.key==='Enter')search()});document.getElementById('player').addEventListener('play',()=>document.getElementById('toggle').textContent='Ⅱ');document.getElementById('player').addEventListener('pause',()=>document.getElementById('toggle').textContent='▶');loadTrending();</script></body></html>";
    }
    private void showMusicExit() {
        musicOpen = false;
        if (web.canGoBack()) web.goBack(); else web.loadUrl("https://www.google.com");
    }

    private void openAddress() {
        musicOpen = false;
        String input = address.getText().toString().trim();
        if (input.isEmpty()) return;
        String url;
        if (input.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) url = input;
        else if (input.contains(" ") || !input.contains(".")) url = "https://www.google.com/search?q=" + Uri.encode(input);
        else url = "https://" + input;
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(address.getWindowToken(), 0);
        web.loadUrl(url);
    }

    @Override public void onBackPressed() {
        if (musicOpen) { musicOpen = false; web.loadUrl("https://www.google.com"); }
        else if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if (web != null) web.restoreState(state);
    }

    @Override protected void onDestroy() {
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }
}