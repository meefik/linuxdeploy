#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${INIT_USER}" ] || INIT_USER="root"

run_part()
{
    local path="$1"
    local action="$2"
    msg -n "${path##*/} ... "
    if [ "${INIT_ASYNC}" = "true" ]; then
        chroot_exec -u ${INIT_USER} "${path} ${action}" 1>&2 &
    else
        chroot_exec -u ${INIT_USER} "${path} ${action}" 1>&2
    fi
    is_ok "fail" "done"
}

do_start()
{
    [ -n "${INIT_PATH}" ] || return 0

    if [ -f "${CHROOT_DIR}${INIT_PATH}" ]; then
        msg ":: Starting ${COMPONENT}: "
        run_part "${INIT_PATH}" start
    else
        local services=$(ls "${CHROOT_DIR}${INIT_PATH}/")
        if [ -n "${services}" ]; then
            msg ":: Starting services: "
            local part
            for part in ${services}
            do
                run_part "${INIT_PATH%/}/${part}" start
            done
        fi
    fi

    return 0
}

do_stop()
{
    [ -n "${INIT_PATH}" ] || return 0

    if [ -f "${CHROOT_DIR}${INIT_PATH}" ]; then
        msg ":: Stopping ${COMPONENT}: "
        run_part "${INIT_PATH}" stop
    else
        local services=$(ls "${CHROOT_DIR}${INIT_PATH}/" | tac)
        if [ -n "${services}" ]; then
            msg ":: Stopping services: "
            local part
            for part in ${services}
            do
                run_part "${INIT_PATH%/}/${part}" stop
            done
        fi
    fi

    return 0
}

do_help()
{
cat <<EOF
   --init-path="${INIT_PATH}"
     Directory or file within the container that you want to execute.

   --init-user="${INIT_USER}"
     Execute as specific user, by default "root".

   --init-async
     Asynchronous startup of processes.

EOF
}
