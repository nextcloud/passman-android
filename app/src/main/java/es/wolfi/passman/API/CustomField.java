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

import org.json.JSONException;
import org.json.JSONObject;

public class CustomField extends Core {

    private String label;
    private String value = null;
    private JSONObject jvalue = null;
    private boolean secret;
    private String field_type;

    public CustomField() {
    }

    public CustomField(JSONObject o) throws JSONException {
        label = o.getString("label");
        secret = o.getBoolean("secret");
        field_type = o.getString("field_type");
        if (field_type.equals("file")) {
            jvalue = o.getJSONObject("value");
        } else {
            value = o.getString("value");
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public JSONObject getJvalue() {
        return jvalue;
    }

    public void setJValue(JSONObject jvalue) {
        this.jvalue = jvalue;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public String getFieldType() {
        return field_type;
    }

    public void setFieldType(String field_type) {
        this.field_type = field_type;
    }

    public JSONObject getAsJSONObject() throws JSONException {
        JSONObject fileObject = new JSONObject();
        fileObject.put("label", label);
        fileObject.put("secret", secret);
        fileObject.put("field_type", field_type);
        if (field_type.equals("file")) {
            fileObject.put("value", jvalue);
        } else {
            fileObject.put("value", value);
        }

        return fileObject;
    }
}
