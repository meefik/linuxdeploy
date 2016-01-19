#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${SUITE}" ] || SUITE="latest"

slackpkg_install()
{
    local packages="$@"
    [ -n "${packages}" ] || return 1
    (set -e
        chroot_exec -u root slackpkg -checkgpg=off -batch=on -default_answer=y update || true
        chroot_exec -u root slackpkg -checkgpg=off -batch=on -default_answer=y install ${packages}
    exit 0)
    return $?
}

slackpkg_repository()
{
    if [ -e "${CHROOT_DIR}/etc/slackpkg/mirrors" ]; then
        cp "${CHROOT_DIR}/etc/slackpkg/mirrors" "${CHROOT_DIR}/etc/slackpkg/mirrors.bak"
    fi
    echo "${SOURCE_PATH}" > "${CHROOT_DIR}/etc/slackpkg/mirrors"
    chmod 644 "${CHROOT_DIR}/etc/slackpkg/mirrors"
    sed -i 's|^WGETFLAGS=.*|WGETFLAGS="--passive-ftp -q"|g' "${CHROOT_DIR}/etc/slackpkg/slackpkg.conf"
}

do_install()
{
    is_archive "${SOURCE_PATH}" && return 0

    msg ":: Installing ${COMPONENT} ... "

    REPO_URL="${SOURCE_PATH%/}/slackware"
    local cache_dir="${CHROOT_DIR}/tmp"
    local extra_packages="l/glibc l/glibc-i18n l/libtermcap l/ncurses ap/diffutils ap/groff ap/man ap/nano ap/slackpkg ap/sudo n/gnupg n/wget"

    msg -n "Preparing for deployment ... "
    (set -e
        cd "${CHROOT_DIR}"
        mkdir etc
        touch etc/fstab
        mkdir tmp; chmod 1777 tmp
    exit 0)
    is_ok "fail" "done" || return 1

    msg -n "Retrieving packages list ... "
    local basic_packages=$(wget -q -O - "${REPO_URL}/a/tagfile" | grep -v -e 'kernel' -e 'efibootmgr' -e 'lilo' -e 'grub' -e 'devs' | awk -F: '{if ($1!="") print "a/"$1}')
    local pkg_list="${cache_dir}/packages.list"
    wget -q -O - "${REPO_URL}/FILE_LIST" | grep -o -e '/.*\.\tgz$' -e '/.*\.\txz$' > "${pkg_list}"
    is_ok "fail" "done" || return 1

    msg "Retrieving base packages: "
    local package i pkg_url pkg_file
    for package in ${basic_packages} ${extra_packages}
    do
        msg -n "${package} ... "
        pkg_url=$(grep -m1 -e "/${package}\-" "${pkg_list}")
        test "${pkg_url}"; is_ok "fail" || return 1
        pkg_file="${pkg_url##*/}"
        # download
        for i in 1 2 3
        do
            wget -q -c -O "${cache_dir}/${pkg_file}" "${REPO_URL}${pkg_url}" && break
            sleep 30s
        done
        # unpack
        case "${pkg_file}" in
        *gz) tar xzf "${cache_dir}/${pkg_file}" -C "${CHROOT_DIR}" --exclude='./dev' --exclude='./sys' --exclude='./proc';;
        *bz2) tar xjf "${cache_dir}/${pkg_file}" -C "${CHROOT_DIR}" --exclude='./dev' --exclude='./sys' --exclude='./proc';;
        *xz) tar xJf "${cache_dir}/${pkg_file}" -C "${CHROOT_DIR}" --exclude='./dev' --exclude='./sys' --exclude='./proc';;
        *) msg "fail"; return 1;;
        esac
        is_ok "fail" "done" || return 1
        # install
        if [ -e "${CHROOT_DIR}/install/doinst.sh" ]; then
            (cd "${CHROOT_DIR}"; . ./install/doinst.sh)
        fi
        if [ -e "${CHROOT_DIR}/install" ]; then
            rm -rf "${CHROOT_DIR}/install"
        fi
    done

    msg -n "Updating repository ... "
    slackpkg_repository
    is_ok "fail" "done"

    msg -n "Clearing cache ... "
    rm -f "${cache_dir}"/*
    is_ok "skip" "done"

    return 0
}
