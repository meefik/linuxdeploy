#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${PRIVILEGED_USERS}" ] || PRIVILEGED_USERS="root"

do_configure()
{
    [ -n "$(which getprop)" ] || return 0

    msg ":: Configuring ${COMPONENT} ... "
    local aid
    for aid in $(cat "${COMPONENT_DIR}/android_groups")
    do
        local xname=$(echo ${aid} | awk -F: '{print $1}')
        local xid=$(echo ${aid} | awk -F: '{print $2}')
        sed -i "s|^${xname}:.*|${xname}:x:${xid}:${USER_NAME}|g" "${CHROOT_DIR}/etc/group"
        local is_group=$(grep -c "^${xname}:" "${CHROOT_DIR}/etc/group")
        [ "${is_group}" -eq 0 ] && echo "${xname}:x:${xid}:${USER_NAME}" >> "${CHROOT_DIR}/etc/group"
        local is_passwd=$(grep -c "^${xname}:" "${CHROOT_DIR}/etc/passwd")
        [ "${is_passwd}" -eq 0 ] && echo "${xname}:x:${xid}:${xid}::/:/bin/false" >> "${CHROOT_DIR}/etc/passwd"
        sed -i 's|^UID_MIN.*|UID_MIN 5000|g' "${CHROOT_DIR}/etc/login.defs"
        sed -i 's|^GID_MIN.*|GID_MIN 5000|g' "${CHROOT_DIR}/etc/login.defs"
    done
    # add users to aid_inet group
    local inet_users="root"
    local uid
    for uid in ${inet_users}
    do
        if [ $(grep -c "^aid_inet:.*${uid}" "${CHROOT_DIR}/etc/group") -eq 0 ]; then
            sed -i "s|^\(aid_inet:.*\)|\1,${uid}|g" "${CHROOT_DIR}/etc/group"
        fi
    done
    return 0
}

do_help()
{
cat <<EOF 1>&3
   --privileged-users=USERS
     Список пользователей через пробел, которых добавить в группы Android.

EOF
}
