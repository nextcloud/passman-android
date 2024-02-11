package es.wolfi.passman.API;

import org.json.JSONException;
import org.json.JSONObject;

public class CredentialACL {
    protected int acl_id;
    protected int item_id;
    protected String item_guid;
    protected String user_id;
    protected int created;
    protected int expire;
    protected int expire_views;
    protected SharingACL permissions;
    protected int vault_id;
    protected String vault_guid;
    protected String shared_key;     // encrypted field
    protected boolean pending;

    public SharingACL getPermissions() {
        return permissions;
    }

    public static CredentialACL fromJSON(JSONObject o) throws JSONException {
        CredentialACL acl = new CredentialACL();
        acl.acl_id = o.getInt("acl_id");
        acl.item_id = o.getInt("item_id");
        acl.item_guid = o.getString("item_guid");
        acl.user_id = o.getString("user_id");
        acl.created = o.getInt("created");
        acl.expire = o.getInt("expire");
        acl.expire_views = o.getInt("expire_views");
        acl.vault_id = o.getInt("vault_id");
        acl.vault_guid = o.getString("vault_guid");
        acl.shared_key = o.getString("shared_key");
        acl.pending = o.getBoolean("pending");
        acl.permissions = new SharingACL(o.getInt("permissions"));
        return acl;
    }

    public JSONObject getAsJSONObject() throws JSONException {
        JSONObject params = new JSONObject();

        JSONObject permissions = null;

        try {
            permissions = new JSONObject();
            permissions.put("permission", this.permissions.getPermission());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        params.put("acl_id", acl_id);
        params.put("item_id", item_id);
        params.put("item_guid", item_guid);
        params.put("user_id", user_id);
        params.put("created", created);
        params.put("expire", expire);
        params.put("expire_views", expire_views);
        params.put("vault_id", vault_id);
        params.put("vault_guid", vault_guid);
        params.put("shared_key", shared_key);
        params.put("permissions", permissions);
        params.put("pending", pending ? 1 : 0);

        return params;
    }
}
