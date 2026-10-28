/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2026, Timo Triebensky (timo@binsky.org)
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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

import es.wolfi.app.passman.R;

/**
 * Since Android 17 (API 37), apps need the runtime permission ACCESS_LOCAL_NETWORK to connect to devices in the
 * local network, like a Nextcloud server at home. Without it, such connections just time out.
 * <p>
 * Usage: request(serverUrl, next) before the first request to the server and forward onRequestPermissionsResult().
 */
public class LocalNetworkPermission {
    private static final String LOG_TAG = "LocalNetworkPermission";
    private static final int ANDROID_17 = 37;
    public static final int REQUEST_CODE = 37;

    private final Activity activity;
    private Runnable pendingAfterRequest;

    public LocalNetworkPermission(Activity activity) {
        this.activity = activity;
    }

    public static boolean isGranted(Context context) {
        return Build.VERSION.SDK_INT < ANDROID_17
                || context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests the permission if it is missing and the server is in the local network, then runs next on the
     * main thread (also if the permission was denied, the connection fails as before then).
     *
     * @param serverUrl url of the Nextcloud server, null runs next right away
     */
    public void request(String serverUrl, Runnable next) {
        String host = serverUrl != null ? Uri.parse(serverUrl).getHost() : null;
        if (host == null || isGranted(activity)) {
            next.run();
            return;
        }

        // resolving the host name must not run on the main thread
        new Thread(() -> {
            boolean local = isInLocalNetwork(host);
            activity.runOnUiThread(() -> {
                if (local) {
                    pendingAfterRequest = next;
                    activity.requestPermissions(new String[]{Manifest.permission.ACCESS_LOCAL_NETWORK}, REQUEST_CODE);
                } else {
                    next.run();
                }
            });
        }).start();
    }

    /**
     * To be called from Activity.onRequestPermissionsResult().
     *
     * @return true if the result was for this permission request
     */
    public boolean onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode != REQUEST_CODE || pendingAfterRequest == null) {
            return false;
        }
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, R.string.local_network_permission_denied, Toast.LENGTH_LONG).show();
        }
        Runnable next = pendingAfterRequest;
        pendingAfterRequest = null;
        next.run();
        return true;
    }

    /**
     * Whether the host resolves to a private (RFC 1918, IPv6 unique local), link-local or shared (RFC 6598, used by VPNs like Tailscale) address.
     */
    static boolean isInLocalNetwork(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isLocalNetworkAddress(address)) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            Log.d(LOG_TAG, "Unable to resolve " + host + ", assuming it is not in the local network");
        }
        return false;
    }

    static boolean isLocalNetworkAddress(InetAddress address) {
        if (address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            // 100.64.0.0/10
            return (bytes[0] & 0xff) == 100 && (bytes[1] & 0xc0) == 64;
        }
        // fc00::/7
        return (bytes[0] & 0xfe) == 0xfc;
    }
}
