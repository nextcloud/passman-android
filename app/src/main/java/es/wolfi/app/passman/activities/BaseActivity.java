package es.wolfi.app.passman.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import es.wolfi.app.passman.PassmanApp;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.VaultLockManager;

public abstract class BaseActivity extends AppCompatActivity {
    private View countdownOverlay;

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        hideCountdownOverlay();
        VaultLockManager.getInstance((PassmanApp) getApplication()).resetTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VaultLockManager.getInstance((PassmanApp) getApplication()).resetTimer();
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
