package es.wolfi.app.passman.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import java.io.FileInputStream;
import java.io.IOException;

import es.wolfi.app.passman.PassmanApp;
import es.wolfi.app.passman.R;
import es.wolfi.app.passman.SettingValues;
import es.wolfi.app.passman.VaultLockManager;
import es.wolfi.app.passman.fragments.CredentialAddFragment;
import es.wolfi.app.passman.fragments.CredentialEditFragment;
import es.wolfi.utils.FileUtils;

public abstract class BaseActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_SCAN_QR_CODE_FOR_OTP_EDIT = 7;
    public static final int REQUEST_CODE_SCAN_QR_CODE_FOR_OTP_ADD = 8;

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

    public void selectFileToAdd(int activityRequestFileCode) {
        //new Intent("android.intent.action.GET_CONTENT").addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, activityRequestFileCode);
    }

    public void scanQRCodeForOTP(int requestCode) {
        startActivityForResult(new Intent(this, ScanQRCodeActivity.class), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCAN_QR_CODE_FOR_OTP_EDIT) { // scan qr code as otp config in credential edit
            if (resultCode != RESULT_OK) {
                return;
            }

            CredentialEditFragment credentialEditFragment = (CredentialEditFragment) getSupportFragmentManager().findFragmentByTag("credentialEdit");
            if (credentialEditFragment != null) {
                credentialEditFragment.processScannedQRCodeData(data.getData().toString());
            }
        }

        if (requestCode == REQUEST_CODE_SCAN_QR_CODE_FOR_OTP_ADD) { // scan qr code as otp config in credential add
            if (resultCode != RESULT_OK) {
                return;
            }

            CredentialAddFragment credentialAddFragment = (CredentialAddFragment) getSupportFragmentManager().findFragmentByTag("credentialAdd");
            if (credentialAddFragment != null) {
                credentialAddFragment.processScannedQRCodeData(data.getData().toString());
            }
        }

        // Following cases should only be handled on positive result
        if (resultCode != RESULT_OK)
            return;

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
            textView.setText(getResources().getQuantityString(R.plurals.vault_locking_in, secondsLeft, secondsLeft));
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
