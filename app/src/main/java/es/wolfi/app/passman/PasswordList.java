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

import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;

import java.util.concurrent.ConcurrentHashMap;

import es.wolfi.passman.API.Core;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.GeneralUtils;

public class PasswordList extends AppCompatActivity implements
        VaultFragment.OnListFragmentInteractionListener,
        CredentialItemFragment.OnListCredentialFragmentInteractionListener,
        VaultLockScreen.VaultUnlockInteractionListener,
        CredentialDisplay.OnCredentialFragmentInteraction,
        CredentialEdit.OnCredentialChangeInteraction {
    SharedPreferences settings;
    SingleTon ton;
    Dialog dialog;

    static int notificationNumber = Integer.MIN_VALUE;

    static boolean running = false;

    // activity event handlers

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GeneralUtils.debug("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_list);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        ton = SingleTon.getTon();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        createNotificationChannel();
    }

    @Override
    public void onStart() {
        super.onStart();

        // @TODO: Display loading screen while checking credentials!
        final AppCompatActivity self = this;

        if (!running) {

            GeneralUtils.debug("Checking Credentials");
            Core.checkLogin(this, false, new FutureCallback<Boolean>() {
                @Override
                public void onCompleted(Exception e, Boolean result) {
                    if (result) {
                        showVaults();
                        return;
                    }
                    // If not logged in, show login form!
                    LoginActivity.launch(self, new ICallback() {
                        @Override
                        public void onTaskFinished() {
                            showVaults();
                        }
                    });

                }
            });
            running = true;
        } else {
            showVaults();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_password_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                showNotImplementedMessage();
                return true;
            case R.id.action_refresh:
                refreshVaults();
                return true;
            case R.id.action_clearsavedcreds:
                clearSavedCredentials();
                return true;
            case R.id.action_lockvaults:
                lockVaults();
                return true;
            case R.id.action_autofill:
                launchAutofillServiceIntent();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_refresh_credentials:
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // activity event handler helpers

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CredentialCopyChannel", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // vault operations and navigation

    public void showVaults() {
        Core.getAPIVersion(this, new FutureCallback<Integer>() {
            @Override
            public void onCompleted(Exception e, Integer result) {

            }
        });

        ConcurrentHashMap<String, Vault> vaults = Vault.getAllVaults();

        if (vaults != null) {
            if (!getSupportFragmentManager().isStateSaved()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.content_password_list, new VaultFragment(), "vaultList")
                        .commit();
            }
            setDialog(false, "Done loading Vaults");
        } else {
            GeneralUtils.debug("Retrieving Vaults");
            refreshVaults();
        }
    }

    public void showActiveVault() {
        GeneralUtils.debug("Opening active vault");
        Vault vault = Vault.getActiveVault();
        if (vault != null && vault.getCredentials() != null) {
            if (vault.is_unlocked()) {
                GeneralUtils.debug("Listing Credentials");

                if (!getSupportFragmentManager().isStateSaved()) {
                    // After refresh, we don't want a load of old CredentialItemFragments
                    getSupportFragmentManager()
                            .popBackStack("credItems", FragmentManager.POP_BACK_STACK_INCLUSIVE);

                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.content_password_list, new CredentialItemFragment(), "credItems")
                            .addToBackStack("credItems")
                            .commit();
                }
            } else {
                setDialog(false, "");
                GeneralUtils.debug("Vault locked");
                showUnlockVault();
            }
        } else {
            loadActiveVault(vault);
        }
    }

    void showUnlockVault() {
        Vault v = Vault.getActiveVault();
        if (v != null && v.unlock(settings.getString(v.guid, ""))) {
            GeneralUtils.debug("Unlocked using stored credentials");
            showActiveVault();
            return;
        } else if (v != null) {
            GeneralUtils.debug("Showing lock screen");
            if (!getSupportFragmentManager().isStateSaved()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_out_left, R.anim.slide_out_left)
                        .replace(R.id.content_password_list, new VaultLockScreen(), "vaultLockScreen")
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    private void loadActiveVault(final Vault oldVaultObj) {
        if (oldVaultObj == null) {
            return;
        }

        GeneralUtils.debug("Loading active Vault");
        setDialog(true, getString(R.string.loadingactivevault));

        Vault.getVault(this, oldVaultObj.guid, new FutureCallback<Vault>() {
            @Override
            public void onCompleted(Exception e, Vault result) {
                GeneralUtils.debug("Back from Async");
                if (e != null) {
                    // Not logged in, restart activity
                    if (e.getMessage() != null && e.getMessage().equals("401")) {
                        recreate();
                    }

                    Log.e(Tag.CREATOR.toString(), "Unknown network error", e);

                    GeneralUtils.toast(findViewById(R.id.content_password_list), R.string.net_error);

                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showVaults();
                        }
                    }, 30000);

                    setDialog(false, "Error loading active vault");
                    return;
                }


                if (oldVaultObj.is_unlocked()) {
                    if (!oldVaultObj.unlockReplacementInstance()) {
                        GeneralUtils.debug("Couldn't transfer lock, showing Vaults.");
                        showVaults();
                    }
                }

                Fragment fragment = getVisibleFragment();
                if (fragment != null) {
                    refreshFragment(fragment);
                } else {
                    setDialog(true, getString(R.string.refreshingdisplay));
                    GeneralUtils.debug("Refreshing display");
                    showActiveVault();
                }
            }
        });
    }

    private void refreshVaults() {
        setDialog(true, getString(R.string.progresstext));
        final Vault activeVault = Vault.getActiveVault();

        Vault.getVaults(this, new FutureCallback<ConcurrentHashMap<String, Vault>>() {
            @Override
            public void onCompleted(Exception e, ConcurrentHashMap<String, Vault> result) {
                if (e != null) {

                    // Not logged in, restart activity
                    if (e.getMessage().equals("401")) {
                        recreate();
                    }

                    GeneralUtils.toast(findViewById(R.id.content_password_list), R.string.net_error);

                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showVaults();
                        }
                    }, 30000);
                    setDialog(false, "refreshVaults bad finish");
                    return;
                }

                Vault.setAllVaults(result);

                if (activeVault != null) {
                    GeneralUtils.debug("Active, loading");
                    Vault newVault = Vault.getVaultByGuid(activeVault.guid);
                    if (newVault != null) {
                        // load the vault, use an old reference to transfer the lock
                        loadActiveVault(activeVault);
                    }
                } else {
                    GeneralUtils.debug("Not active, showing vaults");
                    Fragment fragment = getVisibleFragment();
                    if (fragment != null) {
                        refreshFragment(fragment);
                    } else {
                        showVaults();
                    }
                }
            }
        });
    }


    // menu operations

    private void launchAutofillServiceIntent() {
        final AutofillManager afm = getSystemService(AutofillManager.class);
        if (afm.hasEnabledAutofillServices()) {
            GeneralUtils.toast(findViewById(R.id.content_password_list), R.string.autofill_alreadyenabled);
        } else {
            if (!afm.isAutofillSupported()) {
                GeneralUtils.toast(findViewById(R.id.content_password_list), R.string.autofill_notsupported);
            } else {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }


        }
    }

    private void lockVaults() {
        ConcurrentHashMap<String, Vault> vaultHashMap = Vault.getAllVaults();

        for (Vault v : vaultHashMap.values()) {
            if (v.is_unlocked()) {
                v.lock();
            }
        }
        GeneralUtils.debugAndToast(true, findViewById(R.id.content_password_list), R.string.vaultslocked);

        Vault.unsetActiveVault();

        showVaults();
    }

    private void clearSavedCredentials() {
        lockVaults();
        ConcurrentHashMap<String, Vault> vaultHashMap = Vault.getAllVaults();

        for (Vault v : vaultHashMap.values()) {
            settings.edit()
                    .putString(v.guid, "")
                    .commit();
        }

        GeneralUtils.debugAndToast(true, findViewById(R.id.content_password_list), R.string.credentialscleared);
        showVaults();
    }

    private void showNotImplementedMessage() {
        GeneralUtils.toast(findViewById(R.id.content_password_list), R.string.not_implemented_yet);
    }


    // fragment helpers

    private Fragment getVisibleFragment() {

        for (Fragment currentFrag : getSupportFragmentManager().getFragments()) {
            if (currentFrag.isVisible()) {
                Log.d("Vault", "Fragment Type: " + String.valueOf(currentFrag.getId()));
                return currentFrag;
            }
        }
        Log.d("Vault", "No Visible Fragment");
        return null;
    }

    private void refreshFragment(Fragment fragment) {
        GeneralUtils.debug("Refreshing fragment: " + fragment.getTag());

        if (!getSupportFragmentManager().isStateSaved()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .detach(fragment)
                    .attach(fragment)
                    .commit();
        }
        setDialog(false, "Fragment refreshed.");
        GeneralUtils.debug("Done refreshing fragment: " + fragment.getTag());

        if (fragment.getTag().contains("vaultList") && Vault.getActiveVault() != null) {
            showActiveVault();
        }
    }

    private void setDialog(boolean showDialog, String text) {
        GeneralUtils.debug("Dialog, Show: " + String.valueOf(showDialog) + ": " + text);

        try {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

            View view = getLayoutInflater().inflate(R.layout.progress_dialog, null);

            dialogBuilder.setView(view);
            dialogBuilder.setCancelable(false);

            if (dialog == null) {
                dialog = dialogBuilder.create();
            }

            if (dialog != null) {
                if (showDialog) {
                    TextView progressDialogMsg = (TextView) view.findViewById(R.id.progress_dialog_msg);
                    progressDialogMsg.setText(text);
                    dialog.setContentView(view);
                    dialog.show();
                } else {
                    dialog.dismiss();
                    dialog = null;
                }
            }
        } catch (Exception ex) {
            GeneralUtils.debug("Dialog error, Show: " + String.valueOf(showDialog) + ": " + text + ": " + ex.toString());
        }
    }

    // fragment event handlers

    @Override
    public void onListFragmentInteraction(Vault item) {
        Vault.setActiveVault(item);
        showActiveVault();
    }

    @Override
    public void onCredentialClick(Credential item) {
        if (!getSupportFragmentManager().isStateSaved()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.content_password_list, CredentialDisplay.newInstance(item.getGuid()), "credential")
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public boolean onCredentialLongClick(Credential item) {
        try {
            String itemGuid = item.getGuid();
            String vaultGuid = item.getVault().guid;

            Intent copyUsernameIntent = new Intent(this, PassmanReceiver.class);
            copyUsernameIntent.setAction("COPYUSERNAMEINTENTACTION");
            copyUsernameIntent.putExtra("CredGuid", itemGuid);
            copyUsernameIntent.putExtra("VaultGuid", vaultGuid);

            Intent copyEmailAddressIntent = new Intent(this, PassmanReceiver.class);
            copyEmailAddressIntent.setAction("COPYEMAILINTENTACTION");
            copyEmailAddressIntent.putExtra("CredGuid", itemGuid);
            copyEmailAddressIntent.putExtra("VaultGuid", vaultGuid);

            Intent copyPasswordIntent = new Intent(this, PassmanReceiver.class);
            copyPasswordIntent.setAction("COPYPASSWORDINTENTACTION");
            copyPasswordIntent.putExtra("CredGuid", itemGuid);
            copyPasswordIntent.putExtra("VaultGuid", vaultGuid);

            Intent dismissIntent = new Intent(this, PassmanReceiver.class);
            dismissIntent.setAction("DISMISSCOPYINTENTACTION");

            if (notificationNumber < Integer.MAX_VALUE)
                notificationNumber++;
            else
                notificationNumber = Integer.MIN_VALUE;

            PendingIntent copyUsername =
                    PendingIntent.getBroadcast(this, notificationNumber, copyUsernameIntent, PendingIntent.FLAG_CANCEL_CURRENT);


            PendingIntent copyEmailAddress =
                    PendingIntent.getBroadcast(this, notificationNumber, copyEmailAddressIntent, PendingIntent.FLAG_CANCEL_CURRENT);


            PendingIntent copyPassword =
                    PendingIntent.getBroadcast(this, notificationNumber, copyPasswordIntent, PendingIntent.FLAG_CANCEL_CURRENT);


            PendingIntent dismiss =
                    PendingIntent.getBroadcast(this, notificationNumber, dismissIntent, PendingIntent.FLAG_CANCEL_CURRENT);


            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "CredentialCopyChannel")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(item.getLabel())
                    .setContentText("Use the actions to copy the field.")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Use the actions to copy the field. Touch to dismiss."))
                    .setTimeoutAfter(30000)
                    .setOnlyAlertOnce(false)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(dismiss);

            if (mBuilder != null) {
                String userName = item.getUsername();
                String email = item.getEmail();
                String password = item.getPassword();

                if (userName != null && !userName.isEmpty() && !userName.equals("null")) {
                    mBuilder.addAction(R.drawable.logo_vertical,
                            getString(R.string.notification_copyusername),
                            copyUsername);
                }
                if (email != null && !email.isEmpty() && !email.equals("null")) {
                    mBuilder.addAction(R.drawable.logo_vertical,
                            getString(R.string.notification_copyemail),
                            copyEmailAddress);
                }
                if (password != null && !password.isEmpty() && !password.equals("null")) {
                    mBuilder.addAction(R.drawable.logo_vertical,
                            getString(R.string.notification_copypassword),
                            copyPassword);
                }
                if (mBuilder.mActions.size() > 0) {
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(0, mBuilder.build());
                    return true;
                } else {
                    GeneralUtils.toast(getApplicationContext(), "Username, Email or Password required.");
                }
            }
        } catch (Exception ex) {
            GeneralUtils.debug("Problem sending notification: " + ex.toString());
        }
        return false;
    }

    @Override
    public void onActionCreateClick() {
        GeneralUtils.debug("Create credential clicked");
        if (!getSupportFragmentManager().isStateSaved()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.content_password_list, CredentialEdit.newInstance(null), "createCredential")
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onActionEditClick(String credentialGuid) {
        GeneralUtils.debug("edit credential clicked");
        if (credentialGuid != null) {
            if (!getSupportFragmentManager().isStateSaved()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.content_password_list, CredentialEdit.newInstance(credentialGuid), "createCredential")
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    @Override
    public void onListFragmentCreatedView() {
        GeneralUtils.debug("Credential view created");
        setDialog(false, "Credential view created");
    }

    @Override
    public void onVaultUnlock(Vault vault) {
        GeneralUtils.debug("Vault unlock event");
        if (!getSupportFragmentManager().isStateSaved()) {
            getSupportFragmentManager().popBackStack();
        }
        showActiveVault();
    }

    @Override
    public void OnCredentialChanged(String vaultGuid, String credentialGuid) {
        GeneralUtils.debug("Cred Changed event");
    }
}
