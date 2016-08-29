#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${SUITE}" ] || SUITE="sana"

if [ -z "${ARCH}" ]
then
    case "$(get_platform)" in
    x86) ARCH="i386" ;;
    x86_64) ARCH="amd64" ;;
    arm) ARCH="armhf" ;;
    arm_64) ARCH="arm64" ;;
    esac
fi

[ -n "${SOURCE_PATH}" ] || SOURCE_PATH="http://http.kali.org/kali/"

do_help()
{
cat <<EOF
   --arch="${ARCH}"
     Архитектура сборки дистрибутива, поддерживаются armel, armhf, arm64, i386 и amd64.

   --suite="${SUITE}"
     Версия дистрибутива, поддерживаются версии sana и kali-rolling.

   --source-path="${SOURCE_PATH}"
     Источник установки дистрибутива, можно указать адрес репозитория или путь к rootfs-ахриву.

EOF
}
