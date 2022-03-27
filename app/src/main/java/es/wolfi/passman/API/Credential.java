/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2022, Timo Triebensky (timo@binsky.org)
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

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import es.wolfi.app.passman.R;
import es.wolfi.utils.Filterable;

public class Credential extends Core implements Filterable {
    public int id;


    protected String guid;
    protected int vaultId;
    protected String userId;
    protected String label;
    protected String description;
    protected long created;
    protected long changed;
    protected String tags;
    protected String email;
    protected String username;
    protected String password;
    protected String url;
    protected String favicon;
    protected long renewInterval;
    protected long expireTime;
    protected long deleteTime;
    protected String files;
    protected String customFields;
    protected String otp;
    protected boolean hidden;
    protected String sharedKey;
    private String sharedKeyDecrypted;
    protected String compromised;

    protected Vault vault;

    private ArrayList<String> _encryptedFields = new ArrayList<String>() {
        {
            add("description");
            add("username");
            add("password");
            add("files");
            add("custom_fields");
            add("otp");
            add("email");
            add("tags");
            add("url");
            add("compromised");
        }
    };

    public int getId() {
        return id;
    }

    public String getGuid() {
        return guid;
    }

    public int getVaultId() {
        return vaultId;
    }

    public String getUserId() {
        return userId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return decryptString(description);
    }

    public void setDescription(String description) {
        this.description = encryptString(description);
    }

    public long getCreated() {
        return created;
    }

    public long getChanged() {
        return changed;
    }

    public void setChanged(long changed) {
        this.changed = changed;
    }

    public String getTags() {
        return decryptString(tags);
    }

    public void setTags(String tags) {
        this.tags = encryptString(tags);
    }

    public String getEmail() {
        return decryptString(email);
    }

    public void setEmail(String email) {
        this.email = encryptString(email);
    }

    public String getUsername() {
        return decryptString(username);
    }

    public void setUsername(String username) {
        this.username = encryptString(username);
    }

    public String getPassword() {
        return decryptString(password);
    }

    public void setPassword(String password) {
        this.password = encryptString(password);
    }

    public String getUrl() {
        return decryptString(url);
    }

    public void setUrl(String url) {
        this.url = encryptString(url);
    }

    public String getFavicon() {
        return favicon;
    }

    public void setFavicon(String favicon) {
        this.favicon = favicon;
    }

    public long getRenewInterval() {
        return renewInterval;
    }

