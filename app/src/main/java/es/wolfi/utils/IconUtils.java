package es.wolfi.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.util.Base64;
import android.widget.ImageView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;

import es.wolfi.app.passman.R;

public class IconUtils {
    public static void loadIconToImageView(String favicon, ImageView credentialIconImageView) {
        credentialIconImageView.setImageResource(R.drawable.ic_baseline_lock_24);
        if (favicon != null && !favicon.equals("") && !favicon.equals("null")) {
            try {
                JSONObject icon = new JSONObject(favicon);
                if (!icon.getString("type").equals("false") && !icon.getString("content").equals("")) {
                    byte[] byteImageData = Base64.decode(icon.getString("content"), Base64.DEFAULT);
                    Bitmap bitmapImageData = BitmapFactory.decodeByteArray(byteImageData, 0, byteImageData.length);
                    if (bitmapImageData != null) {
                        credentialIconImageView.setImageBitmap(bitmapImageData);
                    } else if (icon.getString("type").equals("svg+xml") &&
                            android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SVG svg = SVG.getFromInputStream(new ByteArrayInputStream(byteImageData));
                        credentialIconImageView.setImageDrawable(new PictureDrawable(svg.renderToPicture()));
                    }
                }
            } catch (JSONException | SVGParseException e) {
                e.printStackTrace();
            }
        }
    }
}
