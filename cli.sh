#!/bin/bash
################################################################################
#
# Linux Deploy CLI
# (C) 2012-2015 Anton Skshidlevsky <meefik@gmail.com>, GPLv3
#
################################################################################

VERSION="2.0.0"

################################################################################
# Common
################################################################################

msg()
{
    echo "$@"
}

is_ok()
{
    if [ $? -eq 0 ]; then
        if [ -n "$2" ]; then
            msg "$2"
        fi
        return 0
    else
        if [ -n "$1" ]; then
            msg "$1"
        fi
        return 1
    fi
}

get_platform()
{
    local arch="$1"
    if [ -z "${arch}" ]; then
        arch=$(uname -m)
    fi
    case "${arch}" in
    arm*|aarch64)
        echo "arm"
    ;;
    i[3-6]86|x86*|amd64)
        echo "intel"
    ;;
    *)
        echo "unknown"
    ;;
    esac
}

get_uuid()
{
    cat /proc/sys/kernel/random/uuid
}

is_fakeroot()
{
    [ "${FAKEROOT}" = "1" ]
}

multiarch_support()
{
    if [ -d "/proc/sys/fs/binfmt_misc" ]; then
        return 0
    else
        return 1
    fi
}

selinux_support()
{
    if [ -d "/sys/fs/selinux" ]; then
        return 0
    else
        return 1
    fi
}

loop_support()
{
    if [ -n "$(losetup -f)" ]; then
        return 0
    else
        return 1
    fi
}

user_home()
{
    [ -e "${CHROOT_DIR}/etc/passwd" ] || return 1
    local user_name="$1"
    [ -n "${user_name}" ] || return 1
    echo $(grep -m1 "^${user_name}:" "${CHROOT_DIR}/etc/passwd" | awk -F: '{print $6}')
}

user_shell()
{
    [ -e "${CHROOT_DIR}/etc/passwd" ] || return 1
    local user_name="$1"
    [ -n "${user_name}" ] || return 1
    echo $(grep -m1 "^${user_name}:" "${CHROOT_DIR}/etc/passwd" | awk -F: '{print $7}')
}

is_mounted()
{
    local mount_point="$1"
    [ -n "${mount_point}" ] || return 1
    if $(grep -q " ${mount_point} " /proc/mounts); then
        return 0
    else
        return 1
    fi
}

container_mounted()
{
    if [ "${FAKEROOT}" = "1" ]; then
        return 0
    else
        is_mounted "${CHROOT_DIR}"
    fi
}

is_archive()
{
    local src="$1"
    [ -n "${src}" ] || return 1
    if [ -z "${src##*gz}" -o -z "${src##*bz2}" -o -z "${src##*xz}" ]; then
        return 0
    fi
    return 1
}

get_pids()
{
    local item pidfile pid pids
    for item in $*
    do
        pid=""
        pidfile="${CHROOT_DIR}${item}"
        [ -e "${pidfile}" ] && pid=$(cat "${pidfile}")
        if [ -n "${pid}" -a -e "/proc/${pid}" ]; then
            pids="${pids} ${pid}"
        fi
    done
    if [ -n "${pids}" ]; then
        echo ${pids}
        return 0
    else
        return 1
    fi    
}

is_started()
{
    get_pids $* >/dev/null
}

kill_pids()
{
    local pids=$(get_pids $*)
    if [ -n "${pids}" ]; then
        kill -9 ${pids}
        return $?
    fi
    return 0
}

remove_files()
{
    local item target
    for item in $*
    do
        target="${CHROOT_DIR}${item}"
        if [ -e "${target}" ]; then
            rm -f "${target}"
        fi
    done
    return 0
}

make_dirs()
{
    local item target
    for item in $*
    do
        target="${CHROOT_DIR}${item}"
        if [ -d "${target%/*}" -a ! -d "${target}" ]; then
            mkdir "${target}"
        fi
    done
    return 0
}

