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
    if [ $(grep -c -e '^allowed_users' "${CHROOT_DIR}/etc/X11/Xwrapper.config") -gt 0 ]; then
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
        if [ $(grep -c '^INPUT_DEVICES=' "${CHROOT_DIR}/etc/portage/make.conf") -eq 0 ]; then
            echo 'INPUT_DEVICES="evdev"' >> "${CHROOT_DIR}/etc/portage/make.conf"
        else
            sed -i 's|^\(INPUT_DEVICES=\).*|\1"evdev"|g' "${CHROOT_DIR}/etc/portage/make.conf"
        fi
        if [ $(grep -c '^VIDEO_CARDS=' "${CHROOT_DIR}/etc/portage/make.conf") -eq 0 ]; then
            echo 'VIDEO_CARDS="fbdev"' >> "${CHROOT_DIR}/etc/portage/make.conf"
        else
            sed -i 's|^\(VIDEO_CARDS=\).*|\1"fbdev"|g' "${CHROOT_DIR}/etc/portage/make.conf"
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

is_started()
{
    local pid
    local pidfile="${CHROOT_DIR}/tmp/xsession.pid"
    [ -e "${pidfile}" ] && pid=$(cat "${pidfile}")
    if [ -n "${pid}" -a -e "/proc/${pid}" ]; then
        return 0
    else
        return 1
    fi
}

do_start()
{
    fb_refresh()
    {
        [ "${FB_REFRESH}" = "1" ] || return 0
        local fbdev="${FB_DEV##*/}"
        local fbrotate="/sys/class/graphics/${fbdev}/rotate"
        [ -e "${fbrotate}" ] || return 0
        local pid_file="${MNT_TARGET}/tmp/xsession.pid"
        touch "${pid_file}"
        chmod 666 "${pid_file}"
        while [ -e "${pid_file}" ]
        do
            echo 0 > "${fbrotate}"
            sleep 0.01
        done
    }
    msg -n ":: Starting ${COMPONENT} ... "
    fb_refresh &
    (set -e
        sync
        case "${FB_FREEZE}" in
        stop)
            chroot_exec su - ${USER_NAME} -c "xinit -- :${FB_DISPLAY} ${FB_ARGS}" &
            setprop ctl.stop surfaceflinger
            sleep 10
            setprop ctl.stop zygote
        ;;
        pause)
            chroot_exec su - ${USER_NAME} -c "xinit -- :${FB_DISPLAY} ${FB_ARGS}" &
            pkill -STOP system_server
            pkill -STOP surfaceflinger
        ;;
        *)
            chroot_exec su - ${USER_NAME} -c "xinit -- :${FB_DISPLAY} ${FB_ARGS}" &
        ;;
        esac
    exit 0)
    is_ok "fail" "done"
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    local pid=""
    if [ -e "${CHROOT_DIR}/tmp/xsession.pid" ]; then
        pid=$(cat "${CHROOT_DIR}/tmp/xsession.pid")
        rm "${CHROOT_DIR}/tmp/xsession.pid"
    fi
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
cat <<EOF 1>&3
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
