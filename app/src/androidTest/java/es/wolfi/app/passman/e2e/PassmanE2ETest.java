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

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SJCLCrypto;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.app.passman.testutil.TestVault;
import es.wolfi.utils.otp.CodeGenerator;
import es.wolfi.utils.otp.HashingAlgorithm;

/**
 * The main user flows against a real Passman server with the restored test vault (see ci/start-passman-server.sh).
 * <p>
 * The steps build on each other (login, unlock, ...) and the test runner finishes all activities
 * between test methods, so the whole flow is one test. The app must be freshly installed,
 * ci/run-instrumented-tests.sh takes care of that.
 */
@RunWith(AndroidJUnit4.class)
public class PassmanE2ETest {
    private static final String LOG_TAG = "PassmanE2ETest";
    /**
     * Instrumentation argument (am instrument -e screenshotProtection true) to keep the app default
     */
    private static final String SCREENSHOT_PROTECTION_ARGUMENT = "screenshotProtection";
    private static final String FAILURE_SCREENSHOT = "/data/local/tmp/passman-e2e-failure.png";
    private static final String RUN_ID = Long.toString(System.currentTimeMillis() % 1_000_000);
    private static final String NEW_CREDENTIAL_LABEL = "CI credential " + RUN_ID;
    private static final String NEW_VAULT_NAME = "CI vault " + RUN_ID;
    private static final String NEW_VAULT_PASSWORD = "CI vault pässword " + RUN_ID;

    private static TestVault testVault;
    private static PassmanServer server;

    @BeforeClass
    public static void loadTestVault() throws Exception {
        testVault = TestVault.get();
        server = new PassmanServer(testVault.nextcloudUser(), testVault.nextcloudPassword());
    }

    @Test
    public void userFlow() throws Throwable {
        applyScreenshotProtectionSetting();
        try (ActivityScenario<PasswordListActivity> ignored = ActivityScenario.launch(PasswordListActivity.class)) {
            runSteps();
        }
    }

