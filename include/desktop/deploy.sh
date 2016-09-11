#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

DESKTOP="${DESKTOP##*/}"

do_help()
{
cat <<EOF
   --desktop="${DESKTOP}"
     Desktop environment.

EOF
}
