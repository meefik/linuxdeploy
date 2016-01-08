#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

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
    ;;
    partition)
        if [ ! -b "${TARGET_PATH}" ]; then
            msg "fail"; return 1
        fi
    ;;
    directory|ram)
        if [ -e "${TARGET_PATH}" -a ! -d "${TARGET_PATH}" ]; then
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
            if [ "${DISK_SIZE}" -gt 4095 ]; then
                DISK_SIZE=4095
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
        local fs fs_support
        for fs in ext4 ext3 ext2
        do
            if [ "$(grep -c ${fs} /proc/filesystems)" -gt 0 ]; then
                fs_support=${fs}
                break
            fi
        done
        if [ -z "${fs_support}" ]; then
            msg "The filesystems ext2, ext3 or ext4 is not supported."; return 1
        fi
        if [ -z "${FS_TYPE}" -o "${FS_TYPE}" = "auto" ]; then
            FS_TYPE="${fs_support}"
        fi

        msg -n "Making file system (${FS_TYPE}) ... "
        local loop_exist=$(losetup -a | grep -c "${TARGET_PATH}")
        local img_mounted=$(grep -c "${TARGET_PATH}" /proc/mounts)
        if [ "${loop_exist}" -ne 0 -o "${img_mounted}" -ne 0 ]; then
            msg "fail"; return 1
        fi
        mke2fs -qF -t "${FS_TYPE}" -O ^has_journal "${TARGET_PATH}" >/dev/null
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

rootfs_import()
{
    msg "Getting and unpacking rootfs archive: "
    if [ -n "$(echo ${SOURCE_PATH} | grep -i 'gz$')" ]; then
        if [ -e "${SOURCE_PATH}" ]; then
            tar xzpvf "${SOURCE_PATH}" -C "${CHROOT_DIR}"
            is_ok || return 1
        fi
        if [ -n "$(echo ${SOURCE_PATH} | grep -i '^http')" ]; then
            wget -q -O - "${SOURCE_PATH}" | tar xzpv -C "${CHROOT_DIR}"
            is_ok || return 1
        fi
    fi
    if [ -n "$(echo ${SOURCE_PATH} | grep -i 'bz2$')" ]; then
        if [ -e "${SOURCE_PATH}" ]; then
            tar xjpvf "${SOURCE_PATH}" -C "${CHROOT_DIR}"
            is_ok || return 1
        fi
        if [ -n "$(echo ${SOURCE_PATH} | grep -i '^http')" ]; then
            wget -q -O - "${SOURCE_PATH}" | tar xjpv -C "${CHROOT_DIR}"
            is_ok || return 1
        fi
    fi
    if [ -n "$(echo ${SOURCE_PATH} | grep -i 'xz$')" ]; then
        if [ -e "${SOURCE_PATH}" ]; then
            tar xJpvf "${SOURCE_PATH}" -C "${CHROOT_DIR}"
            is_ok || return 1
        fi
        if [ -n "$(echo ${SOURCE_PATH} | grep -i '^http')" ]; then
            wget -q -O - "${SOURCE_PATH}" | tar xJpv -C "${CHROOT_DIR}"
            is_ok || return 1
        fi
    fi
    if [ $(ls "${CHROOT_DIR}" | wc -l) -le 1 ]; then
        msg " ...installation failed."; return 1
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
   --target-type=file|partition|directory|ram
     Вариант развертывания контейнера.

   --disk-size=SIZE
     Размер файла образа, когда выбран тип развертывания "file". Ноль означает автоматический выбор размера образа.

   --fs-type=ext2|ext3|ext4|auto
     Файловая система, которая будет создана внутри образа или на разделе.

EOF
}
