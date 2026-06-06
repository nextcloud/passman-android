package es.wolfi.app.passman.activities;

import androidx.appcompat.app.AppCompatActivity;

import es.wolfi.app.passman.PassmanApp;
import es.wolfi.app.passman.VaultLockManager;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        VaultLockManager.getInstance((PassmanApp) getApplication()).resetTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VaultLockManager.getInstance((PassmanApp) getApplication()).resetTimer();
    }
}
