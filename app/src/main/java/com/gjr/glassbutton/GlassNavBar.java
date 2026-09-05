package com.gjr.glassbutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 玻璃拟态底部导航栏。
 *
 * 整体为玻璃胶囊容器（半透明深色底 + 渐变描边），内部横向排列导航项。
 * 选中项：淡白高亮底（0x2EFFFFFF，全圆角）+ 蓝色图标文字（0xFF0A84FF）
 * 未选中项：透明底 + 白色图标文字
 *
 * 用法（纯代码）：
 *   GlassNavBar nav = new GlassNavBar(context);
 *   nav.addItem(icon1, "主页");
 *   nav.addItem(icon2, "配置");
 *   nav.setSelected(0);
 *   nav.setOnItemSelectedListener(index -> { ... });
 */
public class GlassNavBar extends FrameLayout {

    private final List<View> mItemViews = new ArrayList<>();
    private final List<ImageView> mIcons = new ArrayList<>();
    private final List<TextView> mLabels = new ArrayList<>();
    private int mSelectedIndex = -1;
    private OnItemSelectedListener mListener;

    private float mCornerRadiusDp = 28f;
    private float mHeightDp = 76f;

    public interface OnItemSelectedListener {
        void onItemSelected(int index);
    }

    public GlassNavBar(Context context) {
        this(context, null);
    }

    public GlassNavBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlassNavBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlassNavBar);
            float density = context.getResources().getDisplayMetrics().density;
            mCornerRadiusDp = a.getDimension(R.styleable.GlassNavBar_glassCornerRadius,
                    Math.round(28f * density)) / density;
            mHeightDp = a.getDimension(R.styleable.GlassNavBar_glassHeight,
                    Math.round(76f * density)) / density;
            a.recycle();
        }
        init();
    }

    private void init() {
        // 玻璃容器背景
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(GlassButtonStyle.COLOR_NAV_BG);
        bg.setCornerRadius(Math.round(mCornerRadiusDp * density));
        bg.setStroke(Math.round(1 * density), GlassButtonStyle.COLOR_NAV_BORDER);
        setBackground(bg);

        LinearLayout inner = new LinearLayout(getContext());
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER);
        inner.setId(View.generateViewId());
        inner.setTag("glass_nav_inner");
        FrameLayout.LayoutParams innerParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(inner, innerParams);
    }

    private LinearLayout getInner() {
        return (LinearLayout) findViewWithTag("glass_nav_inner");
    }

    /**
     * 添加一个导航项。
     *
     * @param icon  图标 Drawable（会被着色，建议用纯色矢量图）
     * @param label 文字标签
     */
    public void addItem(Drawable icon, String label) {
        Context ctx = getContext();
        float density = ctx.getResources().getDisplayMetrics().density;

        LinearLayout item = new LinearLayout(ctx);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(Math.round(4 * density), Math.round(4 * density),
                Math.round(4 * density), Math.round(4 * density));

        ImageView iconView = new ImageView(ctx);
        iconView.setImageDrawable(icon);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                Math.round(24 * density), Math.round(24 * density));
        iconParams.gravity = Gravity.CENTER;
        item.addView(iconView, iconParams);

        TextView labelView = new TextView(ctx);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, Math.round(2 * density), 0, 0);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.gravity = Gravity.CENTER;
        item.addView(labelView, labelParams);

        final int index = mItemViews.size();
        item.setOnClickListener(v -> setSelected(index));

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                0, LayoutParams.MATCH_PARENT, 1f);
        itemParams.gravity = Gravity.CENTER;
        getInner().addView(item, itemParams);

        mItemViews.add(item);
        mIcons.add(iconView);
        mLabels.add(labelView);

        if (mSelectedIndex == -1) {
            setSelected(0);
        } else {
            updateItemStyle(index, false);
        }
    }

    /**
     * 设置选中项。
     */
    public void setSelected(int index) {
        if (index < 0 || index >= mItemViews.size()) return;
        if (mSelectedIndex == index) return;
        mSelectedIndex = index;
        for (int i = 0; i < mItemViews.size(); i++) {
            updateItemStyle(i, i == index);
        }
        if (mListener != null) {
            mListener.onItemSelected(index);
        }
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mListener = listener;
    }

    private void updateItemStyle(int index, boolean selected) {
        View item = mItemViews.get(index);
        ImageView icon = mIcons.get(index);
        TextView label = mLabels.get(index);
        if (selected) {
            item.setBackground(GlassButtonStyle.createSelectedHighlight());
            icon.setColorFilter(GlassButtonStyle.COLOR_ACCENT, PorterDuff.Mode.SRC_IN);
            label.setTextColor(GlassButtonStyle.COLOR_ACCENT);
        } else {
            item.setBackground(null);
            icon.setColorFilter(GlassButtonStyle.COLOR_WHITE, PorterDuff.Mode.SRC_IN);
            label.setTextColor(GlassButtonStyle.COLOR_WHITE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 固定高度
        float density = getResources().getDisplayMetrics().density;
        int heightPx = Math.round(mHeightDp * density);
        int hSpec = MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, hSpec);
    }
}
