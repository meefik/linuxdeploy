#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

DISTRIB="${DISTRIB##*/}"

do_help()
{
cat <<EOF
   --method=chroot|proot
     Метод контейнеризации.
     
   --chroot-dir=PATH
     Директория монтирования контейнера для метода контейнеризации "chroot".
     
   --distrib=debian|ubuntu|kalilinux|fedora|archlinux|centos|gentoo|opensuse|slackware
     Кодовое имя дистрибутива, который будет установлен.

   --arch=NAME
     Архитектура сборки дистрибутива, например i386 для debian. См. информацию по конкретному дистрибутиву.

   --suite=NAME
     Версия дистрибутива, например wheezy для debian. См. информацию по конкретному дистрибутиву.

   --source-path=PATH
     Источник установки дистрибутива, можно указать адрес репозитория или путь к rootfs-ахриву.

   --target-path=PATH
     Путь установки, зависит от типа развертывания.

EOF
}
