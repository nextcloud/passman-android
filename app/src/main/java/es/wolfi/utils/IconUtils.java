package es.wolfi.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;

import com.pixplicity.sharp.Sharp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;

import es.wolfi.app.passman.R;

public class IconUtils {
    public static void loadIconToImageView(String favicon, ImageView credentialIconImageView) {
        if (favicon != null && !favicon.equals("") && !favicon.equals("null")) {
            try {
                JSONObject icon = new JSONObject(favicon);
                if (!icon.getString("type").equals("false") && !icon.getString("content").equals("")) {
                    byte[] byteImageData = Base64.decode(icon.getString("content"), Base64.DEFAULT);
                    Bitmap bitmapImageData = BitmapFactory.decodeByteArray(byteImageData, 0, byteImageData.length);
                    if (bitmapImageData == null) {
                        Sharp.loadInputStream(new ByteArrayInputStream(byteImageData)).into(credentialIconImageView);
                    } else {
                        credentialIconImageView.setImageBitmap(bitmapImageData);
                    }
                } else {
                    credentialIconImageView.setImageResource(R.drawable.ic_baseline_lock_24);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            credentialIconImageView.setImageResource(R.drawable.ic_baseline_lock_24);
        }
    }
}
