#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

HOST_PLATFORM=$(get_platform)
GUEST_PLATFORM=$(get_platform "${ARCH}")
if [ -z "${EMULATOR}" -a "${HOST_PLATFORM}" != "${GUEST_PLATFORM}" ]; then
    case "${GUEST_PLATFORM}" in
    arm)
        EMULATOR="qemu-arm-static"
    ;;
    intel)
        EMULATOR="qemu-i386-static"
    ;;
    *)
        EMULATOR=""
    ;;
    esac
fi

do_configure()
{
    if [ -z "${EMULATOR}" -o "${METHOD}" = "proot" ]; then
        return 0
    fi
    multiarch_support || return 0

    msg ":: Configuring ${COMPONENT} ... "

    local qemu_source=$(which "${EMULATOR}")
    local qemu_target="${CHROOT_DIR}/usr/local/bin/${EMULATOR}"
    [ -e "${qemu_target}" ] && return 0
    [ -z "${qemu_source}" ] && return 1
    if [ ! -d "${CHROOT_DIR}/usr/local/bin" ]; then
        mkdir -p "${CHROOT_DIR}/usr/local/bin"
    fi
    cp "${qemu_source}" "${qemu_target}"
    chroot_exec chown root:root "/usr/local/bin/${EMULATOR}"
    chmod 755 "${qemu_target}"
    return 0
}

do_start()
{
    if [ -z "${EMULATOR}" -o "${METHOD}" = "proot" ]; then
        return 0
    fi
    multiarch_support || return 0

    msg -n ":: Starting ${COMPONENT} ... "
    case "$(get_platform)" in
    arm)
        if [ ! -e "${binfmt_dir}/qemu-i386" ]; then
            echo ":qemu-i386:M::\x7fELF\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x02\x00\x03\x00:\xff\xff\xff\xff\xff\xfe\xfe\xff\xff\xff\xff\xff\xff\xff\xff\xff\xfe\xff\xff\xff:/usr/local/bin/${EMULATOR}:" > "${binfmt_dir}/register"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    intel)
        if [ ! -e "${binfmt_dir}/qemu-arm" ]; then
            echo ":qemu-arm:M::\x7fELF\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x02\x00\x28\x00:\xff\xff\xff\xff\xff\xff\xff\x00\xff\xff\xff\xff\xff\xff\xff\xff\xfe\xff\xff\xff:/usr/local/bin/${EMULATOR}:" > "${binfmt_dir}/register"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    *)
        msg "skip"
    ;;
    esac
    return 0
}

do_stop()
{
    if [ -z "${EMULATOR}" -o "${METHOD}" = "proot" ]; then
        return 0
    fi
    multiarch_support || return 0

    local binfmt_dir="/proc/sys/fs/binfmt_misc"
    local binfmt_qemu=""
    case "$(get_platform)" in
    arm)
        binfmt_qemu="${binfmt_dir}/qemu-i386"
    ;;
    intel)
        binfmt_qemu="${binfmt_dir}/qemu-arm"
    ;;
    esac
    if [ -e "${binfmt_qemu}" ]; then
        msg -n ":: Stopping ${COMPONENT} ... "
        echo -1 > "${binfmt_qemu}"
        is_ok "fail" "done"
    fi
    return 0
}

do_help()
{
cat <<EOF
   --emulator=PATH
     Указать какой использовать эмулятор, по умолчанию QEMU.

EOF
}
