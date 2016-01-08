#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${PRIVILEGED_USERS}" ] || PRIVILEGED_USERS="root"

do_configure()
{
    [ -d "/system" ] || return 0

    msg ":: Configuring ${COMPONENT} ... "
    # set min uid and gid
    sed -i 's|^UID_MIN.*|UID_MIN 5000|g' "${CHROOT_DIR}/etc/login.defs"
    sed -i 's|^GID_MIN.*|GID_MIN 5000|g' "${CHROOT_DIR}/etc/login.defs"
    # add android groups
    local aid uid
    for aid in $(cat "${COMPONENT_DIR}/android_groups")
    do
        local xname=$(echo ${aid} | awk -F: '{print $1}')
        local xid=$(echo ${aid} | awk -F: '{print $2}')
        sed -i "s|^${xname}:.*|${xname}:x:${xid}:${USER_NAME}|g" "${CHROOT_DIR}/etc/group"
        if ! $(grep -q "^${xname}:" "${CHROOT_DIR}/etc/group"); then
            echo "${xname}:x:${xid}:${USER_NAME}" >> "${CHROOT_DIR}/etc/group"
        fi
        if ! $(grep -q "^:" "${CHROOT_DIR}/etc/passwd"); then
            echo "${xname}:x:${xid}:${xid}::/:/bin/false" >> "${CHROOT_DIR}/etc/passwd"
        fi
        # add users to aid_inet group
        for uid in ${PRIVILEGED_USERS}
        do
            if ! $(grep -q "^${xname}:.*${uid}" "${CHROOT_DIR}/etc/group"); then
                sed -i "s|^\(${xname}:.*\)|\1,${uid}|g" "${CHROOT_DIR}/etc/group"
            fi
        done
    done
    return 0
}

do_help()
{
cat <<EOF
   --privileged-users=USERS
     Список пользователей через пробел, которых добавить в группы Android.

EOF
}
