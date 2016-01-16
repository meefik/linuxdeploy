#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${INIT_USER}" ] || INIT_USER="root"

do_start()
{
    if [ -n "${INIT_DIR}" ]; then
        local services=$(ls "${CHROOT_DIR}${INIT_DIR}/")
        if [ -n "${services}" ]; then
            msg ":: Starting services: "
            local item
            for item in ${services}
            do
                msg -n "${item} ... "
                if [ "${INIT_BG}" = "1" ]; then
                    chroot_exec -u ${INIT_USER} "${INIT_DIR%/}/${item} start" 1>&2 &
                else
                    chroot_exec -u ${INIT_USER} "${INIT_DIR%/}/${item} start" 1>&2
                fi
                is_ok "fail" "done"
            done
        fi
    fi
    return 0
}

do_stop()
{
    if [ -n "${INIT_DIR}" ]; then
        local services=$(ls "${CHROOT_DIR}${INIT_DIR}/" | tac)
        if [ -n "${services}" ]; then
            msg ":: Stopping services: "
            local item
            for item in ${services}
            do
                msg -n "${item} ... "
                if [ "${INIT_BG}" = "1" ]; then
                    chroot_exec -u ${INIT_USER} "${INIT_DIR%/}/${item} stop" 1>&2 &
                else
                    chroot_exec -u ${INIT_USER} "${INIT_DIR%/}/${item} stop" 1>&2
                fi
                is_ok "fail" "done"
            done
        fi
    fi
    return 0
}

do_help()
{
cat <<EOF
   --init-dir=PATH
     Директория внутри контейнера, в которой находятся скрипты автозапуска.

   --init-user=USER
     Пользователь из-под которого осуществляется запуск.

   --init-bg
     Запускать процессы в фоновом режиме.

EOF
}
