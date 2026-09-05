package com.example.autocreateappstorage;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.gjr.glassbutton.GlassCapsuleButton;

import java.io.File;
import java.util.List;

public class MainActivity extends Activity {

    private TextView statusText;
    private TextView totalAppsText;
    private TextView dirsCreatedText;
    private TextView dirsMissingText;
    private GlassCapsuleButton checkButton;
    private GlassCapsuleButton fixButton;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());

        float density = getResources().getDisplayMetrics().density;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Math.round(24 * density), Math.round(48 * density),
                Math.round(24 * density), Math.round(48 * density));

        // 标题
        TextView title = new TextView(this);
        title.setText("自动创建应用存储");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, Math.round(8 * density));
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("AutoCreateAppStorage v2.0");
        subtitle.setTextColor(0xFF888888);
        subtitle.setTextSize(14);
        subtitle.setPadding(0, 0, 0, Math.round(32 * density));
        content.addView(subtitle);

        // 说明
        TextView hint = new TextView(this);
        hint.setText("本模块通过 Magisk service.d 脚本在开机时自动补全所有第三方应用的 Android/data 私有目录，修复自定义 ROM 下应用误报\"存储空间不足\"的问题。\n\n点击下方按钮可手动检测当前存储目录状态。");
        hint.setTextColor(0xFFAAAAAA);
        hint.setTextSize(14);
        hint.setLineSpacing(Math.round(4 * density), 1.2f);
        hint.setPadding(0, 0, 0, Math.round(32 * density));
        content.addView(hint);

        // 检测结果卡片（半透明玻璃风格）
        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setBackgroundColor(0x1AFFFFFF);
        statusCard.setPadding(Math.round(20 * density), Math.round(20 * density),
                Math.round(20 * density), Math.round(20 * density));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, Math.round(24 * density));
        content.addView(statusCard, cardParams);

        TextView cardTitle = new TextView(this);
        cardTitle.setText("检测结果");
        cardTitle.setTextColor(0xFFFFFFFF);
        cardTitle.setTextSize(16);
        cardTitle.setTypeface(null, Typeface.BOLD);
        cardTitle.setPadding(0, 0, 0, Math.round(12 * density));
        statusCard.addView(cardTitle);

        statusText = new TextView(this);
        statusText.setText("点击下方按钮开始检测");
        statusText.setTextColor(0xFF888888);
        statusText.setTextSize(14);
        statusText.setPadding(0, 0, 0, Math.round(16 * density));
        statusCard.addView(statusText);

        totalAppsText = createStatRow(statusCard, "应用总数", "-", density);
        dirsCreatedText = createStatRow(statusCard, "已创建目录", "-", density);
        dirsMissingText = createStatRow(statusCard, "缺失目录", "-", density);

        // 操作区标题
        TextView sectionLabel = new TextView(this);
        sectionLabel.setText("▎操作");
        sectionLabel.setTextColor(0xFFFFFFFF);
        sectionLabel.setTextSize(15);
        sectionLabel.setTypeface(null, Typeface.BOLD);
        sectionLabel.setPadding(0, Math.round(12 * density), 0, Math.round(12 * density));
        content.addView(sectionLabel);

        // 玻璃胶囊按钮 - 检测存储目录
        checkButton = new GlassCapsuleButton(this);
        checkButton.setText("检测存储目录");
        checkButton.setOnClickListener(v -> checkStorage());
        content.addView(checkButton, matchWidth(density));

        // 玻璃胶囊按钮 - 立即修复
        fixButton = new GlassCapsuleButton(this);
        fixButton.setText("立即修复（需 Root）");
        fixButton.setOnClickListener(v -> fixStorage());
        content.addView(fixButton, matchWidth(density));

        // 底部提示
        TextView footerHint = new TextView(this);
        footerHint.setText("Magisk 模块负责开机自动创建，APK 提供管理界面和手动检测/修复");
        footerHint.setTextColor(0xFF666666);
        footerHint.setTextSize(12);
        footerHint.setGravity(Gravity.CENTER);
        footerHint.setPadding(0, Math.round(48 * density), 0, 0);
        content.addView(footerHint);

        scrollView.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
    }

    private TextView createStatRow(LinearLayout parent, String label, String value, float density) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, Math.round(6 * density), 0, Math.round(6 * density));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(0xFFAAAAAA);
        labelView.setTextSize(14);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(0xFFFFFFFF);
        valueView.setTextSize(14);
        valueView.setTypeface(null, Typeface.BOLD);
        row.addView(valueView);

        parent.addView(row);
        return valueView;
    }

    private LinearLayout.LayoutParams matchWidth(float density) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Math.round(12 * density);
        return lp;
    }

    private void checkStorage() {
        checkButton.setEnabled(false);
        checkButton.setGlassSelected(true);
        statusText.setText("正在检测...");
        statusText.setTextColor(0xFFFFC107);

        new Thread(() -> {
            int totalApps = 0;
            int dirsCreated = 0;
            int dirsMissing = 0;
            StringBuilder missingList = new StringBuilder();

            try {
                android.content.pm.PackageManager pm = getPackageManager();
                List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(0);

                for (android.content.pm.ApplicationInfo app : apps) {
                    if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) continue;

                    totalApps++;
                    String pkg = app.packageName;
                    File dataDir = new File("/storage/emulated/0/Android/data/" + pkg);
                    File filesDir = new File(dataDir, "files");
                    File cacheDir = new File(dataDir, "cache");

                    if (dataDir.exists() && filesDir.exists() && cacheDir.exists()) {
                        dirsCreated++;
                    } else {
                        dirsMissing++;
                        if (missingList.length() < 500) {
                            missingList.append(pkg).append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final int finalTotal = totalApps;
            final int finalCreated = dirsCreated;
            final int finalMissing = dirsMissing;
            final String missing = missingList.toString();

            mainHandler.post(() -> {
                totalAppsText.setText(String.valueOf(finalTotal));
                dirsCreatedText.setText(String.valueOf(finalCreated));
                dirsMissingText.setText(String.valueOf(finalMissing));

                if (finalMissing == 0) {
                    statusText.setText("✓ 所有应用存储目录正常");
                    statusText.setTextColor(0xFF4CAF50);
                } else {
                    statusText.setText("✗ 发现 " + finalMissing + " 个应用缺失存储目录\n\n缺失应用：\n" + missing);
                    statusText.setTextColor(0xFFF44336);
                }
                checkButton.setEnabled(true);
                checkButton.setGlassSelected(false);
            });
        }).start();
    }

    private void fixStorage() {
        fixButton.setEnabled(false);
        fixButton.setGlassSelected(true);
        statusText.setText("正在执行修复...");
        statusText.setTextColor(0xFFFFC107);

        new Thread(() -> {
            boolean success = false;
            try {
                Process su = Runtime.getRuntime().exec("su");
                java.io.DataOutputStream os = new java.io.DataOutputStream(su.getOutputStream());
                os.writeBytes("sh /data/adb/modules/auto_create_app_storage/service.d/auto_storage_fix.sh &\n");
                os.writeBytes("exit\n");
                os.flush();
                su.waitFor();
                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            final boolean finalSuccess = success;
            mainHandler.post(() -> {
                if (finalSuccess) {
                    statusText.setText("✓ 修复命令已执行，等待脚本完成后请重新检测");
                    statusText.setTextColor(0xFF4CAF50);
                    Toast.makeText(MainActivity.this, "修复已启动，脚本在后台运行", Toast.LENGTH_LONG).show();
                } else {
                    statusText.setText("✗ 修复失败，请检查是否已授予 Root 权限");
                    statusText.setTextColor(0xFFF44336);
                }
                fixButton.setEnabled(true);
                fixButton.setGlassSelected(false);
            });
        }).start();
    }
}
