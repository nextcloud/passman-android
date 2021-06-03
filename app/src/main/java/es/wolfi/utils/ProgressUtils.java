package es.wolfi.utils;

import android.app.ProgressDialog;
import android.content.Context;

import es.wolfi.app.passman.R;

public class ProgressUtils {
    /**
     * Creates and starts a unified progress dialog
     *
     * @param context App context from view
     * @return ProgressDialog required to dismiss the dialog
     */
    public static ProgressDialog showLoadingSequence(Context context) {
        final ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle(context.getString(R.string.loading));
        progress.setMessage(context.getString(R.string.wait_while_loading));
        progress.setCancelable(false);
        progress.show();

        return progress;
    }
}
