#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    if [ "${METHOD}" = "proot" ]; then
        ln -sf /proc/mounts "${CHROOT_DIR}/etc/mtab"
    else
        rm -f "${CHROOT_DIR}/etc/mtab"
        grep "${CHROOT_DIR}" /proc/mounts | sed "s|${CHROOT_DIR}/*|/|g" > "${CHROOT_DIR}/etc/mtab"
    fi
    return 0
}

do_start()
{
    if [ "${METHOD}" = "chroot" -a -n "${MOUNTS}" ]; then
        msg ":: Mounting partitions: "
        local item disk_src disk_dst target
        for item in ${MOUNTS}
        do
            disk_src="${item%%:*}"
            disk_dst="${item##*:}"
            [ -n "${disk_src}" -a -n "${disk_dst}" ] || continue
            msg -n "${disk_src} ... "
            target="${CHROOT_DIR}${disk_dst}"
            if ! is_mounted "${target}" ; then
                [ -d "${target}" ] || mkdir -p "${target}"
                [ -e "${disk_src}" ] || mkdir -p "${disk_src}"
                if [ -d "${disk_src}" ]; then
                    mount -o bind "${disk_src}" "${target}"
                    is_ok "fail" "done"
                elif [ -e "${disk_src}" ]; then
                    mount -o rw,relatime "${disk_src}" "${target}"
                    is_ok "fail" "done"
                else
                    msg "skip"
                fi
            else
                msg "skip"
            fi
        done
    fi
    do_configure
}

do_help()
{
cat <<EOF
   --mounts="${MOUNTS}"
     Mounts resources to the container as "SOURCE:TARGET" separated by a space.

EOF
}
