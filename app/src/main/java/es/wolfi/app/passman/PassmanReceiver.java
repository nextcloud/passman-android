package es.wolfi.app.passman;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import es.wolfi.passman.API.Credential;
import es.wolfi.passman.API.Vault;
import es.wolfi.utils.GeneralUtils;

public class PassmanReceiver extends BroadcastReceiver {

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
                        if (c != null)
                        {
                            switch (intentAction) {
                                case "COPYUSERNAMEINTENTACTION":
                                    copyTextToClipboard(context,"Username", c.getUsername());
                                    break;
                                case "COPYEMAILINTENTACTION":
                                    copyTextToClipboard(context,"Email", c.getEmail());
                                    break;
                                case "COPYPASSWORDINTENTACTION":
                                    copyTextToClipboard(context,"Password", c.getPassword());
                                    break;
                            }
                        }
                    }
                }
            }
            else if (intentAction.equals("DISMISSCOPYINTENTACTION"))
            {
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

        GeneralUtils.toast(c.getApplicationContext(),c.getApplicationContext().getString(R.string.copied_to_clipboard) + ": " + label);
    }

}
