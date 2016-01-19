#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${USER_PASSWORD}" ] || USER_PASSWORD="changeme"
[ -n "${VNC_DISPLAY}" ] || VNC_DISPLAY="0"

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="tightvncserver"
        apt_install ${packages}
    ;;
    archlinux:*:*)
        packages="tigervnc"
        pacman_install ${packages}
    ;;
    fedora:*:*)
        packages="tigervnc-server"
        yum_install ${packages}
    ;;
    opensuse:*:*)
        packages="tightvnc"
        zypper_install ${packages}
    ;;
    gentoo:*:*)
        # set server USE flag for tightvnc
        if ! $(grep -q '^net-misc/tightvnc' "${CHROOT_DIR}/etc/portage/package.use"); then
            echo "net-misc/tightvnc server" >> "${CHROOT_DIR}/etc/portage/package.use"
        fi
        if ! $(grep -q '^net-misc/tightvnc.*server' "${CHROOT_DIR}/etc/portage/package.use"); then
            sed -i "s|^\(net-misc/tightvnc.*\)|\1 server|g" "${CHROOT_DIR}/etc/portage/package.use"
        fi
        packages="tightvnc"
        emerge_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg -n ":: Configuring ${COMPONENT} ... "
    local vnc_home="$(user_home ${USER_NAME})/.vnc"
    local vnc_home_chroot="${CHROOT_DIR}${vnc_home}"
    [ -e "${vnc_home_chroot}" ] || mkdir "${vnc_home_chroot}"
    # set vnc password
    echo ${USER_PASSWORD} | chroot_exec "vncpasswd -f" > "${vnc_home_chroot}/passwd" ||
    echo "MPTcXfgXGiY=" | base64 -d > "${vnc_home_chroot}/passwd"
    chmod 600 "${vnc_home_chroot}/passwd"
    rm "${vnc_home_chroot}/xstartup"
    ln -s ../.xinitrc "${vnc_home_chroot}/xstartup"
    chroot_exec chown -R ${USER_NAME}:${USER_NAME} "${vnc_home}"
    is_ok "fail" "done"
    return 0
}

do_start()
{
    msg -n ":: Starting ${COMPONENT} ... "
    is_started /tmp/xsession.pid
    is_ok "skip" || return 0
    # remove locks
    remove_files "/tmp/.X${VNC_DISPLAY}-lock" "/tmp/.X11-unix/X${VNC_DISPLAY}"
    # exec vncserver
    chroot_exec su - ${USER_NAME} -c "vncserver :${VNC_DISPLAY} ${VNC_ARGS}" &
    is_ok "fail" "done"
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    kill_pids /tmp/xsession.pid
    is_ok "fail" "done"
    return 0
}

do_help()
{
cat <<EOF
   --vnc-display=DISPLAY
     Display of VNC server, e.g 0.

   --vnc-args=STR
     Defines other vncserver options, separated by a space.

EOF
}
