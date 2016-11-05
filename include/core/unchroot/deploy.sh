#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local unchroot="${CHROOT_DIR}/sbin/unchroot"
    echo '#!/bin/sh' > "${unchroot}"
    echo 'if [ "$(whoami)" != "root" ]; then' >> "${unchroot}"
    echo 'sudo $0 "$@"; exit $?' >> "${unchroot}"
    echo 'fi' >> "${unchroot}"
    echo 'chroot_exec=$(which chroot)' >> "${unchroot}"
    echo "PATH=$PATH" >> "${unchroot}"
    echo 'if [ $# -eq 0 ]; then' >> "${unchroot}"
    echo '$chroot_exec /proc/1/cwd su -' >> "${unchroot}"
    echo 'else' >> "${unchroot}"
    echo '$chroot_exec /proc/1/cwd "$@"' >> "${unchroot}"
    echo 'fi' >> "${unchroot}"
    chmod 755 "${unchroot}"
    return 0
}
