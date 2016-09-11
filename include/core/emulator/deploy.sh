#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${EMULATOR}" ] || EMULATOR=$(get_qemu ${ARCH})

do_configure()
{
    do_start

    return 0
}

do_start()
{
    [ -n "${EMULATOR}" -a "${METHOD}" = "chroot" ] || return 0
    multiarch_support || return 0

    msg -n ":: Starting ${COMPONENT} ... "
    local qemu_path=$(which ${EMULATOR})
    if [ ! -e "${CHROOT_DIR}${qemu_path%/*}" ]; then
        mkdir -p "${CHROOT_DIR}${qemu_path%/*}"
    fi
    if [ ! -e "${CHROOT_DIR}${qemu_path}" ]; then
        touch "${CHROOT_DIR}${qemu_path}"
    fi
    if ! is_mounted "${CHROOT_DIR}${qemu_path}"
    then
        mount -o bind "${qemu_path}" "${CHROOT_DIR}${qemu_path}"
    fi
    case "$(get_platform)" in
    arm)
        if [ ! -e "/proc/sys/fs/binfmt_misc/qemu-i386" ]; then
            echo ":qemu-i386:M::\x7fELF\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x02\x00\x03\x00:\xff\xff\xff\xff\xff\xfe\xfe\xff\xff\xff\xff\xff\xff\xff\xff\xff\xfe\xff\xff\xff:/usr/local/bin/${EMULATOR}:" > "/proc/sys/fs/binfmt_misc/register"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    intel)
        if [ ! -e "/proc/sys/fs/binfmt_misc/qemu-arm" ]; then
            echo ":qemu-arm:M::\x7fELF\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x02\x00\x28\x00:\xff\xff\xff\xff\xff\xff\xff\x00\xff\xff\xff\xff\xff\xff\xff\xff\xfe\xff\xff\xff:/usr/local/bin/${EMULATOR}:" > "/proc/sys/fs/binfmt_misc/register"
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
    [ -n "${EMULATOR}" -a "${METHOD}" = "chroot" ] || return 0
    multiarch_support || return 0

    msg -n ":: Stopping ${COMPONENT} ... "
    local qemu_path=$(which ${EMULATOR})
    if is_mounted "${CHROOT_DIR}${qemu_path}"
    then
        umount "${CHROOT_DIR}${qemu_path}"
        is_ok "fail" "done"
    else
        msg "skip"
    fi
    return 0
}

do_help()
{
cat <<EOF
   --emulator="${EMULATOR}"
     Specify which to use the emulator, by default QEMU.

EOF
}
