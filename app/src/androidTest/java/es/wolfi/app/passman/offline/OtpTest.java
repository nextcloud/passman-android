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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import es.wolfi.utils.otp.CodeGenerator;
import es.wolfi.utils.otp.HashingAlgorithm;
import es.wolfi.utils.otp.TOTPHelper;

@RunWith(AndroidJUnit4.class)
public class OtpTest {
    @Test
    public void parsesCompleteQrCodeUrl() throws Exception {
        String url = "otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"
                + "&algorithm=SHA256&digits=8&period=60";
        JSONObject otp = TOTPHelper.getCompleteOTPDataFromQrUrl(url);

        assertEquals("totp", otp.getString("type"));
        assertEquals("Example:alice@example.com", otp.getString("label"));
        assertEquals("Example", otp.getString("issuer"));
        assertEquals("JBSWY3DPEHPK3PXP", otp.getString("secret"));
        assertEquals("SHA256", otp.getString("algorithm"));
        assertEquals(8, otp.getInt("digits"));
        assertEquals(60, otp.getInt("period"));
        assertEquals(url, otp.getString("qr_uri"));
    }

    @Test
    public void appliesDefaultsForMinimalQrCodeUrl() throws Exception {
        JSONObject otp = TOTPHelper.getCompleteOTPDataFromQrUrl("otpauth://totp/?secret=JBSWY3DPEHPK3PXP");

        assertEquals("totp", otp.getString("type"));
        assertFalse(otp.has("label"));
        assertFalse(otp.has("issuer"));
        assertEquals("SHA1", otp.getString("algorithm"));
        assertEquals(6, otp.getInt("digits"));
        assertEquals(TOTPHelper.DEFAULT_OTP_PERIOD, otp.getInt("period"));
    }

    @Test
    public void hotpIsDetected() throws Exception {
        JSONObject otp = TOTPHelper.getCompleteOTPDataFromQrUrl("otpauth://hotp/label?secret=JBSWY3DPEHPK3PXP&counter=0");
        assertEquals("hotp", otp.getString("type"));
    }

    @Test
    public void generatesRfc6238CodesOnTheDevice() throws Exception {
        // same vector as the local unit test, but with the crypto providers of the device
        CodeGenerator generator = new CodeGenerator(HashingAlgorithm.SHA1, 8, 30);
        assertEquals("89005924", generator.generate("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 1234567890L));
    }
}
