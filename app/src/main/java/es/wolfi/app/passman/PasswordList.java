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

    static boolean running = false;

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
            showActiveVault();
        }
    }

    public void showVaults() {
        Core.getAPIVersion(this, new FutureCallback<Integer>() {
            @Override
            public void onCompleted(Exception e, Integer result) {

            }
        });
       ConcurrentHashMap<String, Vault> vaults = Vault.getAllVaults();

        Vault.unsetActiveVault();

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

                // Update the vault record to avoid future loads
                Vault.getAllVaults().put(result.guid, result);

                if (oldVaultObj.is_unlocked()) {
                    oldVaultObj.unlockReplacementInstance();
                }

                Vault.setActiveVault(result);

                // if fragment is not null, then refresh the Fragment
                // this has the effect of calling onCreateView again

                Fragment fragment = getVisibleFragment();
                if (fragment != null) {
                    GeneralUtils.debug("Refreshing fragment: " + fragment.getTag());
                    setDialog(true, getString(R.string.refreshingdisplay));

                    if (!getSupportFragmentManager().isStateSaved()) {
                        getSupportFragmentManager()
                                .beginTransaction()
                                .detach(fragment)
                                .attach(fragment)
                                .commit();
                    }

                    if (!fragment.getTag().equals("credItems")) {
                        // && !fragment.getTag().equals("vaultList")) {
                        showActiveVault();
                    }
                } else {
                    setDialog(true, getString(R.string.refreshingdisplay));
                    GeneralUtils.debug("Refreshing display");
                    showActiveVault();
                }
            }
        });
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
            default:
                return super.onOptionsItemSelected(item);
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

    private void refreshVaults() {
        //GeneralUtils.debug("Refreshing");
        setDialog(true, getString(R.string.progresstext));
        // remember the active vault so we can put it back
        final Vault activeVault = Vault.getActiveVault();

        // clear all vaults
        Vault.unsetActiveVault();

       ConcurrentHashMap<String, Vault> currentVaults = Vault.getAllVaults();

        if (currentVaults != null) {
            currentVaults.clear();
        }

        // call the API to refresh the Vaults

        //setDialog(false,"");
        //
        Vault.getVaults(this, new FutureCallback<ConcurrentHashMap<String, Vault>>() {
            @Override
            public void onCompleted(Exception e,ConcurrentHashMap<String, Vault> result) {
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
                    Vault newVault = Vault.getVaultByGuid(activeVault.guid);
                    if (newVault != null) {
                        // load the vault, use an old reference to transfer the lock
                        loadActiveVault(activeVault);
                    }
                } else {
                    showVaults();
                }
                //setDialog(false,"refreshVaults good finished");
            }
        });

    }

    private void showNotImplementedMessage() {
        GeneralUtils.toast(findViewById(R.id.content_password_list), R.string.not_implemented_yet);
    }

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
            Intent copyUsernameIntent = new Intent(this, PassmanReceiver.class);
            copyUsernameIntent.setAction("COPYUSERNAMEINTENTACTION");
            copyUsernameIntent.putExtra("CredGuid", item.getGuid());
            copyUsernameIntent.putExtra("VaultGuid", item.getVault().guid);

            Intent copyEmailAddressIntent = new Intent(this, PassmanReceiver.class);
            copyEmailAddressIntent.setAction("COPYEMAILINTENTACTION");
            copyEmailAddressIntent.putExtra("CredGuid", item.getGuid());
            copyEmailAddressIntent.putExtra("VaultGuid", item.getVault().guid);

            Intent copyPasswordIntent = new Intent(this, PassmanReceiver.class);
            copyPasswordIntent.setAction("COPYPASSWORDINTENTACTION");
            copyPasswordIntent.putExtra("CredGuid", item.getGuid());
            copyPasswordIntent.putExtra("VaultGuid", item.getVault().guid);

            Intent dismissIntent = new Intent(this, PassmanReceiver.class);
            dismissIntent.setAction("DISMISSCOPYINTENTACTION");

            PendingIntent copyUsername =
                    PendingIntent.getBroadcast(this, 0, copyUsernameIntent, 0);

            PendingIntent copyEmailAddress =
                    PendingIntent.getBroadcast(this, 0, copyEmailAddressIntent, 0);

            PendingIntent copyPassword =
                    PendingIntent.getBroadcast(this, 0, copyPasswordIntent, 0);

            PendingIntent dismiss =
                    PendingIntent.getBroadcast(this, 0, dismissIntent, 0);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "CredentialCopyChannel")
                    .setSmallIcon(R.drawable.logo_vertical)
                    .setContentTitle(item.getLabel())
                    .setContentText("Use the actions to copy the field.")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Use the actions to copy the field. Click to dismiss."))
                    .setTimeoutAfter(60000)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(dismiss)
                    .addAction(R.drawable.logo_vertical, getString(R.string.notification_copyusername),
                            copyUsername)
                    .addAction(R.drawable.logo_vertical, getString(R.string.notification_copyemail),
                            copyEmailAddress)
                    .addAction(R.drawable.logo_vertical, getString(R.string.notification_copypassword),
                            copyPassword);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(0, mBuilder.build());
            return true;
        }
        catch (Exception ex)
        {
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

    /*    @Override
    public void onCredentialFragmentInteraction(Credential credential) {
        GeneralUtils.debug("Cred Interaction event");
    }*/

    @Override
    public void OnCredentialChanged(String vaultGuid, String credentialGuid) {
        GeneralUtils.debug("Cred Changed event");
    }
}
