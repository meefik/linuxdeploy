#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${SUITE}" ] || SUITE="stable"

if [ -z "${ARCH}" ]
then
    case "$(get_platform)" in
    x86) ARCH="i386" ;;
    x86_64) ARCH="amd64" ;;
    arm) ARCH="armhf" ;;
    arm_64) ARCH="arm64" ;;
    esac
fi

[ -n "${SOURCE_PATH}" ] || SOURCE_PATH="http://ftp.debian.org/debian/"

apt_install()
{
    local packages="$@"
    [ -n "${packages}" ] || return 1
    (set -e
        chroot_exec -u root apt-get update -yq
        chroot_exec -u root "DEBIAN_FRONTEND=noninteractive apt-get install -yf"
        chroot_exec -u root "DEBIAN_FRONTEND=noninteractive apt-get install ${packages} --no-install-recommends -yq"
        chroot_exec -u root apt-get clean
    exit 0)
    return $?
}

apt_repository()
{
    # Backup sources.list
    if [ -e "${CHROOT_DIR}/etc/apt/sources.list" ]; then
        cp "${CHROOT_DIR}/etc/apt/sources.list" "${CHROOT_DIR}/etc/apt/sources.list.bak"
    fi
    # Fix for resolv problem in stretch
    echo 'Debug::NoDropPrivs true;' > "${CHROOT_DIR}/etc/apt/apt.conf.d/00no-drop-privs"
    # Update sources.list
    echo "deb ${SOURCE_PATH} ${SUITE} main contrib non-free" > "${CHROOT_DIR}/etc/apt/sources.list"
    echo "deb-src ${SOURCE_PATH} ${SUITE} main contrib non-free" >> "${CHROOT_DIR}/etc/apt/sources.list"
}

do_install()
{
    is_archive "${SOURCE_PATH}" && return 0

    msg ":: Installing ${COMPONENT} ... "

    local basic_packages="locales,sudo,man-db"
    #selinux_support && basic_packages="${basic_packages},selinux-basics"

    (set -e
        DEBOOTSTRAP_DIR="$(component_dir bootstrap/debian)/debootstrap"
        . "${DEBOOTSTRAP_DIR}/debootstrap" --no-check-gpg --foreign --extractor=ar --arch="${ARCH}" --include="${basic_packages}" "${SUITE}" "${CHROOT_DIR}" "${SOURCE_PATH}"
    exit 0)
    is_ok || return 1

    component_exec core/emulator core/mnt core/net

    unset DEBOOTSTRAP_DIR
    chroot_exec /debootstrap/debootstrap --no-check-gpg --second-stage
    is_ok || return 1

    msg -n "Updating repository ... "
    apt_repository
    is_ok "fail" "done"

    return 0
}

do_help()
{
cat <<EOF
   --arch="${ARCH}"
     Architecture of Linux distribution, supported "armel", "armhf", "arm64", "i386" and "amd64".

   --suite="${SUITE}"
     Version of Linux distribution, supported versions "wheezy", "jessie" и "stretch" (also can be used "stable", "testing", "unstable" and "sid").

   --source-path="${SOURCE_PATH}"
     Installation source, can specify address of the repository or path to the rootfs archive.

EOF
}
