#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

[ -n "${SUITE}" ] || SUITE="13.2"

if [ -z "${ARCH}" ]
then
    case "$(get_platform)" in
    x86) ARCH="i586" ;;
    x86_64) ARCH="x86_64" ;;
    arm) ARCH="armv7hl" ;;
    arm_64) ARCH="aarch64" ;;
    esac
fi

if [ -z "${SOURCE_PATH}" ]
then
    case "$(get_platform ${ARCH})" in
    x86*) SOURCE_PATH="http://download.opensuse.org/" ;;
    arm*) SOURCE_PATH="http://download.opensuse.org/ports/" ;;
    esac
fi

zypper_install()
{
    local packages="$@"
    [ -n "${packages}" ] || return 1
    (set -e
        chroot_exec -u root zypper --no-gpg-checks --non-interactive install ${packages}
        chroot_exec -u root zypper clean
    exit 0)
    return $?
}

zypper_repository()
{
    case "$(get_platform ${ARCH})" in
    x86*) local repo_url="${SOURCE_PATH%/}/distribution/${SUITE}/repo/oss/suse" ;;
    arm*) local repo_url="${SOURCE_PATH%/}/${ARCH}/distribution/${SUITE}/repo/oss/suse" ;;
    *) return 1 ;;
    esac
    local repo_name="openSUSE-${SUITE}-${ARCH}-Repo-OSS"
    local repo_file="${CHROOT_DIR}/etc/zypp/repos.d/${repo_name}.repo"
    echo "[${repo_name}]" > "${repo_file}"
    echo "name=${repo_name}" >> "${repo_file}"
    echo "enabled=1" >> "${repo_file}"
    echo "autorefresh=0" >> "${repo_file}"
    echo "baseurl=${repo_url}" >> "${repo_file}"
    echo "type=NONE" >> "${repo_file}"
    chmod 644 "${repo_file}"
    sed -i "s|[# ]*arch *=.*|arch = ${ARCH}|" "${CHROOT_DIR}/etc/zypp/zypp.conf"
}

