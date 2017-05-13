#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${USER_PASSWORD}" ] || USER_PASSWORD="changeme"
[ -n "${VNC_DISPLAY}" ] || VNC_DISPLAY="0"
[ -n "${VNC_DEPTH}" ] || VNC_DEPTH="16"
[ -n "${VNC_DPI}" ] || VNC_DPI="75"
[ -n "${VNC_WIDTH}" ] || VNC_WIDTH="800"
[ -n "${VNC_HEIGHT}" ] || VNC_HEIGHT="480"

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*|ubuntu:*|kalilinux:*)
        packages="tightvncserver"
        apt_install ${packages}
    ;;
    archlinux:*)
        packages="tigervnc"
        pacman_install ${packages}
    ;;
    fedora:*)
        packages="tigervnc-server"
        dnf_install ${packages}
    ;;
    centos:*)
        packages="tigervnc-server"
        yum_install ${packages}
    ;;
    opensuse:*)
        packages="tightvnc"
        zypper_install ${packages}
    ;;
    gentoo:*)
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
    msg ":: Configuring ${COMPONENT} ... "
    local vnc_home="$(user_home ${USER_NAME})/.vnc"
    local vnc_home_chroot="${CHROOT_DIR}${vnc_home}"
    [ -e "${vnc_home_chroot}" ] || mkdir "${vnc_home_chroot}"
    # set vnc password
    echo ${USER_PASSWORD} | chroot_exec -u root vncpasswd -f > "${vnc_home_chroot}/passwd" ||
    echo "MPTcXfgXGiY=" | base64 -d > "${vnc_home_chroot}/passwd"
    chmod 600 "${vnc_home_chroot}/passwd"
    rm "${vnc_home_chroot}/xstartup"
    ln -s ../.xinitrc "${vnc_home_chroot}/xstartup"
    chroot_exec -u root chown -R ${USER_NAME}:${USER_NAME} "${vnc_home}"
    return 0
}

do_start()
{
    msg -n ":: Starting ${COMPONENT} ... "
    is_stopped /tmp/xsession.pid
    is_ok "skip" || return 0
    # remove locks
    remove_files "/tmp/.X${VNC_DISPLAY}-lock" "/tmp/.X11-unix/X${VNC_DISPLAY}"
    # exec vncserver
    chroot_exec -u ${USER_NAME} vncserver :${VNC_DISPLAY} -depth ${VNC_DEPTH} -dpi ${VNC_DPI} -geometry ${VNC_WIDTH}x${VNC_HEIGHT} ${VNC_ARGS}
    is_ok "fail" "done"
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    chroot_exec -u ${USER_NAME} vncserver -kill :${VNC_DISPLAY}
    kill_pids /tmp/xsession.pid
    is_ok "fail" "done"
    return 0
}

do_status()
{
    msg -n ":: ${COMPONENT} ... "
    is_started /tmp/xsession.pid
    is_ok "stopped" "started"
    return 0
}

do_help()
{
cat <<EOF
   --vnc-display="${VNC_DISPLAY}"
     Display of VNC server, default 0. TCP port computed as 5900 + display number.

   --vnc-depth="${VNC_DEPTH}"
     Color depth, default 16.

   --vnc-dpi="${VNC_DPI}"
     Dots per inch, default 75.

   --vnc-width="${VNC_WIDTH}"
     Screen width, default 800.

   --vnc-height="${VNC_HEIGHT}"
     Screen height, default 480.

   --vnc-args="${VNC_ARGS}"
     Defines other vncserver options, separated by a space.

EOF
}
