#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

yum_install()
{
    local packages="$@"
    [ -n "${packages}" ] || return 1
    (set -e
        chroot_exec -u root yum install ${packages} --nogpgcheck --skip-broken -y
        chroot_exec -u root yum clean all
    exit 0) 1>&3 2>&3
    return $?
}

yum_groupinstall()
{
    local groupname="$@"
    [ -n "${groupname}" ] || return 1
    (set -e
        chroot_exec -u root yum groupinstall ${groupname} --nogpgcheck --skip-broken -y
        chroot_exec -u root yum clean all
    exit 0) 1>&3 2>&3
    return $?
}

yum_repository()
{
    chroot_exec -u root yum-config-manager --disable '*' >/dev/null
    local repo_file="${CHROOT_DIR}/etc/yum.repos.d/CentOS-${SUITE}-${ARCH}.repo"
    echo "[centos-${SUITE}-${ARCH}]" > "${repo_file}"
    echo "name=CentOS ${SUITE} - ${ARCH}" >> "${repo_file}"
    echo "failovermethod=priority" >> "${repo_file}"
    echo "baseurl=${REPO_URL}" >> "${repo_file}"
    echo "enabled=1" >> "${repo_file}"
    echo "metadata_expire=7d" >> "${repo_file}"
    echo "gpgcheck=0" >> "${repo_file}"
    chmod 644 "${repo_file}"
}

do_install()
{
    is_archive "${SOURCE_PATH}" && return 0

    msg ":: Installing ${COMPONENT} ... "

    local basic_packages="audit-libs basesystem bash bzip2-libs ca-certificates centos-release chkconfig coreutils cpio cracklib cracklib-dicts cryptsetup-libs curl cyrus-sasl-lib dbus dbus-libs device-mapper device-mapper-libs diffutils elfutils-libelf elfutils-libs expat file-libs filesystem gawk gdbm glib2 glibc glibc-common gmp gnupg2 gpgme grep gzip info keyutils-libs kmod kmod-libs krb5-libs libacl libassuan libattr libblkid libcap libcap-ng libcom_err libcurl libdb libdb-utils libffi libgcc libgcrypt libgpg-error libidn libmount libpwquality libselinux libsemanage libsepol libssh2 libstdc++ libtasn1 libuser libutempter libuuid libverto libxml2 lua ncurses ncurses-base ncurses-libs nspr nss nss-softokn nss-softokn-freebl nss-sysinit nss-tools nss-util openldap openssl-libs p11-kit p11-kit-trust pam pcre pinentry pkgconfig popt pth pygpgme pyliblzma python python-chardet python-iniparse python-kitchen python-libs python-pycurl python-urlgrabber pyxattr qrencode-libs readline rootfiles rpm rpm-build-libs rpm-libs rpm-python sed selinux-policy setup shadow-utils shared-mime-info sqlite sudo systemd systemd-libs tzdata ustr util-linux vim-minimal which xz-libs yum yum-metadata-parser yum-plugin-fastestmirror yum-utils zlib"
    REPO_URL="${SOURCE_PATH%/}/${SUITE}/os/${ARCH}"

    msg "URL: ${REPO_URL}"

    msg -n "Preparing for deployment ... "
    tar xzf "${COMPONENT_DIR}/filesystem.tgz" -C "${CHROOT_DIR}"
    is_ok "fail" "done" || return 1

    msg -n "Retrieving packages list ... "
    local pkg_list="${CHROOT_DIR}/tmp/packages.list"
    (set -e
        repodata=$(wget -q -O - "${REPO_URL}/repodata/repomd.xml" | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\-primary\.xml\.gz\)".*$/\1/p')
        [ -z "${repodata}" ] && exit 1
        wget -q -O - "${REPO_URL}/${repodata}" | gzip -dc | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\)".*$/\1/p' > "${pkg_list}"
    exit 0)
    is_ok "fail" "done" || return 1

    msg "Retrieving base packages: "
    local package i pkg_url pkg_file
    for package in ${basic_packages}; do
        msg -n "${package} ... "
        pkg_url=$(grep -m1 -e "^.*/${package}-[0-9][0-9\.\-].*\.rpm$" "${pkg_list}")
        test "${pkg_url}"; is_ok "skip" || continue
        pkg_file="${pkg_url##*/}"
        # download
        for i in 1 2 3
        do
            wget -q -c -O "${CHROOT_DIR}/tmp/${pkg_file}" "${REPO_URL}/${pkg_url}" && break
            sleep 30s
        done
        [ "${package}" = "filesystem" ] && { msg "done"; continue; }
        # unpack
        (cd "${CHROOT_DIR}"; rpm2cpio "./tmp/${pkg_file}" | cpio -idmu)
        is_ok "fail" "done" || return 1
    done

    component_exec core/emulator

    msg "Installing base packages: "
    chroot_exec /bin/rpm -iv --excludepath / --force --nosignature --nodeps --justdb /tmp/*.rpm 1>&3 2>&3
    is_ok "fail" "done" || return 1

    msg -n "Clearing cache ... "
    rm -rf "${CHROOT_DIR}"/tmp/*
    is_ok "skip" "done"

    component_exec core/dns core/mtab

    msg -n "Updating repository ... "
    yum_repository
    is_ok "fail" "done"

    msg "Installing minimal environment: "
    yum_groupinstall "Minimal Install" --exclude filesystem,openssh-server
    is_ok || return 1

    return 0
}
