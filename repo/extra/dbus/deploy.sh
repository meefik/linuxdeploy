#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="dbus"
        apt_install ${packages}
    ;;
    archlinux:*:*)
        packages="dbus"
        pacman_install ${packages}
    ;;
    fedora:*:*)
        packages="dbus"
        yum_install ${packages}
    ;;
    opensuse:*:*)
        packages="dbus"
        zypper_install ${packages}
    ;;
    gentoo:*:*)
        packages="dbus"
        emerge_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local item
    for item in /run/dbus /var/run/dbus
    do
        if [ ! -d "${CHROOT_DIR}${item}" ]; then
            mkdir "${CHROOT_DIR}${item}"
        fi
    done
    chmod 644 "${CHROOT_DIR}/etc/machine-id"
    chroot_exec dbus-uuidgen > "${CHROOT_DIR}/etc/machine-id"
    return 0
}

is_started()
{
    local is_started=""
    local item pidfile pid
    for item in /run/dbus/pid /var/run/messagebus.pid
    do
        pidfile="${CHROOT_DIR}${item}"
        [ -e "${pidfile}" ] && pid=$(cat "${pidfile}")
        if [ -n "${pid}" -a -e "/proc/${pid}" ]; then
            return 0
        fi
    done
    return 1
}

do_start()
{
    msg -n ":: Starting ${COMPONENT} ... "
    is_started && { msg "skip"; return 0; }
    [ -e "${CHROOT_DIR}/run/dbus/pid" ] && rm -f "${CHROOT_DIR}/run/dbus/pid"
    [ -e "${CHROOT_DIR}/var/run/messagebus.pid" ] && rm -f "${CHROOT_DIR}/var/run/messagebus.pid"
    chroot_exec dbus-daemon --system --fork &
    is_ok "fail" "done"
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    local pid=""
    local path
    for path in /run/dbus/pid /var/run/messagebus.pid
    do
        if [ -e "${CHROOT_DIR}${path}" ]; then
            pid=$(cat "${CHROOT_DIR}${path}")
            break
        fi
    done
    if [ -n "${pid}" ]; then
        kill -9 ${pid}
        is_ok "fail" "done"
    else
        msg "done"
    fi
    return 0
}
