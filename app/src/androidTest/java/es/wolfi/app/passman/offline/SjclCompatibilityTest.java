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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static es.wolfi.app.passman.testutil.JsonAssert.assertJsonEquals;
import static es.wolfi.app.passman.testutil.JsonAssert.parse;

import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import es.wolfi.app.passman.SJCLCrypto;
import es.wolfi.app.passman.testutil.TestVault;

/**
 * The native SJCL implementation of the app against data encrypted by the Passman web client.
 * Runs on every ABI, since every ABI has its own build of the native library and OpenSSL.
 */
@RunWith(AndroidJUnit4.class)
public class SjclCompatibilityTest {
    private static final String[] ENCRYPTED_CREDENTIAL_FIELDS = {
            "description", "tags", "email", "username", "password", "url", "files", "custom_fields", "otp", "compromised"
    };

    private static TestVault testVault;

    @BeforeClass
    public static void loadTestVault() throws Exception {
        testVault = TestVault.get();
    }

    @Test
    public void decryptsEveryCredentialFieldOfTheWebClient() throws Exception {
        int compared = 0;
        for (JSONObject encrypted : testVault.backupCredentials()) {
            JSONObject expected = testVault.frontendCredential(encrypted.getString("guid"));
            if (expected == null) {
                // the hidden vault test key is not part of the web client export
                continue;
            }
            assertEquals("label", expected.getString("label"), encrypted.getString("label"));
            for (String field : ENCRYPTED_CREDENTIAL_FIELDS) {
                String message = encrypted.getString("label") + " / " + field;
                Object decrypted = parse(TestVault.decryptRaw(encrypted.getString(field), testVault.vaultPassword()));
                // the web client export inlines the file contents, the credential itself only references them
                assertJsonEquals(message, withoutFileData(expected.opt(field)), decrypted);
            }
            compared++;
        }
        assertEquals(testVault.frontendCredentials.length(), compared);
    }

    @Test
    public void decryptsFilesOfTheWebClient() throws Exception {
        Map<String, JSONObject> expectedFiles = filesOfFrontendExport();
        List<JSONObject> files = testVault.backupFiles();
        assertFalse("backup.json contains no files", files.isEmpty());

        for (JSONObject file : files) {
            JSONObject expected = expectedFiles.get(file.getString("guid"));
            // unlike credential fields, file names and contents are encrypted without JSON encoding
            String filename = TestVault.decryptRaw(file.getString("filename"), testVault.vaultPassword());
            String dataUrl = TestVault.decryptRaw(file.getString("file_data"), testVault.vaultPassword());

            assertTrue(filename, dataUrl.startsWith("data:" + file.getString("mimetype") + ";base64,"));
            if (expected == null) {
                // not referenced by any credential anymore
                continue;
            }
            assertEquals(expected.getString("filename"), filename);
            assertLargeStringEquals(filename, expected.getString("file_data"), dataUrl);

            byte[] content = Base64.decode(dataUrl.substring(dataUrl.indexOf(',') + 1), Base64.DEFAULT);
            assertEquals(filename, file.getLong("size"), content.length);
        }
    }

    @Test
    public void appEncryptionUsesTheParametersOfTheWebClient() throws Exception {
        JSONObject webClient = sjclParameters(testVault.backupCredentials().get(0).getString("password"));
        JSONObject app = sjclParameters(SJCLCrypto.encryptString("test", testVault.vaultPassword(), true));

        for (String parameter : new String[]{"v", "iter", "ks", "ts", "mode", "cipher"}) {
            assertEquals(parameter, webClient.get(parameter), app.get(parameter));
        }
    }

    @Test
    public void roundTrip() throws Exception {
        String[] plaintexts = {
                "",
                "p",
                "äöü ß € 😀 \"quotes\" \\backslash\n newline",
                new String(new char[70000]).replace('\0', 'x'),
        };
        for (String plaintext : plaintexts) {
            String cryptogram = SJCLCrypto.encryptString(plaintext, testVault.vaultPassword(), true);
            assertEquals(plaintext, SJCLCrypto.decryptString(cryptogram, testVault.vaultPassword()));
            // values have to be JSON encoded like the web client does it
            assertEquals(plaintext, parse(TestVault.decryptRaw(cryptogram, testVault.vaultPassword())));
        }
    }

