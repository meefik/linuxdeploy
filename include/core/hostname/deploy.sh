#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    echo 'localhost' > "${CHROOT_DIR}/etc/hostname"
    return 0
}
