#!/bin/sh
# Linux Deploy Component
# (c) Anton Skshidlevsky <meefik@gmail.com>, GPLv3

do_configure()
{
    is_archive "${SOURCE_PATH}" && return 0
    msg ":: Configuring ${COMPONENT} ... "
    case "${DISTRIB}" in
    debian|kalilinux)
        if [ -e "${CHROOT_DIR}/etc/apt/sources.list" ]; then
            cp "${CHROOT_DIR}/etc/apt/sources.list" "${CHROOT_DIR}/etc/apt/sources.list.bak"
        fi
        if [ $(grep -c "${SOURCE_PATH}.*${SUITE}" "${CHROOT_DIR}/etc/apt/sources.list") -eq 0 ]; then
            echo "deb ${SOURCE_PATH} ${SUITE} main contrib non-free" > "${CHROOT_DIR}/etc/apt/sources.list"
            echo "deb-src ${SOURCE_PATH} ${SUITE} main contrib non-free" >> "${CHROOT_DIR}/etc/apt/sources.list"
        fi
    ;;
    ubuntu)
        if [ -e "${CHROOT_DIR}/etc/apt/sources.list" ]; then
            cp "${CHROOT_DIR}/etc/apt/sources.list" "${CHROOT_DIR}/etc/apt/sources.list.bak"
        fi
        if [ $(grep -c "${SOURCE_PATH}.*${SUITE}" "${CHROOT_DIR}/etc/apt/sources.list") -eq 0 ]; then
            echo "deb ${SOURCE_PATH} ${SUITE} main universe multiverse" > "${CHROOT_DIR}/etc/apt/sources.list"
            echo "deb-src ${SOURCE_PATH} ${SUITE} main universe multiverse" >> "${CHROOT_DIR}/etc/apt/sources.list"
        fi
        # Fix for upstart
        if [ -e "${CHROOT_DIR}/sbin/initctl" ]; then
            chroot_exec dpkg-divert --local --rename --add /sbin/initctl
            ln -s /bin/true "${CHROOT_DIR}/sbin/initctl"
        fi
    ;;
    archlinux)
        if [ "$(get_platform ${ARCH})" = "intel" ]
        then local repo="${SOURCE_PATH%/}/\$repo/os/\$arch"
        else local repo="${SOURCE_PATH%/}/\$arch/\$repo"
        fi
        sed -i "s|^[[:space:]]*Architecture[[:space:]]*=.*$|Architecture = ${ARCH}|" "${CHROOT_DIR}/etc/pacman.conf"
        sed -i "s|^[[:space:]]*\(CheckSpace\)|#\1|" "${CHROOT_DIR}/etc/pacman.conf"
        sed -i "s|^[[:space:]]*SigLevel[[:space:]]*=.*$|SigLevel = Never|" "${CHROOT_DIR}/etc/pacman.conf"
        if [ $(grep -c -e "^[[:space:]]*Server" "${CHROOT_DIR}/etc/pacman.d/mirrorlist") -gt 0 ]
        then sed -i "s|^[[:space:]]*Server[[:space:]]*=.*|Server = ${repo}|" "${CHROOT_DIR}/etc/pacman.d/mirrorlist"
        else echo "Server = ${repo}" >> "${CHROOT_DIR}/etc/pacman.d/mirrorlist"
        fi
    ;;
    fedora)
        find "${CHROOT_DIR}/etc/yum.repos.d/" -name '*.repo' | while read f; do sed -i 's/^enabled=.*/enabled=0/g' "${f}"; done
        if [ "$(get_platform ${ARCH})" = "intel" -o "${ARCH}" != "aarch64" -a "${SUITE}" -ge 20 ]
        then local repo="${SOURCE_PATH%/}/fedora/linux/releases/${SUITE}/Everything/${ARCH}/os"
        else local repo="${SOURCE_PATH%/}/fedora-secondary/releases/${SUITE}/Everything/${ARCH}/os"
        fi
        local repo_file="${CHROOT_DIR}/etc/yum.repos.d/fedora-${SUITE}-${ARCH}.repo"
        echo "[fedora-${SUITE}-${ARCH}]" > "${repo_file}"
        echo "name=Fedora ${SUITE} - ${ARCH}" >> "${repo_file}"
        echo "failovermethod=priority" >> "${repo_file}"
        echo "baseurl=${repo}" >> "${repo_file}"
        echo "enabled=1" >> "${repo_file}"
        echo "metadata_expire=7d" >> "${repo_file}"
        echo "gpgcheck=0" >> "${repo_file}"
        chmod 644 "${repo_file}"
        # Fix for yum
        if [ -e "${CHROOT_DIR}/usr/bin/yum-deprecated" ]; then
            rm -f "${CHROOT_DIR}/usr/bin/yum"
            echo '#!/bin/sh' > "${CHROOT_DIR}/usr/bin/yum"
            echo '/usr/bin/yum-deprecated $*' >> "${CHROOT_DIR}/usr/bin/yum"
            chmod 755 "${CHROOT_DIR}/usr/bin/yum"
        fi
    ;;
    centos)
        find "${CHROOT_DIR}/etc/yum.repos.d/" -name '*.repo' | while read f; do sed -i 's/^enabled=.*/enabled=0/g' "${f}"; done
        local repo="${SOURCE_PATH%/}/${SUITE}/os/${ARCH}"
        local repo_file="${CHROOT_DIR}/etc/yum.repos.d/centos-${SUITE}-${ARCH}.repo"
        echo "[centos-${SUITE}-${ARCH}]" > "${repo_file}"
        echo "name=CentOS ${SUITE} - ${ARCH}" >> "${repo_file}"
        echo "failovermethod=priority" >> "${repo_file}"
        echo "baseurl=${repo}" >> "${repo_file}"
        echo "enabled=1" >> "${repo_file}"
        echo "metadata_expire=7d" >> "${repo_file}"
        echo "gpgcheck=0" >> "${repo_file}"
        chmod 644 "${repo_file}"
    ;;
    opensuse)
        if [ "$(get_platform ${ARCH})" = "intel" ]
        then local repo="${SOURCE_PATH%/}/distribution/${SUITE}/repo/oss/"
        else local repo="${SOURCE_PATH%/}/${ARCH}/distribution/${SUITE}/repo/oss/"
        fi
        local repo_name="openSUSE-${SUITE}-${ARCH}-Repo-OSS"
        local repo_file="${CHROOT_DIR}/etc/zypp/repos.d/${repo_name}.repo"
        echo "[${repo_name}]" > "${repo_file}"
        echo "name=${repo_name}" >> "${repo_file}"
        echo "enabled=1" >> "${repo_file}"
        echo "autorefresh=0" >> "${repo_file}"
        echo "baseurl=${repo}" >> "${repo_file}"
        echo "type=NONE" >> "${repo_file}"
        chmod 644 "${repo_file}"
    ;;
    gentoo)
        if [ $(grep -c '^aid_inet:.*,portage' "${CHROOT_DIR}/etc/group") -eq 0 ]; then
            sed -i "s|^\(aid_inet:.*\)|\1,portage|g" "${CHROOT_DIR}/etc/group"
        fi
        # set MAKEOPTS
        local ncpu=$(grep -c ^processor /proc/cpuinfo)
        let ncpu=${ncpu}+1
        if [ $(grep -c '^MAKEOPTS=' "${CHROOT_DIR}/etc/portage/make.conf") -eq 0 ]; then
            echo "MAKEOPTS=\"-j${ncpu}\"" >> "${CHROOT_DIR}/etc/portage/make.conf"
        fi
    ;;
    slackware)
        if [ -e "${CHROOT_DIR}/etc/slackpkg/mirrors" ]; then
            cp "${CHROOT_DIR}/etc/slackpkg/mirrors" "${CHROOT_DIR}/etc/slackpkg/mirrors.bak"
        fi
        echo "${SOURCE_PATH}" > "${CHROOT_DIR}/etc/slackpkg/mirrors"
        chmod 644 "${CHROOT_DIR}/etc/slackpkg/mirrors"
        sed -i 's|^WGETFLAGS=.*|WGETFLAGS="--passive-ftp -q"|g' "${CHROOT_DIR}/etc/slackpkg/slackpkg.conf"
    ;;
    esac
    return 0
}
