package com.example.autocreateappstorage;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public class MainActivity extends Activity {

    private TextView statusText;
    private TextView totalAppsText;
    private TextView dirsCreatedText;
    private TextView dirsMissingText;
    private Button checkButton;
    private Button fixButton;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());

        // 根布局
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF000000);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(48));
        scrollView.addView(root);

        // 标题
        TextView title = new TextView(this);
        title.setText("自动创建应用存储");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title);

        // 副标题
        TextView subtitle = new TextView(this);
        subtitle.setText("AutoCreateAppStorage v2.0");
        subtitle.setTextColor(0xFF888888);
        subtitle.setTextSize(14);
        subtitle.setPadding(0, 0, 0, dp(32));
        root.addView(subtitle);

        // 说明文字
        TextView hint = new TextView(this);
        hint.setText("本模块通过 Magisk service.d 脚本在开机时自动补全所有第三方应用的 Android/data 私有目录，修复自定义 ROM 下应用误报\"存储空间不足\"的问题。\n\n点击下方按钮可手动检测当前存储目录状态。");
        hint.setTextColor(0xFFAAAAAA);
        hint.setTextSize(14);
        hint.setLineSpacing(dp(4), 1.2f);
        hint.setPadding(0, 0, 0, dp(32));
        root.addView(hint);

        // 状态显示区域
        LinearLayout statusBox = new LinearLayout(this);
        statusBox.setOrientation(LinearLayout.VERTICAL);
        statusBox.setBackgroundColor(0xFF1A1A1A);
        statusBox.setPadding(dp(20), dp(20), dp(20), dp(20));
        statusBox.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, 0, 0, dp(24));
        root.addView(statusBox, statusParams);

        // 状态标题
        TextView statusTitle = new TextView(this);
        statusTitle.setText("检测结果");
        statusTitle.setTextColor(0xFFFFFFFF);
        statusTitle.setTextSize(16);
        statusTitle.setTypeface(null, Typeface.BOLD);
        statusTitle.setPadding(0, 0, 0, dp(12));
        statusBox.addView(statusTitle);

        // 状态文字
        statusText = new TextView(this);
        statusText.setText("点击下方按钮开始检测");
        statusText.setTextColor(0xFF888888);
        statusText.setTextSize(14);
        statusText.setPadding(0, 0, 0, dp(16));
        statusBox.addView(statusText);

        // 统计行
        totalAppsText = createStatRow(statusBox, "应用总数", "-");
        dirsCreatedText = createStatRow(statusBox, "已创建目录", "-");
        dirsMissingText = createStatRow(statusBox, "缺失目录", "-");

        // 检测按钮
        checkButton = new Button(this);
        checkButton.setText("检测存储目录");
        checkButton.setTextColor(0xFFFFFFFF);
        checkButton.setTextSize(16);
        checkButton.setBackgroundColor(0xFF2196F3);
        checkButton.setPadding(dp(24), dp(16), dp(24), dp(16));
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 0, 0, dp(16));
        checkButton.setOnClickListener(v -> checkStorage());
        root.addView(checkButton, btnParams);

        // 修复按钮
        fixButton = new Button(this);
        fixButton.setText("立即修复（需 Root）");
        fixButton.setTextColor(0xFFFFFFFF);
        fixButton.setTextSize(16);
        fixButton.setBackgroundColor(0xFF4CAF50);
        fixButton.setPadding(dp(24), dp(16), dp(24), dp(16));
        fixButton.setOnClickListener(v -> fixStorage());
        root.addView(fixButton, btnParams);

        setContentView(scrollView);
    }

    private TextView createStatRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));

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

    private void checkStorage() {
        checkButton.setEnabled(false);
        statusText.setText("正在检测...");
        statusText.setTextColor(0xFFFFC107);

        new Thread(() -> {
            int totalApps = 0;
            int dirsCreated = 0;
            int dirsMissing = 0;
            StringBuilder missingList = new StringBuilder();

            try {
                PackageManager pm = getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(0);

                for (ApplicationInfo app : apps) {
                    // 只检测第三方应用
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;

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
            });
        }).start();
    }

    private void fixStorage() {
        fixButton.setEnabled(false);
        statusText.setText("正在执行修复...");
        statusText.setTextColor(0xFFFFC107);

        new Thread(() -> {
            boolean success = false;
            try {
                // 执行 Magisk 模块的修复脚本
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
            });
        }).start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
