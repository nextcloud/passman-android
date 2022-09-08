package es.wolfi.app.passman.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.Result;
import com.koushikdutta.async.future.FutureCallback;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.wolfi.app.passman.R;
import es.wolfi.utils.QrCodeAnalyzer;

public class ScanQRCodeActivity extends AppCompatActivity {

    public final static String LOG_TAG = ScanQRCodeActivity.class.getSimpleName();

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    PreviewView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qrcode);

        previewView = findViewById(R.id.preview);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    /**
     * Process result from permission request dialog box.
     * If the request has been granted, start Camera. Otherwise display a toast.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * Check if the required camera permission have been granted
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        Context c = this;

        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.scanner);
        findViewById(R.id.bar).startAnimation(animation);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setTargetRotation(Surface.ROTATION_0).build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackgroundExecutor(mExecutor)
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setTargetRotation(Surface.ROTATION_0).build();

                imageAnalysis.setAnalyzer(mExecutor, new QrCodeAnalyzer(new FutureCallback<Result>() {
                    @Override
                    public void onCompleted(Exception e, Result result) {
                        if (result != null) {
                            Intent intent = new Intent();
                            intent.setData(Uri.parse(result.getText()));
                            setResult(RESULT_OK, intent);
                            finish();
                            return;
                        }
                        if (e != null) {
                            e.printStackTrace();
                            Log.e(LOG_TAG, getString(R.string.error_parsing_qr_code), e);

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(c, getString(R.string.error_parsing_qr_code), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }));

                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle((LifecycleOwner) c, cameraSelector, preview, imageAnalysis);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }
}
