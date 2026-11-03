package app.phonedash;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Settings: Notion key, city, add the widget, refresh, updates. */
public class MainActivity extends Activity {
    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private LinearLayout col;
    private TextView notionStatus, widgetStatus;
    private EditText key, city;
    private UpdateFlow updates;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        col.setPadding(pad, pad, pad, pad);
        sv.addView(col);
        setContentView(sv);

        TextView title = text("PhoneDash", 28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(title);
        col.addView(dim("Your daily brief as a home-screen widget. v" + Updater.installedVersionName(this)));

        TextView banner = text("", 15);
        banner.setBackgroundColor(0xFF1A5FCC);
        banner.setPadding(dp(12), dp(10), dp(12), dp(10));
        banner.setVisibility(View.GONE);
        col.addView(banner);

        // Notion
        heading("Daily brief (Notion)");
        notionStatus = dim("");
        col.addView(notionStatus);
        key = new EditText(this);
        key.setHint("Paste your Notion integration key");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setSingleLine(true);
        col.addView(key);
        button("Connect", v -> connect());
        col.addView(dim("The key stays on this phone only. In Notion, the \"Armada Daily Brief\" database "
                + "must be shared with the integration (··· → Connections)."));

        // City
        heading("Weather");
        city = new EditText(this);
        city.setHint("City, e.g. Belgrade (empty = automatic)");
        city.setSingleLine(true);
        city.setText(Prefs.homeCity(this));
        col.addView(city);
        button("Save city", v -> {
            Prefs.setHomeCity(this, city.getText().toString());
            Prefs.raw(this).edit().remove("home_auto_lat").apply();
            RefreshJob.now(this);
            toast("Saved. The widget will refresh.");
        });

        // Widget
        heading("Widget");
        widgetStatus = dim("");
        col.addView(widgetStatus);
        button("Add widget to home screen", v -> pin());
        button("Refresh now", v -> {
            RefreshJob.now(this);
            toast("Refreshing…");
            main.postDelayed(this::showStatus, 4000);
        });
        col.addView(dim("To fill a whole page: long-press the widget, then drag its edges to the "
                + "corners of the screen. Tap an item to tick it off (it syncs to Notion). "
                + "↻ refreshes, ⚙ opens this screen. It also refreshes by itself about every 30 minutes."));

        // Updates
        heading("Updates");
        Button up = button("Check for updates", null);
        updates = new UpdateFlow(this, bg, banner, null, up);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showStatus();
        updates.autoCheck();
        RefreshJob.schedulePeriodic(this);
    }

    @Override
    protected void onDestroy() {
        bg.shutdownNow();
        super.onDestroy();
    }

    private void showStatus() {
        if (Notion.configured(this)) {
            notionStatus.setText("✓ Connected. Key saved on this phone. Paste a new key to replace it.");
        } else {
            notionStatus.setText("Not connected yet.");
        }
        int n = DashWidget.ids(this).length;
        long ok = Prefs.lastOk(this);
        String err = Prefs.lastError(this);
        widgetStatus.setText((n == 0 ? "No widget on the home screen yet." : n + " widget(s) on the home screen.")
                + (ok == 0 ? "" : "\nLast refresh: "
                        + new SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(new Date(ok)))
                + (err.isEmpty() ? "" : "\nProblem: " + err)
                + (Weather.lastError.isEmpty() ? "" : "\nWeather: " + Weather.lastError));
    }

    private void connect() {
        final String token = key.getText().toString().trim();
        if (token.isEmpty()) { toast("Paste the key first"); return; }
        notionStatus.setText("Connecting…");
        bg.execute(() -> {
            String msg;
            boolean ok = false;
            try {
                Notion.ConnectResult r = Notion.connect(token);
                Prefs.setNotionToken(this, token);
                Notion.saveConnection(this, r);
                ok = true;
                msg = (r.created ? "Created \"" + r.dbTitle + "\" in " + r.parentTitle : "Connected to \"" + r.dbTitle + "\"")
                        + (r.addedDone ? " (added a Done column)" : "");
            } catch (Exception e) {
                msg = e.getMessage();
            }
            final String m = msg;
            final boolean fOk = ok;
            main.post(() -> {
                if (isDestroyed()) return;
                if (fOk) {
                    key.setText("");
                    Prefs.setLastError(this, "");
                    RefreshJob.now(this);
                }
                showStatus();
                toast(m);
            });
        });
    }

    private void pin() {
        AppWidgetManager m = AppWidgetManager.getInstance(this);
        if (m.isRequestPinAppWidgetSupported()) {
            m.requestPinAppWidget(new ComponentName(this, DashWidget.class), null, null);
        } else {
            toast("Your launcher can't add it from here: long-press the home screen → Widgets → PhoneDash");
        }
    }

    // ------------------------------------------------------------------ tiny UI helpers

    private void heading(String s) {
        TextView t = text(s, 18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(24), 0, dp(4));
        col.addView(t);
    }

    private TextView text(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        return t;
    }

    private TextView dim(String s) {
        TextView t = text(s, 14);
        t.setTextColor(Color.GRAY);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    private Button button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        if (l != null) b.setOnClickListener(l);
        col.addView(b, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return b;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
