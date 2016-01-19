#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${DISPLAY}" ] || DISPLAY=":0"

do_start()
{
    msg -n ":: Starting ${COMPONENT} ... "
    is_started /tmp/xsession.pid
    is_ok "skip" || return 0
    local cmd="export DISPLAY=${DISPLAY}; ~/.xinitrc"
    chroot_exec su - ${USER_NAME} -c "${cmd}" &
    is_ok "fail" "done"
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    kill_pids /tmp/xsession.pid
    is_ok "fail" "done"
    return 0
}

do_help()
{
cat <<EOF
   --display=HOST:DISPLAY 
     Display of X server, e.g 127.0.0.1:0.

EOF
}
