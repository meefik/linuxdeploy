#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

if [ -z "${ARCH}" ]
then
    case "$(get_platform)" in
    x86) ARCH="i686" ;;
    x86_64) ARCH="amd64" ;;
    arm*) ARCH="armv7a_hardfp" ;;
    esac
fi

[ -n "${SOURCE_PATH}" ] || SOURCE_PATH="http://mirror.yandex.ru/gentoo-distfiles/releases/"

emerge_install()
{
    local packages="$@"
    [ -n "${packages}" ] || return 1
    (set -e
        chroot_exec -u root emerge --autounmask-write ${packages} || {
            chroot_exec -u root etc-update --automode -5
            chroot_exec -u root emerge ${packages}
        }
    exit 0)
    return $?
}

emerge_repository()
{
    if ! $(grep -q '^aid_inet:.*,portage' "${CHROOT_DIR}/etc/group"); then
        sed -i "s|^\(aid_inet:.*\)|\1,portage|g" "${CHROOT_DIR}/etc/group"
    fi
    # set MAKEOPTS
    local ncpu=$(grep -c ^processor /proc/cpuinfo)
    let ncpu=${ncpu}+1
    if ! $(grep -q '^MAKEOPTS=' "${CHROOT_DIR}/etc/portage/make.conf"); then
        echo "MAKEOPTS=\"-j${ncpu}\"" >> "${CHROOT_DIR}/etc/portage/make.conf"
    fi
}

do_install()
{
    is_archive "${SOURCE_PATH}" && return 0

    msg ":: Installing ${COMPONENT} ... "

    msg -n "Preparing for deployment ... "
    (set -e
        cd "${CHROOT_DIR}"
        mkdir tmp; chmod 1777 tmp
    exit 0)
    is_ok "fail" "done" || return 1

    msg -n "Getting repository path ... "

    case "$(get_platform ${ARCH})" in
    x86*) local repo_url="${SOURCE_PATH%/}/x86/autobuilds" ;;
    arm*) local repo_url="${SOURCE_PATH%/}/arm/autobuilds" ;;
    esac
    local stage3="${CHROOT_DIR}/tmp/latest-stage3.tar.bz2"
    local archive=$(wget -q -O - "${repo_url}/latest-stage3-${ARCH}.txt" | grep -v ^# | awk '{print $1}')
    test "${archive}"; is_ok "fail" "done" || return 1

    msg -n "Retrieving stage3 archive ... "
    local i
    for i in 1 2 3
    do
        wget -c -O "${stage3}" "${repo_url}/${archive}" && break
        sleep 30s
    done
    is_ok "fail" "done" || return 1

    msg -n "Unpacking stage3 archive ... "
    (set -e
        tar xjf "${stage3}" -C "${CHROOT_DIR}" --exclude='./dev' --exclude='./sys' --exclude='./proc'
        rm -f "${stage3}"
    exit 0)
    is_ok "fail" "done" || return 1

    component_exec core/emulator core/mnt core/net
    PRIVILEGED_USERS="portage" component_exec core/aid

    msg -n "Updating repository ... "
    emerge_repository
    is_ok "fail" "done"

    msg -n "Updating portage tree ... "
    (set -e
        chroot_exec emerge --sync
        chroot_exec eselect profile set 1
    exit 0) 1>/dev/null
    is_ok "fail" "done" || return 1

    msg "Installing base packages: "
    emerge_install sudo
    is_ok || return 1

    msg -n "Updating configuration ... "
    find "${CHROOT_DIR}/" -name "._cfg0000_*" | while read f; do mv "${f}" "$(echo ${f} | sed 's/._cfg0000_//g')"; done
    is_ok "skip" "done"

    return 0
}

do_help()
{
cat <<EOF
   --arch="${ARCH}"
     Architecture of Linux distribution, supported "armv4tl", "armv5tel", "armv6j", "armv6j_hardfp", "armv7a", "armv7a_hardfp", "i486", "i686" and "amd64".

   --source-path="${SOURCE_PATH}"
     Installation source, can specify address of the repository or path to the rootfs archive.

EOF
}
