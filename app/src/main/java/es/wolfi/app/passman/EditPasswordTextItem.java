/**
 * Passman Android App
 *
 * @copyright Copyright (c) 2017, Andy Scherzinger
 * @copyright Copyright (c) 2017, Sander Brand (brantje@gmail.com)
 * @copyright Copyright (c) 2017, Marcos Zuriaga Miguel (wolfi@wolfi.es)
 * @copyright Copyright (c) 2021, Timo Triebensky (timo@binsky.org)
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

import android.content.Context;
import android.graphics.Canvas;
import android.text.Editable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import es.wolfi.utils.PasswordGenerator;

public class EditPasswordTextItem extends LinearLayout {

    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.toggle_password_visibility_btn)
    ImageButton toggle_password_visibility_btn;
    @BindView(R.id.generate_password_btn)
    ImageButton generate_password_btn;

    public EditPasswordTextItem(Context context) {
        super(context);
        initView();
    }

    public EditPasswordTextItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public EditPasswordTextItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public EditPasswordTextItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    void initView() {
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setOrientation(HORIZONTAL);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.fragment_edit_password_text_item, this, true);

        ButterKnife.bind(this, v);

        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        setPasswordGenerationButtonVisibility(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void setText(String password) {
        this.password.setText(password);
    }

    public Editable getText() {
        return this.password.getText();
    }

    public void setEnabled(boolean enabled) {
        password.setEnabled(enabled);
    }

    public void setPasswordGenerationButtonVisibility(boolean isVisible) {
        if (isVisible) {
            generate_password_btn.setVisibility(View.VISIBLE);
        } else {
            generate_password_btn.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.toggle_password_visibility_btn)
    public void toggleVisibility() {
        switch (password.getInputType()) {
            case InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD:
                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggle_password_visibility_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_off_grey));
                break;
            case InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
                password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggle_password_visibility_btn.setImageDrawable(getResources().getDrawable(R.drawable.ic_eye_grey));
                break;
        }
    }

    @OnClick(R.id.generate_password_btn)
    public void generatePassword() {
        this.password.setText(new PasswordGenerator(getContext()).generateRandomPassword());
    }
}
