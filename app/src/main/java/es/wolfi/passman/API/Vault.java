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
import android.util.Base64;
import android.util.Log;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import es.wolfi.app.passman.SJCLCrypto;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.utils.Filterable;
import es.wolfi.utils.GeneralUtils;
import es.wolfi.utils.JSONUtils;

public class Vault extends Core implements Filterable {
    public int vault_id;
    public String guid;
    public String name;
    public double created;
    public String public_sharing_key;
    public double last_access;
    public String challenge_password;

    ArrayList<Credential> credentials;
    ConcurrentHashMap<String, Integer> credential_guid;

    private String encryption_key = "";

    public void setEncryptionKey(String k) {
        encryption_key = k;
    }

    public String decryptString(String cryptogram) {
        try {
            return SJCLCrypto.decryptString(cryptogram, encryption_key);
        } catch (Exception e) {
            Log.e("Vault", e.toString());
        }
        return "Error decrypting";
    }

    public String encryptString(String plaintext) {
        try {
            // We can't encrypt NULL
            if (plaintext == null) {
                plaintext = "";
            }

            // If this is just an unquoted string then we need to quote it
            if (!JSONUtils.isJSONObject(plaintext) && !JSONUtils.isJSONArray(plaintext)) {
                plaintext = JSONObject.quote(plaintext);
            }

            // Encrypt the plaintext into an SJCL compatible JSON object

            JSONObject SJCLJSONObject = SJCLCrypto.encryptString(plaintext, encryption_key);

            // JSON String representation of the SJCL JSON Object
            String encryptedString = SJCLJSONObject.toString();

            // SJCL doesn't like escaped forward slash - dirty hack to remove them
            encryptedString = encryptedString.replace(
                    "\\",
                    "");

            // Finally base 64 encode it
            return Base64.encodeToString(encryptedString
                            .getBytes("UTF-8"),
                    Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e("Vault", e.getMessage());
            e.printStackTrace();
        }
        return "Error encrypting";
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

    public boolean unlockReplacementInstance() {
        boolean ret = false;
        try {
            Vault v = getVaultByGuid(guid);

            if (v != null && !v.is_unlocked()) {
                return v.unlock(encryption_key);
            }
            else  if (v != null && v.is_unlocked()) {
                return true;
            }

        } catch (Exception ex) {
            GeneralUtils.debug("Exception unlocked: " + ex.toString());
            ret = false;
        }
        return ret;
    }

    public void lock() {
        encryption_key = "";
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

    public ArrayList<Credential> getCredentials() {
        return credentials;
    }

    // Static functions

    // JSON handlers and Vault Builders

    protected static Vault fromJSON(JSONObject o, Vault v) throws JSONException {

        if (v == null) {
            v = new Vault();
        }

        v.vault_id = o.getInt("vault_id");
        v.guid = o.getString("guid");
        v.name = o.getString("name");
        v.created = o.getDouble("created");
        v.public_sharing_key = o.getString("public_sharing_key");
        v.last_access = o.getDouble("last_access");

        if (o.has("credentials")) {

            JSONArray j = o.getJSONArray("credentials");

            if (v.credentials == null) {
                v.credentials = new ArrayList<>();
            }

            if (v.credential_guid == null) {
                v.credential_guid = new ConcurrentHashMap<>();
            }

            for (int i = 0; i < j.length(); i++) {
                Credential c = Credential.fromJSON(j.getJSONObject(i), v);
                if (c.getDeleteTime() == 0) {
                    if (!v.credential_guid.containsKey(c.getGuid())) {
                        v.credentials.add(c);
                        v.credential_guid.put(c.getGuid(), v.credentials.size() - 1);
                    }
                }
            }
            v.challenge_password = v.credentials.get(0).password;
        } else {
            v.challenge_password = o.getString("challenge_password");
        }

        return v;
    }

    protected static Vault fromJSON(JSONObject o) throws JSONException {
        Vault v = new Vault();
        Vault.fromJSON(o, v);
        return v;
    }

    // Credential API Wrappers

    public static void addCredential(boolean wait,
                                     final Context c,
                                     final Vault v,
                                     final Credential cred,
                                     final FutureCallback<Credential> cb) {

        Log.d(Vault.LOG_TAG, "Adding Credential");

        Future<String> future = Vault.requestAPIMethod(c,
                "credentials",
                "POST",
                Credential.toJSON(cred, true).toString(),
                new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if (e != null) {
                            Log.d(Vault.LOG_TAG, e.toString());
                            cb.onCompleted(e, null);
                            return;
                        }

                        try {
                            // Unfortunate the result from the POST API Call
                            // is Bogus so we call another API to get the real credential
                            // back
                            // The guid is valid, so we can use that
                            // TODO: Fix PATCH credential API
                            Credential badCred = Credential.fromJSON(new JSONObject(result));
                            Vault.refreshCredential(true, c, v, badCred, null);

                            Credential newcred = v.findCredentialByGUID(badCred.getGuid());

                            cb.onCompleted(e, newcred);
                        } catch (Exception ex) {
                            Log.d(Vault.LOG_TAG, ex.toString());
                            cb.onCompleted(ex, null);
                        }
                    }
                });

        if (wait) {
            if (future != null) {
                try {
                    Log.d(Vault.LOG_TAG, "Add: Waiting for the future");
                    future.get();
                    Log.d(Vault.LOG_TAG, "Add: Back to the future");
                }
                catch (Exception ex) {
                    Log.d(Vault.LOG_TAG, "Add: Error from the future: " + ex.toString());
                }
            }
        }
    }

    public static void refreshCredential(boolean wait,
                                         final Context c,
                                         final Vault v,
                                         final Credential oldCred,
                                         final FutureCallback<Credential> cb) {

        Log.d(Vault.LOG_TAG, "Refresh Credential");

        Future<String> future = Vault.requestAPIGET(c,
                "credentials/" + oldCred.getGuid(),
                new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if (e != null) {
                            Log.d(Vault.LOG_TAG, e.toString());
                            if (cb != null) {
                                cb.onCompleted(e, null);
                            }
                            return;
                        }
                        try {
                            // Clean up old reference
                            // v.credential_guid.remove(oldCred.getGuid());
                            // v.credentials.remove(oldCred);

                            // Create the new object
                            Credential newCred = Credential.fromJSON(new JSONObject(result), v, oldCred);

                            // And add it back into the Vault
                            if (!v.credential_guid.containsKey(newCred.getGuid())) {
                                v.credentials.add(newCred);
                                v.credential_guid.put(newCred.getGuid(), v.credentials.size() - 1);
                            }

                            if (cb != null) {
                                cb.onCompleted(e, newCred);
                            }
                        } catch (JSONException ex) {
                            Log.d(Vault.LOG_TAG, ex.toString());
                            if (cb != null) {
                                cb.onCompleted(e, null);
                            }
                        }
                        Log.d(Vault.LOG_TAG, "Get: Callback completed");
                    }
                });


