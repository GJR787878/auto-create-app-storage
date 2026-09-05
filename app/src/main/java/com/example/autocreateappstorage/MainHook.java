package com.example.autocreateappstorage;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed 模块入口
 * 本模块的核心功能通过 Magisk service.d 脚本实现（开机自动创建目录）
 * LSPosed 部分主要提供管理界面和状态检测
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "AutoCreateAppStorage";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // 模块核心功能由 Magisk service.d 脚本实现
        // LSPosed hook 部分预留用于未来扩展（如 hook 系统存储检测接口）
        if ("android".equals(lpparam.packageName)) {
            XposedBridge.log(TAG + ": Module loaded in system process");
        }
    }
}
