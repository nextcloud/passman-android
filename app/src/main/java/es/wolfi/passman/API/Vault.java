/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
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

package es.wolfi.passman.API;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import es.wolfi.app.passman.SJCLCrypto;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.utils.CredentialLabelSort;
import es.wolfi.utils.Filterable;

public class Vault extends Core implements Filterable {
    public int vault_id;
    public String guid;
    public String name;
    public double created;
    public String public_sharing_key;
    public double last_access;
    public String challenge_password;

    CopyOnWriteArrayList<Credential> credentials;
    HashMap<String, Integer> credential_guid;

    private String encryption_key = "";

    public void setEncryptionKey(String k) {
        encryption_key = k;
    }

    public String getEncryptionKey() {
        return encryption_key;
    }

    public String decryptString(String cryptogram) {
        return decryptString(cryptogram, this.encryption_key);
    }

    public String decryptString(String cryptogram, String encryption_key) {
        if (cryptogram == null) {
            return "";
        }
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

    public void lock() {
        encryption_key = "";

        for (Credential credential : credentials) {
            credential.resetDecryptedSharedKey();
        }
    }

    public boolean is_unlocked() {
        try {
            if (!encryption_key.isEmpty()) {
                String result = SJCLCrypto.decryptString(challenge_password, encryption_key);
                if (!result.equals("")) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public String encryptString(String plaintext) {
        return encryptString(plaintext, this.encryption_key);
    }

    public String encryptString(String plaintext, String encryption_key) {
        if (plaintext == null) {
            return "";
        }
        try {
            return SJCLCrypto.encryptString(plaintext, encryption_key, true);
        } catch (Exception e) {
            Log.e("Vault", e.getMessage());
            e.printStackTrace();
        }
        return "Error encrypting";
    }

    public String encryptRawStringData(String plaintext) {
        return encryptRawStringData(plaintext, this.encryption_key);
    }

    public String encryptRawStringData(String plaintext, String encryption_key) {
        if (plaintext == null) {
            return "";
        }
        try {
            return SJCLCrypto.encryptString(plaintext, encryption_key, false);
        } catch (Exception e) {
            Log.e("Vault", e.getMessage());
            e.printStackTrace();
        }
        return "Error encrypting";
    }

    public Date getCreatedTime() {
        return new Date((long) created * 1000);
    }

    public Credential findCredentialByGUID(String guid) {
        Log.e("Vault", "GUID: ".concat(guid).concat(" Arr pos: ").concat(String.valueOf(credential_guid.get(guid))));
        return credentials.get(credential_guid.get(guid));
    }

    public Date getLastAccessTime() {
        return new Date((long) last_access * 1000);
    }

    public CopyOnWriteArrayList<Credential> getCredentials() {
        return credentials;
    }

    public static void getVaults(Context c, final FutureCallback<HashMap<String, Vault>> cb) {
        Vault.requestAPIGET(c, "vaults", new FutureCallback<String>() {
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
                } catch (JSONException ex) {
                    cb.onCompleted(ex, null);
                }
            }
        });
    }

    public static void getVault(Context c, String guid, final FutureCallback<Vault> cb) {
        Vault.requestAPIGET(c, "vaults/".concat(guid), new FutureCallback<String>() {
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
                } catch (JSONException ex) {
                    cb.onCompleted(ex, null);
                }
            }
        });
    }

    public static Vault fromJSON(JSONObject o) throws JSONException {
        Vault v = new Vault();

        v.vault_id = o.getInt("vault_id");
        v.guid = o.getString("guid");
        v.name = o.getString("name");
        v.created = o.getDouble("created");
        v.public_sharing_key = o.getString("public_sharing_key");
        v.last_access = o.getDouble("last_access");

        if (o.has("credentials")) {
            JSONArray j = o.getJSONArray("credentials");
            v.credentials = new CopyOnWriteArrayList<Credential>();
            v.credential_guid = new HashMap<>();

            for (int i = 0; i < j.length(); i++) {
                Credential c = Credential.fromJSON(j.getJSONObject(i), v);
                if (c.getDeleteTime() == 0) {
                    v.credentials.add(c);
                    v.credential_guid.put(c.getGuid(), v.credentials.size() - 1);
                }
            }
            v.challenge_password = v.credentials.get(0).password;
        } else {
            v.challenge_password = o.getString("challenge_password");
        }

        return v;
    }

    public void sort(int method) {
        ArrayList<Credential> temp = new ArrayList<>(credentials);
        credential_guid.clear();

        Collections.sort(temp, new CredentialLabelSort(method));
        for (int i = 0; i < temp.size(); i++) {
            credential_guid.put(temp.get(i).getGuid(), i);
        }

        credentials.clear();
        credentials.addAll(temp);
    }

    public void addCredential(Credential credential) {
        credentials.add(credential);
        credential_guid.put(credential.getGuid(), credentials.size() - 1);
    }

    public void updateCredential(Credential updatedCredential) {
        for (Credential credential : credentials) {
            if (credential.getGuid().equals(updatedCredential.getGuid())) {
                int index = credentials.indexOf(credential);
                credentials.set(index, updatedCredential);
            }
        }
    }

    public void deleteCredential(Credential updatedCredential) {
        for (Credential credential : credentials) {
            if (credential.getGuid().equals(updatedCredential.getGuid())) {
                credentials.remove(credential);
                credential_guid.remove(updatedCredential.getGuid());
                break;
            }
        }
    }

    public static Vault getVaultByGuid(String guid) {
        HashMap<String, Vault> vaults = (HashMap<String, Vault>) SingleTon.getTon().getExtra(SettingValues.VAULTS.toString());

        if (vaults != null) {
            return vaults.get(guid);
        }
        return null;
    }

    public static String asJson(Vault vault) throws JSONException {
        if (vault == null) {
            return "";
        }

        JSONObject obj = new JSONObject();
        obj.put("vault_id", vault.vault_id);
        obj.put("guid", vault.guid);
        obj.put("name", vault.name);
        obj.put("created", vault.created);
        obj.put("public_sharing_key", vault.public_sharing_key);
        obj.put("last_access", vault.last_access);
        if (vault.getCredentials() != null) {
            JSONArray credentialArr = new JSONArray();
            for (Credential credential : vault.getCredentials()) {
                try {
                    credentialArr.put(credential.getAsJSONObject());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            obj.put("credentials", credentialArr);
        } else {
            obj.put("challenge_password", vault.challenge_password);
        }
        return obj.toString();
    }

    public static void updateAutofillVault(Vault vault, SharedPreferences settings) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (settings.getString(SettingValues.AUTOFILL_VAULT_GUID.toString(), "").equals(vault.guid)) {
                try {
                    settings.edit().putString(SettingValues.AUTOFILL_VAULT.toString(), Vault.asJson(vault)).apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getFilterableAttribute() {
        return this.name.toLowerCase();
    }
}
