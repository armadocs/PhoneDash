package app.phonedash;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Hands exactly one file, the downloaded update, to Android's installer, read-only. */
public class ApkProvider extends ContentProvider {
    static final String AUTHORITY = "app.phonedash.apk";
    static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/phonedash.apk");
    static final String APK_MIME = "application/vnd.android.package-archive";

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read-only");
        File f = Updater.apkFile(getContext());
        if (!f.exists()) throw new FileNotFoundException("file not available");
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public String getType(Uri uri) { return APK_MIME; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{"phonedash.apk", Updater.apkFile(getContext()).length()});
        return c;
    }

    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
