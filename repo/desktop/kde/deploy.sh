#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="desktop-base x11-xserver-utils xfonts-base xfonts-utils kde-standard"
        apt_install ${packages}
    ;;
    archlinux:*:*)
        packages="xorg-utils xorg-fonts-misc ttf-dejavu kdebase"
        pacman_install ${packages}
    ;;
    fedora:*:*)
        installgroup="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* kde-desktop"
        yum_groupinstall ${installgroup}
    ;;
    opensuse:*:*)
        packages="xorg-x11-fonts-core dejavu-fonts xauth patterns-openSUSE-kde"
        zypper_install ${packages}
    ;;
    gentoo:*:*)
        packages="xauth kde-meta"
        emerge_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local xinitrc="${CHROOT_DIR}$(user_home ${USER_NAME})/.xinitrc"
    echo 'startkde' >> "${xinitrc}"
    return 0
}
