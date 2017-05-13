#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*|ubuntu:*|kalilinux:*)
        packages="desktop-base dbus-x11 x11-xserver-utils xfonts-base xfonts-utils xfce4 xfce4-terminal tango-icon-theme hicolor-icon-theme"
        apt_install ${packages}
    ;;
    archlinux:*)
        packages="xorg-xauth xorg-fonts-misc ttf-dejavu xfce4"
        pacman_install ${packages}
    ;;
    fedora:*)
        packages="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* @xfce-desktop-environment"
        dnf_install ${packages}
    ;;
    centos:*)
        packages="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* @xfce-desktop-environment"
        yum_install ${packages}
    ;;
    opensuse:*)
        packages="xorg-x11-fonts-core dejavu-fonts xauth patterns-openSUSE-xfce"
        zypper_install ${packages}
    ;;
    gentoo:*)
        packages="x11-apps/xauth xfce-base/xfce4-meta"
        emerge_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local xsession="${CHROOT_DIR}$(user_home ${USER_NAME})/.xsession"
    echo 'startxfce4' > "${xsession}"
    return 0
}
