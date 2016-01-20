#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

proot_exit()
{
cat <<'EOF'
exit()
{
    local tracer_pid
    local self_tracer_pid=$(grep ^TracerPid: /proc/self/status 2>/dev/null | cut -f2)
    find /proc/*/status | grep -o '[0-9]\+' | sort -n | while read pid;
    do
        tracer_pid=$(grep ^TracerPid: /proc/$pid/status 2>/dev/null | cut -f2)
        if [ "$self_tracer_pid" = "$tracer_pid" -a "$pid" != "$$" ]; then
            kill -9 $pid
        fi
    done
    unset exit
    exit $1
}
EOF
}

do_configure()
{
    [ "$FAKEROOT" = "1" ] || return 0
    msg ":: Configuring ${COMPONENT} ... "
    if ! $(grep -q '^exit()' "${CHROOT_DIR}/etc/bash.bashrc"); then
        proot_exit >> "${CHROOT_DIR}/etc/bash.bashrc"
    fi
    return 0
}

do_help()
{
cat <<EOF
   --fakeroot
     Позволяет работать с контейнером без прав суперпользователя.

EOF
}
