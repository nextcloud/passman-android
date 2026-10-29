/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2026, Timo Triebensky (timo@binsky.org)
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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

public class ClipboardUtils {

    /**
     * Value of ClipDescription.EXTRA_IS_SENSITIVE (API 33), keyboards like Gboard also respect it on older Android versions.
     */
    public static final String EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE";

    /**
     * @param label     user-visible label of the clip
     * @param text      the text to copy
     * @param sensitive hide the text in the clipboard preview of the system and in the clipboard history of keyboards
     * @return the clip, marked as sensitive if requested
     */
    public static ClipData createClip(String label, String text, boolean sensitive) {
        ClipData clip = ClipData.newPlainText(label, text);
        // the clip description supports extras since Android 7
        if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean(EXTRA_IS_SENSITIVE, true);
            clip.getDescription().setExtras(extras);
        }
        return clip;
    }

    /**
     * @see #createClip(String, String, boolean)
     */
    public static void copyToClipboard(Context context, String label, String text, boolean sensitive) {
        ClipboardManager clipboard = (ClipboardManager) context.getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(createClip(label, text, sensitive));
    }
}
