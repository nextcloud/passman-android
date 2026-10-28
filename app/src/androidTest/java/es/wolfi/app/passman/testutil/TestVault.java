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

import android.content.res.AssetManager;
import android.util.Base64;

import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import es.wolfi.app.passman.SJCLCrypto;

/**
 * The exchangeable test data of ci/test-vault, packaged as assets of the test apk.
 */
public final class TestVault {
    private static TestVault instance;

    public final Properties properties = new Properties();
    /**
     * passman:backup export, every credential field is encrypted with the vault password
     */
    public final JSONObject backup;
    /**
     * plaintext export of the web client, used as expected values
     */
    public final JSONArray frontendCredentials;

    private TestVault() throws IOException, JSONException {
        AssetManager assets = InstrumentationRegistry.getInstrumentation().getContext().getAssets();
        try (InputStream in = assets.open("test-vault.properties")) {
            properties.load(in);
        }
        backup = new JSONObject(readAsset(assets, "backup.json"));
        frontendCredentials = new JSONArray(readAsset(assets, "frontend-export.json"));
    }

    public static synchronized TestVault get() throws IOException, JSONException {
        if (instance == null) {
            instance = new TestVault();
        }
        return instance;
    }

    private static String readAsset(AssetManager assets, String name) throws IOException {
        try (InputStream in = assets.open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    public String nextcloudUser() {
        return properties.getProperty("nextcloud.user");
    }

    public String nextcloudPassword() {
        return properties.getProperty("nextcloud.password");
    }

    public String vaultGuid() {
        return properties.getProperty("vault.guid");
    }

    public String vaultName() {
        return properties.getProperty("vault.name");
    }

    public String vaultPassword() {
        return properties.getProperty("vault.password");
    }

    public JSONObject backupVault() throws JSONException {
        JSONArray vaults = backup.getJSONObject("sections").getJSONArray("vaults");
        for (int i = 0; i < vaults.length(); i++) {
            if (vaultGuid().equals(vaults.getJSONObject(i).getString("guid"))) {
                return vaults.getJSONObject(i);
            }
        }
        throw new IllegalStateException("vault " + vaultGuid() + " is not part of backup.json");
    }

    /**
     * Credentials of the test vault in server order (by id), the first one is the hidden vault test key.
     */
    public List<JSONObject> backupCredentials() throws JSONException {
        String vaultId = backupVault().getString("id");
        JSONArray all = backup.getJSONObject("sections").getJSONArray("credentials");
        List<JSONObject> credentials = new ArrayList<>();
        for (int i = 0; i < all.length(); i++) {
            JSONObject credential = all.getJSONObject(i);
            if (vaultId.equals(credential.getString("vault_id"))) {
                credentials.add(credential);
            }
        }
        Collections.sort(credentials, (left, right) -> Long.compare(left.optLong("id"), right.optLong("id")));
        return credentials;
    }

    public List<JSONObject> backupFiles() throws JSONException {
        JSONArray files = backup.getJSONObject("sections").getJSONArray("files");
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < files.length(); i++) {
            result.add(files.getJSONObject(i));
        }
        return result;
    }

    public JSONObject frontendCredential(String guid) throws JSONException {
        for (int i = 0; i < frontendCredentials.length(); i++) {
            if (guid.equals(frontendCredentials.getJSONObject(i).getString("guid"))) {
                return frontendCredentials.getJSONObject(i);
            }
        }
        return null;
    }

    /**
     * Credentials shown in the app: not hidden and not deleted.
     */
    public List<JSONObject> visibleFrontendCredentials() throws JSONException {
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < frontendCredentials.length(); i++) {
            JSONObject credential = frontendCredentials.getJSONObject(i);
            if (credential.optInt("hidden") == 0 && credential.optLong("delete_time") == 0) {
                result.add(credential);
            }
        }
        return result;
    }

    /**
     * The first visible credential matching the given condition, fails if there is none.
     */
    public JSONObject frontendCredentialWhere(CredentialCondition condition) throws JSONException {
        for (JSONObject credential : visibleFrontendCredentials()) {
            if (condition.matches(credential)) {
                return credential;
            }
        }
        throw new IllegalStateException("ci/test-vault/frontend-export.json lacks a credential needed by this test");
    }

    public interface CredentialCondition {
        boolean matches(JSONObject credential) throws JSONException;
    }

    /**
     * The test vault in the format of the Passman API v2 (GET vaults/{guid}), as parsed by Vault.fromJSON().
     */
    public JSONObject apiVaultJson() throws JSONException {
        JSONObject vault = backupVault();
        JSONObject api = new JSONObject();
        api.put("vault_id", vault.getInt("id"));
        api.put("guid", vault.getString("guid"));
        api.put("name", vault.getString("name"));
        api.put("created", vault.getLong("created"));
        api.put("last_access", vault.getLong("last_access"));
        api.put("public_sharing_key", vault.getString("public_sharing_key"));
        api.put("private_sharing_key", vault.getString("private_sharing_key"));
        api.put("sharing_keys_generated", vault.optLong("sharing_keys_generated"));
        api.put("vault_settings", vault.opt("vault_settings"));

        JSONArray credentials = new JSONArray();
        for (JSONObject credential : backupCredentials()) {
            JSONObject apiCredential = new JSONObject(credential.toString());
            apiCredential.put("credential_id", credential.getInt("id"));
            apiCredential.remove("id");
            credentials.put(apiCredential);
        }
        api.put("credentials", credentials);
        return api;
    }

    /**
     * Decrypts an SJCL cryptogram with the native code of the app and returns the raw plaintext
     * (the web client encrypts JSON.stringify() of every value).
     */
    public static String decryptRaw(String cryptogram, String password) throws Exception {
        String base64 = SJCLCrypto.decryptStringCpp(cryptogram, password);
        return new String(Base64.decode(base64, Base64.DEFAULT), StandardCharsets.UTF_8);
    }
}
