/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2017, Andy Scherzinger
 * @copyright Copyright (c) 2017, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2017, Marcos Zuriaga Miguel (wolfi@wolfi.es)
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
package es.wolfi.app.passman;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.Objects;

import es.wolfi.app.passman.activities.PasswordListActivity;
import es.wolfi.app.passman.databinding.FragmentCopyTextItemBinding;

public class CopyTextItem extends LinearLayout {

    private FragmentCopyTextItemBinding binding;

    TextView text;
    ImageButton copy;
    ImageButton toggle;
    ImageButton open_url_toggle;

    private String rawText = "";
    private boolean passwordMode = false;
    private boolean highlightEnabled = true;

    private static final int CLASS_NONE = -1;
    private static final int CLASS_DIGITS = 0;
    private static final int CLASS_SYMBOLS = 1;
    private static final int CLASS_UPPERCASE = 2;
    private static final int CLASS_LOWERCASE = 3;

    public static final SettingValues[] HIGHLIGHT_COLOR_KEYS = {
            SettingValues.HIGHLIGHT_COLOR_DIGITS,
            SettingValues.HIGHLIGHT_COLOR_SYMBOLS,
            SettingValues.HIGHLIGHT_COLOR_UPPERCASE,
            SettingValues.HIGHLIGHT_COLOR_LOWERCASE,
    };

    public static final int[] HIGHLIGHT_COLOR_DEFAULT_RES = {
            R.color.password_digit,
            R.color.password_symbol,
            R.color.password_default,
            R.color.password_default,
    };

    private final int[] highlightColors = new int[HIGHLIGHT_COLOR_KEYS.length];

    public CopyTextItem(Context context) {
        super(context);
        initView();
    }

    public CopyTextItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CopyTextItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @TargetApi(21)
    public CopyTextItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    void initView() {
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setOrientation(HORIZONTAL);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = FragmentCopyTextItemBinding.inflate(inflater, this);

        text = binding.copyTextText;
        copy = binding.copyBtnCopy;
        toggle = binding.copyBtnToggleVisible;
        open_url_toggle = binding.openUrlBtnToggleVisible;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        highlightEnabled = settings.getBoolean(SettingValues.ENABLE_PASSWORD_CHARACTER_HIGHLIGHTING.toString(), true);
        for (int i = 0; i < HIGHLIGHT_COLOR_KEYS.length; i++) {
            highlightColors[i] = settings.getInt(HIGHLIGHT_COLOR_KEYS[i].toString(),
                    ContextCompat.getColor(getContext(), HIGHLIGHT_COLOR_DEFAULT_RES[i]));
        }

        setModeText();

        binding.copyBtnToggleVisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVisibility();
            }
        });
        binding.copyBtnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyTextToClipboard();
            }
        });
        binding.openUrlBtnToggleVisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExternalURL();
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        toggle.setVisibility(View.GONE);
        open_url_toggle.setVisibility(View.GONE);
    }

    public void setText(String text) {
        this.rawText = text != null ? text : "";
        refreshDisplayedText();
    }

    public TextView getTextView() {
        return this.text;
    }

    public void setModePassword() {
        passwordMode = true;
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        toggle.setVisibility(View.VISIBLE);
        open_url_toggle.setVisibility(View.GONE);
        refreshDisplayedText();
    }

    public void setModeText() {
        passwordMode = false;
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        toggle.setVisibility(View.GONE);
        open_url_toggle.setVisibility(View.GONE);
    }

    public void setModeEmail() {
        passwordMode = false;
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        toggle.setVisibility(View.GONE);
        open_url_toggle.setVisibility(View.GONE);
    }

    public void setModeURL() {
        setModeText();
        open_url_toggle.setVisibility(View.VISIBLE);
    }

    public void setEnabled(boolean enabled) {
        text.setEnabled(enabled);
    }

    public void toggleVisibility() {
        switch (text.getInputType()) {
            case InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD:
                text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggle.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_off_grey));
                break;
            case InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
                text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggle.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_grey));
                break;
        }
        refreshDisplayedText();
    }

    private void refreshDisplayedText() {
        if (passwordMode && isPasswordRevealed() && highlightEnabled) {
            SpannableString spannable = new SpannableString(rawText);
            for (int i = 0; i < rawText.length(); i++) {
                int characterClass = getCharacterClass(rawText.charAt(i));
                if (characterClass == CLASS_NONE) {
                    continue;
                }
                int color = highlightColors[characterClass];
                if (color != Color.TRANSPARENT) {
                    spannable.setSpan(new ForegroundColorSpan(color), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            text.setText(spannable);
        } else {
            text.setText(rawText);
        }
    }

    private static int getCharacterClass(char character) {
        if (Character.isDigit(character)) {
            return CLASS_DIGITS;
        }
        if (Character.isUpperCase(character)) {
            return CLASS_UPPERCASE;
        }
        if (Character.isLowerCase(character)) {
            return CLASS_LOWERCASE;
        }
        // Caseless letters (CJK etc.), whitespace, surrogate halves (emoji) and combining marks stay uncolored
        if (Character.isLetter(character) || Character.isWhitespace(character) || Character.isSpaceChar(character)
                || Character.isISOControl(character) || Character.isSurrogate(character)) {
            return CLASS_NONE;
        }
        int type = Character.getType(character);
        if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK || type == Character.FORMAT) {
            return CLASS_NONE;
        }
        return CLASS_SYMBOLS;
    }

    private boolean isPasswordRevealed() {
        return text.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }

    public void copyTextToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("pss_data", text.getText().toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public void openExternalURL() {
        ((PasswordListActivity) Objects.requireNonNull((Activity) getContext())).openExternalURL(this.text.getText().toString());
    }
}
