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

package es.wolfi.app.passman.offline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.wolfi.utils.ClipboardUtils;

@RunWith(AndroidJUnit4.class)
public class ClipboardUtilsTest {

    @Test
    public void clipContainsTheText() {
        // also runs before Android 7, where the clip can't be marked
        ClipData clip = ClipboardUtils.createClip("label", "secret", true);
        assertEquals(1, clip.getItemCount());
        assertEquals("secret", clip.getItemAt(0).getText().toString());
        assertEquals("label", clip.getDescription().getLabel().toString());
        assertTrue(clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN));
    }

    @Test
    public void sensitiveClipIsMarked() {
        // the clip description supports extras since Android 7
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);

        PersistableBundle extras = ClipboardUtils.createClip("label", "secret", true).getDescription().getExtras();
        assertTrue(extras.getBoolean(ClipboardUtils.EXTRA_IS_SENSITIVE));
    }

    @Test
    public void extraMatchesTheSystemConstant() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU);

        assertEquals(ClipDescription.EXTRA_IS_SENSITIVE, ClipboardUtils.EXTRA_IS_SENSITIVE);
    }

    @Test
    public void otherClipIsNotMarked() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);

        PersistableBundle extras = ClipboardUtils.createClip("label", "user", false).getDescription().getExtras();
        if (extras != null) {
            assertFalse(extras.getBoolean(ClipboardUtils.EXTRA_IS_SENSITIVE));
        }
    }
}