chroot_exec()
{
    unset TMP TEMP TMPDIR LD_PRELOAD LD_DEBUG
    local path="${PATH}:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
    if [ "$1" = "-u" ]; then
        local username="$2"
        shift 2
    fi
    if [ "${FAKEROOT}" = "1" ]; then
        if [ -z "${PROOT_TMP_DIR}" ]; then
            export PROOT_TMP_DIR="${TEMP_DIR}"
        fi
        local mounts="-b /proc -b /dev -b /sys"
        if [ -n "${MOUNTS}" ]; then
            mounts="${mounts} -b ${MOUNTS// / -b }"
        fi
        local emulator
        if [ -n "${EMULATOR}" ]; then
            emulator="-q ${EMULATOR}"
        fi
        if [ -n "${username}" ]; then
            if [ $# -gt 0 ]; then
                proot -r "${CHROOT_DIR}" -w / ${mounts} ${emulator} -0 /bin/su - ${username} -c "$*"
            else
                proot -r "${CHROOT_DIR}" -w / ${mounts} ${emulator} -0 /bin/su - ${username}
            fi
        else
            PATH="${path}" proot -r "${CHROOT_DIR}" -w / ${mounts} ${emulator} -0 $*
        fi
    else
        if [ -n "${username}" ]; then
            if [ $# -gt 0 ]; then
                chroot "${CHROOT_DIR}" /bin/su - ${username} -c "$*"
            else
                chroot "${CHROOT_DIR}" /bin/su - ${username}
            fi
        else
            PATH="${path}" chroot "${CHROOT_DIR}" $*
        fi
    fi
}

################################################################################
# Params
################################################################################

params_read()
{
    local conf_file="$1"
    [ -e "${conf_file}" ] || return 1
    local item key val
    while read item
    do
        key=$(echo ${item} | grep -o '^[0-9A-Z_]\{1,32\}')
        val=${item#${key}=}
        if [ -n "${key}" ]; then
            eval ${key}="${val}"
            if [ -n "${OPTLST##* ${key} *}" ]; then
                OPTLST="${OPTLST}${key} "
            fi
        fi
    done < ${conf_file}
}

params_write()
{
    local conf_file="$1"
    [ -n "${conf_file}" ] || return 1
    echo "# ${conf_file##*/} $(date '+%F %R')" > ${conf_file}
    local key val
    for key in ${OPTLST}
    do
        eval "val=\$${key}"
        if [ -n "${key}" -a -n "${val}" ]; then
            echo "${key}=\"${val}\"" >> ${conf_file}
        fi
    done
}

params_parse()
{
    OPTIND=1
    if [ $# -gt 0 ] ; then
        local item key val
        for item in "$@"
        do
            key=$(expr "${item}" : '--\([0-9a-z-]\{1,32\}=\{0,1\}\)' | tr '\-abcdefghijklmnopqrstuvwxyz' '\_ABCDEFGHIJKLMNOPQRSTUVWXYZ')
            if [ -n "${key##*=*}" ]; then
                val="1"
            else
                key=${key%*=}
                val=$(expr "${item}" : '--[0-9a-z-]\{1,32\}=\(.*\)')
            fi
            if [ -n "${key}" ]; then
                eval ${key}=\"${val}\"
                let OPTIND=OPTIND+1
                if [ -n "${OPTLST##* ${key} *}" ]; then
                    OPTLST="${OPTLST}${key} "
                fi
            fi
        done
    fi
    #echo ${OPTLST} | tr ' ' '\n' | awk '!x[$0]++'
}

params_check()
{
    local params_list="$@"
    local key val params_lost
    for key in ${params_list}
    do
        eval "val=\$${key}"
        if [ -z "${val}" ]; then
            params_lost="${params_lost} ${key}"
        fi
    done
    if [ -n "${params_lost}" ]; then
        msg "Missing parameters:${params_lost}"
        return 1
    fi
    return 0
}


################################################################################
# Configs
################################################################################

config_path()
{
    local conf_file="$1"
    if [ -n "${conf_file}" ]; then
        if [ -n "${conf_file##*/*}" ]; then
            conf_file="${CONFIG_DIR}/${conf_file}.conf"
        fi
        echo "${conf_file}"
    fi
}

config_update()
{
    local source_conf="$1"; shift
    local target_conf="$1"; shift
    params_read "${source_conf}"
    params_parse "$@"
    if [ -z "${CONTAINER_ID}" ]; then
        CONTAINER_ID="$(get_uuid)"
    fi
    CHROOT_DIR="${CHROOT_DIR%/}"
    params_write "${target_conf}"
}

config_list()
{
    local conf_file="$1"
    local conf DISTRIB ARCH SUITE INCLUDE
    for conf in $(ls "${CONFIG_DIR}/")
    do
        local current='-'
        [ "${conf_file##*/}" = "${conf}" ] && current='*'
        unset DISTRIB ARCH SUITE INCLUDE
        eval $(grep '^DISTRIB=' "${CONFIG_DIR}/${conf}")
        eval $(grep '^ARCH=' "${CONFIG_DIR}/${conf}")
        eval $(grep '^SUITE=' "${CONFIG_DIR}/${conf}")
        eval $(grep '^INCLUDE=' "${CONFIG_DIR}/${conf}")
        local formated_desc=$(printf "%-1s %-15s %-10s %-10s %-10s %.28s\n" "${current}" "${conf%.conf}" "${DISTRIB}" "${ARCH}" "${SUITE}" "${INCLUDE}")
        msg "${formated_desc}"
    done
}

config_remove()
{
    local conf_file="$1"
    if [ -e "${conf_file}" ]; then
        rm -f "${conf_file}"
    else
        return 1
    fi
}

################################################################################
# Components
################################################################################

target_check()
{
    local target="$@"
    [ -n "${target}" ] || return 0
    local item
    for item in ${target}
    do
        case "${DISTRIB}:${ARCH}:${SUITE}" in
        ${item})
            return 0
        ;;
        esac
    done
    return 1
}

is_exclude()
{
    local component="$1"
    [ -n "${component}" ] || return 1
    for item in ${EXCLUDE_COMPONENTS}
    do
        case "${component}" in
        ${item}*)
            return 0
        ;;
        esac
    done
    return 1
}

component_depends()
{
    local components="$@"
    [ -n "${components}" ] || return 0
    local component conf_file TARGET DEPENDS
    for component in ${components}
    do
        component="${component%/}"
        # check deadlocks
        [ -z "${IGNORE_DEPENDS##* ${component} *}" ] && continue
        IGNORE_DEPENDS="${IGNORE_DEPENDS}${component} "
        # check component exist
        conf_file="${REPO_DIR}/${component}/deploy.conf"
        [ -e "${conf_file}" ] || continue
        # read component variables
        eval $(grep '^TARGET=' "${conf_file}")
        eval $(grep '^DEPENDS=' "${conf_file}")
        # check compatibility
        if [ "${WITHOUT_CHECK}" != "1" ]; then
            target_check ${TARGET} || continue
        fi
        if [ "${REVERSE_DEPENDS}" = "1" ]; then
            # output
            echo ${component}
            # process depends
            component_depends ${DEPENDS}
        else
            # process depends
            component_depends ${DEPENDS}
            # output
            echo ${component}
        fi
    done
}

component_exec()
{
    local components="$@"
    if [ "${WITHOUT_DEPENDS}" != "1" ]; then
        components=$(IGNORE_DEPENDS=" " component_depends ${components})
    fi
    [ -n "${components}" ] || return 1
    (set -e
        for COMPONENT in ${components}
        do
            COMPONENT_DIR="${REPO_DIR}/${COMPONENT}"
            [ -d "${COMPONENT_DIR}" ] || continue
            unset NAME DESC TARGET PARAMS DEPENDS EXTENDS
            TARGET='*:*:*'
            # read config
            . "${COMPONENT_DIR}/deploy.conf"
            # default functions
            do_install() { return 0; }
            do_configure() { return 0; }
            do_start() { return 0; }
            do_stop() { return 0; }
            do_help() { return 0; }
            # load extends
            for component in ${EXTENDS} ${COMPONENT}
            do
                if [ -e "${REPO_DIR}/${component}/deploy.sh" ]; then
                    . "${REPO_DIR}/${component}/deploy.sh"
                fi
            done
            # exclude components
            is_exclude ${COMPONENT} && continue
            # check parameters
            [ "${WITHOUT_CHECK}" != "1" ] && params_check ${PARAMS}
            # exec action
            [ "${DEBUG_MODE}" = "1" ] && msg "## ${COMPONENT} : ${DO_ACTION}"
            set +e
            eval ${DO_ACTION} || exit 1
            set -e
        done
    exit 0)
    is_ok || return 1
}

component_list()
{
    local components="$@"
    local component output DESC
    if [ -z "${components}" ]; then
        components=$(find "${REPO_DIR}/" -type f -name "deploy.conf" | while read f
            do 
                component="${f%/*}"
                component="${component//${REPO_DIR}\//}"
                echo "${component}"
            done)
    fi
    components=$(IGNORE_DEPENDS=" " component_depends ${components} | sort)
    for component in $components
    do
        # output
        DESC=''
        eval $(grep '^DESC=' "${REPO_DIR}/${component}/deploy.conf")
        output=$(printf "%-30s %.49s\n" "${component}" "${DESC}")
        msg "${output}"
    done
}

################################################################################
# Containers
################################################################################

mount_part()
{
    case "$1" in
    root)
        msg -n "/ ... "
        if ! is_mounted "${CHROOT_DIR}" ; then
            [ -d "${CHROOT_DIR}" ] || mkdir -p "${CHROOT_DIR}"
            local mnt_opts
            [ -d "${TARGET_PATH}" ] && mnt_opts="bind" || mnt_opts="rw,relatime"
            mount -o ${mnt_opts} "${TARGET_PATH}" "${CHROOT_DIR}"
            is_ok "fail" "done" || return 1
        else
            msg "skip"
        fi
    ;;
    proc)
        msg -n "/proc ... "
        local target="${CHROOT_DIR}/proc"
        if ! is_mounted "${target}" ; then
            [ -d "${target}" ] || mkdir -p "${target}"
            mount -t proc proc "${target}"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    sys)
        msg -n "/sys ... "
        local target="${CHROOT_DIR}/sys"
        if ! is_mounted "${target}" ; then
            [ -d "${target}" ] || mkdir -p "${target}"
            mount -t sysfs sys "${target}"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    selinux)
        selinux_support || return 0
        msg -n "/sys/fs/selinux ... "
        local target="${CHROOT_DIR}/sys/fs/selinux"
        if ! is_mounted "${target}" ; then
            if [ -e "/sys/fs/selinux/enforce" ]; then
                cat /sys/fs/selinux/enforce > "${TEMP_DIR}/selinux_state"
                echo 0 > /sys/fs/selinux/enforce
            fi
            mount -t selinuxfs selinuxfs "${target}" &&
            mount -o remount,ro,bind "${target}"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    dev)
        msg -n "/dev ... "
        local target="${CHROOT_DIR}/dev"
        if ! is_mounted "${target}" ; then
            [ -d "${target}" ] || mkdir -p "${target}"
            [ -e "/dev/fd" ] || ln -s /proc/self/fd /dev/
            [ -e "/dev/stdin" ] || ln -s /proc/self/fd/0 /dev/stdin
            [ -e "/dev/stdout" ] || ln -s /proc/self/fd/1 /dev/stdout
            [ -e "/dev/stderr" ] || ln -s /proc/self/fd/2 /dev/stderr
            mount -o bind /dev "${target}"
            mount -o ro,remount "${target}"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    tty)
        msg -n "/dev/tty ... "
        if [ ! -e "/dev/tty0" ]; then
            ln -s /dev/null /dev/tty0
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    pts)
        msg -n "/dev/pts ... "
        local target="${CHROOT_DIR}/dev/pts"
        if ! is_mounted "${target}" ; then
            [ -d "${target}" ] || mkdir -p "${target}"
            mount -o "mode=0620,gid=5" -t devpts devpts "${target}"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    shm)
        msg -n "/dev/shm ... "
        local target="${CHROOT_DIR}/dev/shm"
        if ! is_mounted "${target}" ; then
            [ -d "${target}" ] || mkdir -p "${target}"
            mount -t tmpfs tmpfs "${target}"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    binfmt_misc)
        multiarch_support || return 0
        local binfmt_dir="/proc/sys/fs/binfmt_misc"
        msg -n "${binfmt_dir} ... "
        if [ ! -e "${binfmt_dir}/register" ]; then
            mount -t binfmt_misc binfmt_misc "${binfmt_dir}"
            is_ok "fail" "done"
        else
            msg "skip"
        fi
    ;;
    esac

    return 0
}

