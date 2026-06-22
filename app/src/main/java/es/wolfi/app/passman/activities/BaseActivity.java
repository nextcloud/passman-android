package es.wolfi.app.passman.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import es.wolfi.app.passman.PassmanApp;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.VaultLockManager;

public abstract class BaseActivity extends AppCompatActivity {
    private View countdownOverlay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyScreenshotProtection();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        hideCountdownOverlay();
        VaultLockManager.getInstance((PassmanApp) getApplication()).resetTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyScreenshotProtection();
        VaultLockManager.getInstance((PassmanApp) getApplication()).checkLockOnResume();
    }

    public void applyScreenshotProtection() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = settings.getBoolean(SettingValues.ENABLE_SCREENSHOT_PROTECTION.toString(), true);
        if (enabled) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public void showCountdownOverlay(int secondsLeft) {
        runOnUiThread(() -> {
            if (countdownOverlay == null) {
                countdownOverlay = LayoutInflater.from(this).inflate(R.layout.layout_lock_countdown, null);
                addContentView(
                        countdownOverlay,
                        new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        )
                );
            }
            countdownOverlay.setVisibility(View.VISIBLE);
            TextView textView = countdownOverlay.findViewById(R.id.lock_countdown_text);
            textView.setText(getString(R.string.vault_locking_in, secondsLeft));
        });
    }

    public void hideCountdownOverlay() {
        runOnUiThread(() -> {
            if (countdownOverlay != null) {
                countdownOverlay.setVisibility(View.GONE);
            }
        });
    }
}
