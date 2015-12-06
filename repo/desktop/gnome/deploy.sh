#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="desktop-base x11-xserver-utils xfonts-base xfonts-utils gnome-core"
        apt_install ${packages}
    ;;
    archlinux:*:*)
        packages="xorg-utils xorg-fonts-misc ttf-dejavu gnome"
        pacman_install ${packages}
    ;;
    fedora:*:*)
        installgroup="xorg-x11-server-utils xorg-x11-fonts-misc dejavu-* gnome-desktop"
        yum_groupinstall ${installgroup}
    ;;
    opensuse:*:*)
        packages="xorg-x11-fonts-core dejavu-fonts xauth patterns-openSUSE-gnome"
        zypper_install ${packages}
    ;;
    gentoo:*:*)
        packages="xauth gnome"
        emerge_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local xinitrc="${CHROOT_DIR}$(user_home ${USER_NAME})/.xinitrc"
    echo 'XKL_XMODMAP_DISABLE=1' >> "${xinitrc}"
    echo 'export XKL_XMODMAP_DISABLE' >> "${xinitrc}"
    echo 'if [ -n "`gnome-session -h | grep "\-\-session"`" ]; then' >> "${xinitrc}"
    echo '   gnome-session --session=gnome' >> "${xinitrc}"
    echo 'else' >> "${xinitrc}"
    echo '   gnome-session' >> "${xinitrc}"
    echo 'fi' >> "${xinitrc}"
    return 0
}
