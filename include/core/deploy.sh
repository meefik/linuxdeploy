#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_help()
{
cat <<EOF
   --method="${METHOD}"
     Метод контейнеризации chroot или proot.

   --chroot-dir="${CHROOT_DIR}"
     Директория монтирования контейнера для метода контейнеризации "chroot".

EOF
}
