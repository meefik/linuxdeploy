#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="desktop-base x11-xserver-utils xfonts-base xfonts-utils xterm"
        apt_install ${packages}
    ;;
    archlinux:*:*)
        packages="xorg-utils xorg-fonts-misc ttf-dejavu xterm"
        pacman_install ${packages}
    ;;
    fedora:*:*)
        packages="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* xterm"
        yum_install ${packages}
    ;;
    centos:*:*)
        packages="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* xterm"
        yum_install ${packages}
    ;;
    opensuse:*:*)
        packages="xorg-x11-fonts-core dejavu-fonts xauth xterm"
        zypper_install ${packages}
    ;;
    gentoo:*:*)
        packages="xauth xterm"
        emerge_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local xinitrc="${CHROOT_DIR}$(user_home ${USER_NAME})/.xinitrc"
    echo 'xterm -max' >> "${xinitrc}"
    return 0
}
