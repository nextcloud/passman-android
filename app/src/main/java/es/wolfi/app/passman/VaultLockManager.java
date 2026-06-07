package es.wolfi.app.passman;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import es.wolfi.app.passman.activities.BaseActivity;
import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.passman.API.Vault;

public class VaultLockManager {
    private static final String TAG = "VaultLockManager";
    private static final String CHANNEL_ID = "vault_security";
    private static final int NOTIFICATION_ID = 1001;
    
    private static VaultLockManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable lockRunnable;
    private Runnable countdownRunnable;
    private int timeoutMinutes = 0;
    private long lastInteractionTime = 0;
    private final PassmanApp passmanApp;

    private VaultLockManager(PassmanApp passmanApp) {
        this.passmanApp = passmanApp;
        createNotificationChannel();
    }

    public static synchronized VaultLockManager getInstance(PassmanApp passmanApp) {
        if (instance == null) {
            instance = new VaultLockManager(passmanApp);
        }
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = passmanApp.getString(R.string.security_channel_name);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            NotificationManager notificationManager = passmanApp.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void updateConfig(int minutes) {
        this.timeoutMinutes = minutes;
        resetTimer();
    }

    public void resetTimer() {
        lastInteractionTime = System.currentTimeMillis();
        
        handler.removeCallbacks(lockRunnable);
        handler.removeCallbacks(countdownRunnable);
        
        hideOverlayOnCurrentActivity();

        if (timeoutMinutes <= 0) {
            return;
        }

        long totalDelayMillis = (long) timeoutMinutes * 60 * 1000;
        long countdownStartMillis = totalDelayMillis - 3000;

        if (totalDelayMillis <= 3000) {
            // If timeout is very short, just lock without countdown for now to avoid complexity
            lockRunnable = this::lockActiveVault;
            handler.postDelayed(lockRunnable, totalDelayMillis);
        } else {
            countdownRunnable = () -> startCountdown(3);
            handler.postDelayed(countdownRunnable, countdownStartMillis);
        }
    }

    public void checkLockOnResume() {
        if (timeoutMinutes <= 0) return;

        long currentTime = System.currentTimeMillis();
        long elapsedMillis = currentTime - lastInteractionTime;
        long timeoutMillis = (long) timeoutMinutes * 60 * 1000;

        if (elapsedMillis >= timeoutMillis) {
            Log.d(TAG, "Lock timeout exceeded during background/pause, locking now");
            lockActiveVault();
        } else {
            // Recalculate remaining time and reschedule
            resetTimer();
            // Adjust the timer to account for elapsed time
            handler.removeCallbacks(lockRunnable);
            handler.removeCallbacks(countdownRunnable);
            
            long remainingMillis = timeoutMillis - elapsedMillis;
            if (remainingMillis <= 3000) {
                lockRunnable = this::lockActiveVault;
                handler.postDelayed(lockRunnable, remainingMillis);
            } else {
                countdownRunnable = () -> startCountdown(3);
                handler.postDelayed(countdownRunnable, remainingMillis - 3000);
            }
        }
    }

    private void startCountdown(int seconds) {
        if (seconds <= 0) {
            lockActiveVault();
            return;
        }

        showOverlayOnCurrentActivity(seconds);

        countdownRunnable = () -> startCountdown(seconds - 1);
        handler.postDelayed(countdownRunnable, 1000);
    }

    private void showOverlayOnCurrentActivity(int seconds) {
        Activity currentActivity = passmanApp.getCurrentActivity();
        if (currentActivity instanceof BaseActivity baseActivity) {
            baseActivity.showCountdownOverlay(seconds);
        }
    }

    private void hideOverlayOnCurrentActivity() {
        Activity currentActivity = passmanApp.getCurrentActivity();
        if (currentActivity instanceof BaseActivity baseActivity) {
            baseActivity.hideCountdownOverlay();
        }
    }

    public void lockActiveVault() {
        hideOverlayOnCurrentActivity();
        
        SingleTon ton = SingleTon.getTon();
        Vault vault = (Vault) ton.getExtra(SettingValues.ACTIVE_VAULT.toString());
        if (vault != null && vault.is_unlocked()) {
            Log.d(TAG, "Locking active vault");
            vault.lock();
            ton.addExtra(SettingValues.ACTIVE_VAULT.toString(), vault);
            ton.addExtra(vault.guid, vault);
            
            postLockedNotification();

            Activity currentActivity = passmanApp.getCurrentActivity();
            if (currentActivity instanceof PasswordListActivity passwordListActivity) {
                passwordListActivity.runOnUiThread(passwordListActivity::lockVault);
            }
        }
    }

    private void postLockedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(passmanApp, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(passmanApp.getString(R.string.vault_locked_notification_title))
                .setContentText(passmanApp.getString(R.string.vault_locked_notification_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(passmanApp);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }
}
