#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${DISPLAY}" ] || DISPLAY=":0"

is_started()
{
    local pid
    local pidfile="${CHROOT_DIR}/tmp/xsession.pid"
    [ -e "${pidfile}" ] && pid=$(cat "${pidfile}")
    if [ -n "${pid}" -a -e "/proc/${pid}" ]; then
        return 0
    else
        return 1
    fi
}

do_start()
{
    msg -n ":: Starting ${COMPONENT} ... "
    local cmd="export DISPLAY=${DISPLAY}; ~/.xinitrc"
    chroot_exec su - ${USER_NAME} -c "${cmd}" &
    is_ok "fail" "done"
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    local pid=""
    if [ -e "${CHROOT_DIR}/tmp/xsession.pid" ]; then
        pid=$(cat "${CHROOT_DIR}/tmp/xsession.pid")
    fi
    if [ -n "${pid}" ]; then
        kill -9 ${pid}
        is_ok "fail" "done"
    else
        msg "done"
    fi
    return 0
}

do_help()
{
cat <<EOF 1>&3
   --display=HOST:DISPLAY 
     Display of X server, e.g 127.0.0.1:0.

EOF
}