    /**
     * The screenshot protection (enabled by default) makes failure screenshots black, so the test disables it
     * unless the instrumentation argument screenshotProtection=true asks to keep it.
     */
    private static void applyScreenshotProtectionSetting() {
        boolean enabled = Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString(SCREENSHOT_PROTECTION_ARGUMENT));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(SettingValues.ENABLE_SCREENSHOT_PROTECTION.toString(), enabled)
                .commit();
    }

    private void runSteps() throws Throwable {
        step("step01_loginWithWrongPasswordFails", this::step01_loginWithWrongPasswordFails);
        step("step02_login", this::step02_login);
        step("step03_unlockWithWrongPasswordFails", this::step03_unlockWithWrongPasswordFails);
        step("step04_unlockShowsAllCredentials", this::step04_unlockShowsAllCredentials);
        step("step05_showCredential", this::step05_showCredential);
        step("step06_showOtp", this::step06_showOtp);
        step("step07_showFilesAndCustomFields", this::step07_showFilesAndCustomFields);
        step("step08_addCredential", this::step08_addCredential);
        step("step09_editCredential", this::step09_editCredential);
        step("step10_deleteCredential", this::step10_deleteCredential);
        step("step11_lockVault", this::step11_lockVault);
        step("step12_createVault", this::step12_createVault);
        step("step13_deleteVault", this::step13_deleteVault);
    }

    private interface Step {
        void run() throws Throwable;
    }

    /**
     * Runs one step of the flow and names it in the failure.
     */
    private static void step(String name, Step step) throws Throwable {
        Log.i(LOG_TAG, "step " + name);
        try {
            step.run();
        } catch (Throwable t) {
            takeFailureScreenshot();
            throw new AssertionError("step " + name + " failed: " + t.getMessage(), t);
        }
    }

    /**
     * ci/run-instrumented-tests.sh collects it, the app is already closed when the test run ends.
     */
    private static void takeFailureScreenshot() {
        try (
                ParcelFileDescriptor result = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                        .executeShellCommand("screencap -p " + FAILURE_SCREENSHOT);
                InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(result)
        ) {
            // wait until the command is done
            while (in.read() != -1) ;
        } catch (IOException e) {
            Log.w(LOG_TAG, "failure screenshot not possible", e);
        }
    }

    private void step01_loginWithWrongPasswordFails() {
        // without the Nextcloud files app the manual login form shows up directly
        Wait.untilDisplayed(withId(R.id.host));
        fillLoginForm(testVault.nextcloudPassword() + "-wrong");
        onView(withId(R.id.next)).perform(scrollTo(), click());

        // still on the login form after the server rejected the login
        android.os.SystemClock.sleep(3000);
        Wait.untilDisplayed(withId(R.id.host));
    }

    private void step02_login() {
        fillLoginForm(testVault.nextcloudPassword());
        onView(withId(R.id.next)).perform(scrollTo(), click());

        Wait.untilDisplayed(vaultInList(testVault.vaultName()));
    }

    private void step03_unlockWithWrongPasswordFails() {
        onView(vaultInList(testVault.vaultName())).perform(click());
        Wait.untilDisplayed(withId(R.id.fragment_vault_password));

        onView(withId(R.id.fragment_vault_password)).perform(replaceText(testVault.vaultPassword() + "x"), closeSoftKeyboard());
        onView(withId(R.id.fragment_vault_unlock)).perform(click());

        android.os.SystemClock.sleep(1000);
        onView(withId(R.id.fragment_vault_password)).check(matches(isDisplayed()));
    }

    private void step04_unlockShowsAllCredentials() throws Exception {
        onView(withId(R.id.fragment_vault_password)).perform(replaceText(testVault.vaultPassword()), closeSoftKeyboard());
        onView(withId(R.id.fragment_vault_unlock)).perform(click());

        Wait.untilDisplayed(withId(R.id.addCredentialButton));
        // like the web client: without the hidden vault test key
        Wait.until(withId(R.id.list), credentialListShows(visibleCredentialLabels()), Wait.NETWORK);
    }

    private void step05_showCredential() throws Exception {
        JSONObject credential = testVault.frontendCredentialWhere(
                c -> !c.isNull("username") && !c.isNull("password") && !c.isNull("url")
        );
        openCredential(credential.getString("label"));

        onView(withId(R.id.credential_label)).check(matches(withText(credential.getString("label"))));
        onView(textOf(R.id.credential_user)).check(matches(withText(credential.getString("username"))));
        onView(textOf(R.id.credential_password)).check(matches(withText(credential.getString("password"))));
        onView(textOf(R.id.credential_url)).check(matches(withText(credential.getString("url"))));
        pressBack();
    }

    private void step06_showOtp() throws Exception {
        JSONObject credential = testVault.frontendCredentialWhere(
                c -> c.optJSONObject("otp") != null && c.getJSONObject("otp").has("secret")
        );
        JSONObject otp = credential.getJSONObject("otp");
        openCredential(credential.getString("label"));

        CodeGenerator generator = new CodeGenerator(
                HashingAlgorithm.fromStringOrSha1(otp.optString("algorithm", "SHA1")),
                otp.optInt("digits", 6), otp.optInt("period", 30)
        );
        Wait.until(textOf(R.id.credential_otp), (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            // accept the neighbouring periods, the code may change during the check
            long now = System.currentTimeMillis() / 1000;
            int period = otp.optInt("period", 30);
            String shown = ((TextView) view).getText().toString();
            try {
                List<String> valid = Arrays.asList(
                        generator.generate(otp.getString("secret"), now - period),
                        generator.generate(otp.getString("secret"), now),
                        generator.generate(otp.getString("secret"), now + period)
                );
                assertTrue("shown OTP " + shown + " is none of " + valid, valid.contains(shown));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }, Wait.NETWORK);
        pressBack();
    }

    private void step07_showFilesAndCustomFields() throws Exception {
        JSONObject credential = testVault.frontendCredentialWhere(
                c -> c.getJSONArray("files").length() > 0 && c.getJSONArray("custom_fields").length() > 0
        );
        openCredential(credential.getString("label"));

        String filename = credential.getJSONArray("files").getJSONObject(0).getString("filename");
        String customFieldLabel = credential.getJSONArray("custom_fields").getJSONObject(0).getString("label");
        onView(withId(R.id.filesList)).perform(scrollTo());
        Wait.untilDisplayed(allOf(isDescendantOfA(withId(R.id.filesList)), withText(containsString(filename))));
        onView(withId(R.id.customFieldsList)).perform(scrollTo());
        Wait.untilDisplayed(allOf(isDescendantOfA(withId(R.id.customFieldsList)), withText(customFieldLabel)));
        pressBack();
    }

    private void step08_addCredential() throws Exception {
        onView(withId(R.id.addCredentialButton)).perform(click());
        Wait.untilDisplayed(withId(R.id.add_credential_label));

        onView(withId(R.id.add_credential_label)).perform(replaceText(NEW_CREDENTIAL_LABEL));
        onView(withId(R.id.add_credential_user)).perform(replaceText("ci-user"));
        onView(passwordInput(R.id.add_credential_password)).perform(replaceText("Ci-Pässword-1"));
        onView(withId(R.id.add_credential_email)).perform(replaceText("ci@example.com"));
        onView(withId(R.id.add_credential_url)).perform(replaceText("https://ci.example.com/"), closeSoftKeyboard());
        onView(withId(R.id.SaveCredentialButton)).perform(click());

        Wait.untilDisplayed(withId(R.id.addCredentialButton));
        JSONObject stored = waitForServerCredential(NEW_CREDENTIAL_LABEL);
        assertEquals("ci-user", decrypt(stored.getString("username")));
        assertEquals("Ci-Pässword-1", decrypt(stored.getString("password")));
        assertEquals("ci@example.com", decrypt(stored.getString("email")));
        assertEquals("https://ci.example.com/", decrypt(stored.getString("url")));

        openCredential(NEW_CREDENTIAL_LABEL);
        onView(textOf(R.id.credential_user)).check(matches(withText("ci-user")));
        onView(textOf(R.id.credential_password)).check(matches(withText("Ci-Pässword-1")));
    }

    private void step09_editCredential() throws Exception {
        onView(withId(R.id.editCredentialButton)).perform(click());
        Wait.untilDisplayed(withId(R.id.edit_credential_user));

        onView(withId(R.id.edit_credential_user)).perform(replaceText("ci-user-edited"));
        onView(passwordInput(R.id.edit_credential_password)).perform(replaceText("Ci-Pässword-2"), closeSoftKeyboard());
        onView(withId(R.id.UpdateCredentialButton)).perform(click());

        Wait.untilDisplayed(textOf(R.id.credential_user));
        Wait.until(textOf(R.id.credential_user), matches(withText("ci-user-edited")), Wait.NETWORK);
        onView(textOf(R.id.credential_password)).check(matches(withText("Ci-Pässword-2")));

        JSONObject stored = server.credentialByLabel(testVault.vaultGuid(), NEW_CREDENTIAL_LABEL);
        assertNotNull(stored);
        assertEquals("ci-user-edited", decrypt(stored.getString("username")));
        assertEquals("Ci-Pässword-2", decrypt(stored.getString("password")));
    }

    private void step10_deleteCredential() throws Exception {
        onView(withId(R.id.editCredentialButton)).perform(click());
        Wait.untilDisplayed(withId(R.id.DeleteCredentialButton));
        onView(withId(R.id.DeleteCredentialButton)).perform(click());
        Wait.untilDisplayed(withId(android.R.id.button1));
        onView(withId(android.R.id.button1)).perform(click());

        Wait.untilDisplayed(withId(R.id.addCredentialButton));
        Wait.until(
                () -> server.credentialByLabel(testVault.vaultGuid(), NEW_CREDENTIAL_LABEL).getLong("delete_time") > 0,
                "credential moved to the trash on the server",
                Wait.NETWORK
        );
        onView(withId(R.id.search_input)).perform(clearText(), closeSoftKeyboard());
        Wait.until(withId(R.id.list), credentialListShows(visibleCredentialLabels()), Wait.NETWORK);
    }

    private void step11_lockVault() {
        onView(withId(R.id.VaultLockButton)).perform(click());
        Wait.untilDisplayed(vaultInList(testVault.vaultName()));

        // locked: opening the vault asks for the password again
        onView(vaultInList(testVault.vaultName())).perform(click());
        Wait.untilDisplayed(withId(R.id.fragment_vault_password));
        pressBack();
        Wait.untilDisplayed(vaultInList(testVault.vaultName()));
    }

    private void step12_createVault() throws Exception {
        onView(withId(R.id.add_vault_button)).perform(click());
        Wait.untilDisplayed(withId(R.id.add_vault_name));

        onView(withId(R.id.add_vault_name)).perform(replaceText(NEW_VAULT_NAME));
        onView(passwordInput(R.id.add_vault_password)).perform(replaceText(NEW_VAULT_PASSWORD));
        onView(passwordInput(R.id.add_vault_password_repeat)).perform(replaceText(NEW_VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.SaveVaultButton)).perform(click());

        // creates the sharing keys and the vault test key credential
        Wait.untilDisplayed(vaultInList(NEW_VAULT_NAME), Wait.SLOW);
        JSONObject vault = server.vaultByName(NEW_VAULT_NAME);
        assertNotNull(vault);
        Wait.until(
                () -> server.vault(vault.getString("guid")).getJSONArray("credentials").length() == 1,
                "vault test key credential",
                Wait.NETWORK
        );

        JSONObject testKey = server.vault(vault.getString("guid")).getJSONArray("credentials").getJSONObject(0);
        assertNotEquals("", SJCLCrypto.decryptString(testKey.getString("password"), NEW_VAULT_PASSWORD));
    }

    private void step13_deleteVault() throws Exception {
        onView(
                allOf(
                        withId(R.id.vault_delete_button),
                        isDescendantOfA(withId(R.id.card_view)),
                        isDescendantOfA(cardOfVault(NEW_VAULT_NAME))
                )
        ).perform(click());
        Wait.untilDisplayed(withId(R.id.DeleteVaultButton));

        onView(passwordInput(R.id.delete_vault_password)).perform(replaceText(NEW_VAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.DeleteVaultButton)).perform(click());
        Wait.untilDisplayed(withId(android.R.id.button1));
        onView(withId(android.R.id.button1)).perform(click());

        // back in the vault list (the delete screen shows the vault name, too)
        Wait.untilDisplayed(withId(R.id.add_vault_button), Wait.SLOW);
        Wait.untilGone(vaultInList(NEW_VAULT_NAME), Wait.NETWORK);
        onView(vaultInList(testVault.vaultName())).check(matches(isDisplayed()));
        assertNull("vault still exists on the server", server.vaultByName(NEW_VAULT_NAME));
    }

    private static void fillLoginForm(String password) {
        onView(withId(R.id.protocol)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is(server.url.getScheme().toUpperCase()))).perform(click());
        onView(withId(R.id.host)).perform(replaceText(server.hostAndPort()));
        onView(withId(R.id.user)).perform(replaceText(testVault.nextcloudUser()));
        onView(withId(R.id.pass)).perform(replaceText(password), closeSoftKeyboard());
    }

    private static void openCredential(String label) {
        onView(withId(R.id.search_input)).perform(replaceText(label), closeSoftKeyboard());
        Wait.untilDisplayed(allOf(withId(R.id.content), withText(label)));
        onView(allOf(withId(R.id.content), withText(label))).perform(click());
        Wait.untilDisplayed(allOf(withId(R.id.credential_label), withText(label)));
    }

    private static JSONObject waitForServerCredential(String label) throws Exception {
        Wait.until(
                () -> server.credentialByLabel(testVault.vaultGuid(), label) != null,
                "credential " + label + " on the server",
                Wait.NETWORK
        );
        return server.credentialByLabel(testVault.vaultGuid(), label);
    }

    private static String decrypt(String cryptogram) throws Exception {
        return SJCLCrypto.decryptString(cryptogram, testVault.vaultPassword());
    }

    private static Matcher<View> vaultInList(String name) {
        return allOf(withId(R.id.vault_name), withText(name));
    }

    private static Matcher<View> cardOfVault(String name) {
        return allOf(withId(R.id.card_view), hasDescendant(vaultInList(name)));
    }

    /**
     * The text view inside a CopyTextItem (credential display)
     */
    private static Matcher<View> textOf(int copyTextItemId) {
        return allOf(withId(R.id.copy_text_text), isDescendantOfA(withId(copyTextItemId)));
    }

    /**
     * The input inside an EditPasswordTextItem
     */
    private static Matcher<View> passwordInput(int editPasswordTextItemId) {
        return allOf(withId(R.id.password), isDescendantOfA(withId(editPasswordTextItemId)));
    }

    /**
     * The credential list shows exactly the given labels (order doesn't matter). Every entry is bound to a fresh row
     * like the adapter does it, rows the adapter hides (like the vault test key) don't count as shown.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ViewAssertion credentialListShows(List<String> expectedLabels) {
        return (view, noViewFoundException) -> {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            assertNotNull("no adapter yet", adapter);

            List<String> shownLabels = new ArrayList<>();
            for (int position = 0; position < adapter.getItemCount(); position++) {
                RecyclerView.ViewHolder holder = adapter.createViewHolder(recyclerView, adapter.getItemViewType(position));
                adapter.bindViewHolder(holder, position);
                if (holder.itemView.getVisibility() == View.VISIBLE) {
                    shownLabels.add(((TextView) holder.itemView.findViewById(R.id.content)).getText().toString());
                }
            }

            List<String> missing = new ArrayList<>(expectedLabels);
            for (String label : shownLabels) {
                missing.remove(label);
            }
            List<String> unexpected = new ArrayList<>(shownLabels);
            for (String label : expectedLabels) {
                unexpected.remove(label);
            }
            assertTrue("credential list: missing " + missing + ", unexpected " + unexpected,
                    missing.isEmpty() && unexpected.isEmpty());
        };
    }

    private static List<String> visibleCredentialLabels() throws Exception {
        List<String> labels = new ArrayList<>();
        for (JSONObject credential : testVault.visibleFrontendCredentials()) {
            labels.add(credential.getString("label"));
        }
        return labels;
    }
}
