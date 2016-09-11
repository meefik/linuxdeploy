#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${INIT_USER}" ] || INIT_USER="root"

do_start()
{
    #local init_level=$(grep ':initdefault:' "${CHROOT_DIR}/etc/inittab" | cut -f2 -d:)
    [ -n "${INIT_LEVEL}" ] || return 0

    local services=$(ls "${CHROOT_DIR}/etc/rc${INIT_LEVEL}.d/" | grep '^S')
    if [ -n "${services}" ]; then
        msg ":: Starting ${COMPONENT}: "
        local item
        for item in ${services}
        do
            msg -n "${item/S[0-9][0-9]/} ... "
            if [ "${INIT_ASYNC}" = "true" ]; then
                chroot_exec -u ${INIT_USER} "/etc/rc${INIT_LEVEL}.d/${item} start" 1>&2 &
            else
                chroot_exec -u ${INIT_USER} "/etc/rc${INIT_LEVEL}.d/${item} start" 1>&2
            fi
            is_ok "fail" "done"
        done
    fi

    return 0
}

do_stop()
{
    [ -n "${INIT_LEVEL}" ] || return 0

    local services=$(ls "${CHROOT_DIR}/etc/rc6.d/" | grep '^K')
    if [ -n "${services}" ]; then
        msg ":: Starting ${COMPONENT}: "
        local item
        for item in ${services}
        do
            msg -n "${item/K[0-9][0-9]/} ... "
            if [ "${INIT_ASYNC}" = "true" ]; then
                chroot_exec -u ${INIT_USER} "/etc/rc6.d/${item} stop" 1>&2 &
            else
                chroot_exec -u ${INIT_USER} "/etc/rc6.d/${item} stop" 1>&2
            fi
            is_ok "fail" "done"
        done
    fi

    return 0
}

do_help()
{
cat <<EOF
   --init-level="${INIT_LEVEL}"
     Number of init level, e.g "3".

   --init-user="${INIT_USER}"
     Execute as specific user, by default "root".

   --init-async
     Asynchronous startup of processes.

EOF
}