container_mount()
{
    if [ $# -eq 0 ]; then
        container_mount root proc sys selinux dev tty pts shm binfmt_misc
        return $?
    fi

    params_check CHROOT_DIR TARGET_PATH || return 1

    [ "${FAKEROOT}" != "1" ] || return 0

    msg "Mounting partitions: "
    local item
    for item in $*
    do
        mount_part ${item} || return 1
    done

    return 0
}

container_umount()
{
    params_check CHROOT_DIR TARGET_PATH || return 1
    container_mounted || { msg "The container is not mounted." ; return 0; }

    msg -n "Release resources ... "
    local is_release=0
    local lsof_full=$(lsof | awk '{print $1}' | grep -c '^lsof')
    local i
    for i in 1 2 3
    do
        if [ "${lsof_full}" -eq 0 ]; then
            local pids=$(lsof | grep "${CHROOT_DIR}" | awk '{print $1}' | uniq)
        else
            local pids=$(lsof | grep "${CHROOT_DIR}" | awk '{print $2}' | uniq)
        fi
        if [ -n "${pids}" ]; then
            kill -9 ${pids}
            sleep 1
        else
            is_release=1
            break
        fi
    done
    [ "${is_release}" -eq 1 ]; is_ok "fail" "done"

    msg "Unmounting partitions: "
    local is_mnt=0
    local mask
    for mask in '.*' '*'
    do
        local parts=$(cat /proc/mounts | awk '{print $2}' | grep "^${CHROOT_DIR}/${mask}$" | sort -r)
        local part
        for part in ${parts}
        do
            local part_name=$(echo ${part} | sed "s|^${CHROOT_DIR}/*|/|g")
            msg -n "${part_name} ... "
            if [ -z "${part_name##*selinux*}" -a -e "/sys/fs/selinux/enforce" -a -e "${TEMP_DIR}/selinux_state" ]; then
                cat "${TEMP_DIR}/selinux_state" > /sys/fs/selinux/enforce
            fi
            umount ${part}
            is_ok "fail" "done"
            is_mnt=1
        done
    done
    [ "${is_mnt}" -eq 1 ]; is_ok " ...nothing mounted"

    msg -n "Disassociating loop device ... "
    local loop=$(losetup -a | grep "${TARGET_PATH}" | awk -F: '{print $1}')
    if [ -n "${loop}" ]; then
        losetup -d "${loop}"
    fi
    is_ok "fail" "done"

    if [ -d "${CHROOT_DIR}" ]; then
        rmdir "${CHROOT_DIR}"
    fi

    return 0
}

container_start()
{
    params_check CHROOT_DIR || return 1
    container_mounted || container_mount || return 1

    DO_ACTION='do_start'
    component_exec ${INCLUDE}
}

container_stop()
{
    params_check CHROOT_DIR || return 1
    container_mounted || { msg "The container is already stopped." ; return 1; }

    DO_ACTION='do_stop'
    component_exec ${INCLUDE}
}

container_shell()
{
    params_check CHROOT_DIR || return 1
    container_mounted || container_mount || return 1

    DO_ACTION='do_start'
    component_exec core

    TERM="linux"
    USER="root"
    SHELL="$(user_shell $USER)"
    HOME="$(user_home $USER)"
    PS1="\u@\h:\w\\$ "
    export TERM USER SHELL HOME PS1

    if [ -e "${CHROOT_DIR}/etc/motd" ]; then
        msg $(cat "${CHROOT_DIR}/etc/motd")
    fi

    chroot_exec "$@" 2>&1

    return $?
}

container_export()
{
    local rootfs_file="$1"
    [ -n "${rootfs_file}" ] || return 1

    params_check CHROOT_DIR || return 1
    container_mounted || container_mount root || return 1

    case "${rootfs_file}" in
    *gz)
        msg -n "Exporting rootfs as tar.gz archive ... "
        tar cpzvf "${rootfs_file}" --one-file-system -C "${CHROOT_DIR}" .
        is_ok "fail" "done" || return 1
    ;;
    *bz2)
        msg -n "Exporting rootfs as tar.bz2 archive ... "
        tar cpjvf "${rootfs_file}" --one-file-system -C "${CHROOT_DIR}" .
        is_ok "fail" "done" || return 1
    ;;
    *)
        msg "Incorrect filename, supported only gz or bz2 archives."
        return 1
    ;;
    esac
}

