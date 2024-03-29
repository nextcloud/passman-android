/**
 * Copyright (C) 2015 Bruno Bierbaumer
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.bierbaumer.otp_authenticator;

import android.animation.ObjectAnimator;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.codec.binary.Base32;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class TOTPHelper {
    public static final String SHA1 = "HmacSHA1";

    public static String generate(byte[] secret, int digits, int period) {
        return String.format("%06d", generate(secret, System.currentTimeMillis() / 1000, digits, period));
    }

    public static int generate(byte[] key, long t, int digits, int period) {
        int r = 0;
        try {
            t /= period;
            byte[] data = new byte[8];
            long value = t;
            for (int i = 8; i-- > 0; value >>>= 8) {
                data[i] = (byte) value;
            }

            SecretKeySpec signKey = new SecretKeySpec(key, SHA1);
            Mac mac = Mac.getInstance(SHA1);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);


            int offset = hash[20 - 1] & 0xF;

            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }

            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= Math.pow(10, digits);

            r = (int) truncatedHash;
        } catch (Exception e) {
        }

        return r;
    }

    public static Runnable run(Handler handler, ProgressBar otp_progress, TextView credential_otp,
                               int finalOtpDigits, int finalOtpPeriod, String otpSecret) {
        return new Runnable() {
            @Override
            public void run() {
                int progress = (int) (System.currentTimeMillis() / 1000) % finalOtpPeriod;
                otp_progress.setProgress(progress * 100);

                ObjectAnimator animation = ObjectAnimator.ofInt(otp_progress, "progress", (progress + 1) * 100);
                animation.setDuration(1000);
                animation.setInterpolator(new LinearInterpolator());
                animation.start();

                credential_otp.setText(TOTPHelper.generate(new Base32().decode(otpSecret), finalOtpDigits, finalOtpPeriod));
                handler.postDelayed(this, 1000);
            }
        };
    }

    public static Runnable runAndUpdate(Handler handler, ProgressBar otp_progress, TextView credential_otp,
                                        EditText otpDigits, EditText otpPeriod, EditText otpSecret) {
        return new Runnable() {
            @Override
            public void run() {
                String finalOtpSecret = otpSecret.getText().toString();

                if (!finalOtpSecret.isEmpty()
                        && !otpPeriod.getText().toString().isEmpty()
                        && !otpDigits.getText().toString().isEmpty()) {
                    int finalOtpPeriod = Integer.parseInt(otpPeriod.getText().toString());
                    int finalOtpDigits = Integer.parseInt(otpDigits.getText().toString());

                    otp_progress.setMax(finalOtpPeriod * 100);
                    int progress = (int) (System.currentTimeMillis() / 1000) % finalOtpPeriod;
                    otp_progress.setProgress(progress * 100);

                    ObjectAnimator animation = ObjectAnimator.ofInt(otp_progress, "progress", (progress + 1) * 100);
                    animation.setDuration(1000);
                    animation.setInterpolator(new LinearInterpolator());
                    animation.start();

                    credential_otp.setText(TOTPHelper.generate(new Base32().decode(finalOtpSecret), finalOtpDigits, finalOtpPeriod));
                }

                handler.postDelayed(this, 1000);
            }
        };
    }

    public static JSONObject getCompleteOTPDataAsJSONObject(EditText otp_secret,
                                                            EditText otp_digits,
                                                            EditText otp_period,
                                                            EditText otp_label,
                                                            EditText otp_issuer,
                                                            String qr_uri,
                                                            String algorithm,
                                                            String type) {

        JSONObject otpObj = new JSONObject();

        try {
            if (qr_uri != null && !qr_uri.isEmpty()) {
                otpObj.put("qr_uri", qr_uri);
            }
            if (type != null && !type.isEmpty()) {
                otpObj.put("type", type);
            }

            String label = otp_label.getText().toString();
            if (!label.isEmpty()) {
                otpObj.put("label", label);
            }

            String period = otp_period.getText().toString();
            otpObj.put("period", !period.isEmpty() ? Integer.parseInt(period) : 30);

            String digits = otp_digits.getText().toString();
            otpObj.put("digits", !digits.isEmpty() ? Integer.parseInt(digits) : 6);

            String issuer = otp_issuer.getText().toString();
            if (!issuer.isEmpty()) {
                otpObj.put("issuer", issuer);
            }

            otpObj.put("algorithm", algorithm != null && !algorithm.isEmpty() ? algorithm : "SHA1");

            String secret = otp_secret.getText().toString();
            otpObj.put("secret", !secret.isEmpty() ? secret : "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("TOTPHelper", otpObj.toString());
        return otpObj;
    }

    public static JSONObject getCompleteOTPDataFromQrUrl(String qr_uri) throws JSONException {
        JSONObject otpObj = new JSONObject();

        Uri uri = Uri.parse(qr_uri);
        otpObj.put("qr_uri", qr_uri);

        String type = uri.getHost().equals("totp") ? "totp" : "hotp";
        otpObj.put("type", type);

        String label = uri.getPath().replaceFirst("/", "");
        String algorithm = uri.getQueryParameter("algorithm");
        String period = uri.getQueryParameter("period");
        String digits = uri.getQueryParameter("digits");
        String issuer = uri.getQueryParameter("issuer");

        if (!label.isEmpty()) {
            otpObj.put("label", label);
        }
        if (issuer != null && !issuer.isEmpty()) {
            otpObj.put("issuer", issuer);
        }

        otpObj.put("period", period != null && !period.isEmpty() ? Integer.parseInt(period) : 30);
        otpObj.put("digits", digits != null && !digits.isEmpty() ? Integer.parseInt(digits) : 6);
        otpObj.put("algorithm", algorithm != null && !algorithm.isEmpty() ? algorithm : "SHA1");
        otpObj.put("secret", uri.getQueryParameter("secret"));

        return otpObj;
    }
}
