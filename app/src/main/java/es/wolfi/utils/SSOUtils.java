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

import com.nextcloud.android.sso.FilesAppTypeRegistry;
import com.nextcloud.android.sso.helper.VersionCheckHelper;

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
        final int MIN_NEXTCLOUD_FILES_APP_VERSION_CODE_PROD = 30180090;
        final int MIN_NEXTCLOUD_FILES_APP_VERSION_CODE_DEV = 20211118;

        return VersionCheckHelper.verifyMinVersion(
                context,
                MIN_NEXTCLOUD_FILES_APP_VERSION_CODE_PROD,
                FilesAppTypeRegistry.getInstance().findByAccountType("nextcloud")
        ) || VersionCheckHelper.verifyMinVersion(
                context,
                MIN_NEXTCLOUD_FILES_APP_VERSION_CODE_DEV,
                FilesAppTypeRegistry.getInstance().findByAccountType("nextcloud.beta")
        );
    }
}
