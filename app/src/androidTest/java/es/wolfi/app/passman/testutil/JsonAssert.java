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

package es.wolfi.app.passman.testutil;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public final class JsonAssert {
    private JsonAssert() {
    }

    /**
     * Parses any JSON text, including scalars like "foo", 42, false and null.
     */
    public static Object parse(String json) throws JSONException {
        return new JSONTokener(json).nextValue();
    }

    /**
     * Compares JSON values with JSONassert (strict, but key order and number types don't matter).
     * Unlike JSONassert itself, it accepts scalars like the JSON encoded credential fields, too.
     */
    public static void assertJsonEquals(String message, Object expected, Object actual) throws JSONException {
        if (expected instanceof JSONObject || expected instanceof JSONArray) {
            JSONAssert.assertEquals(message, expected.toString(), String.valueOf(actual), JSONCompareMode.STRICT);
        } else {
            assertEquals(message, expected == null ? JSONObject.NULL : expected, actual == null ? JSONObject.NULL : actual);
        }
    }
}
