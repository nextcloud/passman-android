/**
 *  Passman Android App
 *
 * @copyright Copyright (c) 2016, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2016, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @license GNU AGPL version 3 or any later version
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package es.wolfi.app.passman;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.support.design.widget.Snackbar;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CopyTextItem extends LinearLayout {

    @BindView(R.id.copy_text_text) TextView text;
    @BindView(R.id.copy_btn_copy) ImageButton copy;
    @BindView(R.id.copy_btn_toggle_visible) ImageButton toggle;

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
        View v = inflate(getContext(), R.layout.fragment_copy_text_item, (ViewGroup) getParent());
        addView(v);
        ButterKnife.bind(this, v);

        setModeText();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        toggle.setVisibility(View.GONE);
    }

    public void setText(String text) {
        this.text.setText(text);
    }

    public void setModePassword() {
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        toggle.setVisibility(View.VISIBLE);
    }

    public void setModeText() {
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        toggle.setVisibility(View.GONE);
    }

    public void setModeEmail() {
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        toggle.setVisibility(View.GONE);
    }

    public void setEnabled(boolean enabled) {
        text.setEnabled(enabled);
    }

    @OnClick(R.id.copy_btn_toggle_visible)
    public void toggleVisibility() {
        switch (text.getInputType()) {
            case InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD:
                text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                break;
            case InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
                text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
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
}
