# AutoCreateAppStorageDir

[![Build Magisk Module](https://github.com/GJR787878/auto-create-app-storage/actions/workflows/build.yml/badge.svg)](https://github.com/GJR787878/auto-create-app-storage/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/GJR787878/auto-create-app-storage?label=Release)](https://github.com/GJR787878/auto-create-app-storage/releases/latest)
![Platform](https://img.shields.io/badge/Android-11+-green.svg)
![Magisk](https://img.shields.io/badge/Magisk-26.0+-brightgreen.svg)

**语言：** **中文** | [English](README.en.md) | [Русский](README.ru.md)

> 自动补全缺失的 `/storage/emulated/0/Android/data/<包名>/{files,cache}` 与 `Android/obb` 目录，修复部分自定义 ROM 下应用误报"存储空间不足"的问题。

---

## 问题背景

在部分自定义 ROM（如 crDroid、LineageOS 等）环境下，系统不会自动为已安装应用在 `/storage/emulated/0/Android/data/` 下创建私有目录。当应用调用存储检测接口（如 `isOverStorageFreeSpace`）时，因目标目录不存在而无法获取文件系统信息，**即使实际存储空间充足，也会误报"存储空间不足"**，导致无法添加图片、发送文件、下载附件等。

**典型受影响应用：**
- 豆包 `com.larus.nova`
- Dola `com.larus.wolf`
- 抖音 `com.ss.android.ugc.trill`
- Telegram `org.telegram.messenger`
- 以及所有依赖 Android/data 私有目录的第三方应用

**根因：** `/storage/emulated/0/Android/data/` 目录下几乎为空，应用对应的 `files/` 和 `cache/` 子目录未被系统创建。

---

## 功能特性

- **开机全量修复**：启动后自动扫描所有第三方应用，补全缺失目录
- **实时监听安装**：优先使用 `inotifywait` 监听包数据库变化，新装应用秒级创建目录
- **智能降级**：ROM 无 `inotifywait` 时自动切换为 20 秒轮询模式，无需额外依赖
- **权限修复**：自动 `chown` 为应用所属 UID，确保应用对目录有读写权限
- **SELinux 修复**：自动执行 `restorecon` 恢复正确的安全上下文，避免只建目录但无法访问
- **零冲突**：纯 Shell 脚本实现，不注入 Zygisk，与 LSPosed、Sui、hosts 等模块完全兼容

---

## 环境要求

| 项目 | 要求 |
|---|---|
| Android 版本 | 11 及以上（Scoped Storage 机制） |
| Root | Magisk 26.0+（已测试 Magisk 30.7） |
| 存储 | /data 分区可写 |
| 依赖 | 无额外依赖（inotifywait 缺失时自动降级） |

---

## 安装方法

### 方式一：Magisk 刷入（推荐）

1. 下载最新版模块 zip：
   👉 [Releases 页面下载](https://github.com/GJR787878/auto-create-app-storage/releases/latest)
2. 打开 Magisk → 模块 → 从本地安装
3. 选择下载的 `auto-create-app-storage.zip`
4. 等待安装完成，点击重启

### 方式二：手动构建

```bash
git clone https://github.com/GJR787878/auto-create-app-storage.git
cd auto-create-app-storage
zip -r auto-create-app-storage.zip module.prop service.d/
```

将生成的 zip 传入手机，按方式一刷入。

---

## 验证安装

重启后，在终端（需 Root）执行：

```bash
# 1. 确认模块已启用
ls /data/adb/modules/auto_create_app_storage/

# 2. 确认后台监听进程运行中
ps -A | grep inotifywait

# 3. 检查某个应用的目录是否已创建（以豆包为例）
ls -la /storage/emulated/0/Android/data/com.larus.nova/
```

预期输出应包含 `files` 和 `cache` 两个子目录，且属主为应用自身 UID。

### 功能测试

1. 卸载一个第三方应用（如豆包）
2. 重新安装该应用
3. 等待 5~10 秒
4. 执行 `ls /storage/emulated/0/Android/data/com.larus.nova/`
5. 目录自动出现 → 模块工作正常

---

## 工作原理

```
开机启动
  │
  ├─ service.d 脚本被 Magisk 调用
  │
  ├─ sleep 35（等待系统、存储、Media 服务就绪）
  │
  ├─ 全量扫描：pm list packages -3 → 逐个创建目录
  │     ├─ mkdir -p Android/data/<pkg>/{files,cache}
  │     ├─ mkdir -p Android/obb/<pkg>
  │     ├─ chown <uid>:<uid>（修复所有者）
  │     └─ restorecon -R（修复 SELinux 上下文）
  │
  └─ 持续监听 /data/system/packages.xml
        ├─ 有 inotifywait → 实时监听 modify/attrib 事件
        └─ 无 inotifywait → 每 20 秒检查 mtime 变化
              └─ 检测到变化 → sleep 2 → 重新全量扫描
```

### 为什么需要 restorecon？

仅执行 `mkdir` 创建的目录 SELinux 上下文可能不正确（如被标记为 `untrusted_app_data` 之外的类型），导致应用虽然目录存在但仍无读写权限。`restorecon` 会根据 file_contexts 规则恢复为 `app_data_file`，这是模块能真正解决问题的关键步骤。

---

## 目录结构

```
auto-create-app-storage/
├── module.prop                          # Magisk 模块元信息
├── service.d/
│   └── auto_storage_fix.sh              # 核心脚本（开机执行 + 后台监听）
├── .github/
│   └── workflows/
│       └── build.yml                    # GitHub Actions 自动构建与发布
├── LICENSE                              # MIT 开源协议
├── README.md                            # 中文文档（本文件）
├── README.en.md                         # English documentation
└── README.ru.md                         # Документация на русском
```

---

## 常见问题

### Q: 安装后还是提示存储空间不足？

1. 确认模块在 Magisk 中已启用并重启
2. 检查目录是否真的创建：`ls /storage/emulated/0/Android/data/<包名>/`
3. 如果目录不存在，手动执行一次脚本：`sh /data/adb/modules/auto_create_app_storage/service.d/auto_storage_fix.sh`
4. 确认 SELinux 状态：`getenforce`，如果是 Enforcing 但目录仍无法访问，检查 `ls -Z` 查看上下文

### Q: 系统应用的目录会被创建吗？

不会。脚本只处理第三方应用（`pm list packages -3`），系统应用由系统自身管理。

### Q: 卸载应用后目录会怎样？

系统会自动删除 `/storage/emulated/0/Android/data/<pkg>`。重新安装后，模块检测到包数据库变化会自动重建目录。

### Q: 会影响 /data/data/<pkg> 吗？

不会。模块只处理外部存储 `/storage/emulated/0/Android/data/`，不触碰内部数据目录。

### Q: 与存储隔离类模块冲突吗？

如果使用了 Storage Isolation / 存储隔离类模块，可能会拦截应用对 Android/data 的访问。本模块仅负责创建目录，不改变应用的存储访问策略。

### Q: 如何查看运行日志？

```bash
logcat | grep -i "auto_storage\|inotifywait"
```

---

## 卸载方法

1. Magisk → 模块 → 找到 AutoCreateAppStorageDir → 移除
2. 重启手机
3. 已创建的应用目录不会被删除（由系统或应用自行管理）

---

## 更新日志

### v1.0
- 初始版本
- 开机全量扫描修复所有第三方应用缺失目录
- inotifywait 实时监听 + 轮询降级双模式
- 自动 chown + restorecon 修复权限和 SELinux 上下文
- GitHub Actions 自动构建发布

---

## 许可证

[MIT License](LICENSE)

---

## 致谢

- 本模块受 crDroid / LineageOS 等自定义 ROM 下 Android/data 目录不自动创建问题启发
- 感谢 Magisk 提供的模块化框架
