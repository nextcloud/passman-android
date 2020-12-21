package es.wolfi.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import es.wolfi.app.passman.SettingValues;
import es.wolfi.passman.API.Vault;

public class GeneralUtils {

    private static Toast lastToast;

    private static void showtoast(Context c, @NonNull CharSequence message) {
        Log.d("GeneralUtils", message.toString());
        if (lastToast != null)
            lastToast.setText(message);
        else {
            lastToast = Toast.makeText(c, message, Toast.LENGTH_SHORT);
        }
        lastToast.show();
    }

    public static void debug(@NonNull String message) {
        Log.d("GeneralUtils", message.toString());
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
        debug(message);
        if (toast) {
            toast(c, message);
        }
    }

    public static void debugAndToast(boolean toast, View v, @NonNull String message) {
        debug(message);
        if (toast) {
            toast(v, message);
        }
    }

    public static void debugAndToast(boolean toast, Context c, int resId) {
        debug(c.getString(resId));
        if (toast) {
            toast(c, c.getString(resId));
        }
    }

    public static void debugAndToast(boolean toast, View v, int resId) {
        debug(v.getContext().getString(resId));
        if (toast) {
            toast(v, resId);
        }
    }

    public static void updateAutofillVault(Vault vault, SharedPreferences settings) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (settings.getString(SettingValues.AUTOFILL_VAULT_GUID.toString(), "").equals(vault.guid)) {
                try {
                    settings.edit().putString(SettingValues.AUTOFILL_VAULT.toString(), Vault.asJson(vault)).apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
