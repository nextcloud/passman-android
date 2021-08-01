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
import android.graphics.Canvas;

import com.google.android.material.snackbar.Snackbar;

import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CopyTextItem extends LinearLayout {

    @BindView(R.id.copy_text_text)
    TextView text;
    @BindView(R.id.copy_btn_copy)
    ImageButton copy;
    @BindView(R.id.copy_btn_toggle_visible)
    ImageButton toggle;
    @BindView(R.id.open_url_btn_toggle_visible)
    ImageButton open_url_toggle;

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
        View v = inflater.inflate(R.layout.fragment_copy_text_item, this, true);

        ButterKnife.bind(this, v);

        setModeText();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        toggle.setVisibility(View.GONE);
        open_url_toggle.setVisibility(View.GONE);
    }

    public void setText(String text) {
        this.text.setText(text);
    }

    public void setModePassword() {
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        toggle.setVisibility(View.VISIBLE);
        open_url_toggle.setVisibility(View.GONE);
    }

    public void setModeText() {
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        toggle.setVisibility(View.GONE);
        open_url_toggle.setVisibility(View.GONE);
    }

    public void setModeEmail() {
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

    @OnClick(R.id.copy_btn_toggle_visible)
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
    }

    @OnClick(R.id.copy_btn_copy)
    public void copyTextToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("pss_data", text.getText().toString());
        clipboard.setPrimaryClip(clip);

        Snackbar.make(this, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
    }

    @OnClick(R.id.open_url_btn_toggle_visible)
    public void openExternalURL() {
        ((PasswordList) Objects.requireNonNull((Activity) getContext())).openExternalURL(this.text.getText().toString());
    }
}
