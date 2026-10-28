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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static es.wolfi.app.passman.testutil.JsonAssert.assertJsonEquals;
import static es.wolfi.app.passman.testutil.JsonAssert.parse;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import es.wolfi.app.passman.testutil.TestVault;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;

/**
 * The vault and credential models of the app with the test vault in the format of the Passman API.
 */
@RunWith(AndroidJUnit4.class)
public class VaultModelTest {
    private TestVault testVault;
    private Vault vault;

    @Before
    public void parseVault() throws Exception {
        testVault = TestVault.get();
        vault = Vault.fromJSON(testVault.apiVaultJson());
    }

    @Test
    public void parsesTheVault() {
        assertEquals(testVault.vaultGuid(), vault.guid);
        assertEquals(testVault.vaultName(), vault.getName());
        // every not deleted credential incl. the hidden vault test key
        assertEquals(testVault.frontendCredentials.length() + 1, vault.getCredentials().size());
    }

    @Test
    public void unlocksOnlyWithTheVaultPassword() {
        assertFalse(vault.unlock(testVault.vaultPassword() + "x"));
        assertFalse(vault.is_unlocked());
        assertFalse(vault.unlock(""));

        assertTrue(vault.unlock(testVault.vaultPassword()));
        assertTrue(vault.is_unlocked());

        vault.lock();
        assertFalse(vault.is_unlocked());
    }

    @Test
    public void credentialGettersReturnThePlaintext() throws Exception {
        assertTrue(vault.unlock(testVault.vaultPassword()));

        for (Credential credential : vault.getCredentials()) {
            JSONObject expected = testVault.frontendCredential(credential.getGuid());
            if (expected == null) {
                assertTrue("only the vault test key is missing in the web client export", credential.isHidden());
                continue;
            }
            String label = credential.getLabel();
            assertEquals(expected.getString("label"), label);
            assertEquals(label, stringOrNull(expected, "username"), credential.getUsername());
            assertEquals(label, stringOrNull(expected, "password"), credential.getPassword());
            assertEquals(label, stringOrNull(expected, "email"), credential.getEmail());
            assertEquals(label, stringOrNull(expected, "url"), credential.getUrl());
            assertEquals(label, stringOrNull(expected, "description"), credential.getDescription());
            // only getters used by the app itself, R8 removes the others (tags are covered by SjclCompatibilityTest)
            assertJsonEquals(label + " / otp", expected.get("otp"), parse(credential.getOtp()));
            assertEquals(label, expected.getJSONArray("files").length(), credential.getFilesList().size());
            assertEquals(label, expected.getJSONArray("custom_fields").length(), credential.getCustomFieldsList().size());
            assertEquals(label, String.valueOf(expected.getBoolean("compromised")), credential.getCompromised());
        }
    }

    @Test
    public void updatedCredentialStaysDecryptable() throws Exception {
        assertTrue(vault.unlock(testVault.vaultPassword()));
        Credential credential = Credential.clone(vault.getCredentials().get(1));

        credential.setPassword("new pässword 1!");
        credential.setUsername("new user");
        assertEquals("new pässword 1!", credential.getPassword());
        assertEquals("new user", credential.getUsername());

        // the request payload sends the encrypted values only
        JSONObject request = credential.getAsJsonObjectForApiRequest(true);
        assertNotNull(request);
        assertFalse(request.toString().contains("new pässword 1!"));
    }

    private static String stringOrNull(JSONObject object, String key) {
        return object.isNull(key) ? null : object.optString(key);
    }
}
