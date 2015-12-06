#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

apt_install()
{
    local packages="$@"
    [ -n "${packages}" ] || return 1
    (set -e
        export DEBIAN_FRONTEND=noninteractive
        chroot_exec apt-get update -yq
        chroot_exec apt-get install -yf
        chroot_exec apt-get install ${packages} --no-install-recommends -yq
        chroot_exec apt-get clean
    exit 0) 1>&3 2>&3
    return $?
}

do_install()
{
    is_archive "${SOURCE_PATH}" && return 0

    msg ":: Installing ${COMPONENT} ... "

    local basic_packages="locales,sudo,man-db"
    selinux_support && basic_packages="${basic_packages},selinux-basics"

    (set -e
        DEBOOTSTRAP_DIR="${COMPONENT_DIR}/debootstrap"
        . "${DEBOOTSTRAP_DIR}/debootstrap" --no-check-gpg --foreign --extractor=ar --arch="${ARCH}" --include="${basic_packages}" "${SUITE}" "${CHROOT_DIR}" "${SOURCE_PATH}"
    exit 0) 1>&3 2>&3
    is_ok || return 1

    component_exec core/emulator core/dns core/mtab

    unset DEBOOTSTRAP_DIR
    chroot_exec /debootstrap/debootstrap --second-stage 1>&3 2>&3
    is_ok || return 1

    return 0
}
