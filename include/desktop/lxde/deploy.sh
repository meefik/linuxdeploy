#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*|ubuntu:*|kalilinux:*)
        packages="desktop-base x11-xserver-utils xfonts-base xfonts-utils lxde lxde-common menu-xdg hicolor-icon-theme gtk2-engines"
        apt_install ${packages}
    ;;
    archlinux:*)
        packages="xorg-utils xorg-fonts-misc ttf-dejavu lxde gtk-engines"
        pacman_install ${packages}
    ;;
    fedora:*)
        packages="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* @lxde-desktop"
        dnf_install ${packages}
    ;;
    centos:*)
        packages="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* @lxde-desktop-environment"
        yum_install ${packages}
    ;;
    opensuse:*)
        packages="xorg-x11-fonts-core dejavu-fonts xauth patterns-openSUSE-lxde"
        zypper_install ${packages}
    ;;
    gentoo:*)
        packages="xauth lxde-meta gtk-engines"
        emerge_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local xinitrc="${CHROOT_DIR}$(user_home ${USER_NAME})/.xinitrc"
    echo 'startlxde' >> "${xinitrc}"
    return 0
}
