#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

DISTRIB="${DISTRIB##*/}"

do_help()
{
cat <<EOF
   --distrib="${DISTRIB}"
     Кодовое имя дистрибутива, который будет установлен.

EOF
}
