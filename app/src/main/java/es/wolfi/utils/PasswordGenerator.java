package es.wolfi.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;

import es.wolfi.app.passman.SettingValues;

public class PasswordGenerator {

    private final Context context;
    private int length = 12;
    private boolean useUppercase = true;
    private boolean useLowercase = true;
    private boolean useDigits = true;
    private boolean useSpecialChars = true;
    private boolean avoidAmbiguousCharacters = true;
    private boolean requireEveryCharType = true;

    public PasswordGenerator(Context context) {
        this.context = context;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        try {
            String cs = settings.getString(SettingValues.PASSWORD_GENERATOR_SETTINGS.toString(), null);
            if (cs != null) {
                JSONObject customPasswordGeneratorSettings = new JSONObject(cs);

                if (customPasswordGeneratorSettings.has("length")) {
                    setLength(customPasswordGeneratorSettings.getInt("length"));
                }
                if (customPasswordGeneratorSettings.has("useUppercase")) {
                    setUseUppercase(customPasswordGeneratorSettings.getBoolean("useUppercase"));
                }
                if (customPasswordGeneratorSettings.has("useLowercase")) {
                    setUseLowercase(customPasswordGeneratorSettings.getBoolean("useLowercase"));
                }
                if (customPasswordGeneratorSettings.has("useDigits")) {
                    setUseDigits(customPasswordGeneratorSettings.getBoolean("useDigits"));
                }
                if (customPasswordGeneratorSettings.has("useSpecialChars")) {
                    setUseSpecialChars(customPasswordGeneratorSettings.getBoolean("useSpecialChars"));
                }
                if (customPasswordGeneratorSettings.has("avoidAmbiguousCharacters")) {
                    setAvoidAmbiguousCharacters(customPasswordGeneratorSettings.getBoolean("avoidAmbiguousCharacters"));
                }
                if (customPasswordGeneratorSettings.has("requireEveryCharType")) {
                    setRequireEveryCharType(customPasswordGeneratorSettings.getBoolean("requireEveryCharType"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void applyChanges() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
        String passwordGeneratorSettingsString = null;

        try {
            JSONObject passwordGeneratorSettings = new JSONObject();

            passwordGeneratorSettings.put("length", getLength());
            passwordGeneratorSettings.put("useUppercase", isUseUppercase());
            passwordGeneratorSettings.put("useLowercase", isUseLowercase());
            passwordGeneratorSettings.put("useDigits", isUseDigits());
            passwordGeneratorSettings.put("useSpecialChars", isUseSpecialChars());
            passwordGeneratorSettings.put("avoidAmbiguousCharacters", isAvoidAmbiguousCharacters());
            passwordGeneratorSettings.put("requireEveryCharType", isRequireEveryCharType());

            passwordGeneratorSettingsString = passwordGeneratorSettings.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        settings.edit().putString(SettingValues.PASSWORD_GENERATOR_SETTINGS.toString(), passwordGeneratorSettingsString).apply();
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isUseUppercase() {
        return useUppercase;
    }

    public void setUseUppercase(boolean useUppercase) {
        this.useUppercase = useUppercase;
    }

    public boolean isUseLowercase() {
        return useLowercase;
    }

    public void setUseLowercase(boolean useLowercase) {
        this.useLowercase = useLowercase;
    }

    public boolean isUseDigits() {
        return useDigits;
    }

    public void setUseDigits(boolean useDigits) {
        this.useDigits = useDigits;
    }

    public boolean isUseSpecialChars() {
        return useSpecialChars;
    }

    public void setUseSpecialChars(boolean useSpecialChars) {
        this.useSpecialChars = useSpecialChars;
    }

    public boolean isAvoidAmbiguousCharacters() {
        return avoidAmbiguousCharacters;
    }

    public void setAvoidAmbiguousCharacters(boolean avoidAmbiguousCharacters) {
        this.avoidAmbiguousCharacters = avoidAmbiguousCharacters;
    }

    public boolean isRequireEveryCharType() {
        return requireEveryCharType;
    }

    public void setRequireEveryCharType(boolean requireEveryCharType) {
        this.requireEveryCharType = requireEveryCharType;
    }

    public String generateRandomPassword() {
        StringBuilder generatedPassword = new StringBuilder();
        SecureRandom rand = new SecureRandom();

        String characterPool = "";
        String lowercaseCharacters = "abcdefghjkmnpqrstuvwxyz";
        String uppercaseCharacters = "ABCDEFGHJKMNPQRSTUVWXYZ";
        String digits = "23456789";
        String specialCharacters = ".!@#$%^&*";

        if (!isAvoidAmbiguousCharacters()) {
            lowercaseCharacters += "ilo";
            uppercaseCharacters += "ILO";
            digits += "10";
        }
        if (isUseLowercase()) {
            characterPool += lowercaseCharacters;
        }
        if (isUseUppercase()) {
            characterPool += uppercaseCharacters;
        }
        if (isUseDigits()) {
            characterPool += digits;
        }
        if (isUseSpecialChars()) {
            characterPool += specialCharacters;
        }

        for (int generatorPosition = 0; generatorPosition < getLength(); generatorPosition++) {
            String customCharacterPool = characterPool;

            if (isRequireEveryCharType()) {
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
