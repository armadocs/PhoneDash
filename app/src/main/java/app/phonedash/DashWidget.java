package app.phonedash;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** The full-page home-screen widget: clock, date, weather and the daily brief. */
public class DashWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        RefreshJob.schedulePeriodic(c);
        render(c);
        if (System.currentTimeMillis() - Prefs.lastOk(c) > 10 * 60 * 1000) RefreshJob.now(c);
    }

    @Override
    public void onEnabled(Context c) {
        RefreshJob.schedulePeriodic(c);
        RefreshJob.now(c);
    }

    @Override
    public void onDisabled(Context c) {
        RefreshJob.cancelAll(c);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle opts) {
        render(c);
    }

    // ------------------------------------------------------------------ drawing

    static int[] ids(Context c) {
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, DashWidget.class));
    }

    /** Redraws every PhoneDash widget and reloads its list from the offline copy. */
    static void render(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = ids(c);
        for (int id : ids) m.updateAppWidget(id, build(c, id));
        if (ids.length > 0) m.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    private static RemoteViews build(Context c, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);

        // Weather
        Weather w = Weather.cached(c);
        if (w != null) {
            v.setTextViewText(R.id.weather, Weather.icon(w.code, w.isDay) + " " + w.temp + "°");
            v.setTextViewText(R.id.weather_sub, w.shortPlace() + " · " + w.todayMin + "° / " + w.todayMax + "°"
                    + (w.todayRain >= 20 ? " · ☂ " + w.todayRain + "%" : ""));
        } else {
            v.setTextViewText(R.id.weather, "");
            v.setTextViewText(R.id.weather_sub, Weather.lastError.isEmpty() ? "" : "weather: " + Weather.lastError);
        }

        // Status line
        v.setTextViewText(R.id.status, status(c));

        // List
        Intent svc = new Intent(c, DashListService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        v.setRemoteAdapter(R.id.list, svc);
        v.setEmptyView(R.id.list, R.id.empty);
        v.setTextViewText(R.id.empty, !Notion.configured(c)
                ? "Tap here to connect Notion"
                : Brief.cached(c) == null ? "Loading the daily brief…" : "All clear for today 🎉");

        Intent tick = new Intent(c, ActionReceiver.class).setAction(ActionReceiver.TOGGLE);
        v.setPendingIntentTemplate(R.id.list, PendingIntent.getBroadcast(c, 10, tick,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));

        // Buttons
        PendingIntent app = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.settings, app);
        v.setOnClickPendingIntent(R.id.status, app);
        v.setOnClickPendingIntent(R.id.empty, app);
        v.setOnClickPendingIntent(R.id.weather_box, app);
        v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(c, 2,
                new Intent(c, ActionReceiver.class).setAction(ActionReceiver.REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        v.setOnClickPendingIntent(R.id.clock_box, PendingIntent.getActivity(c, 3,
                new Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        v.setViewVisibility(R.id.refresh, RefreshJob.running ? View.INVISIBLE : View.VISIBLE);
        return v;
    }

    private static String status(Context c) {
        String upd = Prefs.updateAvailable(c);
        if (!upd.isEmpty()) {
            String[] p = upd.split("\\|", 2);
            try {
                if (Integer.parseInt(p[0]) > Updater.installedVersionCode(c) && p.length > 1)
                    return "⬆ Update v" + p[1] + " available · tap to install";
            } catch (NumberFormatException ignored) {
            }
        }
        if (RefreshJob.running) return "Refreshing…";
        Brief b = Brief.cached(c);
        String count = "";
        if (b != null) {
            int open = 0, done = 0;
            for (Brief.Section s : b.sections) {
                if (!"tasks".equals(s.kind)) continue;
                for (Brief.Item it : s.items) { if (it.done) done++; else open++; }
            }
            count = open + " to do" + (done > 0 ? " · " + done + " done today" : "") + " · ";
        }
        String err = Prefs.lastError(c);
        if (!err.isEmpty()) return "⚠ " + err;
        long ok = Prefs.lastOk(c);
        if (ok == 0) return count + "Not updated yet";
        return count + "updated " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ok));
    }
}
