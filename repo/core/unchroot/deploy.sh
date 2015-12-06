#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local unchroot="${CHROOT_DIR}/bin/unchroot"
    echo '#!/bin/sh' > "${unchroot}"
    echo "PATH=${PATH}" >> "${unchroot}"
    echo 'if [ $# -eq 0 ]; then' >> "${unchroot}"
    echo 'chroot /proc/1/cwd su -' >> "${unchroot}"
    echo 'else' >> "${unchroot}"
    echo 'chroot /proc/1/cwd $*' >> "${unchroot}"
    echo 'fi' >> "${unchroot}"
    chmod 755 "${unchroot}"
    return 0
}
