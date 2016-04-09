#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    msg ":: Configuring ${COMPONENT} ... "
    local unchroot="${CHROOT_DIR}/bin/unchroot"
    echo '#!/bin/sh' > "${unchroot}"
    echo 'if [ "$(whoami)" != "root" ]; then' >> "${unchroot}"
    echo 'sudo $0 $@; exit $?' >> "${unchroot}"
    echo 'fi' >> "${unchroot}"
    echo "PATH=$PATH:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin" >> "${unchroot}"
    echo 'if [ $# -eq 0 ]; then' >> "${unchroot}"
    echo 'chroot /proc/1/cwd su -' >> "${unchroot}"
    echo 'else' >> "${unchroot}"
    echo 'chroot /proc/1/cwd $*' >> "${unchroot}"
    echo 'fi' >> "${unchroot}"
    chmod 755 "${unchroot}"
    return 0
}
