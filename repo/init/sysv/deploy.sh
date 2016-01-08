#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_start()
{
    if [ -n "${INIT_LEVEL}" ]; then
        local services=$(ls "${CHROOT_DIR}/etc/rc${INIT_LEVEL}.d/" | grep '^S')
        if [ -n "${services}" ]; then
            msg ":: Starting services: "
            local item
            for item in ${services}
            do
                msg -n "${item/S[0-9][0-9]/} ... "
                chroot_exec su - root -c "/etc/rc${INIT_LEVEL}.d/${item} start"
                is_ok "fail" "done"
            done
        fi
    fi
    return 0
}

do_stop()
{
    if [ -n "${INIT_LEVEL}" ]; then
        local services=$(ls "${CHROOT_DIR}/etc/rc6.d/" | grep '^K')
        if [ -n "${services}" ]; then
            msg ":: Starting services: "
            local item
            for item in ${services}
            do
                msg -n "${item/K[0-9][0-9]/} ... "
                chroot_exec su - root -c "/etc/rc${INIT_LEVEL}.d/${item} stop"
                is_ok "fail" "done"
            done
        fi
    fi
    return 0
}

do_help()
{
cat <<EOF
   --init-level=NUM
     Number of init level, e.g 3.

EOF
}
