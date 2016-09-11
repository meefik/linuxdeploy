#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_help()
{
cat <<EOF
   --method="${METHOD}"
     Containerization method "chroot" or "proot".

   --chroot-dir="${CHROOT_DIR}"
     Mount directory of the container for containerization method "chroot".

EOF
}
