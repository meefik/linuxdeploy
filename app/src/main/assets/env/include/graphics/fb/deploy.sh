#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${FB_DISPLAY}" ] || FB_DISPLAY="0"
[ -n "${FB_DEV}" ] || FB_DEV="/dev/graphics/fb0"
[ -n "${FB_INPUT}" ] || FB_INPUT="/dev/input/event0"

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="xinit xserver-xorg xserver-xorg-video-fbdev xserver-xorg-input-evdev"
        apt_install ${packages}
    ;;
    archlinux:*:*)
        packages="xorg-xinit xorg-server xf86-video-fbdev xf86-input-evdev"
        pacman_install ${packages}
    ;;
    fedora:*:*)
        packages="xorg-x11-xinit xorg-x11-server-Xorg xorg-x11-drv-fbdev xorg-x11-drv-evdev"
        yum_install ${packages}
    ;;
    centos:*:*)
        packages="xorg-x11-xinit xorg-x11-server-Xorg xorg-x11-drv-fbdev xorg-x11-drv-evdev"
        yum_install ${packages}
    ;;
    opensuse:*:*)
        packages="xinit xorg-x11-server xf86-video-fbdev xf86-input-evdev"
        zypper_install ${packages}
    ;;
    gentoo:*:*)
        packages="xinit xorg-server"
        emerge_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    # Xwrapper.config
    mkdir "${CHROOT_DIR}/etc/X11"
    if $(grep -q '^allowed_users' "${CHROOT_DIR}/etc/X11/Xwrapper.config"); then
        sed -i 's/^allowed_users=.*/allowed_users=anybody/g' "${CHROOT_DIR}/etc/X11/Xwrapper.config"
    else
        echo "allowed_users=anybody" >> "${CHROOT_DIR}/etc/X11/Xwrapper.config"
    fi
    local xorg_file="${CHROOT_DIR}/etc/X11/xorg.conf"
    [ -e "${xorg_file}" ] && cp "${xorg_file}" "${xorg_file}.bak"
    cat "${COMPONENT_DIR}/xorg.conf" > "${xorg_file}"
    chmod 644 "${xorg_file}"
    # update xorg.conf
    if [ -n "${FB_DEV}" ]; then
        sed -i "s|Option.*\"fbdev\".*#linuxdeploy|Option \"fbdev\" \"${FB_DEV}\" #linuxdeploy|g" "${xorg_file}"
    fi
    if [ -n "${FB_INPUT}" ]; then
        sed -i "s|Option.*\"Device\".*#linuxdeploy|Option \"Device\" \"${FB_INPUT}\" #linuxdeploy|g" "${xorg_file}"
    fi
    # specific configuration
    case "${DISTRIB}" in
    gentoo)
        # set Xorg make configuration
        if $(grep -q '^INPUT_DEVICES=' "${CHROOT_DIR}/etc/portage/make.conf"); then
            sed -i 's|^\(INPUT_DEVICES=\).*|\1"evdev"|g' "${CHROOT_DIR}/etc/portage/make.conf"
        else
            echo 'INPUT_DEVICES="evdev"' >> "${CHROOT_DIR}/etc/portage/make.conf"
        fi
        if $(grep -q '^VIDEO_CARDS=' "${CHROOT_DIR}/etc/portage/make.conf"); then
            sed -i 's|^\(VIDEO_CARDS=\).*|\1"fbdev"|g' "${CHROOT_DIR}/etc/portage/make.conf"
        else
            echo 'VIDEO_CARDS="fbdev"' >> "${CHROOT_DIR}/etc/portage/make.conf"
        fi
    ;;
    opensuse)
        if [ -e "${CHROOT_DIR}/usr/bin/Xorg" ]
        then
            chmod +s "${CHROOT_DIR}/usr/bin/Xorg"
        fi
    ;;
    esac
    return 0
}

do_start()
{
    fb_refresh()
    {
        [ "${FB_REFRESH}" = "true" ] || return 0
        local fbdev="${FB_DEV##*/}"
        local fbrotate="/sys/class/graphics/${fbdev}/rotate"
        [ -e "${fbrotate}" ] || return 0
        local pid_file="${CHROOT_DIR}/tmp/xsession.pid"
        touch "${pid_file}"
        chmod 666 "${pid_file}"
        while [ -e "${pid_file}" ]
        do
            echo 0 > "${fbrotate}"
            sleep 0.01
        done
    }
    msg -n ":: Starting ${COMPONENT} ... "
    is_stopped /tmp/xsession.pid
    is_ok "skip" || return 0
    fb_refresh &
    (chroot_exec -u ${USER_NAME} xinit -- :${FB_DISPLAY} ${FB_ARGS} ; do_stop) &
    is_ok "fail" "done"
    case "${FB_FREEZE}" in
    stop)
        sync
        setprop ctl.stop surfaceflinger
        sleep 10
        setprop ctl.stop zygote
    ;;
    pause)
        sync
        pkill -STOP system_server
        pkill -STOP surfaceflinger
    ;;
    esac
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    case "${FB_FREEZE}" in
    stop)
        setprop ctl.start surfaceflinger
        setprop ctl.start zygote
    ;;
    pause)
        pkill -CONT surfaceflinger
        pkill -CONT system_server
    ;;
    esac
    kill_pids /tmp/xsession.pid
    is_ok "fail" "done"
    remove_files /tmp/xsession.pid
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
   --fb-display=DISPLAY
     Display of X server, e.g 0.

   --fb-args=STR
     Defines other X options, separated by a space.

   --fb-dev=DEVICE
     Framebuffer device for X, e.g. /dev/graphics/fb0.

   --fb-input=DEVICE
     Input device for X, e.g. /dev/input/event0.

   --fb-freeze=none|pause|stop
     Android UI freeze mode.

   --fb-refresh
     Force refresh a framebuffer.

EOF
}
