#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

GRAPHICS="${GRAPHICS##*/}"

do_help()
{
cat <<EOF
   --graphics=NAME
     Графическая подсистема.

EOF
}
