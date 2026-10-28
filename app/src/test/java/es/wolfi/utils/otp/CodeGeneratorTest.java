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

package es.wolfi.utils.otp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.security.InvalidParameterException;

/**
 * TOTP codes against the test vectors of RFC 6238, appendix B.
 */
public class CodeGeneratorTest {
    // ASCII "12345678901234567890" (20 bytes), "1234...9012" (32 bytes) and "1234...901234" (64 bytes) as base32
    private static final String SEED_SHA1 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    private static final String SEED_SHA256 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA====";
    private static final String SEED_SHA512 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
            + "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA=";

    // time (seconds since epoch), SHA1, SHA256, SHA512
    private static final Object[][] RFC_6238_VECTORS = {
            {59L, "94287082", "46119246", "90693936"},
            {1111111109L, "07081804", "68084774", "25091201"},
            {1111111111L, "14050471", "67062674", "99943326"},
            {1234567890L, "89005924", "91819424", "93441116"},
            {2000000000L, "69279037", "90698825", "38618901"},
            {20000000000L, "65353130", "77737706", "47863826"},
    };

    @Test
    public void rfc6238Sha1() throws CodeGenerationException {
        CodeGenerator generator = new CodeGenerator(HashingAlgorithm.SHA1, 8, 30);
        for (Object[] vector : RFC_6238_VECTORS) {
            assertEquals("T=" + vector[0], vector[1], generator.generate(SEED_SHA1, (long) vector[0]));
        }
    }

    @Test
    public void rfc6238Sha256() throws CodeGenerationException {
        CodeGenerator generator = new CodeGenerator(HashingAlgorithm.SHA256, 8, 30);
        for (Object[] vector : RFC_6238_VECTORS) {
            assertEquals("T=" + vector[0], vector[2], generator.generate(SEED_SHA256, (long) vector[0]));
        }
    }

    @Test
    public void rfc6238Sha512() throws CodeGenerationException {
        CodeGenerator generator = new CodeGenerator(HashingAlgorithm.SHA512, 8, 30);
        for (Object[] vector : RFC_6238_VECTORS) {
            assertEquals("T=" + vector[0], vector[3], generator.generate(SEED_SHA512, (long) vector[0]));
        }
    }

    @Test
    public void sixDigitsAreTheLastSixOfTheEightDigitCode() throws CodeGenerationException {
        CodeGenerator generator = new CodeGenerator();
        for (Object[] vector : RFC_6238_VECTORS) {
            String eightDigits = (String) vector[1];
            assertEquals(eightDigits.substring(2), generator.generate(SEED_SHA1, (long) vector[0]));
        }
    }

    @Test
    public void codeChangesWithThePeriod() throws CodeGenerationException {
        CodeGenerator generator = new CodeGenerator();
        assertEquals(generator.generate(SEED_SHA1, 60), generator.generate(SEED_SHA1, 89));
        assertNotEquals(generator.generate(SEED_SHA1, 89), generator.generate(SEED_SHA1, 90));
    }

    @Test
    public void invalidParametersAreRejected() {
        assertThrows(InvalidParameterException.class, () -> new CodeGenerator(null, 6, 30));
        assertThrows(InvalidParameterException.class, () -> new CodeGenerator(HashingAlgorithm.SHA1, 0, 30));
        assertThrows(InvalidParameterException.class, () -> new CodeGenerator(HashingAlgorithm.SHA1, 6, 0));
    }

    @Test
    public void hashingAlgorithmFromString() {
        assertEquals(HashingAlgorithm.SHA256, HashingAlgorithm.fromStringOrSha1("sha256"));
        assertEquals(HashingAlgorithm.SHA512, HashingAlgorithm.fromStringOrSha1("SHA512"));
        assertEquals(HashingAlgorithm.SHA1, HashingAlgorithm.fromStringOrSha1("SHA1"));
        assertEquals(HashingAlgorithm.SHA1, HashingAlgorithm.fromStringOrSha1("unknown"));
    }
}
