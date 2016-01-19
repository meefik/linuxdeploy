#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

INIT="${INIT##*/}"

do_help()
{
cat <<EOF
   --init=NAME
     Система инициализации.

EOF
}
