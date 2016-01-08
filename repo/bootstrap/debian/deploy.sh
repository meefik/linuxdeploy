#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

apt_install()
{
    local packages="$@"
    [ -n "${packages}" ] || return 1
    (set -e
        chroot_exec -u root apt-get update -yq
        chroot_exec -u root "DEBIAN_FRONTEND=noninteractive apt-get install -yf"
        chroot_exec -u root "DEBIAN_FRONTEND=noninteractive apt-get install ${packages} --no-install-recommends -yq"
        chroot_exec -u root apt-get clean
    exit 0)
    return $?
}

apt_repository()
{
    case "${DISTRIB}" in
    debian|kalilinux)
        if [ -e "${CHROOT_DIR}/etc/apt/sources.list" ]; then
            cp "${CHROOT_DIR}/etc/apt/sources.list" "${CHROOT_DIR}/etc/apt/sources.list.bak"
        fi
        if [ $(grep -c "${SOURCE_PATH}.*${SUITE}" "${CHROOT_DIR}/etc/apt/sources.list") -eq 0 ]; then
            echo "deb ${SOURCE_PATH} ${SUITE} main contrib non-free" > "${CHROOT_DIR}/etc/apt/sources.list"
            echo "deb-src ${SOURCE_PATH} ${SUITE} main contrib non-free" >> "${CHROOT_DIR}/etc/apt/sources.list"
        fi
    ;;
    ubuntu)
        if [ -e "${CHROOT_DIR}/etc/apt/sources.list" ]; then
            cp "${CHROOT_DIR}/etc/apt/sources.list" "${CHROOT_DIR}/etc/apt/sources.list.bak"
        fi
        if [ $(grep -c "${SOURCE_PATH}.*${SUITE}" "${CHROOT_DIR}/etc/apt/sources.list") -eq 0 ]; then
            echo "deb ${SOURCE_PATH} ${SUITE} main universe multiverse" > "${CHROOT_DIR}/etc/apt/sources.list"
            echo "deb-src ${SOURCE_PATH} ${SUITE} main universe multiverse" >> "${CHROOT_DIR}/etc/apt/sources.list"
        fi
        # Fix for upstart
        if [ -e "${CHROOT_DIR}/sbin/initctl" ]; then
            chroot_exec dpkg-divert --local --rename --add /sbin/initctl
            ln -s /bin/true "${CHROOT_DIR}/sbin/initctl"
        fi
    ;;
    esac
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
    exit 0)
    is_ok || return 1

    component_exec core/emulator core/dns core/mtab

    unset DEBOOTSTRAP_DIR
    chroot_exec /debootstrap/debootstrap --second-stage
    is_ok || return 1

    msg -n "Updating repository ... "
    apt_repository
    is_ok "fail" "done"

    return 0
}
