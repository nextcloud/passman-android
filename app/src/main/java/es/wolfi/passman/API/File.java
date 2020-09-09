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

import com.koushikdutta.async.future.FutureCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class File extends Core {

    public int file_id;
    public String filename;
    public String guid;
    public double size;
    public double created;
    public String mimetype;

    public File(JSONObject o) throws JSONException {
        file_id = o.getInt("file_id");
        filename = o.getString("filename");
        guid = o.getString("guid");
        size = o.getDouble("size");
        created = o.getDouble("created");
        mimetype = o.getString("mimetype");
    }

    public int getFileId() {
        return file_id;
    }

    public String getFilename() {
        return filename;
    }

    public String getGuid() {
        return guid;
    }

    public double getSize() {
        return size;
    }

    public double getCreated() {
        return created;
    }

    public String getMimetype() {
        return mimetype;
    }

    public JSONObject getAsJSONObject() throws JSONException {
        JSONObject fileObject = new JSONObject();
        fileObject.put("file_id", file_id);
        fileObject.put("filename", filename);
        fileObject.put("guid", guid);
        fileObject.put("size", size);
        fileObject.put("created", created);
        fileObject.put("mimetype", mimetype);

        return fileObject;
    }

    public void download(Context context, FutureCallback<String> cb) {
        requestAPIGET(context, "file/" + file_id, cb);
    }
}
