/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @license GNU AGPL version 3 or any later version
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package es.wolfi.passman.API;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import es.wolfi.app.passman.SJCLCrypto;

public class Vault extends Core {
    public int vault_id;
    public String guid;
    public String name;
    public double created;
    public String public_sharing_key;
    public double last_access;
    public String challenge_password;

    ArrayList<Credential> credentials;
    HashMap<String, Integer> credential_guid;

    private String encryption_key = "";

    public void setEncryptionKey(String k) {
        encryption_key = k;
    }

    public String decryptString(String cryptogram) {
        try {
            return SJCLCrypto.decryptString(cryptogram, encryption_key);
        } catch (Exception e) {
            Log.e("Vault", e.getMessage());
            e.printStackTrace();
        }
        return "Error decrypting";
    }

    public boolean unlock(String key) {
        encryption_key = key;

        // Check if the key was correct
        if (is_unlocked()) {
            return true;
        }

        encryption_key = "";
        return false;
    }

    public boolean is_unlocked() {
        try {
            if (!encryption_key.isEmpty()) {
                SJCLCrypto.decryptString(challenge_password, encryption_key);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String encryptString(String plaintext) {
        // TODO: Implement encryption
        return "";
    }

    public Date getCreatedTime(){
        return new Date((long) created * 1000);
    }

    public Credential findCredentialByGUID(String guid) {
        Log.e("Vault", "GUID: ".concat(guid).concat(" Arr pos: ").concat(String.valueOf(credential_guid.get(guid))));
        return credentials.get(credential_guid.get(guid));
    }

    public Date getLastAccessTime(){
        return new Date((long) last_access * 1000);
    }

    public ArrayList<Credential> getCredentials() {
        return credentials;
    }

    public static void getVaults(Context c, final FutureCallback<HashMap<String, Vault>> cb) {
        Vault.requestAPIGET(c, "vaults",new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if (e != null) {
                    cb.onCompleted(e, null);
                    return;
                }

//                Log.e(Vault.LOG_TAG, result);
//                cb.onCompleted(e, null);
                try {
                    JSONArray data = new JSONArray(result);
                    HashMap<String, Vault> l = new HashMap<String, Vault>();
                    for (int i = 0; i < data.length(); i++) {
                        Vault v = Vault.fromJSON(data.getJSONObject(i));
                        l.put(v.guid, v);
                    }

                    cb.onCompleted(e, l);
                }
                catch (JSONException ex) {
                    cb.onCompleted(ex, null);
                }
            }
        });
    }

    public static void getVault(Context c, String guid, final FutureCallback<Vault> cb) {
        Vault.requestAPIGET(c, "vaults/".concat(guid),new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if (e != null) {
                    cb.onCompleted(e, null);
                    return;
                }

                try {
                    JSONObject data = new JSONObject(result);

                    Vault v = Vault.fromJSON(data);

                    cb.onCompleted(e, v);
                }
                catch (JSONException ex) {
                    cb.onCompleted(ex, null);
                }
            }
        });
    }

    protected static Vault fromJSON(JSONObject o) throws JSONException{
        Vault v = new Vault();

        v.vault_id = o.getInt("vault_id");
        v.guid = o.getString("guid");
        v.name = o.getString("name");
        v.created = o.getDouble("created");
        v.public_sharing_key = o.getString("public_sharing_key");
        v.last_access = o.getDouble("last_access");

        if (o.has("credentials")){
            JSONArray j = o.getJSONArray("credentials");
            v.credentials = new ArrayList<Credential>();
            v.credential_guid = new HashMap<>();

            for (int i = 0; i < j.length(); i++) {
                Credential c = Credential.fromJSON(j.getJSONObject(i), v);
                v.credentials.add(c);
                v.credential_guid.put(c.getGuid(), i);
            }

            v.challenge_password = v.credentials.get(0).password;
        }
        else {
            v.challenge_password = o.getString("challenge_password");
        }

        return v;
    }
}