do_install()
{
    is_archive "${SOURCE_PATH}" && return 0

    msg ":: Installing ${COMPONENT} ... "

    local basic_packages=""
    case "${SUITE}" in
    12.3) basic_packages="filesystem aaa_base aaa_base-extras autoyast2-installation bash bind-libs bind-utils branding-openSUSE bridge-utils bzip2 coreutils cpio cracklib cracklib-dict-full cron cronie cryptsetup curl cyrus-sasl dbus-1 dbus-1-x11 device-mapper dhcpcd diffutils dirmngr dmraid e2fsprogs elfutils file fillup findutils fontconfig gawk gio-branding-openSUSE glib2-tools glibc glibc-extra glibc-i18ndata glibc-locale gnu-unifont-bitmap-fonts-20080123 gpg2 grep groff gzip hwinfo ifplugd info initviocons iproute2 iputils-s20101006 kbd kpartx krb5 less-456 libX11-6 libX11-data libXau6 libXext6 libXft2 libXrender1 libacl1 libadns1 libaio1 libasm1 libassuan0 libattr1 libaudit1 libaugeas0 libblkid1 libbz2-1 libcairo2 libcap-ng0 libcap2 libcom_err2 libcrack2 libcryptsetup4 libcurl4 libdaemon0 libdb-4_8 libdbus-1-3 libdrm2 libdw1 libedit0 libelf0 libelf1 libestr0 libexpat1 libext2fs2 libffi4 libfreetype6 libgcc_s1 libgcrypt11 libgdbm4 libgio-2_0-0 libglib-2_0-0 libgmodule-2_0-0 libgmp10 libgnutls28 libgobject-2_0-0 libgpg-error0 libgssglue1 libharfbuzz0 libhogweed2 libicu49 libidn11 libiw30 libjson0 libkeyutils1 libkmod2-12 libksba8 libldap-2_4-2 liblua5_1 liblzma5 libmagic1 libmicrohttpd10 libmodman1 libmount1 libncurses5 libncurses6 libnettle4 libnl3-200 libopenssl1_0_0 libp11-kit0 libpango-1_0-0 libparted0 libpci3 libpcre1 libpipeline1 libpixman-1-0 libply-boot-client2 libply-splash-core2 libply-splash-graphics2 libply2 libpng15-15 libpolkit0 libpopt0 libprocps1 libproxy1 libpth20 libpython2_7-1_0 libqrencode3 libreadline6 libreiserfs-0_3-0 libselinux1 libsemanage1 libsepol1 libsolv-tools libssh2-1 libstdc++6 libstorage4 libtasn1 libtasn1-6 libtirpc1 libudev1-195 libusb-0_1-4 libusb-1_0-0 libustr-1_0-1 libuuid1 libwrap0 libxcb-render0 libxcb-shm0 libxcb1 libxml2-2 libxtables9 libyui-ncurses-pkg4 libyui-ncurses4 libyui4 libz1 libzio1 libzypp logrotate lsscsi lvm2 man man-pages mdadm mkinitrd module-init-tools multipath-tools ncurses-utils net-tools netcfg openSUSE-build-key openSUSE-release-12.3 openSUSE-release-ftp-12.3 openslp openssl pam pam-config pango-tools parted pciutils pciutils-ids perl perl-Bootloader perl-Config-Crontab perl-XML-Parser perl-XML-Simple perl-base perl-gettext permissions pinentry pkg-config polkit procps python-base rpcbind rpm rsyslog sed shadow shared-mime-info sudo suse-module-tools sysconfig sysfsutils syslog-service systemd-195 systemd-presets-branding-openSUSE systemd-sysvinit-195 sysvinit-tools tar tcpd terminfo-base timezone-2012j tunctl u-boot-tools udev-195 unzip update-alternatives util-linux vim vim-base vlan wallpaper-branding-openSUSE wireless-tools wpa_supplicant xz yast2 yast2-bootloader yast2-core yast2-country yast2-country-data yast2-firstboot yast2-hardware-detection yast2-installation yast2-packager yast2-perl-bindings yast2-pkg-bindings yast2-proxy yast2-slp yast2-storage yast2-trans-stats yast2-transfer yast2-update yast2-xml yast2-ycp-ui-bindings zypper"
    ;;
    13.2) basic_packages="filesystem aaa_base aaa_base-extras autoyast2-installation bash bind-libs bind-utils branding-openSUSE bridge-utils bzip2 coreutils cpio cracklib cracklib-dict-full cron cronie cryptsetup curl cyrus-sasl dbus-1 dbus-1-x11 device-mapper dhcpcd diffutils dirmngr dmraid e2fsprogs elfutils file fillup findutils fontconfig gawk gio-branding-openSUSE glib2-tools glibc glibc-extra glibc-i18ndata glibc-locale gnu-unifont-bitmap-fonts-20080123 gpg2 grep groff gzip hwinfo ifplugd info initviocons iproute2 iputils-s20121221 kbd kpartx krb5 less-458 libX11-6 libX11-data libXau6 libXext6 libXft2 libXrender1 libacl1 libadns1 libaio1 libasm1 libassuan0 libattr1 libaudit1 libaugeas0 libblkid1 libbz2-1 libcairo2 libcap-ng0 libcap2 libcom_err2 libcrack2 libcryptsetup4 libcurl4 libdaemon0 libdb-4_8 libdbus-1-3 libdrm2 libdw1 libedit0 libelf0 libelf1 libestr0 libexpat1 libext2fs2 libffi4 libfreetype6 libgcc_s1 libgcrypt20 libgdbm4 libgio-2_0-0 libglib-2_0-0 libgmodule-2_0-0 libgmp10 libgnutls28 libgobject-2_0-0 libgpg-error0 libgssglue1 libharfbuzz0 libhogweed2 libicu53_1 libidn11 libiw30 libjson-c2 libkeyutils1 libkmod2-18 libksba8 libldap-2_4-2 liblua5_1 liblua5_2 liblzma5 libmagic1 libmicrohttpd10 libmodman1 libmount1 libncurses5 libncurses6 libnettle4 libnl3-200 libopenssl1_0_0 libp11-kit0 libpango-1_0-0 libparted0 libpci3 libpcre1 libpipeline1 libpixman-1-0 libply-boot-client2 libply-splash-core2 libply-splash-graphics2 libply2 libpng16-16 libpolkit0 libpopt0 libprocps3 libproxy1 libpth20 libpython2_7-1_0 libqrencode3 libreadline6 libreiserfs-0_3-0 libsasl2-3 libselinux1 libsemanage1 libsepol1 libsolv-tools libssh2-1 libstdc++6 libstorage5 libtasn1 libtasn1-6 libtirpc1 libudev1-210 libusb-0_1-4 libusb-1_0-0 libustr-1_0-1 libuuid1 libwrap0 libxcb-render0 libxcb-shm0 libxcb1 libxml2-2 libxtables10 libyui-ncurses-pkg6 libyui-ncurses6 libyui6 libz1 libzio1 libzypp logrotate lsscsi lvm2 man man-pages mdadm multipath-tools ncurses-utils net-tools netcfg openSUSE-build-key openSUSE-release-13.2 openSUSE-release-ftp-13.2 openslp openssl pam pam-config pango-tools parted pciutils pciutils-ids perl perl-Bootloader perl-Config-Crontab perl-XML-Parser perl-XML-Simple perl-base perl-gettext permissions pinentry pkg-config polkit procps python-base rpcbind rpm rsyslog sed shadow shared-mime-info sudo suse-module-tools sysconfig sysfsutils syslog-service systemd-210 systemd-presets-branding-openSUSE systemd-sysvinit-210 sysvinit-tools tar tcpd terminfo-base timezone-2014h tunctl u-boot-tools udev-210 unzip update-alternatives util-linux vim vlan wallpaper-branding-openSUSE which wireless-tools wpa_supplicant xz yast2 yast2-bootloader yast2-core yast2-country yast2-country-data yast2-firstboot yast2-hardware-detection yast2-installation yast2-packager yast2-perl-bindings yast2-pkg-bindings yast2-proxy yast2-slp yast2-storage yast2-trans-stats yast2-transfer yast2-update yast2-xml yast2-ycp-ui-bindings zypper"
    ;;
    esac

    case "$(get_platform ${ARCH})" in
    x86*) local repo_url="${SOURCE_PATH%/}/distribution/${SUITE}/repo/oss/suse" ;;
    arm*) local repo_url="${SOURCE_PATH%/}/${ARCH}/distribution/${SUITE}/repo/oss/suse" ;;
    esac

    msg "URL: ${repo_url}"

    msg -n "Preparing for deployment ... "
    tar xzf "${COMPONENT_DIR}/filesystem.tgz" -C "${CHROOT_DIR}"
    is_ok "fail" "done" || return 1

    msg -n "Retrieving packages list ... "
    local pkg_list="${CHROOT_DIR}/tmp/packages.list"
    (set -e
        repodata=$(wget -q -O - "${repo_url}/repodata/repomd.xml" | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\-primary\.xml\.gz\)".*$/\1/p')
        [ -z "${repodata}" ] && exit 1
        wget -q -O - "${repo_url}/${repodata}" | gzip -dc | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\)".*$/\1/p' > "${pkg_list}"
    exit 0)
    is_ok "fail" "done" || return 1

    msg "Retrieving base packages: "
    local package i pkg_url pkg_file
    for package in ${basic_packages}; do
        msg -n "${package} ... "
        pkg_url=$(grep -e "^${ARCH}" -e "^noarch" "${pkg_list}" | grep -m1 -e "/${package}-[0-9]\{1,4\}\..*\.rpm$")
        test "${pkg_url}"; is_ok "fail" || return 1
        pkg_file="${pkg_url##*/}"
        # download
        for i in 1 2 3
        do
            wget -q -c -O "${CHROOT_DIR}/tmp/${pkg_file}" "${repo_url}/${pkg_url}" && break
            sleep 30s
        done
        [ "${package}" = "filesystem" ] && { msg "done"; continue; }
        # unpack
        (cd "${CHROOT_DIR}"; rpm2cpio "./tmp/${pkg_file}" | cpio -idmu)
        is_ok "fail" "done" || return 1
    done

    component_exec core/emulator

    msg "Updating a packages database ... "
    chroot_exec /bin/rpm -iv --excludepath / --force --nosignature --nodeps --justdb /tmp/*.rpm >/dev/null
    is_ok "fail" "done" || return 1

    msg -n "Updating repository ... "
    zypper_repository
    is_ok "fail" "done"

    msg -n "Clearing cache ... "
    rm -rf "${CHROOT_DIR}"/tmp/*
    is_ok "skip" "done"

    return 0
}

do_help()
{
cat <<EOF
   --arch="${ARCH}"
     Architecture of Linux distribution, supported "armv6hl", "armv7hl", "aarch64", "i586" and "x86_64".

   --suite="${SUITE}"
     Version of Linux distribution, supported versions "12.3" and "13.2".

   --source-path="${SOURCE_PATH}"
     Installation source, can specify address of the repository or path to the rootfs archive.

EOF
}
