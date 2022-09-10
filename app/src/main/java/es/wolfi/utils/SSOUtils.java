/**
 * Passman Android App
 *
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

package es.wolfi.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.nextcloud.android.sso.model.FilesAppType;

import java.util.Arrays;
import java.util.List;

/**
 * Utils for Nextcloud files app based SSO
 */
public class SSOUtils {

    /**
     * Checks whether a supported Nextcloud files app is installed or not.
     *
     * @param context Context to get packageManager
     * @return whether a supported Nextcloud files app is installed or not
     */
    public static boolean isNextcloudFilesAppInstalled(Context context) {
        List<String> APPS = Arrays.asList(FilesAppType.PROD.packageId, FilesAppType.DEV.packageId);

        boolean returnValue = false;
        PackageManager pm = context.getPackageManager();
        for (String app : APPS) {
            try {
                PackageInfo pi = pm.getPackageInfo(app, PackageManager.GET_ACTIVITIES);
                // Nextcloud Files app version 30180090 is required by the used SSO library
                if ((pi.versionCode >= 30180090 && pi.packageName.equals("com.nextcloud.client")) ||
                        pi.versionCode >= 20211118 && pi.packageName.equals("com.nextcloud.android.beta")) {
                    returnValue = true;
                    break;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return returnValue;
    }
}
