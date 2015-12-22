#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${SUITE}" ] || SUITE="latest"

emerge_install()
{
    local packages="$@"
    [ -n "${packages}" ] || return 1
    (set -e
        chroot_exec emerge --autounmask-write ${packages} || {
            mv "${CHROOT_DIR}/etc/portage/._cfg0000_package.use" "${CHROOT_DIR}/etc/portage/package.use"
            chroot_exec emerge ${packages}
        }
    exit 0) 1>&3 2>&3
    return $?
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
    local repo="${SOURCE_PATH%/}/autobuilds"
    local stage3="${CHROOT_DIR}/tmp/latest-stage3.tar.bz2"
    local archive=$(wget -q -O - "${repo}/latest-stage3-${ARCH}.txt" | grep -v ^# | awk '{print $1}')
    test "${archive}"; is_ok "fail" "done" || return 1

    msg -n "Retrieving stage3 archive ... "
    local i
    for i in 1 2 3
    do
        wget -c -O "${stage3}" "${repo}/${archive}" && break
        sleep 30s
    done
    is_ok "fail" "done" || return 1

    msg -n "Unpacking stage3 archive ... "
    (set -e
        tar xjf "${stage3}" -C "${CHROOT_DIR}" --exclude='./dev' --exclude='./sys' --exclude='./proc'
        rm -f "${stage3}"
    exit 0)
    is_ok "fail" "done" || return 1

    component_exec core/emulator core/dns core/mtab core/groups core/repository

    msg -n "Updating portage tree ... "
    (set -e
        chroot_exec emerge --sync
        chroot_exec eselect profile set 1
    exit 0) 1>/dev/null
    is_ok "fail" "done" || return 1

    msg "Installing base packages: "
    (set -e
        chroot_exec emerge sudo
    exit 0) 1>&3 2>&3
    is_ok || return 1

    msg -n "Updating configuration ... "
    find "${CHROOT_DIR}/" -name "._cfg0000_*" | while read f; do mv "${f}" "$(echo ${f} | sed 's/._cfg0000_//g')"; done
    is_ok "skip" "done"

    return 0
}
