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

package es.wolfi.app.passman;

public enum SettingValues {
    HOST("host"),
    USER("user"),
    PASSWORD("password"),
    VAULTS("vaults"),
    ACTIVE_VAULT("active_vault"),
    AUTOFILL_VAULT_GUID("autofill_vault_guid"),
    AUTOFILL_VAULT("autofill_vault"),
    ENABLE_APP_START_DEVICE_PASSWORD("enable_app_start_device_password"),
    ENABLE_CREDENTIAL_LIST_ICONS("enable_credential_list_icons"),
    REQUEST_CONNECT_TIMEOUT("request_connect_timeout"),
    REQUEST_RESPONSE_TIMEOUT("request_response_timeout"),
    CLEAR_CLIPBOARD_DELAY("clear_clipboard_delay"),
    PASSWORD_GENERATOR_SETTINGS("password_generator_settings"),
    ENABLE_PASSWORD_GENERATOR_SHORTCUT("enable_password_generator_shortcut"),
    OFFLINE_STORAGE("offline_storage");

    private final String name;

    SettingValues(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
