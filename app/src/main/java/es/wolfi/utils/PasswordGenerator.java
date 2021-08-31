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

        String characterPool = "";
        String lowercaseCharacters = "abcdefghjkmnpqrstuvwxyz";
        String uppercaseCharacters = "ABCDEFGHJKMNPQRSTUVWXYZ";
        String digits = "23456789";
        String specialCharacters = ".!@#$%^&*";
        int length = passwordGeneratorSettings.getInt("length");

        if (!passwordGeneratorSettings.getBoolean("avoidAmbiguousCharacters")) {
            lowercaseCharacters += "ilo";
            uppercaseCharacters += "ILO";
            digits += "10";
        }
        if (passwordGeneratorSettings.getBoolean("useLowercase")) {
            characterPool += lowercaseCharacters;
        }
        if (passwordGeneratorSettings.getBoolean("useUppercase")) {
            characterPool += uppercaseCharacters;
        }
        if (passwordGeneratorSettings.getBoolean("useDigits")) {
            characterPool += digits;
        }
        if (passwordGeneratorSettings.getBoolean("useSpecialChars")) {
            characterPool += specialCharacters;
        }

        for (int i = 0; i < length; i++) {
            Random rand = new Random();
            generatedPassword.append(characterPool.charAt(rand.nextInt(characterPool.length())));
        }

        return generatedPassword.toString();
    }
}
