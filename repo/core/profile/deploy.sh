#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${USER_NAME}" ] || USER_NAME="android"
[ -n "${USER_PASSWORD}" ] || USER_PASSWORD="changeme"

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    if [ -z "${USER_NAME%aid_*}" ]; then
        echo "Username \"${USER_NAME}\" is reserved."; return 1
    fi
    # user profile
    if [ "${USER_NAME}" != "root" ]; then
        chroot_exec groupadd ${USER_NAME}
        chroot_exec useradd -m -g ${USER_NAME} -s /bin/bash ${USER_NAME}
        chroot_exec usermod -g ${USER_NAME} ${USER_NAME}
    fi
    local user_home="$(user_home ${USER_NAME})"
    local path_str="PATH=${PATH}"
    if ! $(grep -q "${path_str}" "${CHROOT_DIR}${user_home}/.profile"); then
        echo ${path_str} >> "${CHROOT_DIR}${user_home}/.profile"
    fi
    # set password for user
    echo ${USER_NAME}:${USER_PASSWORD} | chroot_exec chpasswd
    # set permissions
    chroot_exec chown -R ${USER_NAME}:${USER_NAME} "${user_home}"
    return 0
}

do_help()
{
cat <<EOF 1>&3
   --user-name=USER
     Имя пользователя, который будет создан после установки дистрибутива.

   --user-password=PASSWORD
     Пароль пользователя будет назначен указанному пользователю.

EOF
}
