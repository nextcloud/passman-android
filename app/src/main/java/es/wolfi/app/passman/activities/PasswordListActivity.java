/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2021, Timo Triebensky (timo@binsky.org)
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

package es.wolfi.app.passman.activities;

import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.koushikdutta.async.future.FutureCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

import es.wolfi.app.passman.OfflineStorage;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.SettingsCache;
import es.wolfi.app.passman.SingleTon;
import es.wolfi.app.passman.fragments.CredentialAddFragment;
import es.wolfi.app.passman.fragments.CredentialDisplayFragment;
import es.wolfi.app.passman.fragments.CredentialEditFragment;
import es.wolfi.app.passman.fragments.CredentialItemFragment;
import es.wolfi.app.passman.fragments.SettingsFragment;
import es.wolfi.app.passman.fragments.VaultFragment;
import es.wolfi.app.passman.fragments.VaultLockScreenFragment;
import es.wolfi.passman.API.Core;
import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.File;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.FileUtils;
import es.wolfi.utils.KeyStoreUtils;
import es.wolfi.utils.ProgressUtils;

public class PasswordListActivity extends AppCompatActivity implements
        VaultFragment.OnListFragmentInteractionListener,
        CredentialItemFragment.OnListFragmentInteractionListener,
        VaultLockScreenFragment.VaultUnlockInteractionListener,
        CredentialDisplayFragment.OnCredentialFragmentInteraction,
        CredentialDisplayFragment.OnListFragmentInteractionListener {
    SharedPreferences settings;
    SingleTon ton;

    private static final int REQUEST_CODE_KEYGUARD = 0;
    private static final int REQUEST_CODE_AUTHENTICATE = 1;
    private static final int REQUEST_CODE_CREATE_DOCUMENT = 2;

    static boolean running = false;

    private AppCompatImageButton VaultLockButton;
    private static String activatedBeforeRecreate = "";
    private String intentFilecontent = "";
    HashMap<String, Integer> visibleButtonsBeforeEnterSettings = new HashMap<String, Integer>();
    private ClipboardManager.OnPrimaryClipChangedListener onPrimaryClipChangedListener;
    private int lastCredentialListPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_list);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        ton = SingleTon.getTon();

        updateShortcuts();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        this.VaultLockButton = (AppCompatImageButton) findViewById(R.id.VaultLockButton);
        this.VaultLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lockVault();
            }
        });
        this.VaultLockButton.setVisibility(View.INVISIBLE);

        checkFragmentPosition(true);
        if (running) return;

        new SettingsCache().loadSharedPreferences(getBaseContext());
        KeyStoreUtils.initialize(settings);
        new OfflineStorage(getBaseContext());
        initialAuthentication(false);
    }

    private void initialAuthentication(boolean skipKeyguard) {
        if (!skipKeyguard && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP &&
                settings.getBoolean(SettingValues.ENABLE_APP_START_DEVICE_PASSWORD.toString(), false)) {
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

            if (km.isKeyguardSecure()) {
                Intent authIntent = km.createConfirmDeviceCredentialIntent(getString(R.string.unlock_passman), getString(R.string.unlock_passman_message_device_auth));
                startActivityForResult(authIntent, REQUEST_CODE_KEYGUARD);
            } else {
                initialAuthentication(true);
            }
        } else {
            final ProgressDialog progress = ProgressUtils.showLoadingSequence(this);
            progress.show();

            Core.checkLogin(this, false, new FutureCallback<Boolean>() {
                @Override
                public void onCompleted(Exception e, Boolean loggedIn) {
                    // To dismiss the dialog
                    progress.dismiss();

                    attachClipboardListener();

                    if (loggedIn) {
                        showVaults();
                    } else {
                        // If not logged in, show login form!
                        Intent intent = new Intent(PasswordListActivity.this, LoginActivity.class);
                        startActivityForResult(intent, REQUEST_CODE_AUTHENTICATE);
                    }
                }
            });

            running = true;
        }
    }

    public void attachClipboardListener() {
        ClipboardManager clipBoard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (onPrimaryClipChangedListener != null) {
            clipBoard.removePrimaryClipChangedListener(onPrimaryClipChangedListener);
        }
        int delaySeconds = settings.getInt(SettingValues.CLEAR_CLIPBOARD_DELAY.toString(), 0);
        if (delaySeconds > 0) {
            clipBoard.addPrimaryClipChangedListener(getOnPrimaryClipChangedListener(delaySeconds * 1000));
        }
    }

    private ClipboardManager.OnPrimaryClipChangedListener getOnPrimaryClipChangedListener(int delayMillis) {
        onPrimaryClipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                ClipboardManager clipBoard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = clipBoard.getPrimaryClip();
                ClipData.Item item = clipData.getItemAt(0);
                String clipboardData = item.getText().toString();

                if (!clipboardData.equals("")) {
                    Log.d("clipboard", "got new value");
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // check if clipboard is readable, otherwise clear by default
                            if (clipBoard.hasPrimaryClip()) {
                                ClipData clipDataAfterDelay = clipBoard.getPrimaryClip();
                                ClipData.Item itemAfterDelay = clipDataAfterDelay.getItemAt(0);
                                String clipboardDataAfterDelay = itemAfterDelay.getText().toString();

                                // check that clipboard data was changed in the meantime
                                if (!clipboardData.equals(clipboardDataAfterDelay)) {
                                    return;
                                }
                            }
                            clipBoard.setPrimaryClip(ClipData.newPlainText("", ""));
                            Log.d("clipboard", "cleared");
                        }
                    }, delayMillis);
                }
            }
        };

        return onPrimaryClipChangedListener;
    }

    private void updateShortcuts() {
        if (settings.getBoolean(SettingValues.ENABLE_PASSWORD_GENERATOR_SHORTCUT.toString(), true) &&
                android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Intent shortcutActivityIntent = new Intent(this, ShortcutActivity.class);
            shortcutActivityIntent.setAction(ShortcutActivity.GENERATE_PASSWORD_INTENT_ACTION);

            ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(this, ShortcutActivity.GENERATE_PASSWORD_ID)
                    .setShortLabel(getString(R.string.generate_password))
                    .setLongLabel(getString(R.string.generate_password_to_clipboard))
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_baseline_refresh_24))
                    .setIntent(shortcutActivityIntent)
                    .build();
            ShortcutManagerCompat.addDynamicShortcuts(this, Collections.singletonList(shortcut));
        } else if (!settings.getBoolean(SettingValues.ENABLE_PASSWORD_GENERATOR_SHORTCUT.toString(), true) &&
                android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManagerCompat.removeDynamicShortcuts(this, Collections.singletonList(ShortcutActivity.GENERATE_PASSWORD_ID));
        }
    }

    public void showVaults() {
        this.VaultLockButton.setVisibility(View.INVISIBLE);

        HashMap<String, Vault> vaults = (HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString());
        if (vaults != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.content_password_list, new VaultFragment(), "vaults")
                    .addToBackStack(null)
                    .commit();
        } else {
            this.runOnUiThread(() -> {
                final ProgressDialog progress = ProgressUtils.showLoadingSequence(this);
                progress.show();

                Vault.getVaults(this, (e, result) -> {
                    progress.dismiss();
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
            });
        }
    }

    public void showActiveVault() {
        final ProgressDialog progress = ProgressUtils.showLoadingSequence(this);
        progress.show();
        Vault vault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        if (vault.getCredentials() != null) {
            if (vault.is_unlocked()) {
                this.VaultLockButton.setVisibility(View.VISIBLE);
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
            progress.dismiss();
        } else {
            final AppCompatActivity self = this;
            Vault.getVault(this, vault.guid, new FutureCallback<Vault>() {
                @Override
                public void onCompleted(Exception e, Vault result) {
                    progress.dismiss();
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
                    self.runOnUiThread(() -> {
                        showActiveVault();
                        Vault.updateAutofillVault(result, settings);
                    });
                }
            });
        }
    }

    void showUnlockVault() {
        activatedBeforeRecreate = "unlockVault";
        this.VaultLockButton.setVisibility(View.VISIBLE);
        Vault v = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        if (v.unlock(KeyStoreUtils.getString(v.guid, ""))) {
            showActiveVault();
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_out_left, R.anim.slide_out_left)
                .replace(R.id.content_password_list, new VaultLockScreenFragment(), "vault")
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

    public void addCredentialToCurrentLocalVaultList(Credential credential) {
        final Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
        v.addCredential(credential);

        ((HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString())).put(v.guid, v);
        ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), v);
        Vault.updateAutofillVault(v, settings);
        try {
            OfflineStorage.getInstance().putObject(v.guid, Vault.asJson(v));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Fragment vaultFragment = getSupportFragmentManager().findFragmentByTag("vault");

        if (vaultFragment != null && vaultFragment.isVisible()) {
            Log.e("refreshVault", "load credentials into content password list");
            CredentialItemFragment credentialItems = (CredentialItemFragment)
                    getSupportFragmentManager().findFragmentById(R.id.content_password_list);
            assert credentialItems != null;
            credentialItems.loadCredentialList(findViewById(R.id.content_password_list));
        }
    }

    public void editCredentialInCurrentLocalVaultList(Credential credential) {
        final Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
        v.updateCredential(credential);

        ((HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString())).put(v.guid, v);
        ton.removeExtra(SettingValues.ACTIVE_VAULT.toString());
        ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), v);
        Vault.updateAutofillVault(v, settings);
        try {
            OfflineStorage.getInstance().putObject(v.guid, Vault.asJson(v));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Fragment vaultFragment = getSupportFragmentManager().findFragmentByTag("vault");

        if (vaultFragment != null && vaultFragment.isVisible()) {
            Log.e("refreshVault", "load credentials into content password list");
            CredentialItemFragment credentialItems = (CredentialItemFragment)
                    getSupportFragmentManager().findFragmentById(R.id.content_password_list);
            assert credentialItems != null;
            credentialItems.loadCredentialList(findViewById(R.id.content_password_list));
        }
    }

    public void deleteCredentialInCurrentLocalVaultList(Credential credential) {
        final Vault v = (Vault) SingleTon.getTon().getExtra(SettingValues.ACTIVE_VAULT.toString());
        v.deleteCredential(credential);

        ((HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString())).put(v.guid, v);
        ton.removeExtra(SettingValues.ACTIVE_VAULT.toString());
        ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), v);
        Vault.updateAutofillVault(v, settings);
        try {
            OfflineStorage.getInstance().putObject(v.guid, Vault.asJson(v));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Fragment vaultFragment = getSupportFragmentManager().findFragmentByTag("vault");

        if (vaultFragment != null && vaultFragment.isVisible()) {
            Log.e("refreshVault", "remove credential from content password list");
            CredentialItemFragment credentialItems = (CredentialItemFragment)
                    getSupportFragmentManager().findFragmentById(R.id.content_password_list);
            assert credentialItems != null;
            credentialItems.loadCredentialList(findViewById(R.id.content_password_list));
        }
    }

    public void addVaultToCurrentLocalVaultList(Vault vault) {
        HashMap<String, Vault> vaults = (HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString());
        vaults.put(vault.guid, vault);
        ton.removeExtra(SettingValues.VAULTS.toString());
        ton.addExtra(SettingValues.VAULTS.toString(), vaults);
    }

    public void deleteVaultInCurrentLocalVaultList(Vault vault) {
        HashMap<String, Vault> vaults = (HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString());
        vaults.remove(vault.guid);
        ton.removeExtra(SettingValues.VAULTS.toString());
        ton.addExtra(SettingValues.VAULTS.toString(), vaults);
    }

    void refreshVault() {
        final Vault vault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        ProgressDialog progress = ProgressUtils.showLoadingSequence(this);
        progress.show();
        Vault.getVault(this, vault.guid, new FutureCallback<Vault>() {
            @Override
            public void onCompleted(Exception e, Vault result) {
                progress.dismiss();
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
                Vault.updateAutofillVault(result, settings);

                HashMap<String, Vault> vaults = (HashMap<String, Vault>) ton.getExtra(SettingValues.VAULTS.toString());
                vaults.put(vault.guid, result);

                FragmentManager fm = getSupportFragmentManager();
                Fragment vaultFragment = fm.findFragmentByTag("vault");

                if (vaultFragment != null && vaultFragment.isVisible()) {
                    Log.e("refreshVault", "load credentials into content password list");
                    CredentialItemFragment credentialItems = (CredentialItemFragment)
                            getSupportFragmentManager().findFragmentById(R.id.content_password_list);
                    assert credentialItems != null;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            credentialItems.loadCredentialList(findViewById(R.id.content_password_list));
                        }
                    });
                }
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

    public void applyNewSettings(boolean doRebirth) {
        Toast.makeText(this, R.string.successfully_saved, Toast.LENGTH_SHORT).show();

        updateShortcuts();

        if (doRebirth) {
            triggerRebirth(this);
        } else {
            final ProgressDialog progress = ProgressUtils.showLoadingSequence(this);
            progress.show();
            Core.checkLogin(this, false, new FutureCallback<Boolean>() {
                @Override
                public void onCompleted(Exception e, Boolean loggedIn) {
                    progress.dismiss();

                    if (loggedIn) {
                        showVaults();
                    } else {
                        // If not logged in, show login form!
                        Intent intent = new Intent(PasswordListActivity.this, LoginActivity.class);
                        startActivityForResult(intent, REQUEST_CODE_AUTHENTICATE);
                    }
                }
            });
        }
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

    void settingsButtonPressed() {
        visibleButtonsBeforeEnterSettings.put("VaultLockButton", this.VaultLockButton.getVisibility());

        this.VaultLockButton.setVisibility(View.INVISIBLE);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.content_password_list, SettingsFragment.newInstance(), "settings")
                .addToBackStack(null)
                .commit();
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

        if (!running) {
            initialAuthentication(false);
            return false;
        }

        switch (id) {
            case R.id.action_settings:
                settingsButtonPressed();
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

    public void showLockVaultButton() {
        this.runOnUiThread(() -> this.VaultLockButton.setVisibility(View.VISIBLE));
    }

    private void showNotImplementedMessage() {
        Toast.makeText(this, R.string.not_implemented_yet, Toast.LENGTH_SHORT).show();
    }

    public void openExternalURL(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    @Override
    public void onListFragmentInteraction(Vault item) {
        ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), item);
        showActiveVault();
    }

    @Override
    public void onListFragmentInteraction(Credential item) {
        this.VaultLockButton.setVisibility(View.INVISIBLE);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.content_password_list, CredentialDisplayFragment.newInstance(item.getGuid()), "credential")
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
    }

    @Override
    public void setLastCredentialListPosition(int pos) {
        lastCredentialListPosition = pos;
    }

    @Override
    public int getLastCredentialListPosition() {
        return lastCredentialListPosition;
    }

    @Override
    public void onListFragmentInteraction(File item) {
        Vault v = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());

        final ProgressDialog progress = ProgressUtils.showLoadingSequence(this);
        progress.setMessage(getString(R.string.wait_while_downloading));
        progress.show();

        FutureCallback<String> cb = new FutureCallback<String>() {
            @Override
            public void onCompleted(Exception e, String result) {
                if (result != null) {
                    try {
                        JSONObject o = new JSONObject(result);
                        if (o.has("file_data")) {
                            runOnUiThread(() -> progress.setMessage(getString(R.string.wait_while_decrypting)));
                            String[] decryptedSplitString = v.decryptString(o.getString("file_data")).split(",");
                            if (decryptedSplitString.length == 2) {
                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.putExtra(Intent.EXTRA_TITLE, item.getFilename());
                                intent.setType(item.getMimetype());

                                // intent.putExtra and a later intent.getExtra seem not to work
                                //intent.putExtra("custom_data", decryptedSplitString[1]);
                                intentFilecontent = decryptedSplitString[1];

                                // Optionally, specify a URI for the directory that should be opened in
                                // the system file picker when your app creates the document.
                                //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

                                startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT);
                            }
                        }
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_downloading_file), Toast.LENGTH_SHORT).show();
                    Log.e("FileSave", getString(R.string.error_downloading_file));
                }
                progress.dismiss();
            }
        };
        item.download(getApplicationContext(), cb);
    }

    public void selectFileToAdd(int activityRequestFileCode) {
        //new Intent("android.intent.action.GET_CONTENT").addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, activityRequestFileCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_KEYGUARD) { // initial authentication
            if (resultCode != RESULT_OK) {
                finishAffinity();
                return;
            }
            Log.e("initial authentication", "successful");
            initialAuthentication(true);
        }

        if (requestCode == REQUEST_CODE_AUTHENTICATE) {
            if (resultCode == RESULT_CANCELED) {
                // User cancelled login (i.e. touched "back" button)
                finish();
            } else {
                // Proceed
                showVaults();
            }
        }

        // Following cases should only be handled on positive result
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT) { // download file
            if (data != null) {
                Uri uri = data.getData();

                if (uri != null) {
                    try {
                        byte[] filecontent = Base64.decode(intentFilecontent, Base64.DEFAULT);
                        intentFilecontent = "";
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                        if (pfd != null) {
                            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                            fileOutputStream.write(filecontent);
                            fileOutputStream.close();
                            pfd.close();
                            Toast.makeText(getApplicationContext(), getString(R.string.successfully_saved), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Toast.makeText(getApplicationContext(), getString(R.string.error_writing_file), Toast.LENGTH_SHORT).show();
        }

        if (requestCode >= FileUtils.activityRequestFileCode.credentialEditFile.ordinal() && requestCode <= FileUtils.activityRequestFileCode.credentialAddCustomFieldFile.ordinal()) { //add file
            if (data != null) {
                Uri uri = data.getData();

                if (uri != null) {
                    try {
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                        if (pfd != null) {
                            FileInputStream fileInputStream = new FileInputStream(pfd.getFileDescriptor());
                            int fileSize = fileInputStream.available();
                            byte[] fileContent = new byte[fileSize];

                            int num = fileInputStream.read(fileContent);
                            fileInputStream.close();
                            String realEncodedFile = Base64.encodeToString(fileContent, Base64.DEFAULT | Base64.NO_WRAP);
                            pfd.close();

                            String mimeType = getContentResolver().getType(uri);
                            String fileName = "unknown";
                            String filePathFromUri = FileUtils.getPath(this, uri);
                            if (filePathFromUri != null) {
                                java.io.File file = new java.io.File(filePathFromUri);
                                fileName = file.getName();
                            }

                            try {
                                String encodedFile = String.format("data:%s;base64,%s", mimeType, realEncodedFile);
                                if (requestCode == FileUtils.activityRequestFileCode.credentialEditFile.ordinal() || requestCode == FileUtils.activityRequestFileCode.credentialEditCustomFieldFile.ordinal()) {
                                    CredentialEditFragment credentialEditFragment = (CredentialEditFragment) getSupportFragmentManager().findFragmentByTag("credentialEdit");

                                    // generalize requestCode for usage with generalized ResponseHandler instances
                                    if (requestCode == FileUtils.activityRequestFileCode.credentialEditCustomFieldFile.ordinal()) {
                                        requestCode = FileUtils.activityRequestFileCode.credentialAddCustomFieldFile.ordinal();
                                    } else {
                                        requestCode = FileUtils.activityRequestFileCode.credentialAddFile.ordinal();
                                    }

                                    if (credentialEditFragment != null) {
                                        credentialEditFragment.addSelectedFile(encodedFile, fileName, mimeType, fileSize, requestCode);
                                    }
                                } else if (requestCode == FileUtils.activityRequestFileCode.credentialAddFile.ordinal() || requestCode == FileUtils.activityRequestFileCode.credentialAddCustomFieldFile.ordinal()) {
                                    CredentialAddFragment credentialAddFragment = (CredentialAddFragment) getSupportFragmentManager().findFragmentByTag("credentialAdd");
                                    if (credentialAddFragment != null) {
                                        credentialAddFragment.addSelectedFile(encodedFile, fileName, mimeType, fileSize, requestCode);
                                    }
                                }
                                return;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Toast.makeText(getApplicationContext(), getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkFragmentPosition(boolean positive) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment vaultFragment = fm.findFragmentByTag("vault");
        Fragment vaultsFragment = fm.findFragmentByTag("vaults");
        Fragment credentialFragment = fm.findFragmentByTag("credential");
        Fragment settingsFragment = fm.findFragmentByTag("settings");

        if (positive) {
            if ((vaultFragment != null && vaultFragment.isVisible()) || (activatedBeforeRecreate.equals("vault"))) {
                this.VaultLockButton.setVisibility(View.VISIBLE);
                activatedBeforeRecreate = "";
            } else if (activatedBeforeRecreate.equals("unlockVault")) {
                this.VaultLockButton.setVisibility(View.VISIBLE);
            } else if (credentialFragment != null && credentialFragment.isVisible()) {
                this.VaultLockButton.setVisibility(View.INVISIBLE);
            } else if (vaultsFragment != null && vaultsFragment.isVisible()) {
                running = true;
            }
        } else {
            if (vaultFragment != null && vaultFragment.isVisible()) {
                this.VaultLockButton.setVisibility(View.INVISIBLE);
            } else if (credentialFragment != null && credentialFragment.isVisible()) {
                this.VaultLockButton.setVisibility(View.VISIBLE);
            } else if (vaultsFragment != null && vaultsFragment.isVisible()) {
                running = false;
            } else if (settingsFragment != null && settingsFragment.isVisible()) {
                if (visibleButtonsBeforeEnterSettings.containsKey("VaultLockButton")) {
                    this.VaultLockButton.setVisibility(visibleButtonsBeforeEnterSettings.get("VaultLockButton"));
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        checkFragmentPosition(false);
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        OfflineStorage.getInstance().commit();
        super.onPause();
    }
}
