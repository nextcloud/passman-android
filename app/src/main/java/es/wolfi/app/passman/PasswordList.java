/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
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

package es.wolfi.app.passman;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;

import java.util.HashMap;

import es.wolfi.passman.API.Core;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.GeneralUtils;

public class PasswordList extends AppCompatActivity implements
        VaultFragment.OnListFragmentInteractionListener,
        CredentialItemFragment.OnListFragmentInteractionListener,
        VaultLockScreen.VaultUnlockInteractionListener,
        CredentialDisplay.OnCredentialFragmentInteraction,
        CredentialCreate.OnCredentialCreateInteraction
{
    SharedPreferences settings;
    SingleTon ton;
    Dialog dialog;

    //static boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_list);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        ton = SingleTon.getTon();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!getSupportFragmentManager().isStateSaved()) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.content_password_list, CredentialCreate.newInstance(), "createCredential")
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
        //fab.hide();

        //if (running) return;

        //running = true;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // @TODO: Display loading screen while checking credentials!
        final AppCompatActivity self = this;

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
    }

    public void showVaults() {
        Core.getAPIVersion(this, new FutureCallback<Integer>() {
            @Override
            public void onCompleted(Exception e, Integer result) {

            }
        });
        HashMap<String, Vault> vaults = (HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString());

        ton.removeExtra(SettingValues.ACTIVE_VAULT.toString());

        if (vaults != null) {
            if (!getSupportFragmentManager().isStateSaved()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.content_password_list, new VaultFragment(), "vaultList")
                        .commit();
            }
            setDialog(false, "Done loading Vaults");
        }
        else {
            GeneralUtils.debug("Retrieving Vaults");
            refreshVaults();
        }
    }

    public void showActiveVault() {
        GeneralUtils.debug("Opening active vault");
        Vault vault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
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
            }
            else {
                setDialog(false,"");
                GeneralUtils.debug("Vault locked");
                showUnlockVault();
            }
        }
        else {
            loadActiveVault(vault);
        }
    }

    private void loadActiveVault(final Vault oldVaultObj) {
        if (oldVaultObj == null) {
            return;
        }

        GeneralUtils.debug("Loading active Vault");
        setDialog(true,getString(R.string.loadingactivevault));
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
                    setDialog(false,"Error loading active vault");
                    return;
                }

                // Update the vault record to avoid future loads
                ((HashMap<String, Vault>)ton.getExtra(SettingValues.VAULTS.toString())).put(result.guid, result);

                if (oldVaultObj.is_unlocked()) {
                    oldVaultObj.unlockReplacementInstance();
                }

                ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), result);

                // if fragment is not null, then refresh the Fragment
                // this has the effect of calling onCreateView again

                Fragment fragment = getVisibleFragment();
                if (fragment != null) {
                    GeneralUtils.debug("Refreshing fragment: " + fragment.getTag());
                    setDialog(true,getString(R.string.refreshingdisplay));

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
                }
                else {
                    setDialog(true,getString(R.string.refreshingdisplay));
                    GeneralUtils.debug("Refreshing display");
                    showActiveVault();
                }
            }
        });
    }

    void showUnlockVault() {
        Vault v = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        if (v != null && v.unlock(settings.getString(v.guid, ""))) {
            GeneralUtils.debug("Unlocked using stored credentials");
            showActiveVault();
            return;
        }
        else if (v != null) {
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

    private void launchAutofillServiceIntent()
    {
        final AutofillManager afm = getSystemService(AutofillManager.class);
        if (afm.hasEnabledAutofillServices()) {
            GeneralUtils.toast(findViewById(R.id.content_password_list),R.string.autofill_alreadyenabled);
        }
        else {
            if (!afm.isAutofillSupported()) {
                GeneralUtils.toast(findViewById(R.id.content_password_list),R.string.autofill_notsupported);
            }
            else {
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
            case R.id.action_settings :
                showNotImplementedMessage();
                return true;
            case R.id.action_refresh :
                refreshVaults();
                return true;
            case R.id.action_clearsavedcreds :
                clearSavedCredentials();
                return true;
            case R.id.action_lockvaults :
                lockVaults();
                return true;
            case R.id.action_autofill :
                launchAutofillServiceIntent();
                return true;
            case android.R.id.home :
                onBackPressed();
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    private void lockVaults() {
        HashMap<String, Vault> vaultHashMap =
                (HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString());
        for (Vault v: vaultHashMap.values()) {
            if (v.is_unlocked()) {
                v.lock();
            }
        }
        GeneralUtils.debugAndToast(true,findViewById(R.id.content_password_list),R.string.vaultslocked);
        showVaults();
    }

    private void clearSavedCredentials() {
        lockVaults();
        HashMap<String, Vault> vaultHashMap =
                (HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString());
        for (Vault v: vaultHashMap.values()) {
            settings.edit()
                    .putString(v.guid,"")
                    .commit();
        }

        GeneralUtils.debugAndToast(true,findViewById(R.id.content_password_list),R.string.credentialscleared);
        showVaults();
    }

    private Fragment getVisibleFragment()
    {

        for(Fragment currentFrag: getSupportFragmentManager().getFragments()) {
            if(currentFrag.isVisible()) {
                Log.d("Vault", "Fragment Type: " + String.valueOf(currentFrag.getId()));
                return currentFrag;
            }
        }
        Log.d("Vault", "No Visible Fragment");
        return null;
    }

    private void setDialog(boolean showDialog, String text){
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
        }
        catch (Exception ex)
        {
            GeneralUtils.debug("Dialog error, Show: " + String.valueOf(showDialog) + ": " + text + ": " + ex.toString() );
        }
    }

    private void refreshVaults()
    {
        //GeneralUtils.debug("Refreshing");
        setDialog(true, getString(R.string.progresstext));
        // remember the active vault so we can put it back
        final Vault activeVault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());

        // clear all vaults
        ton.removeExtra(SettingValues.ACTIVE_VAULT.toString());

        HashMap<String, Vault> currentVaults = (HashMap<String, Vault>)ton
            .getExtra(SettingValues.VAULTS.toString());

        if (currentVaults != null) {
            currentVaults.clear();
        }

        // call the API to refresh the Vaults

        //setDialog(false,"");
        //
        Vault.getVaults(this, new FutureCallback<HashMap<String, Vault>>() {
            @Override
            public void onCompleted(Exception e, HashMap<String, Vault> result) {
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
                    setDialog(false,"refreshVaults bad finish");
                    return;
                }

                ton.addExtra(SettingValues.VAULTS.toString(), result);

                if (activeVault != null) {
                    Vault v = result.get(activeVault.guid);
                    if (v != null) {
                        // load the vault, use an old reference to transfer the lock
                        loadActiveVault(activeVault);
                    }
                }
                else
                {
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
        ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), item);
        showActiveVault();
    }

    @Override
    public void onListFragmentInteraction(Credential item) {
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
    public void onListFragmentCreatedView() {
        GeneralUtils.debug("Credential view created");
        setDialog(false,"Credential view created");
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
    public void onCredentialFragmentInteraction(Credential credential) {
        GeneralUtils.debug("Cred Interaction event");
    }

    @Override
    public void OnCredentialCreated(Vault v, Credential credential) {
        GeneralUtils.debug("Cred Created event");
    }
}
