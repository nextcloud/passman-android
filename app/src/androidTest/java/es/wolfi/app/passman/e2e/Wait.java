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

package es.wolfi.app.passman.e2e;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.os.SystemClock;
import android.view.View;

import androidx.test.espresso.ViewAssertion;

import org.hamcrest.Matcher;

/**
 * Polling waits. The app does its HTTP requests on threads Espresso doesn't know about,
 * so Espresso can't wait for them by itself.
 */
final class Wait {
    static final long NETWORK = 30_000;
    static final long SLOW = 120_000;
    private static final long POLL_INTERVAL = 250;

    private Wait() {
    }

    static void untilDisplayed(Matcher<View> matcher) {
        until(matcher, matches(isDisplayed()), NETWORK);
    }

    static void untilDisplayed(Matcher<View> matcher, long timeoutMillis) {
        until(matcher, matches(isDisplayed()), timeoutMillis);
    }

    static void untilGone(Matcher<View> matcher, long timeoutMillis) {
        until(matcher, doesNotExist(), timeoutMillis);
    }

    static void until(Matcher<View> matcher, ViewAssertion assertion, long timeoutMillis) {
        long end = SystemClock.uptimeMillis() + timeoutMillis;
        while (true) {
            try {
                onView(matcher).check(assertion);
                return;
            } catch (RuntimeException | AssertionError e) {
                // NoMatchingViewException, AssertionFailedError or a dialog (like the progress dialog) in front
                if (SystemClock.uptimeMillis() > end) {
                    throw e;
                }
                SystemClock.sleep(POLL_INTERVAL);
            }
        }
    }

    static void until(Condition condition, String description, long timeoutMillis) throws Exception {
        long end = SystemClock.uptimeMillis() + timeoutMillis;
        while (!condition.isMet()) {
            if (SystemClock.uptimeMillis() > end) {
                throw new AssertionError("timed out waiting for: " + description);
            }
            SystemClock.sleep(POLL_INTERVAL);
        }
    }

    interface Condition {
        boolean isMet() throws Exception;
    }
}
