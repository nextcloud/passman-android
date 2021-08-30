package es.wolfi.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Random;

import es.wolfi.app.passman.SettingValues;

public class PasswordGenerator {
    private static JSONObject getDefaultPasswordGeneratorSettings() throws JSONException {
        JSONObject passwordGeneratorSettings = new JSONObject();

        passwordGeneratorSettings.put("length", 12);
        passwordGeneratorSettings.put("useUppercase", true);
        passwordGeneratorSettings.put("useLowercase", true);
        passwordGeneratorSettings.put("useDigits", true);
        passwordGeneratorSettings.put("useSpecialChars", true);
        passwordGeneratorSettings.put("avoidAmbiguousCharacters", true);
        passwordGeneratorSettings.put("requireEveryCharType", true);

        return passwordGeneratorSettings;
    }

    public static JSONObject getPasswordGeneratorSettings(Context context) throws JSONException {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        JSONObject passwordGeneratorSettings = getDefaultPasswordGeneratorSettings();

        try {
            String cs = settings.getString(SettingValues.PASSWORD_GENERATOR_SETTINGS.toString(), null);
            if (cs != null) {
                JSONObject customPasswordGeneratorSettings = new JSONObject(cs);
                Iterator<String> keyIterator = passwordGeneratorSettings.keys();
                while (keyIterator.hasNext()) {
                    String key = keyIterator.next();
                    if (!passwordGeneratorSettings.get(key).equals(customPasswordGeneratorSettings.get(key))) {
                        passwordGeneratorSettings.put(key, customPasswordGeneratorSettings.get(key));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return passwordGeneratorSettings;
    }

    public static String generateRandomPassword(Context context) throws JSONException {
        JSONObject passwordGeneratorSettings = getPasswordGeneratorSettings(context);
        StringBuilder generatedPassword = new StringBuilder();

        // Variable naming corresponds to the javascript part in passwordgen.js of Passman
        String h = "";
        String u = "abcdefghjkmnpqrstuvwxyz";
        String l = "ABCDEFGHJKMNPQRSTUVWXYZ";
        String c = "23456789";
        String v = "!@#$%^&*";
        int length = passwordGeneratorSettings.getInt("length");

        if (!passwordGeneratorSettings.getBoolean("avoidAmbiguousCharacters")) {
            u += "ilo";
            l += "ILO";
            c += "10";
        }
        if (passwordGeneratorSettings.getBoolean("useLowercase")) {
            h += u;
        }
        if (passwordGeneratorSettings.getBoolean("useUppercase")) {
            h += l;
        }
        if (passwordGeneratorSettings.getBoolean("useDigits")) {
            h += c;
        }
        if (passwordGeneratorSettings.getBoolean("useSpecialChars")) {
            h += v;
        }

        for (int i = 0; i < length; i++) {
            Random rand = new Random();
            generatedPassword.append(h.charAt(rand.nextInt(h.length())));
        }

        return generatedPassword.toString();
    }
}
