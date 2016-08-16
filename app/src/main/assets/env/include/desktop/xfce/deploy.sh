#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="desktop-base x11-xserver-utils xfonts-base xfonts-utils xfce4 xfce4-terminal tango-icon-theme hicolor-icon-theme"
        apt_install ${packages}
    ;;
    archlinux:*:*)
        packages="xorg-utils xorg-fonts-misc ttf-dejavu xfce4"
        pacman_install ${packages}
    ;;
    fedora:*:*)
        installgroup="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* xfce-desktop"
        yum_groupinstall ${installgroup}
    ;;
    centos:*:*)
        installgroup="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* xfce-desktop-environment"
        yum_groupinstall ${installgroup}
    ;;
    opensuse:*:*)
        packages="xorg-x11-fonts-core dejavu-fonts xauth patterns-openSUSE-xfce"
        zypper_install ${packages}
    ;;
    gentoo:*:*)
        packages="xauth xfce4-meta"
        emerge_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local xinitrc="${CHROOT_DIR}$(user_home ${USER_NAME})/.xinitrc"
    echo 'startxfce4' >> "${xinitrc}"
    return 0
}
