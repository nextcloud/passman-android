/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2017, Andy Scherzinger
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

package es.wolfi.utils;

import android.graphics.Color;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * Util implementation for color calculations.
 */
public class ColorUtils {

    public static int calculateColor(String name) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        int hash = name.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;

        //pastelize color
        r = (r + 127) / 2;
        g = (g + 127) / 2;
        b = (b + 127) / 2;

        return Color.rgb(r, g, b);
    }
}
