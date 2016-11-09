#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${SUITE}" ] || SUITE="xenial"

if [ -z "${ARCH}" ]
then
    case "$(get_platform)" in
    x86) ARCH="i386" ;;
    x86_64) ARCH="amd64" ;;
    arm) ARCH="armhf" ;;
    arm_64) ARCH="arm64" ;;
    esac
fi

if [ -z "${SOURCE_PATH}" ]
then
    case "$(get_platform ${ARCH})" in
    x86*) SOURCE_PATH="http://archive.ubuntu.com/ubuntu/" ;;
    arm*) SOURCE_PATH="http://ports.ubuntu.com/" ;;
    esac
fi

apt_repository()
{
    # Backup sources.list
    if [ -e "${CHROOT_DIR}/etc/apt/sources.list" ]; then
        cp "${CHROOT_DIR}/etc/apt/sources.list" "${CHROOT_DIR}/etc/apt/sources.list.bak"
    fi
    # Fix for resolv problem in xenial
    echo 'Debug::NoDropPrivs true;' > "${CHROOT_DIR}/etc/apt/apt.conf.d/00no-drop-privs"
    # Update sources.list
    echo "deb ${SOURCE_PATH} ${SUITE} main universe multiverse" > "${CHROOT_DIR}/etc/apt/sources.list"
    echo "deb-src ${SOURCE_PATH} ${SUITE} main universe multiverse" >> "${CHROOT_DIR}/etc/apt/sources.list"
    # Fix for upstart
    if [ -e "${CHROOT_DIR}/sbin/initctl" ]; then
        chroot_exec -u root dpkg-divert --local --rename --add /sbin/initctl
        ln -s /bin/true "${CHROOT_DIR}/sbin/initctl"
    fi
}

do_help()
{
cat <<EOF
   --arch="${ARCH}"
     Architecture of Linux distribution, supported "armel", "armhf", "arm64", "i386" and "amd64".

   --suite="${SUITE}"
     Version of Linux distribution, supported versions "precise", "trusty", "vivid", "wily", "xenial" and "yakkety".

   --source-path="${SOURCE_PATH}"
     Installation source, can specify address of the repository or path to the rootfs archive.

EOF
}
