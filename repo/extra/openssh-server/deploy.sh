#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${SSH_PORT}" ] || SSH_PORT="22"

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="openssh-server"
        apt_install ${packages}
    ;;
    archlinux:*:*)
        packages="openssh"
        pacman_install ${packages}
    ;;
    fedora:*:*)
        packages="openssh-server"
        yum_install ${packages}
    ;;
    opensuse:*:*)
        packages="openssh"
        zypper_install ${packages}
    ;;
    gentoo:*:*)
        packages="openssh"
        emerge_install ${packages}
    ;;
    slackware:*:*)
        packages="openssh"
        slackpkg_install ${packages}
    ;;
    esac
}

is_started()
{
    local item pidfile pid
    for item in /var/run/sshd.pid /run/sshd.pid
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
    # prepare var
    [ -e "${CHROOT_DIR}/var/run" -a ! -e "${CHROOT_DIR}/var/run/sshd" ] && mkdir "${CHROOT_DIR}/var/run/sshd"
    [ -e "${CHROOT_DIR}/run" -a ! -e "${CHROOT_DIR}/run/sshd" ] && mkdir "${CHROOT_DIR}/run/sshd"
    # generate keys
    if [ -z "$(ls \"${CHROOT_DIR}/etc/ssh/\" | grep key)" ]; then
        chroot_exec su - root -c 'ssh-keygen -A' >/dev/null
    fi
    # exec sshd
    local sshd='`which sshd`'
    chroot_exec su - root -c "${sshd} -p ${SSH_PORT}"
    is_ok "fail" "done"
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    local pid=""
    local path
    for path in /run/sshd.pid /var/run/sshd.pid
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

do_help()
{
cat <<EOF
   --ssh-port=PORT
     Port of SSH server.

EOF
}
