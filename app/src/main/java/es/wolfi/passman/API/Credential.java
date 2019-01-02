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

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

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

    protected Vault vault;

    public int getId() {
        return id;
    }

    public String getGuid() {
        return guid;
    }

    public int getVaultId() {
        return vaultId;
    }

    public String getUserId() { return userId; }

    public Credential()
    {
        super();
        this.userId = Core.username;
    }

    public String getLabel() {
        return label;
    }

    public Credential setLabel(String label) {
        this.label = label;
	    return this;
    }

    public String getDescription() {
        return vault.decryptString(description);
    }

    public Credential setDescription(String description) {
        this.description = vault.encryptString(description);
	    return this;
    }

    public long getCreated() {
        return created;
    }

    public long getChanged() {
        return changed;
    }

    public Credential setChanged(long changed) {
        this.changed = changed;
	    return this;
    }

    public String getTags() {
        return vault.decryptString(tags);
    }

    public Credential setTags(String tags) {
        this.tags = vault.encryptString(tags);
	    return this;
    }

    public String getEmail() {
        return vault.decryptString(email);
    }

    public Credential setEmail(String email) {
        this.email = vault.encryptString(email);
	    return this;
    }

    public String getUsername() {
        return vault.decryptString(username);
    }

    public Credential setUsername(String username) {
        this.username = vault.encryptString(username);
	    return this;
    }

    public String getPassword() {
        return vault.decryptString(password);
    }

    public Credential setPassword(String password) {
        this.password = vault.encryptString(password);
	    return this;
    }

    public String getUrl() {
        return vault.decryptString(url);
    }

    public Credential setUrl(String url) {
        this.url = vault.encryptString(url);
	return this;
    }

    public String getFavicon() {
        return favicon;
    }

    public Credential setFavicon(String favicon) {
        this.favicon = favicon;
	    return this;
    }

    public long getRenewInterval() {
        return renewInterval;
    }

    public Credential setRenewInterval(long renewInterval) {
        this.renewInterval = renewInterval;
	    return this;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public Credential setExpireTime(long expireTime) {
        this.expireTime = expireTime;
	    return this;
    }

    public long getDeleteTime() {
        return deleteTime;
    }

    public Credential setDeleteTime(long deleteTime) {
        this.deleteTime = deleteTime;
	    return this;
    }

    public String getFiles() {
        return vault.decryptString(files);
    }

    public Credential setFiles(String files) {
        this.files = vault.encryptString(files);
	    return this;
    }

    public String getCustomFields() {
        return vault.decryptString(customFields);
    }

    public Credential setCustomFields(String customFields) {
        this.customFields = vault.encryptString(customFields);
	    return this;
    }

    public String getOtp() {
        return vault.decryptString(otp);
    }

    public Credential setOtp(String otp) {
        this.otp = vault.encryptString(otp);
	    return this;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Credential setHidden(boolean hidden) {
        this.hidden = hidden;
	    return this;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public Credential setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
	    return this;
    }

    public Vault getVault() {
        return vault;
    }

    public Credential setVault(Vault v) {
        vault = v;
        vaultId = v.vault_id;
	    return this;
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

        try {
            c.favicon = j.getString("favicon");
        }
        catch (JSONException ex) {
            try {
                c.favicon = j.getString("icon");
            }
            catch (JSONException ex2) {
                Log.e("Credential parse", "error, it has no icon or favicon field!", ex2);
            }
        }

        if (j.isNull("renew_interval")) {
            c.renewInterval = 0;
        }
        else {
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
    
    // Treat null objects as JSONObject.NULL
    private static Object ValueObjectOrNULLObject(Object valueObject)
    {
        if (valueObject == null)
            return JSONObject.NULL;
        
        return valueObject;
    }

    public static JSONObject toJSON(Credential c, boolean forCreation) {
        try {
            JSONObject j = new JSONObject();

            if (!forCreation) {
                j.put("credential_id", ValueObjectOrNULLObject(c.id));
                j.put("guid", ValueObjectOrNULLObject(c.guid));
                //j.put("created", ValueObjectOrNULLObject(c.created));
                //j.put("changed", ValueObjectOrNULLObject(c.changed));
            }

            j.put("vault_id", ValueObjectOrNULLObject(c.vaultId));
            j.put("user_id", ValueObjectOrNULLObject(c.userId));
            j.put("label", ValueObjectOrNULLObject(c.label));
            j.put("description", ValueObjectOrNULLObject(c.description));

            j.put("tags", ValueObjectOrNULLObject(c.tags));
            j.put("email", ValueObjectOrNULLObject(c.email));
            j.put("username", ValueObjectOrNULLObject(c.username));
            j.put("password", ValueObjectOrNULLObject(c.password));
            j.put("url", ValueObjectOrNULLObject(c.url));

            j.put("favicon", ValueObjectOrNULLObject(c.favicon));

            j.put("icon", ValueObjectOrNULLObject(c.favicon));

            j.put("renew_interval", ValueObjectOrNULLObject(c.renewInterval));

            j.put("expire_time", ValueObjectOrNULLObject(c.expireTime));
            j.put("delete_time", ValueObjectOrNULLObject(c.deleteTime));
            j.put("files", ValueObjectOrNULLObject(c.files));
            j.put("custom_fields", ValueObjectOrNULLObject(c.customFields));
            j.put("otp", ValueObjectOrNULLObject(c.otp));
            j.put("hidden", ValueObjectOrNULLObject(c.hidden ? 1 : 0));
            j.put("shared_key", ValueObjectOrNULLObject(c.sharedKey));

            return j;
        }
        catch (Exception ex)
        {
            Log.d("Passman", ex.toString());
        }
        return new JSONObject();
    }

    public static Credential fromJSON(JSONObject j, Vault v) throws JSONException {
        Credential c = Credential.fromJSON(j);
        c.setVault(v);
        return c;
    }

    @Override
    public String getFilterableAttribute() {
        return getLabel().toLowerCase();
    }
}
