#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_install()
{
    msg ":: Installing ${COMPONENT} ... "
    local packages=""
    case "${DISTRIB}:${ARCH}:${SUITE}" in
    debian:*:*|ubuntu:*:*|kalilinux:*:*)
        packages="apache2"
        apt_install ${packages}
    ;;
    esac
}

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    if [ "${METHOD}" = "proot" ]; then
        if ! $(grep -q '^APACHE_ULIMIT_MAX_FILES='); then
            echo "APACHE_ULIMIT_MAX_FILES='/bin/true'" >> "${CHROOT_DIR}/etc/apache2/envvars"
        fi
    fi
    if [ -n "${APACHE_PORT}" ]; then
        sed -i "s|^NameVirtualHost.*|NameVirtualHost *:${APACHE_PORT}|g" "${CHROOT_DIR}/etc/apache2/ports.conf"
        sed -i "s|^Listen.*|Listen ${APACHE_PORT}|g" "${CHROOT_DIR}/etc/apache2/ports.conf"
    fi
    return 0
}

do_start()
{
    msg -n ":: Starting ${COMPONENT} ... "
    is_started /run/apache2.pid
    is_ok "skip" || return 0
    chroot_exec -u root service apache2 start &
    is_ok "fail" "done"
    return 0
}

do_stop()
{
    msg -n ":: Stopping ${COMPONENT} ... "
    kill_pids /run/apache2.pid
    is_ok "fail" "done"
    return 0
}

do_help()
{
cat <<EOF
   --apache-port=PORT
     Порт веб-сервера Apache.

EOF
}
