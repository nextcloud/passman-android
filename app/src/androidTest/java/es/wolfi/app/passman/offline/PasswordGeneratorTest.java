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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import es.wolfi.utils.PasswordGenerator;

@RunWith(AndroidJUnit4.class)
public class PasswordGeneratorTest {
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = ".!@#$%^&*";
    private static final String AMBIGUOUS = "iloILO10";
    private static final int RUNS = 200;

    private PasswordGenerator generator;

    @Before
    public void createGenerator() {
        generator = new PasswordGenerator(InstrumentationRegistry.getInstrumentation().getTargetContext());
        generator.setLength(12);
        generator.setUseLowercase(true);
        generator.setUseUppercase(true);
        generator.setUseDigits(true);
        generator.setUseSpecialChars(true);
        generator.setAvoidAmbiguousCharacters(true);
        generator.setRequireEveryCharType(true);
    }

    @Test
    public void containsEveryCharacterTypeAndNoAmbiguousCharacters() {
        for (int i = 0; i < RUNS; i++) {
            String password = generator.generateRandomPassword();
            assertEquals(password, 12, password.length());
            assertTrue(password, containsAny(password, LOWERCASE));
            assertTrue(password, containsAny(password, UPPERCASE));
            assertTrue(password, containsAny(password, DIGITS));
            assertTrue(password, containsAny(password, SPECIAL));
            assertFalse(password, containsAny(password, AMBIGUOUS));
        }
    }

    @Test
    public void respectsDisabledCharacterTypes() {
        generator.setRequireEveryCharType(false);
        generator.setUseUppercase(false);
        generator.setUseSpecialChars(false);
        generator.setLength(64);
        for (int i = 0; i < RUNS; i++) {
            String password = generator.generateRandomPassword();
            assertEquals(64, password.length());
            assertTrue(password, onlyContains(password, LOWERCASE + DIGITS));
        }
    }

    @Test
    public void ambiguousCharactersCanBeAllowed() {
        generator.setRequireEveryCharType(false);
        generator.setAvoidAmbiguousCharacters(false);
        generator.setUseUppercase(false);
        generator.setUseLowercase(false);
        generator.setUseSpecialChars(false);
        generator.setLength(500);
        String password = generator.generateRandomPassword();
        assertTrue(password, onlyContains(password, DIGITS));
        assertTrue("0 and 1 are used, too", password.contains("0") && password.contains("1"));
    }

    @Test
    public void passwordsAreRandom() {
        assertNotEquals(generator.generateRandomPassword(), generator.generateRandomPassword());
    }

    private static boolean containsAny(String text, String characters) {
        for (char c : text.toCharArray()) {
            if (characters.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean onlyContains(String text, String characters) {
        for (char c : text.toCharArray()) {
            if (characters.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }
}
