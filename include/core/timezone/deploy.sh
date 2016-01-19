#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local timezone
    if [ -n "$(which getprop)" ]; then
        timezone=$(getprop persist.sys.timezone)
    elif [ -e "/etc/timezone" ]; then
        timezone=$(cat /etc/timezone)
    fi
    if [ -n "${timezone}" ]; then
        rm -f "${CHROOT_DIR}/etc/localtime"
        cp "${CHROOT_DIR}/usr/share/zoneinfo/${timezone}" "${CHROOT_DIR}/etc/localtime"
        echo ${timezone} > "${CHROOT_DIR}/etc/timezone"
    fi
    return 0
}