    @Test
    public void encryptionIsRandomized() throws Exception {
        String first = SJCLCrypto.encryptString("same", testVault.vaultPassword(), true);
        String second = SJCLCrypto.encryptString("same", testVault.vaultPassword(), true);
        assertNotEquals(first, second);
        assertNotEquals(sjclParameters(first).getString("iv"), sjclParameters(second).getString("iv"));
        assertNotEquals(sjclParameters(first).getString("salt"), sjclParameters(second).getString("salt"));
    }

    @Test
    public void wrongPasswordIsRejected() throws Exception {
        String cryptogram = testVault.backupCredentials().get(0).getString("password");
        assertThrows(Exception.class, () -> SJCLCrypto.decryptStringCpp(cryptogram, testVault.vaultPassword() + "x"));
        assertThrows(Exception.class, () -> SJCLCrypto.decryptStringCpp(cryptogram, ""));
    }

    @Test
    public void manipulatedCryptogramIsRejected() throws Exception {
        String cryptogram = testVault.backupCredentials().get(1).getString("password");
        JSONObject parameters = sjclParameters(cryptogram);
        byte[] ct = Base64.decode(parameters.getString("ct"), Base64.DEFAULT);
        ct[0] ^= 1;
        parameters.put("ct", Base64.encodeToString(ct, Base64.NO_WRAP));
        String manipulated = Base64.encodeToString(parameters.toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);

        assertThrows(Exception.class, () -> SJCLCrypto.decryptStringCpp(manipulated, testVault.vaultPassword()));
    }

    /**
     * A copy of the JSON value without the file_data keys on any level.
     */
    private static Object withoutFileData(Object json) throws Exception {
        if (json instanceof JSONObject) {
            JSONObject copy = new JSONObject(json.toString());
            copy.remove("file_data");
            for (Iterator<String> keys = copy.keys(); keys.hasNext(); ) {
                String key = keys.next();
                copy.put(key, withoutFileData(copy.get(key)));
            }
            return copy;
        }
        if (json instanceof JSONArray) {
            JSONArray copy = new JSONArray();
            for (int i = 0; i < ((JSONArray) json).length(); i++) {
                copy.put(withoutFileData(((JSONArray) json).get(i)));
            }
            return copy;
        }
        return json;
    }

    /**
     * Like assertEquals, but without printing megabytes of data on a mismatch.
     */
    private static void assertLargeStringEquals(String message, String expected, String actual) {
        if (expected.equals(actual)) {
            return;
        }
        int diff = 0;
        while (diff < Math.min(expected.length(), actual.length()) && expected.charAt(diff) == actual.charAt(diff)) {
            diff++;
        }
        fail(message + ": expected " + expected.length() + " chars but was " + actual.length()
                + ", first difference at index " + diff);
    }

    private static JSONObject sjclParameters(String cryptogram) throws Exception {
        return new JSONObject(new String(Base64.decode(cryptogram, Base64.DEFAULT), StandardCharsets.UTF_8));
    }

    /**
     * All file objects of the web client export (credential files and file custom fields) by guid.
     */
    private static Map<String, JSONObject> filesOfFrontendExport() throws Exception {
        Map<String, JSONObject> files = new HashMap<>();
        for (int i = 0; i < testVault.frontendCredentials.length(); i++) {
            JSONObject credential = testVault.frontendCredentials.getJSONObject(i);
            JSONArray credentialFiles = credential.optJSONArray("files");
            for (int f = 0; credentialFiles != null && f < credentialFiles.length(); f++) {
                files.put(credentialFiles.getJSONObject(f).getString("guid"), credentialFiles.getJSONObject(f));
            }
            JSONArray customFields = credential.optJSONArray("custom_fields");
            for (int f = 0; customFields != null && f < customFields.length(); f++) {
                JSONObject customField = customFields.getJSONObject(f);
                if ("file".equals(customField.optString("field_type"))) {
                    JSONObject file = customField.getJSONObject("value");
                    files.put(file.getString("guid"), file);
                }
            }
        }
        return files;
    }
}
