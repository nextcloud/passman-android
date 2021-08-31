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

    public static void setPasswordGeneratorSettings(Context context, JSONObject passwordGeneratorSettings) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String passwordGeneratorSettingsString = null;

        try {
            JSONObject defaultPasswordGeneratorSettings = getDefaultPasswordGeneratorSettings();
            Iterator<String> keyIterator = defaultPasswordGeneratorSettings.keys();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();

                if (passwordGeneratorSettings.has(key)) {
                    defaultPasswordGeneratorSettings.put(key, passwordGeneratorSettings.get(key));
                }
            }
            passwordGeneratorSettingsString = defaultPasswordGeneratorSettings.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        settings.edit().putString(SettingValues.PASSWORD_GENERATOR_SETTINGS.toString(), passwordGeneratorSettingsString).apply();
    }

    public static String generateRandomPassword(Context context) throws JSONException {
        JSONObject passwordGeneratorSettings = getPasswordGeneratorSettings(context);
        StringBuilder generatedPassword = new StringBuilder();
        Random rand = new Random();

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

        for (int generatorPosition = 0; generatorPosition < length; generatorPosition++) {
            String customCharacterPool = characterPool;

            if (passwordGeneratorSettings.getBoolean("requireEveryCharType")) {
                customCharacterPool = "";
                switch (generatorPosition) {
                    case 0:
                        customCharacterPool += lowercaseCharacters;
                        break;
                    case 1:
                        customCharacterPool += uppercaseCharacters;
                        break;
                    case 2:
                        customCharacterPool += digits;
                        break;
                    case 3:
                        customCharacterPool += specialCharacters;
                        break;
                    default:
                        customCharacterPool = characterPool;
                        break;
                }
            }

            generatedPassword.append(customCharacterPool.charAt(rand.nextInt(customCharacterPool.length())));
        }

        StringBuilder generatedPasswordShuffled = new StringBuilder();
        while (generatedPassword.length() != 0) {
            int index = rand.nextInt(generatedPassword.length());
            char c = generatedPassword.charAt(index);
            generatedPasswordShuffled.append(c);
            generatedPassword.deleteCharAt(index);
        }

        return generatedPasswordShuffled.toString();
    }
}
