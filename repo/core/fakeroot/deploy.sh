#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

proot_exit()
{
cat <<'EOF'
exit()
{
    local self_tracer_pid=$(grep ^TracerPid: /proc/self/status 2>/dev/null | cut -f2)
    find /proc/*/status | while read f;
    do
        if [ "$self_tracer_pid" = "$(grep ^TracerPid: $f 2>/dev/null | cut -f2)" ]; then
            kill -9 $(grep ^Pid: $f | cut -f2)
        fi
    done
    return $1
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
