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
import static org.junit.Assert.assertThrows;

import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;

import es.wolfi.app.passman.SJCLCrypto;

/**
 * Cryptograms come from the server or the offline cache, so the native SJCL implementation has to reject
 * malformed ones with an exception instead of crashing the app.
 */
@RunWith(AndroidJUnit4.class)
public class SjclInputValidationTest {
    private static final String PASSWORD = "vault password";
    private static final String PLAINTEXT = "secret";
    private static final String[] STRING_FIELDS = {"iv", "adata", "salt", "ct"};
    private static final String[] NUMBER_FIELDS = {"iter", "ks", "ts"};

    private JSONObject parameters;

    @Before
    public void encrypt() throws Exception {
        parameters = new JSONObject(new String(
                Base64.decode(SJCLCrypto.encryptString(PLAINTEXT, PASSWORD, true), Base64.DEFAULT),
                StandardCharsets.UTF_8
        ));
    }

    @Test
    public void unmodifiedCryptogramIsDecrypted() throws Exception {
        assertEquals(PLAINTEXT, SJCLCrypto.decryptString(cryptogram(parameters.toString()), PASSWORD));
    }

    @Test
    public void noJsonObjectIsRejected() {
        for (String json : new String[]{"no json", "[]", "\"text\"", "42"}) {
            assertRejected(json, json);
        }
    }

    @Test
    public void missingFieldIsRejected() throws Exception {
        for (String field : concat(STRING_FIELDS, NUMBER_FIELDS)) {
            JSONObject manipulated = new JSONObject(parameters.toString());
            manipulated.remove(field);
            assertRejected("without " + field, manipulated.toString());
        }
    }

    @Test
    public void stringFieldOfWrongTypeIsRejected() throws Exception {
        for (String field : STRING_FIELDS) {
            for (Object value : new Object[]{1, true, JSONObject.NULL, new JSONObject()}) {
                JSONObject manipulated = new JSONObject(parameters.toString()).put(field, value);
                assertRejected(field + ": " + value, manipulated.toString());
            }
        }
    }

    @Test
    public void numberFieldOfWrongTypeOrRangeIsRejected() throws Exception {
        for (String field : NUMBER_FIELDS) {
            for (Object value : new Object[]{"1000", true, JSONObject.NULL, 0, -1, 1.5, 1e300, -1e300, 2147483648L}) {
                JSONObject manipulated = new JSONObject(parameters.toString()).put(field, value);
                assertRejected(field + ": " + value, manipulated.toString());
            }
        }
    }

    private static void assertRejected(String message, String json) {
        assertThrows(message, Exception.class, () -> SJCLCrypto.decryptStringCpp(cryptogram(json), PASSWORD));
    }

    private static String cryptogram(String json) {
        return Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    /**
     * A new array with the elements of both arrays, e.g. to check all string and number fields in one loop.
     * Java has no built-in array concatenation, and Stream.concat would need API 24 (minSdk is 23).
     */
    private static String[] concat(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
