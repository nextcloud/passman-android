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

/**
 * Util implementation for color calculations.
 */
public class ColorUtils {

    /**
     * calculates a color for a given name.
     *
     * @param name the name String
     * @return the calculated color
     */
    public static int calculateColor(String name) {
        return calculateColor(name, false);
    }

    /**
     * calculates a color for a given name.
     *
     * @param name           the name String
     * @param darkBackground lighten the color to be readable on a dark background (dark mode)
     * @return the calculated color
     */
    public static int calculateColor(String name, boolean darkBackground) {
        int hash = name.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;

        //pastelize color
        int mix = darkBackground ? 255 : 127;
        r = (r + mix) / 2;
        g = (g + mix) / 2;
        b = (b + mix) / 2;

        return Color.rgb(r, g, b);
    }
}
