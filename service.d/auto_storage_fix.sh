#!/system/bin/sh
# AutoCreateAppStorageDir - Magisk service.d script
# 自动补全 Android/data/<pkg> 目录，修复假存储空间不足
MODDIR=${0%/*}

# 等待系统、存储、media 服务完全就绪
sleep 35

PKG_DB="/data/system/packages.xml"

create_app_dir() {
    PKG="$1"
    BASE="/storage/emulated/0/Android/data/${PKG}"
    OBB_DIR="/storage/emulated/0/Android/obb/${PKG}"

    mkdir -p "${BASE}/files"
    mkdir -p "${BASE}/cache"
    mkdir -p "${OBB_DIR}"

    # 获取应用 UID (格式: package:com.xxx uid:12345)
    APP_UID=$(pm list packages -U "${PKG}" 2>/dev/null | awk -F'uid:' '{print $2}' | tr -d ' ')
    if [ -n "${APP_UID}" ]; then
        chown -R "${APP_UID}:${APP_UID}" "${BASE}" 2>/dev/null
        chown -R "${APP_UID}:${APP_UID}" "${OBB_DIR}" 2>/dev/null
    fi

    # 修复 SELinux 上下文（必不可少，否则APP无读写权限）
    restorecon -R "${BASE}" >/dev/null 2>&1
    restorecon -R "${OBB_DIR}" >/dev/null 2>&1
}

scan_all_pkg() {
    pm list packages -3 2>/dev/null | awk -F: '{print $2}' | while read -r pkg; do
        [ -z "${pkg}" ] && continue
        create_app_dir "${pkg}"
    done
}

# 开机首次执行全量修复
scan_all_pkg

# 优先使用 inotifywait 实时监听；不存在则降级为轮询
if command -v inotifywait >/dev/null 2>&1; then
    exec inotifywait -m -e modify,attrib "${PKG_DB}" 2>/dev/null | while read -r _; do
        sleep 2
        scan_all_pkg
    done
else
    # 降级方案：每 20 秒检查 packages.xml 修改时间
    LAST_MTIME=0
    while true; do
        CUR_MTIME=$(stat -c %Y "${PKG_DB}" 2>/dev/null || echo 0)
        if [ "${CUR_MTIME}" != "${LAST_MTIME}" ]; then
            LAST_MTIME="${CUR_MTIME}"
            sleep 2
            scan_all_pkg
        fi
        sleep 20
    done
fi
