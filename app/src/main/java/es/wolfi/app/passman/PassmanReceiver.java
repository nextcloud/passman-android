package es.wolfi.app.passman;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;

public class PassmanReceiver extends BroadcastReceiver {

    public static final String CopyUsernameIntentAction = "COPYUSERNAMEINTENTACTION";
    public static final String CopyEmailIntentAction = "COPYEMAILINTENTACTION";
    public static final String CopyPasswordIntentAction = "COPYPASSWORDINTENTACTION";
    public static final String DismissCopyIntentAction = "DISMISSCOPYINTENTACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String intentAction = intent.getAction();

        if (intentAction != null) {
            if (intentAction.startsWith("COPY")) {
                String credGuid = intent.getStringExtra("CredGuid");
                String vaultGuid = intent.getStringExtra("VaultGuid");

                if (credGuid != null && vaultGuid != null) {
                    Vault v = Vault.getVaultByGuid(vaultGuid);
                    if (v != null) {
                        Credential c = v.findCredentialByGUID(credGuid);
                        if (c != null) {
                            switch (intentAction) {
                                case CopyUsernameIntentAction:
                                    copyTextToClipboard(context, "Username", c.getUsername());
                                    break;
                                case CopyEmailIntentAction:
                                    copyTextToClipboard(context, "Email", c.getEmail());
                                    break;
                                case CopyPasswordIntentAction:
                                    copyTextToClipboard(context, "Password", c.getPassword());
                                    break;
                            }
                        }
                    }
                }
            } else if (intentAction.equals(DismissCopyIntentAction)) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

                // notificationId is a unique int for each notification that you must define
                notificationManager.cancelAll();
            }
        }
    }

    public void copyTextToClipboard(Context c, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) c.getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(c, c.getApplicationContext().getString(R.string.copied_to_clipboard) + ": " + label, Toast.LENGTH_SHORT).show();
    }

}
