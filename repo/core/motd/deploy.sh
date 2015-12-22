#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local linux_version="GNU/Linux"
    if [ -f "${CHROOT_DIR}/etc/os-release" ]
    then
        linux_version=$(. "${CHROOT_DIR}/etc/os-release"; echo ${PRETTY_NAME})
    elif [ -f "${CHROOT_DIR}/etc/gentoo-release" ]
    then
        linux_version=$(cat "${CHROOT_DIR}/etc/gentoo-release")
    elif [ -f "${CHROOT_DIR}/etc/fedora-release" ]
    then
        linux_version=$(cat "${CHROOT_DIR}/etc/fedora-release")
    elif [ -f "${CHROOT_DIR}/etc/redhat-release" ]
    then
        linux_version=$(cat "${CHROOT_DIR}/etc/redhat-release")
    elif [ -f "${CHROOT_DIR}/etc/centos-release" ]
    then
        linux_version=$(cat "${CHROOT_DIR}/etc/centos-release")
    elif [ -f "${CHROOT_DIR}/etc/arch-release" ]
    then
        linux_version="Arch Linux"
    elif [ -f "${CHROOT_DIR}/etc/debian_version" ]
    then
        linux_version=$(printf "Debian GNU/Linux "; cat "${CHROOT_DIR}/etc/debian_version")
    fi
    local motd="${linux_version} [running via Linux Deploy]"
    rm -f "${CHROOT_DIR}/etc/motd"
    echo ${motd} > "${CHROOT_DIR}/etc/motd"
    return 0
}
