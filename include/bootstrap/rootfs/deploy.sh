#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${TARGET_TYPE}" ] || TARGET_TYPE="directory"
[ -n "${TARGET_PATH}" ] || TARGET_PATH="${ENV_DIR}/rootfs/${PROFILE}"
[ -n "${FS_TYPE}" ] || FS_TYPE="auto"
[ -n "${DISK_SIZE}" ] || DISK_SIZE="0"

rootfs_make()
{
    msg -n "Checking installation path ... "
    case "${TARGET_TYPE}" in
    file)
        if [ -e "${TARGET_PATH}" -a ! -f "${TARGET_PATH}" ]; then
            msg "fail"; return 1
        fi
        mkdir -p "${TARGET_PATH%/*}"
    ;;
    directory|ram)
        if [ -e "${TARGET_PATH}" -a ! -d "${TARGET_PATH}" ]; then
            msg "fail"; return 1
        fi
        mkdir -p "${TARGET_PATH}"
    ;;
    partition)
        if [ ! -b "${TARGET_PATH}" ]; then
            msg "fail"; return 1
        fi
    ;;
    esac
    msg "done"

    if [ "${TARGET_TYPE}" = "file" ]; then
        local file_size=0
        if [ -f "${TARGET_PATH}" ]; then
            file_size=$(stat -c %s "${TARGET_PATH}")
        fi
        if [ -z "${DISK_SIZE}" -o "${DISK_SIZE}" -le 0 ]; then
            local block_size=$(stat -c %s -f "${TARGET_PATH%/*}")
            local available_size=$(stat -c %a -f "${TARGET_PATH%/*}")
            let available_size="${block_size}*${available_size}+${file_size}"
            let DISK_SIZE="(${available_size}-${available_size}/10)/1048576"
            if [ "${DISK_SIZE}" -gt 2047 ]; then
                DISK_SIZE=2047
            fi
            if [ "${DISK_SIZE}" -lt 512 ]; then
                DISK_SIZE=512
            fi
        fi
        let file_size="${file_size}/1048576"
        if [ "${DISK_SIZE}" != "${file_size}" ]; then
            msg -n "Making new disk image (${DISK_SIZE} MB) ... "
            dd if=/dev/zero of="${TARGET_PATH}" bs=1048576 seek="$(expr ${DISK_SIZE} - 1)" count=1 ||
            dd if=/dev/zero of="${TARGET_PATH}" bs=1048576 count="${DISK_SIZE}"
            is_ok "fail" "done" || return 1
        fi
    fi

    if [ "${TARGET_TYPE}" = "file" -o "${TARGET_TYPE}" = "partition" ]; then
        msg -n "Making file system ... "
        local loop_exist=$(losetup -a | grep -c "${TARGET_PATH}")
        local img_mounted=$(grep -c "${TARGET_PATH}" /proc/mounts)
        if [ "${loop_exist}" -ne 0 -o "${img_mounted}" -ne 0 ]; then
            msg "fail"; return 1
        fi
        local fs_args
        if [ -n "${FS_TYPE}" -a "${FS_TYPE}" != "auto" ]; then
            fs_args="-t ${FS_TYPE}"
        fi
        mke2fs -qF -O ^has_journal ${fs_args} "${TARGET_PATH}" >/dev/null
        is_ok "fail" "done" || return 1
    fi

    if [ "${TARGET_TYPE}" = "directory" ]; then
        if [ -d "${TARGET_PATH}" ]; then
            chmod -R 755 "${TARGET_PATH}"
            rm -rf "${TARGET_PATH}"
        fi
        mkdir -p "${TARGET_PATH}"
    fi

    if [ "${TARGET_TYPE}" = "ram" ]; then
        umount "${TARGET_PATH}"
        if [ -z "${DISK_SIZE}" -o "${DISK_SIZE}" -le 0 ]; then
            local ram_free=$(grep ^MemFree /proc/meminfo | awk '{print $2}')
            let DISK_SIZE="${ram_free}/1024"
        fi
        msg -n "Making new disk image (${DISK_SIZE} MB) ... "
        if [ ! -d "${TARGET_PATH}" ]; then
            mkdir "${TARGET_PATH}"
        fi
        mount -t tmpfs -o size="${DISK_SIZE}M" tmpfs "${TARGET_PATH}"
        is_ok "fail" "done" || return 1
    fi

    return 0
}

do_install()
{
    if [ "${CHROOT_DIR}" != "${TARGET_PATH}" ]; then
        container_mounted && { msg "The container is already mounted."; return 1; }
    fi

    msg ":: Installing ${COMPONENT} ... "

    rootfs_make || return 1
    container_mount || return 1

    if is_archive "${SOURCE_PATH}" ; then
        rootfs_import "${SOURCE_PATH}"
    fi
}

do_help()
{
cat <<EOF
   --target-type="${TARGET_TYPE}"
     The container deployment type, can specify "file", "directory", "partition" or "ram".

   --target-path="${TARGET_PATH}"
     Installation path depends on the type of deployment.

   --disk-size="${DISK_SIZE}"
     Image file size when selected type of deployment "file". Zero means the automatic selection of the image size.

   --fs-type="${FS_TYPE}"
     File system that will be created inside a image file or on a partition. Supported "ext2", "ext3", "ext4" or "auto".

EOF
}
