#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n ${DNS} ] || DNS="auto"

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local dns dns_list
    if [ -z "${DNS}" -o "${DNS}" = "auto" ]; then
        if [ -n "$(which getprop)" ]; then
            dns=$(getprop net.dns1)
            [ -n "${dns}" ] && dns_list="${dns}"
            dns=$(getprop net.dns2)
            [ -n "${dns}" ] && dns_list="${dns_list} ${dns}"
        fi
        if [ -z "${dns_list}" -a -e "/etc/resolv.conf" ]; then
            dns_list=$(grep "^nameserver" /etc/resolv.conf | awk '{print $2}')
        fi
        [ -z "${dns_list}" ] && dns_list="8.8.8.8"
    else
        dns_list="${DNS}"
    fi
    printf '' > "${CHROOT_DIR}/etc/resolv.conf"
    for dns in ${dns_list}
    do
        echo "nameserver ${dns}" >> "${CHROOT_DIR}/etc/resolv.conf"
    done
    return 0
}

do_start()
{
    do_configure
}

do_help()
{
cat <<EOF 1>&3
   --dns=IP|auto
     IP-адрес DNS сервера.

EOF
}
