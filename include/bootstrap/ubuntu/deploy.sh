#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${SUITE}" ] || SUITE="trusty"

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
     Архитектура сборки дистрибутива, поддерживаются armel, armhf, arm64, i386 и amd64.

   --suite="${SUITE}"
     Версия дистрибутива, поддерживаются версии lucid, precise, trusty, vivid, wily и xenial.

   --source-path="${SOURCE_PATH}"
     Источник установки дистрибутива, можно указать адрес репозитория или путь к rootfs-ахриву.

EOF
}
