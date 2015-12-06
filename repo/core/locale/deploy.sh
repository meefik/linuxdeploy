#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${LOCALE}" ] || LOCALE="${LANG}"

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    if [ -n "${LOCALE}" -a "${LOCALE}" != "C" -a "${LOCALE}" != "POSIX" ]; then
        local inputfile=$(echo ${LOCALE} | awk -F. '{print $1}')
        local charmapfile=$(echo ${LOCALE} | awk -F. '{print $2}')
        chroot_exec localedef -i ${inputfile} -c -f ${charmapfile} ${LOCALE}
    else
        LOCALE="C"
    fi
    case "${DISTRIB}" in
    debian|ubuntu|kalilinux)
        echo "LANG=${LOCALE}" > "${CHROOT_DIR}/etc/default/locale"
    ;;
    archlinux)
        echo "LANG=${LOCALE}" > "${CHROOT_DIR}/etc/locale.conf"
    ;;
    fedora)
        echo "LANG=${LOCALE}" > "${CHROOT_DIR}/etc/sysconfig/i18n"
    ;;
    opensuse)
        echo "RC_LANG=${LOCALE}" > "${CHROOT_DIR}/etc/sysconfig/language"
    ;;
    slackware)
        sed -i "s|^export LANG=.*|export LANG=${LOCALE}|g" "${CHROOT_DIR}/etc/profile.d/lang.sh"
    ;;
    esac
    return 0
}

do_help()
{
cat <<EOF 1>&3
   --locale=LOCALE
     Локализация дистрибутива, например ru_RU.UTF-8.

EOF
}
