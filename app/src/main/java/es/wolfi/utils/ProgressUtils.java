package es.wolfi.utils;

import android.app.ProgressDialog;
import android.content.Context;

import es.wolfi.app.passman.R;

public class ProgressUtils {
    /**
     * Creates and starts a unified loading sequence dialog
     *
     * @param context App context from view
     * @return ProgressDialog required to dismiss the dialog
     */
    public static ProgressDialog showLoadingSequence(Context context) {
        return show(context, context.getString(R.string.loading), context.getString(R.string.wait_while_loading), false);
    }

    /**
     * Creates and starts a custom progress dialog
     *
     * @param context    App context from view
     * @param title      Progress dialog title
     * @param message    Progress dialog message
     * @param cancelable Set if the dialog should be cancelable by the user
     * @return ProgressDialog required to dismiss the dialog
     */
    public static ProgressDialog show(Context context, String title, String message, boolean cancelable) {
        final ProgressDialog progress = new ProgressDialog(context);
        progress.setTitle(title);
        progress.setMessage(message);
        progress.setCancelable(cancelable);
        progress.show();

        return progress;
    }
}
