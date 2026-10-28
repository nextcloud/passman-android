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

package es.wolfi.passman.API;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

public class CoreVersionTest {
    @Before
    public void resetVersion() {
        Core.version_name = null;
        Core.version_number = 0;
    }

    @Test
    public void appliesServerVersion() throws JSONException {
        assertTrue(Core.applyVersionJSON("{\"version\":\"2.6.5\"}"));
        assertEquals("2.6.5", Core.version_name);
        assertEquals(265, Core.version_number);
    }

    @Test
    public void ignoresResponseWithoutVersion() throws JSONException {
        assertFalse(Core.applyVersionJSON("{\"message\":\"not found\"}"));
        assertEquals(null, Core.version_name);
        assertEquals(0, Core.version_number);
    }

    @Test
    public void rejectsInvalidResponses() {
        assertThrows(JSONException.class, () -> Core.applyVersionJSON("<html>login</html>"));
        assertThrows(NumberFormatException.class, () -> Core.applyVersionJSON("{\"version\":\"2.6.5-beta\"}"));
    }
}
