package app.phonedash;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

final class Ui {
    private Ui() {}

    static void start(Context c, Intent i) {
        try {
            c.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(c, "Can't open that on this phone", Toast.LENGTH_LONG).show();
        }
    }
}
