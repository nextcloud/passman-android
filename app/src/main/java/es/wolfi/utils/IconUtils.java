/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2021, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2021, Marcos Zuriaga Miguel (wolfi@wolfi.es)
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

package es.wolfi.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.util.Base64;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;

import es.wolfi.app.passman.R;

public class IconUtils {
    public static void loadIconToImageView(String favicon, ImageView credentialIconImageView) {
        credentialIconImageView.setImageResource(R.drawable.ic_baseline_lock_24);
        if (favicon != null && !favicon.equals("") && !favicon.equals("null")) {
            try {
                JSONObject icon = new JSONObject(favicon);
                if (!icon.getString("type").equals("false") && !icon.getString("content").equals("")) {
                    byte[] byteImageData = Base64.decode(icon.getString("content"), Base64.DEFAULT);
                    Bitmap bitmapImageData = BitmapFactory.decodeByteArray(byteImageData, 0, byteImageData.length);
                    if (bitmapImageData != null) {
                        credentialIconImageView.setImageBitmap(bitmapImageData);
                    } else if (icon.getString("type").equals("svg+xml") &&
                            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SVG svg = SVG.getFromInputStream(new ByteArrayInputStream(byteImageData));
                        credentialIconImageView.setImageDrawable(new PictureDrawable(svg.renderToPicture()));
                    }
                }
            } catch (JSONException | SVGParseException e) {
                e.printStackTrace();
            }
        }
    }
}
