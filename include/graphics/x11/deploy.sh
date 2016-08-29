#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${X11_DISPLAY}" ] || X11_DISPLAY="0"

do_start()
{
    msg -n ":: Starting ${COMPONENT} ... "
    is_stopped /tmp/xsession.pid
    is_ok "skip" || return 0
    local cmd="export DISPLAY=${X11_HOST}:${X11_DISPLAY}; ~/.xinitrc"
    chroot_exec -u ${USER_NAME} ${cmd} &
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

do_status()
{
    msg -n ":: ${COMPONENT} ... "
    is_started /tmp/xsession.pid
    is_ok "stopped" "started"
    return 0
}

do_help()
{
cat <<EOF
   --x11-display="${X11_DISPLAY}"
     Display of X server, default 0.

   --x11-host="${X11_HOST}"
     Host of X server, default 127.0.0.1.

EOF
}
