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

package es.wolfi.app.passman.e2e;

import android.net.Uri;
import android.util.Base64;

import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Direct access to the Passman API of the test server, to check what the app really stored.
 */
final class PassmanServer {
    /**
     * Instrumentation argument (am instrument -e serverUrl ...), the emulator reaches the host via 10.0.2.2
     */
    static final String SERVER_URL_ARGUMENT = "serverUrl";
    private static final String API = "/index.php/apps/passman/api/v2/";

    final Uri url;
    private final String user;
    private final String password;

    PassmanServer(String user, String password) {
        String serverUrl = InstrumentationRegistry.getArguments().getString(SERVER_URL_ARGUMENT, "http://10.0.2.2:8080");
        this.url = Uri.parse(serverUrl);
        this.user = user;
        this.password = password;
    }

    /**
     * host[:port] as entered in the login form
     */
    String hostAndPort() {
        return url.getPort() > 0 ? url.getHost() + ":" + url.getPort() : url.getHost();
    }

    JSONArray vaults() throws IOException, JSONException {
        return new JSONArray(get("vaults"));
    }

    JSONObject vaultByName(String name) throws IOException, JSONException {
        JSONArray vaults = vaults();
        for (int i = 0; i < vaults.length(); i++) {
            if (name.equals(vaults.getJSONObject(i).getString("name"))) {
                return vaults.getJSONObject(i);
            }
        }
        return null;
    }

    JSONObject vault(String guid) throws IOException, JSONException {
        return new JSONObject(get("vaults/" + guid));
    }

    /**
     * The credential with the given label in the vault, including deleted ones (delete_time != 0).
     */
    JSONObject credentialByLabel(String vaultGuid, String label) throws IOException, JSONException {
        JSONArray credentials = vault(vaultGuid).getJSONArray("credentials");
        for (int i = 0; i < credentials.length(); i++) {
            if (label.equals(credentials.getJSONObject(i).getString("label"))) {
                return credentials.getJSONObject(i);
            }
        }
        return null;
    }

    private String get(String endpoint) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url + API + endpoint).openConnection();
        String credentials = Base64.encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        connection.setRequestProperty("Authorization", "Basic " + credentials);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        try {
            if (connection.getResponseCode() != 200) {
                throw new IOException("GET " + endpoint + " returned HTTP " + connection.getResponseCode());
            }
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            connection.disconnect();
        }
    }
}