container_status()
{
    msg -n "Linux Deploy: "
    msg "${VERSION}"

    local model=$(getprop ro.product.model)
    if [ -n "${model}" ]; then
        msg -n "Device: "
        msg "${model}"
    fi

    local android=$(getprop ro.build.version.release)
    if [ -n "${android}" ]; then
        msg -n "Android: "
        msg "${android}"
    fi

    msg -n "Architecture: "
    msg "$(uname -m)"

    msg -n "Kernel: "
    msg "$(uname -r)"

    msg -n "Memory: "
    local mem_total=$(grep ^MemTotal /proc/meminfo | awk '{print $2}')
    let mem_total=${mem_total}/1024
    local mem_free=$(grep ^MemFree /proc/meminfo | awk '{print $2}')
    let mem_free=${mem_free}/1024
    msg "${mem_free}/${mem_total} MB"

    msg -n "Swap: "
    local swap_total=$(grep ^SwapTotal /proc/meminfo | awk '{print $2}')
    let swap_total=${swap_total}/1024
    local swap_free=$(grep ^SwapFree /proc/meminfo | awk '{print $2}')
    let swap_free=${swap_free}/1024
    msg "${swap_free}/${swap_total} MB"

    msg -n "SELinux: "
    selinux_support && msg "yes" || msg "no"

    msg -n "Loop devices: "
    loop_support && msg "yes" || msg "no"

    msg -n "Support binfmt_misc: "
    multiarch_support && msg "yes" || msg "no"

    msg -n "Supported FS: "
    local supported_fs=$(printf '%s ' $(grep -v nodev /proc/filesystems | sort))
    msg "${supported_fs}"

    msg -n "Container: "
    msg "${CONTAINER}"

    msg -n "Installed system: "
    local linux_version=$([ -r "${CHROOT_DIR}/etc/os-release" ] && . "${CHROOT_DIR}/etc/os-release"; [ -n "${PRETTY_NAME}" ] && echo "${PRETTY_NAME}" || echo "unknown")
    msg "${linux_version}"

    msg "Running services: "
    if [ -n "${STARTUP}" ]; then
        local WITHOUT_DEPENDS=1
        local DO_ACTION='is_started'
        local component
        for component in ${STARTUP}
        do
            msg -n "* ${component}: "
            component_exec "${components}" && msg "yes" || msg "no"
        done
    else
        msg " ...no active services"
    fi

    msg "Mounted parts on Linux: "
    local is_mnt=0
    local item
    for item in $(grep "${CHROOT_DIR}" /proc/mounts | awk '{print $2}' | sed "s|${CHROOT_DIR}/*|/|g")
    do
        msg "* ${item}"
        local is_mnt=1
    done
    [ "${is_mnt}" -ne 1 ] && msg " ...nothing mounted"

    msg "Available mount points: "
    local is_mountpoints=0
    local mp
    for mp in $(grep -v "${CHROOT_DIR}" /proc/mounts | grep ^/ | awk '{print $2":"$3}')
    do
        local part=$(echo ${mp} | awk -F: '{print $1}')
        local fstype=$(echo ${mp} | awk -F: '{print $2}')
        local block_size=$(stat -c '%s' -f ${part})
        local available=$(stat -c '%a' -f ${part} | awk '{printf("%.1f",$1*'${block_size}'/1024/1024/1024)}')
        local total=$(stat -c '%b' -f ${part} | awk '{printf("%.1f",$1*'${block_size}'/1024/1024/1024)}')
        if [ -n "${available}" -a -n "${total}" ]; then
            msg "* ${part}: ${available}/${total} GB (${fstype})"
            is_mountpoints=1
        fi
    done
    [ "${is_mountpoints}" -ne 1 ] && msg " ...no mount points"

    msg "Available partitions: "
    local is_partitions=0
    local dev
    for dev in /sys/block/*/dev
    do
        if [ -f ${dev} ]; then
            local devname=$(echo ${dev} | sed -e 's@/dev@@' -e 's@.*/@@')
            [ -e "/dev/${devname}" ] && local devpath="/dev/${devname}"
            [ -e "/dev/block/${devname}" ] && local devpath="/dev/block/${devname}"
            [ -n "${devpath}" ] && local parts=$(fdisk -l ${devpath} | grep ^/dev/ | awk '{print $1}')
            local part
            for part in ${parts}
            do
                local size=$(fdisk -l ${part} | grep 'Disk.*bytes' | awk '{ sub(/,/,""); print $3" "$4}')
                local type=$(fdisk -l ${devpath} | grep ^${part} | tr -d '*' | awk '{str=$6; for (i=7;i<=10;i++) if ($i!="") str=str" "$i; printf("%s",str)}')
                msg "* ${part}: ${size} (${type})"
                local is_partitions=1
            done
        fi
    done
    [ "${is_partitions}" -ne 1 ] && msg " ...no available partitions"
}