    public void setRenewInterval(long renewInterval) {
        this.renewInterval = renewInterval;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public long getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(long deleteTime) {
        this.deleteTime = deleteTime;
    }

    public List<File> getFilesList() {
        String fileString = this.getFiles();
        List<File> fileList = new ArrayList<File>();

        if (fileString != null && !fileString.equals("[]") && !fileString.equals("")) {
            try {
                JSONArray files = new JSONArray(fileString);
                for (int i = 0; i < files.length(); i++) {
                    JSONObject o = files.getJSONObject(i);
                    fileList.add(new File(o));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return fileList;
    }

    public String getFiles() {
        return decryptString(files);
    }

    public void setFiles(String files) {
        if (files.equals("")) {
            this.files = encryptString(files);
            return;
        }
        this.files = encryptRawStringData(files);
    }

    public List<CustomField> getCustomFieldsList() {
        String customFieldsString = this.getCustomFields();
        List<CustomField> customFieldsList = new ArrayList<CustomField>();

        if (customFieldsString != null && !customFieldsString.equals("[]") && !customFieldsString.equals("")) {
            try {
                JSONArray customFields = new JSONArray(customFieldsString);
                for (int i = 0; i < customFields.length(); i++) {
                    JSONObject o = customFields.getJSONObject(i);
                    customFieldsList.add(new CustomField(o));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return customFieldsList;
    }

    public String getCustomFields() {
        return decryptString(customFields);
    }

    public void setCustomFields(String customFields) {
        if (customFields.equals("")) {
            this.customFields = encryptString(customFields);
            return;
        }
        this.customFields = encryptRawStringData(customFields);
    }

    public String getOtp() {
        return decryptString(otp);
    }

    public void setOtp(String otp) {
        this.otp = encryptString(otp);
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public String getCompromised() {
        if (compromised != null && !compromised.equals("null")) {
            String decryptedCompromised = decryptString(compromised);
            if (decryptedCompromised != null && !decryptedCompromised.equals("")) {
                return decryptedCompromised;
            }
        }
        return "false";
    }

    public void setCompromised(boolean compromised) {
        this.compromised = encryptRawStringData(compromised ? "true" : "false");
    }

    public Vault getVault() {
        return vault;
    }

    public void setVault(Vault v) {
        vault = v;
        vaultId = vault.vault_id;
    }

    public String encryptString(String plaintext) {
        if (this.isEncryptedWithSharedKey()) {
            return vault.encryptString(plaintext, this.sharedKeyDecrypted);
        }
        return vault.encryptString(plaintext);
    }

    public String encryptRawStringData(String plaintext) {
        if (this.isEncryptedWithSharedKey()) {
            return vault.encryptRawStringData(plaintext, this.sharedKeyDecrypted);
        }
        return vault.encryptRawStringData(plaintext);
    }

    public String decryptString(String cryptogram) {
        if (this.isEncryptedWithSharedKey()) {
            return vault.decryptString(cryptogram, this.sharedKeyDecrypted);
        }
        return vault.decryptString(cryptogram);
    }

    private boolean isEncryptedWithSharedKey() {
        if (this.sharedKeyDecrypted == null && this.sharedKey != null && this.sharedKey.length() > 1 && !this.sharedKey.equals("null")) {
            this.sharedKeyDecrypted = vault.decryptString(this.sharedKey);
        }
        return this.sharedKeyDecrypted != null && this.sharedKeyDecrypted.length() > 1 && !this.sharedKeyDecrypted.equals("null");
    }

    public void resetDecryptedSharedKey() {
        this.sharedKeyDecrypted = null;
    }

    public JSONObject getAsJSONObject() throws JSONException {
        JSONObject params = new JSONObject();

        JSONObject icon = null;

        try {
            if (favicon != null && !favicon.equals("") && !favicon.equals("null")) {
                icon = new JSONObject(favicon);
            } else {
                icon = new JSONObject();
                icon.put("type", false);
                icon.put("content", "");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        params.put("user_id", getUserId());
        params.put("credential_id", getId());
        params.put("guid", getGuid());
        params.put("shared_key", getSharedKey());
        params.put("vault_id", getVaultId());
        params.put("label", label);
        params.put("description", description);
        params.put("created", getCreated());
        params.put("changed", getChanged());
        params.put("tags", tags);
        params.put("email", email);
        params.put("icon", icon);
        params.put("username", username);
        params.put("password", password);
        params.put("url", url);
        params.put("renew_interval", getRenewInterval());
        params.put("expire_time", getExpireTime());
        params.put("delete_time", getDeleteTime());
        params.put("files", files);
        params.put("custom_fields", customFields);
        params.put("otp", otp);
        params.put("compromised", compromised);
        params.put("hidden", isHidden() ? 1 : 0);

        return params;
    }

    public JSONObject getAsJsonObjectForApiRequest(boolean forUpdate) throws JSONException {
        JSONObject params = new JSONObject();
        JSONObject icon = null;

        if (forUpdate) {
            params.put("credential_id", getId());
            params.put("guid", getGuid());

            if (favicon != null && !favicon.equals("") && !favicon.equals("null")) {
                try {
                    icon = new JSONObject(favicon);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                icon = new JSONObject();
                icon.put("type", false);
                icon.put("content", "");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        params.put("vault_id", getVaultId());
        params.put("label", label);
        params.put("description", description);
        params.put("created", getCreated());
        params.put("changed", getChanged());
        params.put("tags", tags);
        params.put("email", email);
        params.put("icon", icon);
        params.put("username", username);
        params.put("password", password);
        params.put("url", url);
        params.put("renew_interval", getRenewInterval());
        params.put("expire_time", getExpireTime());
        params.put("delete_time", getDeleteTime());
        params.put("files", files);
        params.put("custom_fields", customFields);
        params.put("otp", otp);
        params.put("compromised", compromised);
        params.put("hidden", isHidden());

        return params;
    }

    public static Credential fromJSON(JSONObject j) throws JSONException {
        Credential c = new Credential();

        c.id = j.getInt("credential_id");
        c.guid = j.getString("guid");
        c.vaultId = j.getInt("vault_id");
        c.userId = j.getString("user_id");
        c.label = j.getString("label");
        c.description = j.getString("description");
        c.created = j.getLong("created");
        c.changed = j.getLong("changed");
        c.tags = j.getString("tags");
        c.email = j.getString("email");
        c.username = j.getString("username");
        c.password = j.getString("password");
        c.url = j.getString("url");
        c.compromised = j.getString("compromised");

        try {
            if (j.has("favicon")) {
                c.favicon = j.getString("favicon");
            } else if (j.has("icon")) {
                c.favicon = j.getString("icon");
            }
        } catch (JSONException ex) {
            try {
                c.favicon = j.getString("icon");
            } catch (JSONException ex2) {
                Log.e("Credential parse", "error, it has no icon or favicon field!", ex2);
            }
        }

        if (j.isNull("renew_interval")) {
            c.renewInterval = 0;
        } else {
            c.renewInterval = j.getLong("renew_interval");
        }

        c.expireTime = j.getLong("expire_time");
        c.deleteTime = j.getLong("delete_time");
        c.files = j.getString("files");
        c.customFields = j.getString("custom_fields");
        c.otp = j.getString("otp");
        c.hidden = (j.getInt("hidden") > 0);
        c.sharedKey = j.getString("shared_key");

        return c;
    }

    public static Credential fromJSON(JSONObject j, Vault v) throws JSONException {
        Credential c = Credential.fromJSON(j);
        c.setVault(v);
        return c;
    }

    public static Credential clone(Credential input) throws JSONException {
        return Credential.fromJSON(input.getAsJSONObject(), input.getVault());
    }

    public void save(Context c, final AsyncHttpResponseHandler responseHandler) {
        try {
            requestAPI(c, "credentials", getAsJsonObjectForApiRequest(false), "POST", responseHandler);
        } catch (MalformedURLException | JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void update(Context c, final AsyncHttpResponseHandler responseHandler) {
        try {
            requestAPI(c, "credentials/" + getGuid(), getAsJsonObjectForApiRequest(true), "PATCH", responseHandler);
        } catch (MalformedURLException | JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void sendFileDeleteRequest(Context c, int file_id, final AsyncHttpResponseHandler responseHandler) {
        JSONObject params = new JSONObject();
        try {
            requestAPI(c, "file/" + file_id, params, "DELETE", responseHandler);
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void uploadFile(Context c, String encodedFile, String fileName, String mimeType, int fileSize, final AsyncHttpResponseHandler responseHandler, ProgressDialog progress) {
        JSONObject params = new JSONObject();

        progress.setMessage(c.getString(R.string.wait_while_encrypting));
        try {
            params.put("filename", encryptString(fileName));
            params.put("data", encryptRawStringData(encodedFile));
            params.put("mimetype", mimeType);
            params.put("size", fileSize);
            progress.setMessage(c.getString(R.string.wait_while_uploading));
            requestAPI(c, "file", params, "POST", responseHandler);
        } catch (MalformedURLException | JSONException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFilterableAttribute() {
        return getLabel().toLowerCase();
    }
}
