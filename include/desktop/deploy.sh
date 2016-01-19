#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

DESKTOP="${DESKTOP##*/}"

do_help()
{
cat <<EOF
   --desktop=NAME
     Окружение рабочего стола.

EOF
}
