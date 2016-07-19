#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${INIT_USER}" ] || INIT_USER="root"

run_part()
{
    local path="$1"
    local action="$2"
    msg -n "${item} ... "
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
        run_part "${CHROOT_DIR}${INIT_PATH}" stop
    else
        local services=$(ls "${CHROOT_DIR}${INIT_PATH}/")
        if [ -n "${services}" ]; then
            msg ":: Starting services: "
            local item
            for item in ${services}
            do
                run_part "${INIT_PATH%/}/${item}" start
            done
        fi
    fi

    return 0
}

do_stop()
{
    [ -n "${INIT_PATH}" ] || return 0

    if [ -f "${CHROOT_DIR}${INIT_PATH}" ]; then
        run_part "${CHROOT_DIR}${INIT_PATH}" stop
    else
        local services=$(ls "${CHROOT_DIR}${INIT_PATH}/" | tac)
        if [ -n "${services}" ]; then
            msg ":: Stopping services: "
            local item
            for item in ${services}
            do
                run_part "${INIT_PATH%/}/${item}" stop
            done
        fi
    fi

    return 0
}

do_help()
{
cat <<EOF
   --init-path=PATH
     Директория или файл внутри контейнера, которые нужно выполнить.

   --init-user=USER
     Пользователь из-под которого осуществляется запуск, по умолчанию root.

   --init-async
     Асинхронный запуск процессов.

EOF
}
