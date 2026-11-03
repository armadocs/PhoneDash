package app.phonedash;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

/** Loads weather + the Notion brief in the background: every ~30 min, and on demand. */
public class RefreshJob extends JobService {
    private static final int PERIODIC = 1, NOW = 2;
    static volatile boolean running;

    static void schedulePeriodic(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null || js.getPendingJob(PERIODIC) != null) return;
        js.schedule(new JobInfo.Builder(PERIODIC, new ComponentName(c, RefreshJob.class))
                .setPeriodic(30 * 60 * 1000L, 10 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build());
    }

    static void now(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js == null) return;
        JobInfo.Builder b = new JobInfo.Builder(NOW, new ComponentName(c, RefreshJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        int r = JobScheduler.RESULT_FAILURE;
        if (Build.VERSION.SDK_INT >= 31) {
            try { r = js.schedule(b.setExpedited(true).build()); } catch (Exception ignored) {}
            b.setExpedited(false);
        }
        if (r != JobScheduler.RESULT_SUCCESS) js.schedule(b.build());
    }

    static void cancelAll(Context c) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (js != null) { js.cancel(PERIODIC); js.cancel(NOW); }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        final Context c = getApplicationContext();
        running = true;
        DashWidget.render(c);
        new Thread(() -> {
            try {
                refresh(c);
            } finally {
                running = false;
                DashWidget.render(c);
                jobFinished(params, false);
            }
        }, "refresh").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    /** Network work: call off the UI thread. */
    static void refresh(Context c) {
        try { Weather.fetch(c); } catch (Exception ignored) { /* shown via Weather.lastError */ }
        try {
            Brief.load(c);
            Prefs.setLastOk(c, System.currentTimeMillis());
            Prefs.setLastError(c, "");
        } catch (Exception e) {
            String m = e.getMessage();
            Prefs.setLastError(c, m == null || m.isEmpty() ? e.getClass().getSimpleName() : m);
        }
        if (Updater.autoCheckDue(c)) {
            try {
                Updater.Info info = Updater.fetchLatest(c);
                Prefs.setUpdateAvailable(c, Updater.isNewer(c, info) ? info.versionCode + "|" + info.versionName : "");
            } catch (Exception ignored) {
            }
        }
    }
}
