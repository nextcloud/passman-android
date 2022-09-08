package es.wolfi.utils;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.koushikdutta.async.future.FutureCallback;

import java.nio.ByteBuffer;

public class QrCodeAnalyzer implements ImageAnalysis.Analyzer {

    public final static String LOG_TAG = QrCodeAnalyzer.class.getSimpleName();

    private final QRCodeReader qrCodeReader = new QRCodeReader();
    private final FutureCallback<Result> callback;
    private boolean foundToken = false;

    public QrCodeAnalyzer(FutureCallback<Result> callback) {
        this.callback = callback;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        if (foundToken) {
            return;
        }

        Result result = null;
        Exception exception = null;

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

        byte[] imageData = new byte[buffer.remaining()];
        buffer.get(imageData);

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                imageData,
                image.getWidth(),
                image.getHeight(),
                0,
                0,
                image.getWidth(),
                image.getHeight(),
                false
        );

        try {
            result = qrCodeReader.decode(new BinaryBitmap(new HybridBinarizer(source)));
            foundToken = true;
        } catch (NotFoundException | ChecksumException ignored) {
            // Whenever reader fails to detect a QR code in image it throws NotFoundException
            // Whenever reader detect a QR code with inconsistent QR points it throws ChecksumException
        } catch (FormatException e) {
            exception = e;
        } finally {
            qrCodeReader.reset();
        }

        image.close();
        callback.onCompleted(exception, result);
    }
}
