# AutoCreateAppStorageDir

[![Build Magisk Module](https://github.com/GJR787878/auto-create-app-storage/actions/workflows/build.yml/badge.svg)](https://github.com/GJR787878/auto-create-app-storage/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/GJR787878/auto-create-app-storage?label=Release)](https://github.com/GJR787878/auto-create-app-storage/releases/latest)
![Platform](https://img.shields.io/badge/Android-11+-green.svg)
![Magisk](https://img.shields.io/badge/Magisk-26.0+-brightgreen.svg)

**Language:** [中文](../README.md) | **English** | [Русский](README.ru.md)

> Automatically creates missing `/storage/emulated/0/Android/data/<package>/{files,cache}` and `Android/obb` directories, fixing the false "Storage space is running out" error on custom ROMs.

---

## Background

On some custom ROMs (such as crDroid, LineageOS, etc.), the system does not automatically create private directories under `/storage/emulated/0/Android/data/` for installed apps. When an app calls a storage detection API (e.g. `isOverStorageFreeSpace`), it cannot obtain filesystem information because the target directory does not exist — **even with plenty of free space, the app falsely reports "insufficient storage"**, preventing users from attaching images, sending files, or downloading attachments.

**Typically affected apps:**
- Doubao `com.larus.nova`
- Dola `com.larus.wolf`
- TikTok / Douyin `com.ss.android.ugc.trill`
- Telegram `org.telegram.messenger`
- Any third-party app that relies on the Android/data private directory

**Root cause:** The `/storage/emulated/0/Android/data/` directory is nearly empty; the app's `files/` and `cache/` subdirectories were never created by the system.

---

## Features

- **Boot-time full scan**: Automatically scans all third-party apps after boot and creates missing directories
- **Real-time install monitoring**: Uses `inotifywait` to watch the package database; newly installed apps get directories within seconds
- **Smart fallback**: Automatically switches to 20-second polling when `inotifywait` is unavailable — no extra dependencies required
- **Ownership fix**: Automatically `chown`s directories to the app's UID, ensuring read/write access
- **SELinux fix**: Automatically runs `restorecon` to restore the correct security context — directories exist AND are accessible
- **Zero conflicts**: Pure Shell script implementation, no Zygisk injection — fully compatible with LSPosed, Sui, hosts, and other modules

---

## Requirements

| Item | Requirement |
|---|---|
| Android version | 11 or higher (Scoped Storage) |
| Root | Magisk 26.0+ (tested with Magisk 30.7) |
| Storage | Writable /data partition |
| Dependencies | None (auto-fallback when inotifywait is missing) |

---

## Installation

### Method 1: Flash via Magisk (recommended)

1. Download the latest module zip:
   👉 [Download from Releases](https://github.com/GJR787878/auto-create-app-storage/releases/latest)
2. Open Magisk → Modules → Install from storage
3. Select the downloaded `auto-create-app-storage.zip`
4. Wait for installation to complete, then reboot

### Method 2: Build manually

```bash
git clone https://github.com/GJR787878/auto-create-app-storage.git
cd auto-create-app-storage
zip -r auto-create-app-storage.zip module.prop service.d/
```

Transfer the generated zip to your phone and flash it using Method 1.

---

## Verify Installation

After reboot, run in a terminal (Root required):

```bash
# 1. Confirm the module is enabled
ls /data/adb/modules/auto_create_app_storage/

# 2. Confirm the background listener is running
ps -A | grep inotifywait

# 3. Check that an app's directory was created (Doubao example)
ls -la /storage/emulated/0/Android/data/com.larus.nova/
```

Expected output should include `files` and `cache` subdirectories, owned by the app's UID.

### Functional test

1. Uninstall a third-party app (e.g. Doubao)
2. Reinstall the app
3. Wait 5–10 seconds
4. Run `ls /storage/emulated/0/Android/data/com.larus.nova/`
5. Directories appear automatically → module is working

---

## How It Works

```
Boot
  │
  ├─ Magisk invokes the service.d script
  │
  ├─ sleep 35 (wait for system, storage, and Media services)
  │
  ├─ Full scan: pm list packages -3 → create dirs for each
  │     ├─ mkdir -p Android/data/<pkg>/{files,cache}
  │     ├─ mkdir -p Android/obb/<pkg>
  │     ├─ chown <uid>:<uid> (fix ownership)
  │     └─ restorecon -R (fix SELinux context)
  │
  └─ Continuously watch /data/system/packages.xml
        ├─ inotifywait available → real-time modify/attrib events
        └─ inotifywait missing → check mtime every 20 seconds
              └─ change detected → sleep 2 → re-scan all packages
```

### Why is restorecon necessary?

Directories created with `mkdir` alone may have an incorrect SELinux context (e.g. not labeled as `app_data_file`), so the app still cannot read/write even though the directory exists. `restorecon` applies the correct `app_data_file` label from file_contexts — this is the critical step that actually solves the problem.

---

## Directory Structure

```
auto-create-app-storage/
├── module.prop                          # Magisk module metadata
├── service.d/
│   └── auto_storage_fix.sh              # Core script (boot + background watcher)
├── .github/
│   └── workflows/
│       └── build.yml                    # GitHub Actions auto-build & release
├── LICENSE                              # MIT License
├── README.md                            # Chinese documentation
├── README.en.md                         # English documentation (this file)
└── README.ru.md                         # Russian documentation
```

---

## FAQ

### Q: Still getting "insufficient storage" after installation?

1. Confirm the module is enabled in Magisk and you have rebooted
2. Check if directories were actually created: `ls /storage/emulated/0/Android/data/<package>/`
3. If missing, run the script manually: `sh /data/adb/modules/auto_create_app_storage/service.d/auto_storage_fix.sh`
4. Check SELinux mode: `getenforce`; if Enforcing and still inaccessible, check context with `ls -Z`

### Q: Are system app directories created?

No. The script only processes third-party apps (`pm list packages -3`). System apps are managed by the system itself.

### Q: What happens to directories when I uninstall an app?

The system automatically deletes `/storage/emulated/0/Android/data/<pkg>`. After reinstalling, the module detects the package database change and recreates the directories automatically.

### Q: Does this affect /data/data/<pkg>?

No. The module only handles external storage `/storage/emulated/0/Android/data/` and never touches internal data directories.

### Q: Conflicts with storage isolation modules?

If you use Storage Isolation modules, they may intercept app access to Android/data. This module only creates directories and does not change an app's storage access policy.

### Q: How do I view runtime logs?

```bash
logcat | grep -i "auto_storage\|inotifywait"
```

---

## Uninstallation

1. Magisk → Modules → find AutoCreateAppStorageDir → Remove
2. Reboot
3. Created app directories are not deleted (managed by the system or the app itself)

---

## Changelog

### v1.0
- Initial release
- Boot-time full scan to fix missing directories for all third-party apps
- Dual-mode: inotifywait real-time monitoring + polling fallback
- Automatic chown + restorecon for ownership and SELinux context
- GitHub Actions automated build and release

---

## License

[MIT License](LICENSE)

---

## Acknowledgments

- Inspired by the Android/data directory not being auto-created issue on crDroid / LineageOS and other custom ROMs
- Thanks to the Magisk project for the modular framework
