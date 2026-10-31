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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;

import es.wolfi.app.passman.adapters.CustomFieldEditAdapter;
import es.wolfi.app.passman.autofill.AutofillHelper;
import es.wolfi.app.passman.testutil.TestVault;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;

/**
 * A credential added from the autofill manual search must be matched the next time the same website or app requests autofill.
 */
@RunWith(AndroidJUnit4.class)
public class AutofillMatchingTest {
    private static final String BROWSER = "com.android.chrome";
    private static final String OTHER_BROWSER = "org.mozilla.firefox";
    private static final String APP = "com.example.app";

    private Vault vault;

    @Before
    public void unlockVault() throws Exception {
        TestVault testVault = TestVault.get();
        vault = Vault.fromJSON(testVault.apiVaultJson());
        assertTrue(vault.unlock(testVault.vaultPassword()));
    }

    @Test
    public void websiteCredentialIsMatchedByItsDomain() {
        Credential credential = addedCredential(website("gitlab.com", "https"), BROWSER);

        assertEquals("https://gitlab.com", credential.getUrl());
        assertTrue(matches(credential, website("gitlab.com", "https"), BROWSER));
        assertTrue("the domain matches in every browser", matches(credential, website("gitlab.com", "https"), OTHER_BROWSER));
        assertFalse(matches(credential, website("gitlab.example.com", "https"), BROWSER));
    }

    @Test
    public void websiteCredentialWithoutSchemeIsMatchedByItsDomain() {
        // the web scheme is only available since Android 9
        Credential credential = addedCredential(website("gitlab.com", null), BROWSER);

        assertEquals("gitlab.com", credential.getUrl());
        assertTrue(matches(credential, website("gitlab.com", null), BROWSER));
        assertFalse(matches(credential, website("gitlab.example.com", null), BROWSER));
    }

    @Test
    public void appCredentialIsMatchedByItsPackageName() {
        Credential credential = addedCredential(new AutofillHelper.WebDomainResult(), APP);

        assertTrue(matches(credential, new AutofillHelper.WebDomainResult(), APP));
        assertFalse(matches(credential, new AutofillHelper.WebDomainResult(), "com.example.other"));
        assertFalse("an app credential must not match websites", matches(credential, website("gitlab.com", "https"), BROWSER));
    }

    /**
     * The credential as the add form saves it, the custom fields are written back by its adapter.
     */
    private Credential addedCredential(AutofillHelper.WebDomainResult domain, String requesterPackageName) {
        Credential credential = AutofillHelper.newCredentialForRequester(vault, domain, requesterPackageName);
        credential.setCustomFields(new CustomFieldEditAdapter(credential).getCustomFieldsString());
        return credential;
    }

    private static AutofillHelper.WebDomainResult website(String domain, String scheme) {
        AutofillHelper.WebDomainResult result = new AutofillHelper.WebDomainResult();
        result.addDomain(domain, scheme);
        return result;
    }

    private static boolean matches(Credential credential, AutofillHelper.WebDomainResult domain, String requesterPackageName) {
        ArrayList<Credential> credentials = new ArrayList<>(Collections.singletonList(credential));
        return AutofillHelper.findMatchingCredentials(credentials, requesterPackageName, domain).contains(credential);
    }
}
