package app.phonedash;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Feeds the widget's scrolling list: section headers and items from the offline brief. */
public class DashListService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    private static final class Row {
        Brief.Section section;
        Brief.Item item; // null = section header
    }

    private static final class Factory implements RemoteViewsFactory {
        private final Context c;
        private final List<Row> rows = new ArrayList<>();

        Factory(Context c) { this.c = c; }

        @Override public void onCreate() {}
        @Override public void onDestroy() { rows.clear(); }

        @Override
        public void onDataSetChanged() {
            rows.clear();
            Brief b = Brief.cached(c);
            if (b == null) return;
            for (Brief.Section s : b.sections) {
                if (s.items.isEmpty()) continue;
                Row h = new Row();
                h.section = s;
                rows.add(h);
                for (Brief.Item it : s.items) {
                    Row r = new Row();
                    r.section = s;
                    r.item = it;
                    rows.add(r);
                }
            }
        }

        @Override public int getCount() { return rows.size(); }
        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 3; }
        @Override public long getItemId(int position) { return position; }
        @Override public boolean hasStableIds() { return false; }

        static boolean tickable(Brief.Section s) {
            return "tasks".equals(s.kind) || "later".equals(s.kind);
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= rows.size()) return null;
            Row r = rows.get(position);
            Brief.Section s = r.section;
            int accent = accent(s.kind);

            if (r.item == null) {
                RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.row_header);
                v.setTextViewText(R.id.h_title, s.title.toUpperCase(Locale.getDefault()));
                v.setTextColor(R.id.h_title, accent);
                int done = 0;
                for (Brief.Item it : s.items) if (it.done) done++;
                v.setTextViewText(R.id.h_count, tickable(s) && done > 0
                        ? done + " / " + s.items.size() : String.valueOf(s.items.size()));
                return v;
            }

            Brief.Item it = r.item;
            boolean tick = tickable(s);
            RemoteViews v = new RemoteViews(c.getPackageName(), tick ? R.layout.row_task : R.layout.row_info);
            CharSequence title = it.title;
            if (it.done) {
                SpannableString ss = new SpannableString(it.title);
                ss.setSpan(new StrikethroughSpan(), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                title = ss;
            }
            v.setTextViewText(R.id.r_title, title);
            v.setTextColor(R.id.r_title, it.done ? 0x80F2F5F8 : 0xFFF2F5F8);
            String detail = it.detail == null ? "" : it.detail.trim();
            v.setTextViewText(R.id.r_detail, detail);
            v.setViewVisibility(R.id.r_detail, detail.isEmpty() || it.done ? View.GONE : View.VISIBLE);
            String when = when(it.when);
            v.setTextViewText(R.id.r_when, when);
            v.setViewVisibility(R.id.r_when, when.isEmpty() ? View.GONE : View.VISIBLE);

            if (tick) {
                v.setImageViewResource(R.id.r_box, it.done ? R.drawable.box_on : R.drawable.box_off);
                if (!it.pageId.isEmpty()) {
                    v.setOnClickFillInIntent(R.id.row, new Intent()
                            .putExtra(ActionReceiver.EXTRA_PAGE, it.pageId)
                            .putExtra(ActionReceiver.EXTRA_DONE, it.done));
                }
            } else {
                v.setTextColor(R.id.r_dot, accent);
            }
            return v;
        }

        private static int accent(String kind) {
            switch (kind) {
                case "tasks": return Color.parseColor("#F0A44B");
                case "info": return Color.parseColor("#4CC38A");
                case "events": return Color.parseColor("#5EA2FF");
                default: return Color.parseColor("#9AA7B4");
            }
        }

        /** "Today 09:00", "Tomorrow", "Thu 6 Nov". */
        static String when(String w) {
            if (w == null || w.length() < 10) return "";
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(w.substring(0, 10));
                Calendar day = Calendar.getInstance();
                day.setTime(d);
                Calendar today = Calendar.getInstance();
                String label;
                if (sameDay(day, today)) label = "Today";
                else {
                    today.add(Calendar.DAY_OF_YEAR, 1);
                    label = sameDay(day, today) ? "Tomorrow"
                            : new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(d);
                }
                if (w.length() >= 16 && w.charAt(10) == 'T') label += " " + w.substring(11, 16);
                return label;
            } catch (Exception e) {
                return "";
            }
        }

        private static boolean sameDay(Calendar a, Calendar b) {
            return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                    && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
        }
    }
}
