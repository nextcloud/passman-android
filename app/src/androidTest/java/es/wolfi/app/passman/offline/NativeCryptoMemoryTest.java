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

package es.wolfi.app.passman.offline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Debug;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.wolfi.app.passman.SJCLCrypto;

/**
 * The native SJCL implementation must release its buffers, otherwise every decrypted credential field would stay in the native heap.
 */
@RunWith(AndroidJUnit4.class)
public class NativeCryptoMemoryTest {
    private static final String TAG = "NativeCryptoMemoryTest";
    private static final String PASSWORD = "vault password";
    private static final int RUNS = 1000;
    // a leak of the plaintext buffer alone would grow the heap by more than RUNS * PLAINTEXT.length() bytes
    private static final long MAX_GROWTH_BYTES = 512 * 1024;

    private static final String PLAINTEXT = createPlaintext();

    private static String createPlaintext() {
        StringBuilder plaintext = new StringBuilder();
        while (plaintext.length() < 2048) {
            plaintext.append("secret äöü € 😀 ");
        }
        return plaintext.toString();
    }

    @Test
    public void decryptDoesNotLeakNativeMemory() throws Exception {
        String encrypted = SJCLCrypto.encryptString(PLAINTEXT, PASSWORD, true);
        // warm up allocator pools and lazy initializations of OpenSSL
        for (int i = 0; i < 20; i++) {
            assertEquals(PLAINTEXT, SJCLCrypto.decryptString(encrypted, PASSWORD));
        }

        long before = Debug.getNativeHeapAllocatedSize();
        for (int i = 0; i < RUNS; i++) {
            SJCLCrypto.decryptString(encrypted, PASSWORD);
        }
        assertGrowthBelowLimit("decrypt", before, Debug.getNativeHeapAllocatedSize());
    }

    @Test
    public void encryptDoesNotLeakNativeMemory() throws Exception {
        for (int i = 0; i < 20; i++) {
            SJCLCrypto.encryptString(PLAINTEXT, PASSWORD, true);
        }

        long before = Debug.getNativeHeapAllocatedSize();
        for (int i = 0; i < RUNS; i++) {
            SJCLCrypto.encryptString(PLAINTEXT, PASSWORD, true);
        }
        assertGrowthBelowLimit("encrypt", before, Debug.getNativeHeapAllocatedSize());
    }

    private static void assertGrowthBelowLimit(String operation, long before, long after) {
        long growth = after - before;
        Log.i(TAG, operation + ": native heap grew by " + growth + " bytes in " + RUNS + " runs");
        assertTrue(operation + " leaks native memory, the heap grew by " + growth + " bytes in " + RUNS + " runs",
                growth < MAX_GROWTH_BYTES);
    }
}
