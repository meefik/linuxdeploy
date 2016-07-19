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
        [ "${METHOD}" = "proot" ] && packages="${packages} fakechroot"
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

do_start()
{
    msg -n ":: Starting ${COMPONENT} ... "
    is_started /var/run/sshd.pid /run/sshd.pid
    is_ok "skip" || return 0
    make_dirs /run/sshd /var/run/sshd
    # generate keys
    if [ -z "$(ls \"${CHROOT_DIR}/etc/ssh/\" | grep key)" ]; then
        chroot_exec su - root -c 'ssh-keygen -A' >/dev/null
    fi
    # exec sshd
    if [ "${METHOD}" = "proot" ]; then
        chroot_exec su - root -c "fakechroot $(which sshd) -p ${SSH_PORT} ${SSH_ARGS}" &
    else
        chroot_exec su - root -c "$(which sshd) -p ${SSH_PORT} ${SSH_ARGS}"
    fi
    is_ok "fail" "done"
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    kill_pids /run/sshd.pid /var/run/sshd.pid
    is_ok "fail" "done"
    return 0
}

do_help()
{
cat <<EOF
   --ssh-port=PORT
     Port of SSH server.

   --ssh-args=STR
     Defines other sshd options, separated by a space.

EOF
}
