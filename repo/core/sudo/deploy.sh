#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local sudo_str="${USER_NAME} ALL=(ALL:ALL) NOPASSWD:ALL"
    if ! $(grep "${sudo_str}" "${CHROOT_DIR}/etc/sudoers"); then
        chmod 640 "${CHROOT_DIR}/etc/sudoers"
        echo ${sudo_str} >> "${CHROOT_DIR}/etc/sudoers"
        chmod 440 "${CHROOT_DIR}/etc/sudoers"
    fi
    return 0
}

