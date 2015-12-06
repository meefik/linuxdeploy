#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local is_localhost=$(grep -c "^127.0.0.1" "${CHROOT_DIR}/etc/hosts")
    if [ "${is_localhost}" -eq 0 ]; then
        echo '127.0.0.1 localhost' >> "${CHROOT_DIR}/etc/hosts"
    fi
    return 0
}
