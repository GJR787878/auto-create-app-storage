package com.gjr.glassbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RadioButton;

/**
 * 玻璃拟态胶囊单选按钮。
 *
 * 去掉原生 RadioButton 的圆圈，套用玻璃胶囊样式：
 *   - 未选中：通透玻璃底、细描边、白色文字
 *   - 选中（checked=true）：填充加深、加粗加亮描边、蓝色文字
 *
 * 可直接放入 RadioGroup 使用，与原生 RadioGroup 完全兼容。
 *
 * 用法（纯代码）：
 *   RadioGroup group = new RadioGroup(context);
 *   GlassRadioButton rb1 = new GlassRadioButton(context);
 *   rb1.setText("选项一");
 *   group.addView(rb1);
 */
public class GlassRadioButton extends RadioButton {

    private float mCornerRadiusDp = 28f;
    private GlassButtonDrawable mGlassDrawable;

    public GlassRadioButton(Context context) {
        this(context, null);
    }

    public GlassRadioButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlassRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassRadioButton);
            float density = context.getResources().getDisplayMetrics().density;
            mCornerRadiusDp = a.getDimension(R.styleable.GlassRadioButton_glassCornerRadius,
                    Math.round(28f * density)) / density;
            a.recycle();
        }
        init();
    }

    private void init() {
        setButtonDrawable(null);
        setAllCaps(false);
        setTextSize(14);
        float density = getResources().getDisplayMetrics().density;
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        setPadding(Math.round(24 * density), Math.round(16 * density),
                Math.round(24 * density), Math.round(16 * density));
        applyGlassBackground();
    }

    private void applyGlassBackground() {
        float density = getResources().getDisplayMetrics().density;
        float cornerPx = mCornerRadiusDp * density;
        float borderPx = (isChecked() ? 2f : 1f) * density;
        mGlassDrawable = new GlassButtonDrawable(cornerPx, borderPx, isChecked());
        setBackground(mGlassDrawable);
        setTextColor(isChecked() ? GlassButtonStyle.COLOR_ACCENT : GlassButtonStyle.COLOR_WHITE);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        applyGlassBackground();
    }

    @Override
    public void toggle() {
        super.toggle();
        applyGlassBackground();
    }

    /**
     * 设置圆角半径（dp）。
     */
    public void setGlassCornerRadius(float cornerRadiusDp) {
        mCornerRadiusDp = cornerRadiusDp;
        applyGlassBackground();
    }
}
