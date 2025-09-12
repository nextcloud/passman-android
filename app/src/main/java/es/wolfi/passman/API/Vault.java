/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2021, Timo Triebensky (timo@binsky.org)
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
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.koushikdutta.async.future.FutureCallback;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.OfflineStorageValues;
import es.wolfi.app.passman.SJCLCrypto;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.utils.CredentialLabelSort;
import es.wolfi.utils.Filterable;
import es.wolfi.utils.KeyStoreUtils;

public class Vault extends Core implements Filterable {
    public int vault_id;
    public String guid;
    public String name;
    public double created;
    public String public_sharing_key;
    public double last_access;
    public String challenge_password;
    public int sharing_keys_generated;
    public boolean delete_request_pending;
    public JSONObject vault_settings = null;
    public static Integer[] keyStrengths = {1024, 2048, 4096};

    ArrayList<Credential> credentials;
    HashMap<String, Integer> credential_guid = new HashMap<>();

    private String encryption_key = "";

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

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
        if (credential_guid != null && guid != null) {
            Integer arrPos = credential_guid.get(guid);

            Log.e("Vault", "GUID: ".concat(guid).concat(" Arr pos: ").concat(String.valueOf(arrPos)));
            if (arrPos != null) {
                return credentials.get(arrPos);
            }
        }
        return null;
    }

    public Date getLastAccessTime() {
        return new Date((long) last_access * 1000);
    }

    public ArrayList<Credential> getCredentials() {
        return credentials;
    }

    public static void getVaults(Context c, final FutureCallback<HashMap<String, Vault>> cb) {
        Vault.requestAPIGET(c, "vaults", new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if (e != null) {
                    Log.d("vaults cached", OfflineStorage.getInstance().has(OfflineStorageValues.VAULTS.toString()) ? "yes" : "no");
                    if (OfflineStorage.getInstance().isEnabled() && OfflineStorage.getInstance().has(OfflineStorageValues.VAULTS.toString())) {
                        result = OfflineStorage.getInstance().getString(OfflineStorageValues.VAULTS.toString(), null);
                    }
                    if (result == null || !OfflineStorage.getInstance().isEnabled() ||
                            !OfflineStorage.getInstance().has(OfflineStorageValues.VAULTS.toString())) {
                        cb.onCompleted(e, null);
                        return;
                    }
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

                    OfflineStorage.getInstance().putObject(OfflineStorageValues.VAULTS.toString(), result);
                    OfflineStorage.getInstance().commit();
                    cb.onCompleted(null, l);
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
                    if (OfflineStorage.getInstance().isEnabled() && OfflineStorage.getInstance().has(guid)) {
                        result = OfflineStorage.getInstance().getString(guid, null);
                    }
                    if (result == null || !OfflineStorage.getInstance().isEnabled() ||
                            !OfflineStorage.getInstance().has(guid)) {
                        cb.onCompleted(e, null);
                        return;
                    }
                }

                try {
                    JSONObject data = new JSONObject(result);
                    Vault v = Vault.fromJSON(data);
                    OfflineStorage.getInstance().putObject(guid, result);
                    //cb.onCompleted(null, v);
                    v.loadCredentialsSharedWithUs(c, cb);
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

        if (o.has("credentials") && !o.getString("credentials").equals("null")) {
            JSONArray j = o.getJSONArray("credentials");
            v.credentials = new ArrayList<Credential>();
            v.credential_guid = new HashMap<>();

            for (int i = 0; i < j.length(); i++) {
                Credential c = Credential.fromJSON(j.getJSONObject(i), v);
                if (c.getDeleteTime() == 0) {
                    v.credentials.add(c);
                    v.credential_guid.put(c.getGuid(), v.credentials.size() - 1);
                }
            }
            v.challenge_password = v.credentials.get(0).password;
        } else if (o.has("challenge_password")) {
            v.challenge_password = o.getString("challenge_password");
        }

        if (o.has("vault_settings") && !o.getString("vault_settings").equals("null")) {
            v.vault_settings = new JSONObject(new String(Base64.decode(o.getString("vault_settings"), Base64.DEFAULT)));
        } else {
            v.vault_settings = new JSONObject();
        }

        if (o.has("delete_request_pending")) {
            v.delete_request_pending = o.getBoolean("delete_request_pending");
        } else {
            v.delete_request_pending = false;
        }

        if (o.has("sharing_keys_generated")) {
            v.sharing_keys_generated = o.getInt("sharing_keys_generated");
        }

        return v;
    }

    /**
     * Loads credentials shared with the vault directly into the current vault data structure.
     * Should be called only once after updating vault.credentials.
     */
    public void loadCredentialsSharedWithUs(Context c, final FutureCallback<Vault> cb) {
        Vault self = this;
        Vault.requestAPIGET(c, "sharing/vault/".concat(guid).concat("/get"), new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                final String offlineStorageSharedCredentialsKey = "shared_" + guid;

                if (e != null) {
                    if (OfflineStorage.getInstance().isEnabled() && OfflineStorage.getInstance().has(offlineStorageSharedCredentialsKey)) {
                        result = OfflineStorage.getInstance().getString(offlineStorageSharedCredentialsKey, null);
                    }
                }

                if (result != null) {
                    try {
                        JSONArray data = new JSONArray(result);
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject sharedCredentialData = data.getJSONObject(i);

                            CredentialACL acl = CredentialACL.fromJSON(sharedCredentialData);
                            Credential c = Credential.fromJSON(sharedCredentialData.getJSONObject("credential_data"), self);
                            c.setSharedKey(acl.shared_key);
                            c.setCredentialACL(acl);
                            if (c.getDeleteTime() == 0) {
                                self.credentials.add(c);
                                self.credential_guid.put(c.getGuid(), self.credentials.size() - 1);
                            }
                        }
                        OfflineStorage.getInstance().putObject(offlineStorageSharedCredentialsKey, result);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }

                // successful callback response, even if shared credentials could not be loaded
                // so the user will at least see the main vault content / offline cached version of it
                cb.onCompleted(null, self);
            }
        });
    }

    public void sort(int method, boolean caseInsensitiveSort) {
        credential_guid.clear();
        Collections.sort(credentials, new CredentialLabelSort(method, caseInsensitiveSort));
        for (int i = 0; i < credentials.size(); i++) {
            credential_guid.put(credentials.get(i).getGuid(), i);
        }
    }

    public void addCredential(Credential credential) {
        credentials.add(credential);
        credential_guid.put(credential.getGuid(), credentials.size() - 1);
    }

    public void updateCredential(Credential updatedCredential) {
        int newIndex = -1;
        for (Credential credential : credentials) {
            if (credential.getGuid().equals(updatedCredential.getGuid())) {
                newIndex = credentials.indexOf(credential);
                break;
            }
        }
        if (newIndex >= 0) {
            credentials.set(newIndex, updatedCredential);
        }
    }

    public void deleteCredential(Credential updatedCredential) {
        Credential credentialToRemove = null;
        for (Credential credential : credentials) {
            if (credential.getGuid().equals(updatedCredential.getGuid())) {
                credentialToRemove = credential;
                break;
            }
        }
        if (credentialToRemove != null) {
            credentials.remove(credentialToRemove);
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
        obj.put("delete_request_pending", vault.delete_request_pending);
        obj.put("sharing_keys_generated", vault.sharing_keys_generated);

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

        if (vault.vault_settings != null) {
            obj.put("vault_settings", Base64.encodeToString(vault.vault_settings.toString().getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
        }

        return obj.toString();
    }

    public static JSONObject getAsJsonObjectForApiRequest(Vault vault, boolean forEdit) throws JSONException {
        JSONObject params = new JSONObject();

        params.put("vault_id", vault.vault_id);
        params.put("guid", vault.guid);
        params.put("name", vault.name);
        params.put("created", vault.created);
        params.put("public_sharing_key", vault.public_sharing_key);
        params.put("last_access", vault.last_access);

        if (forEdit) {
            params.put("delete_request_pending", vault.delete_request_pending);
            params.put("sharing_keys_generated", vault.sharing_keys_generated);
            if (vault.vault_settings != null) {
                params.put("vault_settings", Base64.encodeToString(vault.vault_settings.toString().getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
            }
        }

        return params;
    }

    public static void updateAutofillVault(Vault vault, SharedPreferences settings) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (settings.getString(SettingValues.AUTOFILL_VAULT_GUID.toString(), "").equals(vault.guid)) {
                try {
                    KeyStoreUtils.putString(SettingValues.AUTOFILL_VAULT.toString(), Vault.asJson(vault));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateSharingKeys(int keyStrength, Context context, AsyncHttpResponseHandler createInitialSharingKeysResponseHandler) {
        Pair<String, String> keyPair = getNewPEMKeyPair(keyStrength);
        if (keyPair != null) {
            public_sharing_key = keyPair.first;

            try {
                JSONObject params = getAsJsonObjectForApiRequest(this, false);
                params.put("private_sharing_key", encryptRawStringData(keyPair.second));
                Vault.requestAPI(context, "vaults/" + guid + "/sharing-keys", params, "POST", createInitialSharingKeysResponseHandler);
            } catch (MalformedURLException | JSONException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public static Pair<String, String> getNewPEMKeyPair(int keyStrength) {
        Pair<String, String> pairPublicPrivatePEM = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(keyStrength);
            KeyPair keyPair = kpg.generateKeyPair();

            // Convert PublicKey to PEM format
            StringWriter publicWriter = new StringWriter();
            JcaPEMWriter publicPemWriter = new JcaPEMWriter(publicWriter);
            publicPemWriter.writeObject(keyPair.getPublic());
            publicPemWriter.flush();
            publicPemWriter.close();
            String publicPem = publicWriter.toString();

            // Convert PrivateKey to PEM format
            StringWriter privateWriter = new StringWriter();
            JcaPEMWriter privatePemWriter = new JcaPEMWriter(privateWriter);
            privatePemWriter.writeObject(keyPair.getPrivate());
            privatePemWriter.flush();
            privatePemWriter.close();
            String privatePem = privateWriter.toString();

            pairPublicPrivatePEM = new Pair<>(publicPem, privatePem);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        return pairPublicPrivatePEM;
    }

    public void save(Context c, final AsyncHttpResponseHandler responseHandler) {
        JSONObject params = new JSONObject();

        try {
            params.put("vault_name", this.name);
            requestAPI(c, "vaults", params, "POST", responseHandler);
        } catch (MalformedURLException | JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void edit(Context c, final AsyncHttpResponseHandler responseHandler) {
        try {
            JSONObject params = getAsJsonObjectForApiRequest(this, true);
            requestAPI(c, "vaults/" + this.guid, params, "PATCH", responseHandler);
        } catch (MalformedURLException | JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * deleteVaultContents() should be called before delete() to remove vaults credentials and files
     *
     * @param context
     * @param responseHandler
     */
    public void deleteVaultContents(Context context, final AsyncHttpResponseHandler responseHandler) {
        JSONObject collectionToDelete = new JSONObject();
        JSONArray fileIds = new JSONArray();

        for (Credential c : this.getCredentials()) {
            for (File f : c.getFilesList()) {
                fileIds.put(f.getFileId());
            }
        }

        try {
            collectionToDelete.put("file_ids", fileIds);
            requestAPI(context, "files/delete", collectionToDelete, "POST", responseHandler);
        } catch (MalformedURLException | JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * delete() is automatically called by the VaultDeleteResponseHandler when calling deleteVaultContents() with passing the isDeleteVaultContentRequest as true
     *
     * @param context
     * @param responseHandler
     */
    public void delete(Context context, final AsyncHttpResponseHandler responseHandler) {
        try {
            requestAPI(context, "vaults/" + this.guid, new JSONObject(), "DELETE", responseHandler);
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFilterableAttribute() {
        return this.name.toLowerCase();
    }
}
