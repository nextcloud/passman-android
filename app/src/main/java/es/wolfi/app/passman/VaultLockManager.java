package es.wolfi.app.passman;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.passman.API.Vault;

public class VaultLockManager {
    private static final String TAG = "VaultLockManager";
    private static VaultLockManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable lockRunnable;
    private int timeoutMinutes = 0;
    private final PassmanApp passmanApp;

    private VaultLockManager(PassmanApp passmanApp) {
        this.passmanApp = passmanApp;
    }

    /**
     * @param passmanApp Application instance the VaultLockManager is initially bound to (only needed on first call)
     */
    public static synchronized VaultLockManager getInstance(PassmanApp passmanApp) {
        if (instance == null) {
            instance = new VaultLockManager(passmanApp);
        }
        return instance;
    }

    public void updateConfig(int minutes) {
        this.timeoutMinutes = minutes;
        resetTimer();
    }

    public void resetTimer() {
        handler.removeCallbacks(lockRunnable);
        if (timeoutMinutes <= 0) {
            return;
        }

        lockRunnable = () -> {
            Log.d(TAG, "Inactivity timeout reached, locking vault");
            lockActiveVault();
        };

        handler.postDelayed(lockRunnable, (long) timeoutMinutes * 60 * 1000);
    }

    private void lockActiveVault() {
        SingleTon ton = SingleTon.getTon();
        Vault vault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        if (vault != null && vault.is_unlocked()) {
            vault.lock();
            // Update in singleton
            ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), vault);
            ton.addExtra(vault.guid, vault);
            
            // If the current activity is PasswordListActivity, tell it to update UI
            Activity currentActivity = passmanApp.getCurrentActivity();
            if (currentActivity instanceof PasswordListActivity passwordListActivity) {
                passwordListActivity.runOnUiThread(passwordListActivity::lockVault);
            }
        }
    }
}
