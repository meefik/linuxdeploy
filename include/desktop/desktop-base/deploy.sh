#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local xinitrc="$(user_home ${USER_NAME})/.xinitrc"
    local xsession="$(user_home ${USER_NAME})/.xsession"
    local xinitrc_chroot="${CHROOT_DIR}${xinitrc}"
    local xsession_chroot="${CHROOT_DIR}${xsession}"
    [ -e "${xinitrc_file}" -o -L "${xinitrc_file}" ] && rm -f "${xinitrc_chroot}"
    echo 'XAUTHORITY=$HOME/.Xauthority' > "${xinitrc_chroot}"
    echo 'export XAUTHORITY' >> "${xinitrc_chroot}"
    echo "LANG=$LOCALE" >> "${xinitrc_chroot}"
    echo 'export LANG' >> "${xinitrc_chroot}"
    echo 'echo $$ > /tmp/xsession.pid' >> "${xinitrc_chroot}"
    echo '. $HOME/.xsession' >> "${xinitrc_chroot}"
    chmod 755 "${xinitrc_chroot}"
    chroot_exec -u root chown ${USER_NAME}:${USER_NAME} "${xinitrc}"
    touch "${xsession_chroot}"
    chmod 644 "${xsession_chroot}"
    chroot_exec -u root chown ${USER_NAME}:${USER_NAME} "${xsession}"
    return 0
}