helper()
{
cat <<EOF
Linux Deploy ${VERSION}
(c) 2012-2015 Anton Skshidlevsky, GPLv3

USAGE:
   linuxdeploy [OPTIONS] COMMAND ...

OPTIONS:
   -f FILE - configuration file
   -d - enable debug mode
   -t - enable trace mode

COMMANDS:
   config [...] [PARAMETERS] [NAME ...] - управление конфигурациями
      - без параметров выводит список конфигураций
      -r - удалить текущую конфигурацию
      -i FILE - импортировать конфигурацию
      -x - дамп текущей конфигурации
      -l - список подключенных компонентов
      -a - список всех компонентов
   deploy [-i|-c] [-n NAME] [NAME ...] - установка дистрибутива и подключенных компонентов
      -i - только установить, без конфигурирования
      -с - только конфигурировать, без установки
      -n NAME - пропустить установку указанного компонента
   export <FILE> - экспортировать контейнер как rootfs-архив (tgz или tbz2)
   shell [-u USER] [APP] - смонтировать контейнер, если не смонтирован, и выполнить указанную команду внутри контейнера, по умолчанию /bin/bash
      -u USER - переключиться на указанного пользователя
   mount - смонтировать контейнер
   umount - размонтировать контейнер
   start - смонтировать контейнер, если не смонтирован, и запустить все подключенные компоненты
   stop [-u] - остановить все подключенные компоненты
      -u - размонтировать контейнер
   status - отобразить состояние контейнера и компонетнов
   help [NAME ...] - вызвать справку

EOF
}

