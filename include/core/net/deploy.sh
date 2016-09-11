#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${DNS}" ] || DNS="auto"

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
    if [ -n "${NET_TRIGGER}" ]; then
        msg ":: Starting ${COMPONENT} ... "
        chroot_exec -u root "${NET_TRIGGER}" &
    fi
    return 0
}

do_help()
{
cat <<EOF
   --dns="${DNS}"
     IP-address of DNS server, can specify multiple addresses separated by a space.

   --net-trigger="${NET_TRIGGER}"
     Path to a script inside the container to process changes the network.

EOF
}
