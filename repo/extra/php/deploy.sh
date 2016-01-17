#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="php5"
        apt_install ${packages}
    ;;
    esac
}

