#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

apt_repository()
{
    # Backup sources.list
    if [ -e "${CHROOT_DIR}/etc/apt/sources.list" ]; then
        cp "${CHROOT_DIR}/etc/apt/sources.list" "${CHROOT_DIR}/etc/apt/sources.list.bak"
    fi
    # Fix for resolv problem in xenial
    echo 'Debug::NoDropPrivs true;' > "${CHROOT_DIR}/etc/apt/apt.conf.d/00no-drop-privs"
    # Update sources.list
    echo "deb ${SOURCE_PATH} ${SUITE} main universe multiverse" > "${CHROOT_DIR}/etc/apt/sources.list"
    echo "deb-src ${SOURCE_PATH} ${SUITE} main universe multiverse" >> "${CHROOT_DIR}/etc/apt/sources.list"
    # Fix for upstart
    if [ -e "${CHROOT_DIR}/sbin/initctl" ]; then
        chroot_exec -u root dpkg-divert --local --rename --add /sbin/initctl
        ln -s /bin/true "${CHROOT_DIR}/sbin/initctl"
    fi
}

do_install()
{
    is_archive "${SOURCE_PATH}" && return 0

    msg ":: Installing ${COMPONENT} ... "

    msg -n "Updating repository ... "
    apt_repository
    is_ok "fail" "done"

    return 0
}
