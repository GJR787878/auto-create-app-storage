package com.example.autocreateappstorage;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.gjr.glassbutton.GlassCapsuleButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private TextView totalAppsText;
    private TextView dirsCreatedText;
    private TextView dirsMissingText;
    private GlassCapsuleButton checkButton;
    private GlassCapsuleButton fixButton;
    private GlassCapsuleButton logButton;
    private Handler mainHandler;
    private SharedPreferences sp;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());
        sp = getSharedPreferences("acs", MODE_PRIVATE);
        density = getResources().getDisplayMetrics().density;

        // §3.4 平板适配检测
        boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        // §3.6 根布局 padding：顶48、左右24、底32
        int padLeft = Math.round(24 * density);
        int padTop = Math.round(48 * density);
        int padRight = Math.round(24 * density);
        int padBottom = Math.round(32 * density);
        // §3.4 平板适配：内容区让出左侧导航宽度
        if (isTablet) {
            padLeft += Math.round(120 * density);
        }
        content.setPadding(padLeft, padTop, padRight, padBottom);

        // 平板内容宽度上限 760dp（§3.4）
        int maxContentWidth = Math.round(760 * density);
        ViewGroup.LayoutParams contentLp = new ViewGroup.LayoutParams(
                Math.min(ViewGroup.LayoutParams.MATCH_PARENT, maxContentWidth),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        // 标题
        TextView title = new TextView(this);
        title.setText("自动创建应用存储");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, Math.round(8 * density));
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("AutoCreateAppStorage v2.1");
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

        // 检测结果区域
        TextView cardTitle = new TextView(this);
        cardTitle.setText("检测结果");
        cardTitle.setTextColor(0xFFFFFFFF);
        cardTitle.setTextSize(16);
        cardTitle.setTypeface(null, Typeface.BOLD);
        cardTitle.setPadding(0, 0, 0, Math.round(12 * density));
        content.addView(cardTitle);

        statusText = new TextView(this);
        statusText.setText("点击下方按钮开始检测");
        statusText.setTextColor(0xFF888888);
        statusText.setTextSize(14);
        statusText.setPadding(0, 0, 0, Math.round(16 * density));
        content.addView(statusText);

        totalAppsText = createStatRow(content, "应用总数", "-");
        dirsCreatedText = createStatRow(content, "已创建目录", "-");
        dirsMissingText = createStatRow(content, "缺失目录", "-");

        // 操作区标题
        TextView sectionLabel = new TextView(this);
        sectionLabel.setText("操作");
        sectionLabel.setTextColor(0xFFCCCCCC);
        sectionLabel.setTextSize(14);
        sectionLabel.setPadding(0, Math.round(24 * density), 0, Math.round(12 * density));
        content.addView(sectionLabel);

        // §3.6 所有操作按钮用 GlassCapsuleButton
        checkButton = new GlassCapsuleButton(this);
        checkButton.setText("检测存储目录");
        checkButton.setOnClickListener(v -> checkStorage());
        content.addView(checkButton, matchWidth());

        fixButton = new GlassCapsuleButton(this);
        fixButton.setText("立即修复（需 Root）");
        fixButton.setOnClickListener(v -> fixStorage());
        content.addView(fixButton, matchWidth());

        // 新增：修复日志按钮
        logButton = new GlassCapsuleButton(this);
        logButton.setText("查看修复日志");
        logButton.setOnClickListener(v -> showFixLog());
        content.addView(logButton, matchWidth());

        // 底部提示
        TextView footerHint = new TextView(this);
        footerHint.setText("Magisk 模块负责开机自动创建，APK 提供管理界面和手动检测/修复");
        footerHint.setTextColor(0xFF666666);
        footerHint.setTextSize(12);
        footerHint.setGravity(Gravity.CENTER);
        footerHint.setPadding(0, Math.round(48 * density), 0, 0);
        content.addView(footerHint);

        scrollView.addView(content, contentLp);
        root.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);

        // §3.6 loadConfig()：进入页面恢复状态
        loadConfig();
    }

    private void loadConfig() {
        // 恢复上次检测结果
        totalAppsText.setText(sp.getString("totalApps", "-"));
        dirsCreatedText.setText(sp.getString("dirsCreated", "-"));
        dirsMissingText.setText(sp.getString("dirsMissing", "-"));
        String lastStatus = sp.getString("lastStatus", "点击下方按钮开始检测");
        statusText.setText(lastStatus);
    }

    private TextView createStatRow(LinearLayout parent, String label, String value) {
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

    private LinearLayout.LayoutParams matchWidth() {
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

                // 用 su 检测目录（Root 权限绕过分区存储，普通 File.exists() 在 Android 11+ 不准）
                StringBuilder suCmd = new StringBuilder();
                for (android.content.pm.ApplicationInfo app : apps) {
                    if ((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                    totalApps++;
                    String pkg = app.packageName;
                    // 输出格式：pkg|exists（1=存在，0=缺失）
                    suCmd.append("if [ -d \"/storage/emulated/0/Android/data/").append(pkg).append("/files\" ] && [ -d \"/storage/emulated/0/Android/data/").append(pkg).append("/cache\" ]; then echo \"").append(pkg).append("|1\"; else echo \"").append(pkg).append("|0\"; fi\n");
                }

                // 执行 su 命令
                Process su = Runtime.getRuntime().exec("su");
                java.io.DataOutputStream os = new java.io.DataOutputStream(su.getOutputStream());
                os.writeBytes(suCmd.toString());
                os.writeBytes("exit\n");
                os.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(su.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 2) {
                        if (parts[1].equals("1")) {
                            dirsCreated++;
                        } else {
                            dirsMissing++;
                            if (missingList.length() < 500) {
                                missingList.append(parts[0]).append("\n");
                            }
                        }
                    }
                }
                reader.close();
                su.waitFor();
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

                // §3.6 状态持久化
                sp.edit()
                        .putString("totalApps", String.valueOf(finalTotal))
                        .putString("dirsCreated", String.valueOf(finalCreated))
                        .putString("dirsMissing", String.valueOf(finalMissing))
                        .apply();

                String status;
                if (finalMissing == 0) {
                    status = "✓ 所有应用存储目录正常";
                    statusText.setTextColor(0xFF4CAF50);
                } else {
                    status = "✗ 发现 " + finalMissing + " 个应用缺失存储目录\n\n缺失应用：\n" + missing;
                    statusText.setTextColor(0xFFF44336);
                }
                statusText.setText(status);
                sp.edit().putString("lastStatus", status).apply();

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
                String status;
                if (finalSuccess) {
                    status = "✓ 修复命令已执行，等待脚本完成后请重新检测";
                    statusText.setTextColor(0xFF4CAF50);
                    Toast.makeText(MainActivity.this, "修复已启动，脚本在后台运行", Toast.LENGTH_LONG).show();
                } else {
                    status = "✗ 修复失败，请检查是否已授予 Root 权限";
                    statusText.setTextColor(0xFFF44336);
                }
                statusText.setText(status);
                sp.edit().putString("lastStatus", status).apply();
                fixButton.setEnabled(true);
                fixButton.setGlassSelected(false);
            });
        }).start();
    }

    // 新增：显示修复日志
    private void showFixLog() {
        logButton.setEnabled(false);
        logButton.setGlassSelected(true);

        new Thread(() -> {
            StringBuilder logContent = new StringBuilder();
            try {
                // 尝试读取修复脚本的日志文件
                String[] logPaths = {
                        "/data/adb/modules/auto_create_app_storage/service.d/fix_log.txt",
                        "/data/local/tmp/auto_storage_fix.log"
                };

                boolean found = false;
                for (String path : logPaths) {
                    File logFile = new File(path);
                    if (logFile.exists()) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                new java.io.FileInputStream(logFile)));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            logContent.append(line).append("\n");
                        }
                        reader.close();
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    logContent.append("暂无修复日志记录。\n\n修复脚本执行后，日志会保存在模块目录下。");
                }
            } catch (Exception e) {
                logContent.append("读取日志失败：").append(e.getMessage());
            }

            final String finalLog = logContent.toString();
            mainHandler.post(() -> {
                // §3.6 深色弹窗 + 玻璃胶囊按钮
                LinearLayout dialogRoot = new LinearLayout(this);
                dialogRoot.setOrientation(LinearLayout.VERTICAL);
                dialogRoot.setBackgroundColor(0xFF1C1C1E);
                int pad = Math.round(20 * density);
                dialogRoot.setPadding(pad, Math.round(20 * density), pad, Math.round(16 * density));

                // 标题
                TextView title = new TextView(this);
                title.setText("修复日志");
                title.setTextColor(0xFFFFFFFF);
                title.setTextSize(18);
                title.setTypeface(null, Typeface.BOLD);
                title.setPadding(0, 0, 0, Math.round(12 * density));
                dialogRoot.addView(title);

                // 日志内容（包 ScrollView，§6 #29）
                ScrollView scroll = new ScrollView(this);
                TextView logView = new TextView(this);
                logView.setText(finalLog);
                logView.setTextColor(0xFFCCCCCC);
                logView.setTextSize(13);
                logView.setPadding(0, 0, 0, Math.round(16 * density));
                scroll.addView(logView);
                LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                scrollLp.height = Math.round(300 * density); // 固定高度
                dialogRoot.addView(scroll, scrollLp);

                // 底部玻璃胶囊关闭按钮
                GlassCapsuleButton closeBtn = new GlassCapsuleButton(this);
                closeBtn.setText("关闭");
                LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                closeLp.topMargin = Math.round(16 * density);
                dialogRoot.addView(closeBtn, closeLp);

                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(dialogRoot)
                        .create();
                dialog.show();

                closeBtn.setOnClickListener(v -> dialog.dismiss());

                logButton.setEnabled(true);
                logButton.setGlassSelected(false);
            });
        }).start();
    }
}
