#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    if ! $(grep -q "^127.0.0.1" "${CHROOT_DIR}/etc/hosts"); then
        echo '127.0.0.1 localhost' >> "${CHROOT_DIR}/etc/hosts"
    fi
    return 0
}
