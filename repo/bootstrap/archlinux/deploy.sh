#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${SUITE}" ] || SUITE="latest"

pacman_install()
{
    local packages="$@"
    [ -n "${packages}" ] || return 1
    (set -e
        #rm -f ${CHROOT_DIR}/var/lib/pacman/db.lck || true
        chroot_exec -u root pacman -Syq --force --noconfirm ${packages}
        rm -f "${CHROOT_DIR}"/var/cache/pacman/pkg/* || true
    exit 0) 1>&3 2>&3
    return $?
}

pacman_repository()
{
    sed -i "s|^[[:space:]]*Architecture[[:space:]]*=.*$|Architecture = ${ARCH}|" "${CHROOT_DIR}/etc/pacman.conf"
    sed -i "s|^[[:space:]]*\(CheckSpace\)|#\1|" "${CHROOT_DIR}/etc/pacman.conf"
    sed -i "s|^[[:space:]]*SigLevel[[:space:]]*=.*$|SigLevel = Never|" "${CHROOT_DIR}/etc/pacman.conf"
    if $(grep "^[[:space:]]*Server" "${CHROOT_DIR}/etc/pacman.d/mirrorlist")
    then sed -i "s|^[[:space:]]*Server[[:space:]]*=.*|Server = ${REPO_URL}|" "${CHROOT_DIR}/etc/pacman.d/mirrorlist"
    else echo "Server = ${REPO_URL}" >> "${CHROOT_DIR}/etc/pacman.d/mirrorlist"
    fi
}

do_install()
{
    is_archive "${SOURCE_PATH}" && return 0

    msg ":: Installing ${COMPONENT} ... "

    local basic_packages="filesystem acl archlinux-keyring attr bash bzip2 ca-certificates coreutils cracklib curl db e2fsprogs expat findutils gawk gcc-libs gdbm glibc gmp gnupg gpgme grep keyutils krb5 libarchive libassuan libcap libgcrypt libgpg-error libgssglue libidn libksba libldap libsasl libssh2 libtirpc linux-api-headers lzo ncurses nettle openssl pacman pacman-mirrorlist pam pambase perl pinentry pth readline run-parts sed shadow sudo tzdata util-linux xz which zlib"

    if [ "$(get_platform ${ARCH})" = "intel" ]
    then REPO_URL="${SOURCE_PATH%/}/core/os/${ARCH}"
    else REPO_URL="${SOURCE_PATH%/}/${ARCH}/core"
    fi

    local cache_dir="${CHROOT_DIR}/var/cache/pacman/pkg"

    msg "URL: ${REPO_URL}"

    msg -n "Preparing for deployment ... "
    (set -e
        cd "${CHROOT_DIR}"
        mkdir etc
        echo "root:x:0:0:root:/root:/bin/bash" > etc/passwd
        echo "root:x:0:" > etc/group
        touch etc/fstab
        mkdir tmp; chmod 1777 tmp
        mkdir -p var/tmp; chmod 1777 var/tmp
        mkdir -p var/games; chmod 775 var/games
        mkdir -p var/spool/mail; chmod 1777 var/spool/mail
        mkdir -p "${cache_dir}"
    exit 0)
    is_ok "fail" "done" || return 1

    msg -n "Retrieving packages list ... "
    local pkg_list=$(wget -q -O - "${REPO_URL}/" | sed -n '/<a / s/^.*<a [^>]*href="\([^\"]*\)".*$/\1/p' | awk -F'/' '{print $NF}' | sort -rn)
    is_ok "fail" "done" || return 1

    msg "Retrieving base packages: "
    for package in ${basic_packages}; do
        msg -n "${package} ... "
        local pkg_file=$(echo "${pkg_list}" | grep -m1 -e "^${package}-[[:digit:]].*\.xz$" -e "^${package}-[[:digit:]].*\.gz$")
        test "${pkg_file}"; is_ok "fail" || return 1
        # download
        local i
        for i in 1 2 3
        do
            wget -q -c -O "${cache_dir}/${pkg_file}" "${REPO_URL}/${pkg_file}" && break
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
    done

    component_exec core/emulator core/dns core/mtab

    msg -n "Updating repository ... "
    pacman_repository
    is_ok "fail" "done"

    msg "Installing base packages: "
    extra_packages=$(chroot_exec -u root /usr/bin/pacman --noconfirm -Sg base | awk '{print $2}' | grep -v -e 'linux' -e 'kernel' | xargs)
    pacman_install ${basic_packages} ${extra_packages}
    is_ok || return 1

    msg -n "Clearing cache ... "
    rm -f "${cache_dir}"/* "${CHROOT_DIR}/.INSTALL" "${CHROOT_DIR}/.MTREE" "${CHROOT_DIR}/.PKGINFO" $(find "${CHROOT_DIR}" -type f -name "*.pacorig")
    is_ok "skip" "done"

    return 0
}
