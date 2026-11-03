package app.phonedash;

import android.content.Context;
import android.content.SharedPreferences;

/** App settings, stored privately on this phone (never in the APK or on GitHub). */
final class Prefs {
    private Prefs() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("phonedash", Context.MODE_PRIVATE);
    }

    /** Notion internal-integration key and the "Daily Brief" database id. */
    static String notionToken(Context c) { return p(c).getString("notion_token", "").trim(); }
    static void setNotionToken(Context c, String v) { p(c).edit().putString("notion_token", v == null ? "" : v.trim()).apply(); }
    static String notionDb(Context c) { return p(c).getString("notion_db", "").trim(); }
    static void setNotionDb(Context c, String v) { p(c).edit().putString("notion_db", v == null ? "" : v.trim()).apply(); }

    /** Home city for weather; empty = worked out from the internet address. */
    static String homeCity(Context c) { return p(c).getString("home_city", "").trim(); }
    static void setHomeCity(Context c, String v) { p(c).edit().putString("home_city", v == null ? "" : v.trim()).apply(); }

    /** Widget status: when the last refresh worked, and the last error ("" = none). */
    static long lastOk(Context c) { return p(c).getLong("last_ok", 0); }
    static void setLastOk(Context c, long v) { p(c).edit().putLong("last_ok", v).apply(); }
    static String lastError(Context c) { return p(c).getString("last_error", ""); }
    static void setLastError(Context c, String v) { p(c).edit().putString("last_error", v == null ? "" : v).apply(); }

    /** Newer version seen by the background check ("" = none). */
    static String updateAvailable(Context c) { return p(c).getString("update_available", ""); }
    static void setUpdateAvailable(Context c, String v) { p(c).edit().putString("update_available", v == null ? "" : v).apply(); }

    static SharedPreferences raw(Context c) { return p(c); }
}
