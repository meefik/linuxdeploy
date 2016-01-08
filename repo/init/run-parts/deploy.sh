#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

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
                chroot_exec su - root -c "${INIT_DIR%/}/${item} start"
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
                chroot_exec su - root -c "${INIT_DIR%/}/${item} stop"
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

EOF
}