do_info()
{
cat <<EOF
Name: ${COMPONENT}
Description: ${DESC}
Target: ${TARGET}
Depends: ${DEPENDS}
Help:

EOF
}

################################################################################

# parse options
OPTIND=1
while getopts :f:dt FLAG
do
    case "${FLAG}" in
    f)
        CONF_FILE="${OPTARG}"
    ;;
    d)
        DEBUG_MODE="1"
    ;;
    t)
        TRACE_MODE="1"
    ;;
    *)
        let OPTIND=OPTIND+1
        break
    ;;
    esac
done
shift $((OPTIND-1))

# log level
exec 3>&1
if [ "${DEBUG_MODE}" != "1" -a "${TRACE_MODE}" != "1" ]; then
    exec 2>/dev/null
fi
if [ "${TRACE_MODE}" = "1" ]; then
    set -x
fi

# init env
umask 0022
unset LANG
if [ -z "${ENV_DIR}" ]; then
    ENV_DIR=$(readlink "$0")
    ENV_DIR="${ENV_DIR%/*}"
    if [ -z "${ENV_DIR}" ]; then
        ENV_DIR=$(cd "${0%/*}"; pwd)
    fi
fi
if [ -z "${CONFIG_DIR}" ]; then
    CONFIG_DIR="${ENV_DIR}/config"
