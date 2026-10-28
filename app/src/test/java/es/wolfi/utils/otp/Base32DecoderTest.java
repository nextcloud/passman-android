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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.apache.commons.codec.binary.Base32;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * The fallback decoder (used if commons-codec is not usable on the device) must match commons-codec.
 */
public class Base32DecoderTest {
    @Test
    public void rfc4648Vectors() {
        assertEquals("", Base32Decoder.decodeBase32ToString(""));
        assertEquals("f", Base32Decoder.decodeBase32ToString("MY======"));
        assertEquals("fo", Base32Decoder.decodeBase32ToString("MZXQ===="));
        assertEquals("foo", Base32Decoder.decodeBase32ToString("MZXW6==="));
        assertEquals("foob", Base32Decoder.decodeBase32ToString("MZXW6YQ="));
        assertEquals("fooba", Base32Decoder.decodeBase32ToString("MZXW6YTB"));
        assertEquals("foobar", Base32Decoder.decodeBase32ToString("MZXW6YTBOI======"));
    }

    @Test
    public void matchesCommonsCodec() {
        String[] secrets = {
                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
                "JBSWY3DPEHPK3PXP",
                "ACCDRANNTT",
                "jbswy3dpehpk3pxp",
        };
        Base32 codec = new Base32();
        for (String secret : secrets) {
            assertArrayEquals(secret, codec.decode(secret.toUpperCase()), Base32Decoder.decodeBase32(secret));
        }
    }

    @Test
    public void nullAndPaddingOnly() {
        assertArrayEquals(new byte[0], Base32Decoder.decodeBase32(null));
        assertArrayEquals(new byte[0], Base32Decoder.decodeBase32("===="));
    }

    @Test
    public void invalidCharacterIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Base32Decoder.decodeBase32("MZXW1==="));
    }

    @Test
    public void utf8() {
        String text = "Pässwörd";
        String encoded = new Base32().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        assertEquals(text, Base32Decoder.decodeBase32ToString(encoded));
    }
}
