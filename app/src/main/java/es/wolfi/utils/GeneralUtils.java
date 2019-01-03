package es.wolfi.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class GeneralUtils {

    private static Toast lastToast;

    private static void showtoast(Context c, @NonNull CharSequence message)
    {
        Log.d("GeneralUtils", message.toString());
        if (lastToast != null)
            lastToast.setText(message);
        else {
            lastToast = Toast.makeText(c, message, Toast.LENGTH_SHORT);
        }
        lastToast.show();
    }

    public static void debug(@NonNull String message) {
        Log.d("GeneralUtils",message.toString());
    }

    public static void toast(Context c, @NonNull CharSequence message) {
        showtoast(c, message);
    }

    public static void toast(Context c, int resId) {
        showtoast(c, c.getString(resId));
    }

    public static void toast(View view, @NonNull CharSequence message) {

        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
    }

    public static void toast(View view, int resId) {
        Snackbar.make(view, resId, Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
    }

    public static void debugAndToast(boolean toast, Context c, @NonNull String message) {
        if (toast) {
            toast(c, message);
        }
        debug(message);
    }

    public static void debugAndToast(boolean toast, View v, @NonNull String message) {
        if (toast) {
            toast(v, message);
        }
        debug(message);
    }

    public static void debugAndToast(boolean toast, Context c,  int resId) {
        if (toast) {
            toast(c, c.getString(resId));
        }
        debug(c.getString(resId));
    }

    public static void debugAndToast(boolean toast, View v,  int resId) {
        if (toast) {
            toast(v, resId);
        }
        debug(v.getContext().getString(resId));
    }
}
