/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2021, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2021, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2024, Timo Triebensky (timo@binsky.org)
 * @license GNU AGPL version 3 or any later version
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.wolfi.utils.otp;

import android.animation.ObjectAnimator;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;


public class TOTPHelper {
    public static final String LOG_TAG = "TOTPHelper";
    public static final int DEFAULT_OTP_DIGITS = 6;
    public static final int DEFAULT_OTP_PERIOD = 30;

    public static Runnable run(Handler handler, ProgressBar otp_progress, TextView credential_otp,
                               int finalOtpDigits, int finalOtpPeriod, String otpSecret, HashingAlgorithm hashingAlgorithm) {
        return new Runnable() {
            @Override
            public void run() {
                int progress = (int) (System.currentTimeMillis() / 1000) % finalOtpPeriod;
                otp_progress.setProgress(progress * 100);

                ObjectAnimator animation = ObjectAnimator.ofInt(otp_progress, "progress", (progress + 1) * 100);
                animation.setDuration(1000);
                animation.setInterpolator(new LinearInterpolator());
                animation.start();

                CodeGenerator codeGenerator = new CodeGenerator(hashingAlgorithm, finalOtpDigits, finalOtpPeriod);
                try {
                    credential_otp.setText(codeGenerator.generate(otpSecret));
                } catch (CodeGenerationException e) {
                    Log.e(LOG_TAG, e.toString());
                    credential_otp.setText("");
                }
                handler.postDelayed(this, 1000);
            }
        };
    }

    public static Runnable runAndUpdate(Handler handler, ProgressBar otp_progress, TextView credential_otp,
                                        EditText otpDigits, EditText otpPeriod, EditText otpSecret, Spinner hashingAlgorithmSpinner) {
        return new Runnable() {
            @Override
            public void run() {
                String finalOtpSecret = otpSecret.getText().toString();
                int finalOtpPeriod = !otpPeriod.getText().toString().isEmpty() ? Integer.parseInt(otpPeriod.getText().toString()) : DEFAULT_OTP_PERIOD;
                int finalOtpDigits = !otpDigits.getText().toString().isEmpty() ? Integer.parseInt(otpDigits.getText().toString()) : DEFAULT_OTP_DIGITS;

                if (!finalOtpSecret.isEmpty()) {
                    otp_progress.setMax(finalOtpPeriod * 100);
                    int progress = (int) (System.currentTimeMillis() / 1000) % finalOtpPeriod;
                    otp_progress.setProgress(progress * 100);

                    ObjectAnimator animation = ObjectAnimator.ofInt(otp_progress, "progress", (progress + 1) * 100);
                    animation.setDuration(1000);
                    animation.setInterpolator(new LinearInterpolator());
                    animation.start();

                    CodeGenerator codeGenerator = new CodeGenerator(
                            HashingAlgorithm.fromStringOrSha1(hashingAlgorithmSpinner.getSelectedItem().toString()),
                            finalOtpDigits,
                            finalOtpPeriod
                    );
                    try {
                        credential_otp.setText(codeGenerator.generate(finalOtpSecret));
                    } catch (CodeGenerationException e) {
                        Log.e(LOG_TAG, e.toString());
                        credential_otp.setText("");
                    }
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
            otpObj.put("period", !period.isEmpty() ? Integer.parseInt(period) : DEFAULT_OTP_PERIOD);

            String digits = otp_digits.getText().toString();
            otpObj.put("digits", !digits.isEmpty() ? Integer.parseInt(digits) : 6);

            String issuer = otp_issuer.getText().toString();
            if (!issuer.isEmpty()) {
                otpObj.put("issuer", issuer);
            }

            otpObj.put("algorithm", algorithm != null && !algorithm.isEmpty() ? algorithm : HashingAlgorithm.SHA1.getFriendlyName());

            String secret = otp_secret.getText().toString();
            otpObj.put("secret", !secret.isEmpty() ? secret : "");
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
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

        otpObj.put("period", period != null && !period.isEmpty() ? Integer.parseInt(period) : DEFAULT_OTP_PERIOD);
        otpObj.put("digits", digits != null && !digits.isEmpty() ? Integer.parseInt(digits) : 6);
        otpObj.put("algorithm", algorithm != null && !algorithm.isEmpty() ? algorithm : HashingAlgorithm.SHA1.getFriendlyName());
        otpObj.put("secret", uri.getQueryParameter("secret"));

        return otpObj;
    }
}
