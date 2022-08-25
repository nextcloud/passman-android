/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2021, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2021, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2021, Timo Triebensky (timo@binsky.org)
 * @license GNU AGPL version 3 or any later version
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    /**
     * Checks if a dialog is shown and calls dismiss() on it if possible
     *
     * @param progress progress dialog to dismiss
     */
    public static void dismiss(ProgressDialog progress) {
        if (progress != null) {
            if (progress.isShowing()) {
                progress.dismiss();
            }
        }
    }
}