fi
if [ -z "${REPO_DIR}" ]; then
    REPO_DIR="${ENV_DIR}/repo"
fi
if [ -z "${TEMP_DIR}" ]; then
    TEMP_DIR="${ENV_DIR}/tmp"
    [ -d "${TEMP_DIR}" ] || mkdir "${TEMP_DIR}"
fi
CONF_FILE=$(config_path "${CONF_FILE}")

# read config
OPTLST=" " # space is required
params_read "${CONF_FILE}"

# fix params
WITHOUT_CHECK=0
WITHOUT_DEPENDS=0
REVERSE_DEPENDS=0
EXCLUDE_COMPONENTS=""
CHROOT_DIR="${CHROOT_DIR%/}"
if [ "${FAKEROOT}" = "1" -a -z "${CHROOT_DIR}" ]; then
    CHROOT_DIR="${TARGET_PATH%/}"
fi

# exec command
OPTCMD="$1"; shift
case "${OPTCMD}" in
config|conf)
    if [ $# -eq 0 ]; then
        config_list "${CONF_FILE}"
        exit $?
    fi
    conf_file="${CONF_FILE}"

    # parse options
    OPTIND=1
    while getopts :i:rxla FLAG
    do
        case "${FLAG}" in
        r)
            config_remove "${CONF_FILE}"
            exit $?
        ;;
        x)
            dump_flag=1
        ;;
        i)
            conf_file=$(config_path "${OPTARG}")
        ;;
        l)
            list_flag=1
        ;;
        a)
            WITHOUT_CHECK=1
        ;;
        *)
            break
        ;;
        esac
    done
    shift $((OPTIND-1))

    if [ "${dump_flag}" = "1" ]; then
        [ -e "${CONF_FILE}" ] && cat "${CONF_FILE}"
    elif [ "${list_flag}" = "1" ]; then
        if [ $# -eq 0 ]; then
            component_list "${INCLUDE}"
        else
            component_list "$@"
        fi
    else
        config_update "${conf_file}" "${CONF_FILE}" "$@"
    fi
;;
deploy)
    DO_ACTION='do_install && do_configure'

    # parse options
    OPTIND=1
    while getopts :n:ic FLAG
    do
        case "${FLAG}" in
        i)
            DO_ACTION='do_install'
        ;;
        c)
            DO_ACTION='do_configure'
        ;;
        n)
            EXCLUDE_COMPONENTS="${EXCLUDE_COMPONENTS} ${OPTARG}"
        ;;
        *)
            break
        ;;
        esac
    done
    shift $((OPTIND-1))

    if [ $# -gt 0 ]; then
        component_exec "$@"
    else
        component_exec "${INCLUDE}"
    fi
;;
export)
    container_export "$@"
;;
shell)
    container_shell "$@"
;;
mount)
    container_mount
;;
umount)
    container_umount
;;
start)
    container_start
;;
stop)
    # parse options
    OPTIND=1
    while getopts :u FLAG
    do
        case "${FLAG}" in
        u)
            umount_flag=1
        ;;
        *)
            break
        ;;
        esac
    done
    shift $((OPTIND-1))

    container_stop || exit 1

    if [ "${umount_flag}" = "1" ]; then
        container_umount
    fi
;;
status)
    container_status
;;
help)
    if [ $# -eq 0 ]; then
        helper
        if [ -n "${INCLUDE}" ]; then
            msg "PARAMETERS: "
            WITHOUT_CHECK=1
            REVERSE_DEPENDS=1
            DO_ACTION='do_help'
            component_exec "${INCLUDE}" || 
            msg -e '   Included components do not have parameters.\n'
        fi
    else
        WITHOUT_CHECK=1
        WITHOUT_DEPENDS=1
        DO_ACTION='do_info && do_help'
        component_exec "$@"
    fi
;;

*)
    helper
;;
esac
