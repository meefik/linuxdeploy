#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

INIT="${INIT##*/}"

do_help()
{
cat <<EOF 1>&3
   --init=NAME
     Система инициализации.

EOF
}
