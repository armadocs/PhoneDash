package app.phonedash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Taps from the widget: refresh, and tick/untick an item (written back to Notion). Not exported. */
public class ActionReceiver extends BroadcastReceiver {
    static final String REFRESH = "app.phonedash.REFRESH";
    static final String TOGGLE = "app.phonedash.TOGGLE";
    static final String EXTRA_PAGE = "page";
    static final String EXTRA_DONE = "done";

    @Override
    public void onReceive(Context ctx, Intent i) {
        final Context c = ctx.getApplicationContext();
        if (REFRESH.equals(i.getAction())) {
            RefreshJob.now(c);
            return;
        }
        if (!TOGGLE.equals(i.getAction())) return;
        final String page = i.getStringExtra(EXTRA_PAGE);
        if (page == null || page.isEmpty()) return;
        final boolean now = !i.getBooleanExtra(EXTRA_DONE, false);

        // Show the tick straight away, then tell Notion; undo if Notion says no.
        Brief.toggleCached(c, page, now);
        DashWidget.render(c);
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                Notion.setDone(c, page, now);
                Prefs.setLastError(c, "");
            } catch (Exception e) {
                Brief.toggleCached(c, page, !now);
                Prefs.setLastError(c, "Couldn't tick in Notion: " + e.getMessage());
            } finally {
                DashWidget.render(c);
                pr.finish();
            }
        }, "tick").start();
    }
}
