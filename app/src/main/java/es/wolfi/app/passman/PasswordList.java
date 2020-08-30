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

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatImageButton;

import android.util.Log;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.koushikdutta.async.future.FutureCallback;

import java.util.HashMap;
import java.util.Objects;

import es.wolfi.passman.API.Core;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;

public class PasswordList extends AppCompatActivity implements
        VaultFragment.OnListFragmentInteractionListener,
        CredentialItemFragment.OnListFragmentInteractionListener,
        VaultLockScreen.VaultUnlockInteractionListener,
        CredentialDisplay.OnCredentialFragmentInteraction {
    SharedPreferences settings;
    SingleTon ton;

    static boolean running = false;

    private AppCompatImageButton VaultLockButton;
    //private AppCompatImageButton saveCredentialButton;
    private FloatingActionButton addCredentialsButton;
    private static CredentialAdd currentCredentialAdd;
    private static String activatedBeforeRecreate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_list);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        ton = SingleTon.getTon();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        /*
        this.saveCredentialButton = (AppCompatImageButton) findViewById(R.id.SaveCredentialButton);
        this.saveCredentialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Saving credentials not implemented yet", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                if (currentCredentialAdd == null){
                    return;
                }
                currentCredentialAdd.save(getApplicationContext(), new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        Snackbar.make(view, "Successfully saved: " + result, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                });
            }
        });
        this.saveCredentialButton.setVisibility(View.INVISIBLE);
         */

        this.VaultLockButton = (AppCompatImageButton) findViewById(R.id.VaultLockButton);
        this.VaultLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lockVault();
            }
        });
        this.VaultLockButton.setVisibility(View.INVISIBLE);

        this.addCredentialsButton = (FloatingActionButton) findViewById(R.id.addCredentialsButton);
        this.addCredentialsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCredentialsButton.hide();
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.content_password_list, CredentialAdd.newInstance(), "credential")
                        .addToBackStack(null)
                        .commit();
            }
        });
        this.addCredentialsButton.hide();

        checkFragmentPosition(true);
        if (running) return;

        final ProgressDialog progress = getProgressDialog();
        progress.show();

        final AppCompatActivity self = this;
        Core.checkLogin(this, false, new FutureCallback<Boolean>() {
            @Override
            public void onCompleted(Exception e, Boolean result) {
                // To dismiss the dialog
                progress.dismiss();

                if (result) {
                    showVaults();
                    return;
                }

                // If not logged in, show login form!
                LoginActivity.launch(self, () -> showVaults());
            }
        });

        running = true;
    }

    private ProgressDialog getProgressDialog(){
        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog

        return progress;
    }

    public void showVaults() {
        this.VaultLockButton.setVisibility(View.INVISIBLE);
        Core.getAPIVersion(this, new FutureCallback<Integer>() {
            @Override
            public void onCompleted(Exception e, Integer result) {

            }
        });
        HashMap<String, Vault> vaults = (HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString());
        if (vaults != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.content_password_list, new VaultFragment(), "vaults")
                    .commit();
        } else {
            Vault.getVaults(this, (e, result) -> {
                if (e != null) {
                    // Not logged in, restart activity
                    if (Objects.equals(e.getMessage(), "401")) {
                        recreate();
                    }

                    Toast.makeText(getApplicationContext(), getString(R.string.net_error), Toast.LENGTH_LONG).show();

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showVaults();
                        }
                    }, 30000);
                    return;
                }

                ton.addExtra(SettingValues.VAULTS.toString(), result);
                showVaults();
            });
        }
    }

    public void showActiveVault() {
        ProgressDialog progress = getProgressDialog();
        progress.show();
        Vault vault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        if (vault.getCredentials() != null) {
            if (vault.is_unlocked()) {
                this.VaultLockButton.setVisibility(View.VISIBLE);
                this.addCredentialsButton.show();
                activatedBeforeRecreate = "vault";
                Log.v("Open vault", String.valueOf(vault.vault_id));
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.content_password_list, new CredentialItemFragment(), "vault")
                        .addToBackStack(null)
                        .commit();
            } else {
                showUnlockVault();
            }
        } else {
            Vault.getVault(this, vault.guid, new FutureCallback<Vault>() {
                @Override
                public void onCompleted(Exception e, Vault result) {
                    if (e != null) {
                        // Not logged in, restart activity
                        if (e.getMessage() != null && e.getMessage().equals("401")) {
                            recreate();
                        }

                        Log.e(Tag.CREATOR.toString(), "Unknown network error", e);

                        Toast.makeText(getApplicationContext(), getString(R.string.net_error), Toast.LENGTH_LONG).show();
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showVaults();
                            }
                        }, 30000);
                        return;
                    }

                    // Update the vault record to avoid future loads
                    ((HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString())).put(result.guid, result);

                    ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), result);
                    showActiveVault();
                }
            });
        }
        progress.dismiss();
    }

    void showUnlockVault() {
        activatedBeforeRecreate = "unlockVault";
        this.VaultLockButton.setVisibility(View.VISIBLE);
        Vault v = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        if (v.unlock(settings.getString(v.guid, ""))) {
            showActiveVault();
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_out_left, R.anim.slide_out_left)
                .replace(R.id.content_password_list, new VaultLockScreen(), "vault")
                .addToBackStack(null)
                .commit();
    }

    void lockVault() {
        final Vault vault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        vault.lock();
        ton.removeExtra(vault.guid);
        ton.addExtra(vault.guid, vault);

        ton.removeExtra(SettingValues.ACTIVE_VAULT.toString());
        ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), vault);
        settings.edit().remove(vault.guid).apply();

        Fragment credentialFragment = getSupportFragmentManager().findFragmentByTag("credential");
        if (credentialFragment != null && credentialFragment.isVisible()) {
            onBackPressed();
        }

        onBackPressed();
    }

    void refreshVault() {
        final Vault vault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        Vault.getVault(this, vault.guid, new FutureCallback<Vault>() {
            @Override
            public void onCompleted(Exception e, Vault result) {
                if (e != null) {
                    // Not logged in, restart activity
                    if (e.getMessage() != null && e.getMessage().equals("401")) {
                        recreate();
                    }

                    Log.e(Tag.CREATOR.toString(), "Unknown network error", e);

                    Toast.makeText(getApplicationContext(), getString(R.string.net_error), Toast.LENGTH_LONG).show();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showVaults();
                        }
                    }, 30000);
                    return;
                }

                result.setEncryptionKey(vault.getEncryptionKey());
                ton.removeExtra(vault.guid);
                ton.addExtra(vault.guid, result);
                ton.removeExtra(SettingValues.ACTIVE_VAULT.toString());
                ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), result);

                HashMap<String, Vault> vaults = (HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString());
                vaults.put(vault.guid, result);

                recreate();
            }
        });
    }

    void refreshVaults() {
        ton.removeExtra(SettingValues.VAULTS.toString());
        showVaults();
    }

    public static void triggerRebirth(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        assert intent != null;
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    void refreshButtonPressed() {
        boolean atLeastOneFragmentIsVisible = false;
        for (Fragment x : getSupportFragmentManager().getFragments()) {
            if (x.isVisible()) {
                atLeastOneFragmentIsVisible = true;
            }
        }
        if (!atLeastOneFragmentIsVisible) {
            triggerRebirth(this);
        }

        FragmentManager fm = getSupportFragmentManager();
        Fragment vaultFragment = fm.findFragmentByTag("vault");
        Fragment vaultsFragment = fm.findFragmentByTag("vaults");
        if (vaultFragment != null && vaultFragment.isVisible()) {
            refreshVault();
        } else if (vaultsFragment != null && vaultsFragment.isVisible()) {
            refreshVaults();
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
                refreshButtonPressed();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showNotImplementedMessage() {
        Toast.makeText(this, R.string.not_implemented_yet, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onListFragmentInteraction(Vault item) {
        ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), item);
        showActiveVault();
    }

    @Override
    public void onListFragmentInteraction(Credential item) {
        this.VaultLockButton.setVisibility(View.INVISIBLE);
        this.addCredentialsButton.hide();
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.content_password_list, CredentialDisplay.newInstance(item.getGuid()), "credential")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onVaultUnlock(Vault vault) {
        getSupportFragmentManager().popBackStack();
        showActiveVault();
    }

    @Override
    public void onCredentialFragmentInteraction(Credential credential) {
        this.addCredentialsButton.hide();
    }

    private void checkFragmentPosition(boolean positive) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment vaultFragment = fm.findFragmentByTag("vault");
        Fragment vaultsFragment = fm.findFragmentByTag("vaults");
        Fragment credentialFragment = fm.findFragmentByTag("credential");

        if (positive) {
            if ((vaultFragment != null && vaultFragment.isVisible()) || (activatedBeforeRecreate.equals("vault"))) {
                this.VaultLockButton.setVisibility(View.VISIBLE);
                this.addCredentialsButton.show();
                activatedBeforeRecreate = "";
            } else if (activatedBeforeRecreate.equals("unlockVault")) {
                this.VaultLockButton.setVisibility(View.VISIBLE);
            } else if (credentialFragment != null && credentialFragment.isVisible()) {
                this.VaultLockButton.setVisibility(View.INVISIBLE);
                this.addCredentialsButton.hide();
            } else if (vaultsFragment != null && vaultsFragment.isVisible()) {
                running = true;
            }
        } else {
            if (vaultFragment != null && vaultFragment.isVisible()) {
                this.VaultLockButton.setVisibility(View.INVISIBLE);
                this.addCredentialsButton.hide();
            } else if (credentialFragment != null && credentialFragment.isVisible()) {
                this.VaultLockButton.setVisibility(View.VISIBLE);
                this.addCredentialsButton.show();
            } else if (vaultsFragment != null && vaultsFragment.isVisible()) {
                running = false;
            }
        }
    }

    @Override
    public void onBackPressed() {
        checkFragmentPosition(false);
        super.onBackPressed();
    }
}