        if (wait) {
            if (future != null) {
                try {
                    Log.d(Vault.LOG_TAG, "Get: Waiting for the future");
                    future.get();
                    Log.d(Vault.LOG_TAG, "Get: Back to the future");
                }
                catch (Exception ex) {
                    Log.d(Vault.LOG_TAG, "Get: Error from the future: " + ex.toString());
                }
            }
        }
    }

    public static void saveCredential(boolean wait,
                                      final Context c,
                                      final Vault v,
                                      final Credential cred,
                                      final FutureCallback<Credential> cb) {

        Log.d(Vault.LOG_TAG, "Saving Credential");

        Future<String> future = Vault.requestAPIMethod(c,
                "credentials/" + cred.getGuid(),
                "PATCH",
                Credential.toJSON(cred, false).toString(),
                new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if (e != null) {
                            Log.d(Vault.LOG_TAG, e.toString());
                            if (cb != null) {
                                cb.onCompleted(e, null);
                            }
                            return;
                        }

                        try {
                            // Unfortunate the result from the PATCH API Call
                            // is Bogus so we call another API to get the real credential
                            // back
                            // TODO: Fix PATCH credential API

                            Vault.refreshCredential(true, c, v, cred, null);

                            Credential newcred = v.findCredentialByGUID(cred.getGuid());

                            if (cb != null) {
                                cb.onCompleted(e, newcred);
                            }
                        } catch (Exception ex) {
                            Log.d(Vault.LOG_TAG, ex.toString());
                            if (cb != null) {
                                cb.onCompleted(ex, null);
                            }
                        }
                    }
                });

        if (wait) {
            if (future != null) {
                try {
                    Log.d(Vault.LOG_TAG, "Save: Waiting for the future");
                    future.get();
                    Log.d(Vault.LOG_TAG, "Save: Back to the future");
                } catch (Exception ex) {
                    Log.d(Vault.LOG_TAG, "Save: Error from the future: " + ex.toString());
                }
            }
        }
    }

    // Vault Helpers

    public static void getVaults(Context c, final FutureCallback<ConcurrentHashMap<String, Vault>> cb) {
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
                    ConcurrentHashMap<String, Vault> l = getAllVaults();

                    if (l == null) {
                        l = new ConcurrentHashMap<>();
                    }

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject o = data.getJSONObject(i);
                        Vault v = getVaultByGuid(o.getString("guid"));
                        v = Vault.fromJSON(o, v);
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
                    Vault v = getVaultByGuid(data.getString("guid"));
                    v = Vault.fromJSON(data, v);
                    getAllVaults().put(v.guid, v);


                    cb.onCompleted(e, v);
                } catch (JSONException ex) {
                    cb.onCompleted(ex, null);
                }
            }
        });
    }

    public static ConcurrentHashMap<String,Vault> getAllVaults() {
        return (ConcurrentHashMap<String, Vault>)
            SingleTon.getTon().getExtra(SettingValues.VAULTS.toString());
    }

    public static void setAllVaults(ConcurrentHashMap<String,Vault> vaults) {
        SingleTon.getTon().addExtra(SettingValues.VAULTS.toString(), vaults);
    }

    public static Vault getVaultByGuid(String guid) {
        ConcurrentHashMap<String, Vault> vaults = getAllVaults();

        if (vaults != null)
        {
            return vaults.get(guid);
        }
        return null;
    }

    public static Vault getActiveVault() {
        return (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
    }

    public static void setActiveVault(Vault activeVault) {
        SingleTon.getTon().addExtra(SettingValues.ACTIVE_VAULT.toString(), activeVault);
    }

    public static void unsetActiveVault() {
        SingleTon.getTon().removeExtra(SettingValues.ACTIVE_VAULT.toString());
    }

    @Override
    public String getFilterableAttribute() {
        return this.name.toLowerCase();
    }
}
