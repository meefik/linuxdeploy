################################################################################
#
# Linux Deploy
# (C) 2012-2015 Anton Skshidlevsky <meefik@gmail.com>, GPLv3
#
################################################################################

msg()
{
echo "$@"
}

get_platform()
{
local arch="$1"
[ -z "${arch}" ] && arch=$(uname -m)
case "${arch}" in
arm*|aarch64)
    echo "arm"
;;
i[3-6]86|x86*|amd64)
    echo "intel"
;;
*)
    echo "unknown"
esac
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
is_mounted "${CHROOT_DIR}"
}

chroot_exec()
{
unset TMP TEMP TMPDIR LD_PRELOAD LD_DEBUG
if [ "$1" = "-u" ]; then
    local username="$2"
        shift 2
        if [ $# -gt 0 ]; then
        chroot "${CHROOT_DIR}" /bin/su - ${username} -c "$*"
    else
        if [ -e "${CHROOT_DIR}/etc/motd" ]; then
            msg $(cat "${CHROOT_DIR}/etc/motd")
        fi
        chroot "${CHROOT_DIR}" /bin/su - ${username}
    fi
else
    if [ $# -gt 0 ]; then
        chroot "${CHROOT_DIR}" $*
    else
        if [ -e "${CHROOT_DIR}/etc/motd" ]; then
            msg $(cat "${CHROOT_DIR}/etc/motd")
        fi
        chroot "${CHROOT_DIR}"
    fi
fi
}

ssh_started()
{
local is_started=""
for f in /var/run/sshd.pid /run/sshd.pid
do
    local pidfile="${CHROOT_DIR}${f}"
    local pid=$([ -e "${pidfile}" ] && cat ${pidfile})
    if [ -n "${pid}" ]; then
        is_started=$(ps | awk '{if ($1 == '${pid}') print $1}')
        [ -n "${is_started}" ] && return 0
    fi
done
return 1
}

gui_started()
{
local is_started=""
local pidfile="${CHROOT_DIR}/tmp/xsession.pid"
local pid=$([ -e "${pidfile}" ] && cat ${pidfile})
[ -n "${pid}" ] && is_started=$(ps | awk '{if ($1 == '${pid}') print $1}')
[ -z "${is_started}" ] && return 1 || return 0
}

container_prepare()
{
container_mounted && { msg "The container is already mounted."; return 1; }

msg -n "Checking installation path ... "
case "${TARGET_TYPE}" in
file)
    if [ -e "${TARGET_PATH}" -a ! -f "${TARGET_PATH}" ]; then
        msg "fail"; return 1
    fi
;;
partition)
    if [ ! -b "${TARGET_PATH}" ]; then
        msg "fail"; return 1
    fi
;;
directory|ram)
    if [ -e "${TARGET_PATH}" -a ! -d "${TARGET_PATH}" ]; then
        msg "fail"; return 1
    fi
;;
esac
msg "done"

if [ "${TARGET_TYPE}" = "file" ]; then
        local file_size=0
        if [ -f "${TARGET_PATH}" ]; then
            file_size=$(stat -c %s "${TARGET_PATH}")
        fi
    if [ "${DISK_SIZE}" -eq 0 ]; then
        local dir_name=$(dirname "${TARGET_PATH}")
        local block_size=$(stat -c %s -f "${dir_name}")
        local available_size=$(stat -c %a -f "${dir_name}")
        let available_size="(${block_size}*${available_size})+${file_size}"
        let DISK_SIZE="(${available_size}-${available_size}/10)/1048576"
        [ "${DISK_SIZE}" -gt 4095 ] && DISK_SIZE=4095
        [ "${DISK_SIZE}" -lt 512 ] && DISK_SIZE=512
    fi
        let file_size="${file_size}/1048576"
        if [ "${DISK_SIZE}" != "${file_size}" ]; then
        msg -n "Making new disk image (${DISK_SIZE} MB) ... "
        dd if=/dev/zero of="${TARGET_PATH}" bs=1048576 seek=$(expr ${DISK_SIZE} - 1) count=1 ||
        dd if=/dev/zero of="${TARGET_PATH}" bs=1048576 count=${DISK_SIZE}
        [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
        fi
fi

if [ "${TARGET_TYPE}" = "file" -o "${TARGET_TYPE}" = "partition" ]; then
    local fs_support=""
    for fs in ext4 ext3 ext2
    do
        if [ "$(grep -c ${fs} /proc/filesystems)" -gt 0 ]; then
            fs_support=${fs}
            break
        fi
    done
    [ -z "${fs_support}" ] && { msg "The filesystems ext2, ext3 or ext4 is not supported."; return 1; }
    [ "${FS_TYPE}" = "auto" ] && FS_TYPE=${fs_support}

    msg -n "Making file system (${FS_TYPE}) ... "
    local loop_exist=$(losetup -a | grep -c "${TARGET_PATH}")
    local img_mounted=$(grep -c "${TARGET_PATH}" /proc/mounts)
    [ "${loop_exist}" -ne 0 -o "${img_mounted}" -ne 0 ] && { msg "fail"; return 1; }
    mke2fs -qF -t ${FS_TYPE} -O ^has_journal "${TARGET_PATH}"
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
fi

if [ "${TARGET_TYPE}" = "ram" ]; then
    umount "${TARGET_PATH}"
    if [ "${DISK_SIZE}" -eq 0 ]; then
        local ram_free=$(grep ^MemFree /proc/meminfo | awk '{print $2}')
        let DISK_SIZE="${ram_free}/1024"
    fi
    msg -n "Making new disk image (${DISK_SIZE} MB) ... "
    [ -d "${TARGET_PATH}" ] || mkdir "${TARGET_PATH}"
    mount -t tmpfs -o size=${DISK_SIZE}M tmpfs "${TARGET_PATH}"
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
fi

return 0
}

mount_part()
{
case "$1" in
root)
    msg -n "/ ... "
    if ! is_mounted "${CHROOT_DIR}" ; then
        [ -d "${CHROOT_DIR}" ] || mkdir -p "${CHROOT_DIR}"
        [ -d "${TARGET_PATH}" ] && local mnt_opts="bind" || local mnt_opts="rw,relatime"
        mount -o ${mnt_opts} "${TARGET_PATH}" "${CHROOT_DIR}"
        [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
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
        [ $? -eq 0 ] && msg "done" || msg "fail"
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
        [ $? -eq 0 ] && msg "done" || msg "fail"
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
            cat /sys/fs/selinux/enforce > "${ENV_DIR%/}/etc/selinux_state"
            echo 0 > /sys/fs/selinux/enforce
        fi
        mount -t selinuxfs selinuxfs "${target}" &&
        mount -o remount,ro,bind "${target}"
        [ $? -eq 0 ] && msg "done" || msg "fail"
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
        [ $? -eq 0 ] && msg "done" || msg "fail"
    else
        msg "skip"
    fi
;;
tty)
    msg -n "/dev/tty ... "
    if [ ! -e "/dev/tty0" ]; then
        ln -s /dev/null /dev/tty0
        [ $? -eq 0 ] && msg "done" || msg "fail"
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
        [ $? -eq 0 ] && msg "done" || msg "fail"
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
        [ $? -eq 0 ] && msg "done" || msg "fail"
    else
        msg "skip"
    fi
;;
binfmt_misc)
    multiarch_support || return 0
    local binfmt_dir="/proc/sys/fs/binfmt_misc"
    msg -n "${binfmt_dir} ... "
    [ -e "${binfmt_dir}/register" ] || mount -t binfmt_misc binfmt_misc "${binfmt_dir}"
    case "$(get_platform)" in
    arm)
        if [ ! -e "${binfmt_dir}/qemu-i386" ]; then
            echo ':qemu-i386:M::\x7fELF\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x02\x00\x03\x00:\xff\xff\xff\xff\xff\xfe\xfe\xff\xff\xff\xff\xff\xff\xff\xff\xff\xfe\xff\xff\xff:/usr/local/bin/qemu-i386-static:' > "${binfmt_dir}/register"
            [ $? -eq 0 ] && msg "done" || msg "fail"
        else
            msg "skip"
        fi
    ;;
    intel)
        if [ ! -e "${binfmt_dir}/qemu-arm" ]; then
            echo ':qemu-arm:M::\x7fELF\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x02\x00\x28\x00:\xff\xff\xff\xff\xff\xff\xff\x00\xff\xff\xff\xff\xff\xff\xff\xff\xfe\xff\xff\xff:/usr/local/bin/qemu-arm-static:' > "${binfmt_dir}/register"
            [ $? -eq 0 ] && msg "done" || msg "fail"
        else
            msg "skip"
        fi
    ;;
    *)
        msg "skip"
    ;;
    esac
;;
custom)
    for disk in ${CUSTOM_MOUNTS}
    do
        local disk_name=$(basename /root/${disk})
        msg -n "/mnt/${disk_name} ... "
        local target="${CHROOT_DIR}/mnt/${disk_name}"
        if ! is_mounted "${target}" ; then
            if [ -d "${disk}" ]; then
                [ -d "${target}" ] || mkdir -p "${target}"
                mount -o bind "${disk}" "${target}"
                [ $? -eq 0 ] && msg "done" || msg "fail"
            elif [ -e "${disk}" ]; then
                [ -d "${target}" ] || mkdir -p "${target}"
                mount -o rw,relatime "${disk}" "${target}"
                [ $? -eq 0 ] && msg "done" || msg "fail"
            else
                msg "skip"
            fi
        else
            msg "skip"
        fi
    done
;;
esac

return 0
}

container_mount()
{
if [ $# -eq 0 ]; then
    container_mount root proc sys selinux dev tty pts shm binfmt_misc custom
    [ $? -ne 0 ] && return 1
else
    msg "Mounting partitions: "
    for i in $*
    do
        mount_part $i
        [ $? -ne 0 ] && return 1
    done
fi

return 0
}

container_umount()
{
container_mounted || { msg "The container is not mounted." ; return 0; }

msg -n "Release resources ... "
local is_release=0
local lsof_full=$(lsof | awk '{print $1}' | grep -c '^lsof')
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
[ "${is_release}" -eq 1 ] && msg "done" || msg "fail"

msg "Unmounting partitions: "
local is_mounted=0
for i in '.*' '*'
do
    local parts=$(cat /proc/mounts | awk '{print $2}' | grep "^${CHROOT_DIR}/${i}$" | sort -r)
    for p in ${parts}
    do
        local pp=$(echo ${p} | sed "s|^${CHROOT_DIR}/*|/|g")
        msg -n "${pp} ... "
        local selinux=$(echo ${pp} | grep -ci "selinux")
        if [ "${selinux}" -gt 0 -a -e "/sys/fs/selinux/enforce" -a -e "${ENV_DIR%/}/etc/selinux_state" ]; then
            cat "${ENV_DIR%/}/etc/selinux_state" > /sys/fs/selinux/enforce
        fi
        umount ${p}
        [ $? -eq 0 ] && msg "done" || msg "fail"
        is_mounted=1
    done
done
if multiarch_support ; then
    local binfmt_dir="/proc/sys/fs/binfmt_misc"
    local binfmt_qemu=""
    case "$(get_platform)" in
    arm)
        binfmt_qemu="${binfmt_dir}/qemu-i386"
    ;;
    intel)
        binfmt_qemu="${binfmt_dir}/qemu-arm"
    ;;
    esac
    if [ -e "${binfmt_qemu}" ]; then
        msg -n "${binfmt_dir} ... "
        echo -1 > "${binfmt_qemu}"
        [ $? -eq 0 ] && msg "done" || msg "fail"
        is_mounted=1
    fi
fi
[ "${is_mounted}" -ne 1 ] && msg " ...nothing mounted"

msg -n "Disassociating loop device ... "
local loop=$(losetup -a | grep "${TARGET_PATH}" | awk -F: '{print $1}')
if [ -n "${loop}" ]; then
    losetup -d "${loop}"
fi
[ $? -eq 0 ] && msg "done" || msg "fail"

return 0
}

container_start()
{
if [ $# -eq 0 ]; then
    container_mount
    [ $? -ne 0 ] && return 1

    container_configure dns mtab

    msg "Starting services: "

    for i in ${STARTUP}
    do
        container_start ${i}
        [ $? -eq 0 ] || return 1
    done

    [ -z "${STARTUP}" ] && msg "...no active services"

    return 0
fi

dbus_init()
{
[ -e "${CHROOT_DIR}/run/dbus/pid" ] && rm -f "${CHROOT_DIR}/run/dbus/pid"
[ -e "${CHROOT_DIR}/var/run/dbus/dbus.pid" ] && rm -f "${CHROOT_DIR}/var/run/dbus/dbus.pid"
[ -e "${CHROOT_DIR}/var/run/messagebus.pid" ] && rm -f "${CHROOT_DIR}/var/run/messagebus.pid"
chroot_exec -u root "dbus-daemon --system --fork"
}

fb_refresh()
{
[ "${FB_REFRESH}" = "1" ] || return 0
local fbdev="${FB_DEV##*/}"
local fbrotate="/sys/class/graphics/${fbdev}/rotate"
[ -e "${fbrotate}" ] || return 0
local pid_file="${CHROOT_DIR}/tmp/xsession.pid"
touch "${pid_file}"
chmod 666 "${pid_file}"
while [ -e "${pid_file}" ]
do
    echo 0 > "${fbrotate}"
    sleep 0.01
done
}

case "$1" in
ssh)
    msg -n "SSH [:${SSH_PORT}] ... "
    ssh_started && { msg "skip"; return 0; }
    # prepare var
    [ -e "${CHROOT_DIR}/var/run" -a ! -e "${CHROOT_DIR}/var/run/sshd" ] && mkdir "${CHROOT_DIR}/var/run/sshd"
    [ -e "${CHROOT_DIR}/run" -a ! -e "${CHROOT_DIR}/run/sshd" ] && mkdir "${CHROOT_DIR}/run/sshd"
    # generate keys
    if [ -z "$(ls ${CHROOT_DIR}/etc/ssh/ | grep key)" ]; then
        chroot_exec -u root "ssh-keygen -A"
        echo
    fi
    # exec sshd
    local sshd='`which sshd`'
    chroot_exec -u root "${sshd} -p ${SSH_PORT}"
    [ $? -eq 0 ] && msg "done" || msg "fail"
;;
vnc)
    local vncport=5900
    let vncport=${vncport}+${VNC_DISPLAY}
    msg -n "VNC [:${vncport}] ... "
    gui_started && { msg "skip"; return 0; }
    dbus_init
    # remove locks
    [ -e "${CHROOT_DIR}/tmp/.X${VNC_DISPLAY}-lock" ] && rm -f "${CHROOT_DIR}/tmp/.X${VNC_DISPLAY}-lock"
    [ -e "${CHROOT_DIR}/tmp/.X11-unix/X${VNC_DISPLAY}" ] && rm -f "${CHROOT_DIR}/tmp/.X11-unix/X${VNC_DISPLAY}"
    # exec vncserver
    chroot_exec -u ${USER_NAME} "vncserver :${VNC_DISPLAY} -depth ${VNC_DEPTH} -geometry ${VNC_GEOMETRY} -dpi ${VNC_DPI} ${VNC_ARGS}"
    [ $? -eq 0 ] && msg "done" || msg "fail"
;;
xserver)
    msg -n "X Server [${XSERVER_HOST}:${XSERVER_DISPLAY}] ... "
    gui_started && { msg "skip"; return 0; }
    dbus_init
    chroot_exec -u ${USER_NAME} "export DISPLAY=${XSERVER_HOST}:${XSERVER_DISPLAY}; ~/.xinitrc &"
    [ $? -eq 0 ] && msg "done" || msg "fail"
;;
framebuffer)
    msg -n "Framebuffer [:${FB_DISPLAY}] ... "
    gui_started && { msg "skip"; return 0; }
    # update xorg.conf
    sed -i "s|Option.*\"fbdev\".*#linuxdeploy|Option \"fbdev\" \"${FB_DEV}\" #linuxdeploy|g" "${CHROOT_DIR}/etc/X11/xorg.conf"
    sed -i "s|Option.*\"Device\".*#linuxdeploy|Option \"Device\" \"${FB_INPUT}\" #linuxdeploy|g" "${CHROOT_DIR}/etc/X11/xorg.conf"
    dbus_init
    fb_refresh &
    (set -e
        sync
        case "${FB_FREEZE}" in
        stop)
            chroot_exec -u ${USER_NAME} "xinit -- :${FB_DISPLAY} -dpi ${FB_DPI} ${FB_ARGS} &"
            setprop ctl.stop surfaceflinger
            sleep 10
            setprop ctl.stop zygote
        ;;
        pause)
            chroot_exec -u ${USER_NAME} "xinit -- :${FB_DISPLAY} -dpi ${FB_DPI} ${FB_ARGS} &"
            pkill -STOP system_server
            pkill -STOP surfaceflinger
        ;;
        *)
            chroot_exec -u ${USER_NAME} "xinit -- :${FB_DISPLAY} -dpi ${FB_DPI} ${FB_ARGS} &"
        ;;
        esac
    exit 0)
    [ $? -eq 0 ] && msg "done" || msg "fail"
;;
custom)
    for script in ${CUSTOM_SCRIPTS}
    do
        msg -n "${script} ... "
        chroot_exec -u root "${script} start"
        [ $? -eq 0 ] && msg "done" || msg "fail"
    done
;;
esac

return 0
}

container_stop()
{
container_mounted || { msg "The container is already stopped." ; return 0; }

if [ $# -eq 0 ]; then
    msg "Stopping services: "

    for i in ${STARTUP}
    do
        container_stop ${i}
        [ $? -eq 0 ] || return 1
    done

    [ -z "${STARTUP}" ] && msg "...no active services"

    container_umount
    [ $? -ne 0 ] && return 1 || return 0
fi

sshd_kill()
{
local pid=""
for path in /run/sshd.pid /var/run/sshd.pid
do
    if [ -e "${CHROOT_DIR}${path}" ]; then
        pid=$(cat "${CHROOT_DIR}${path}")
        break
    fi
done
if [ -n "${pid}" ]; then
    kill -9 ${pid} || return 1
fi
return 0
}

xsession_kill()
{
local pid=""
if [ -e "${CHROOT_DIR}/tmp/xsession.pid" ]; then
    pid=$(cat "${CHROOT_DIR}/tmp/xsession.pid")
    rm -f "${CHROOT_DIR}/tmp/xsession.pid"
fi
if [ -n "${pid}" ]; then
    kill -9 ${pid} || return 1
fi
return 0
}

case "$1" in
ssh)
    msg -n "SSH ... "
    sshd_kill
    [ $? -eq 0 ] && msg "done" || msg "fail"
;;
vnc)
    msg -n "VNC ... "
    xsession_kill
    chroot_exec -u ${USER_NAME} "vncserver -kill :${VNC_DISPLAY}"
    [ $? -eq 0 ] && msg "done" || msg "fail"
;;
xserver)
    msg -n "X Server ... "
    xsession_kill
    [ $? -eq 0 ] && msg "done" || msg "fail"
;;
framebuffer)
    msg -n "Framebuffer ... "
    case "${FB_FREEZE}" in
    stop)
        setprop ctl.start surfaceflinger
        setprop ctl.start zygote
    ;;
    pause)
        pkill -CONT surfaceflinger
        pkill -CONT system_server
    ;;
    esac
    xsession_kill
    [ $? -eq 0 ] && msg "done" || msg "fail"
;;
custom)
    for script in ${CUSTOM_SCRIPTS}
    do
        msg -n "${script} ... "
        chroot_exec -u root "${script} stop"
        [ $? -eq 0 ] && msg "done" || msg "fail"
    done
;;
esac

return 0
}

container_chroot()
{
container_mounted || container_mount
[ $? -ne 0 ] && return 1

container_configure dns mtab >/dev/null

USER="root"
HOME=$(grep -m1 "^${USER}:" "${CHROOT_DIR}/etc/passwd" | awk -F: '{print $6}')
SHELL=$(grep -m1 "^${USER}:" "${CHROOT_DIR}/etc/passwd" | awk -F: '{print $7}')
LANG="${LOCALE}"
PS1="\u@\h:\w\\$ "
export USER HOME SHELL LANG PS1

chroot_exec "$@" 2>&1

return $?
}

configure_part()
{
msg -n "$1 ... "
(set -e
    case "$1" in
    dns)
        if [ -z "${SERVER_DNS}" ]; then
            local dns1=$(getprop net.dns1 || true)
            local dns2=$(getprop net.dns2 || true)
            local dns_list="${dns1} ${dns2}"
            [ -z "${dns1}" -a -z "${dns2}" ] && dns_list="8.8.8.8"
        else
            local dns_list=$(echo ${SERVER_DNS} | tr ',;' ' ')
        fi
        printf '' > "${CHROOT_DIR}/etc/resolv.conf"
        for dns in ${dns_list}
        do
            echo "nameserver ${dns}" >> "${CHROOT_DIR}/etc/resolv.conf"
        done
    ;;
    mtab)
        rm -f "${CHROOT_DIR}/etc/mtab" || true
        grep "${CHROOT_DIR}" /proc/mounts | sed "s|${CHROOT_DIR}/*|/|g" > "${CHROOT_DIR}/etc/mtab"
    ;;
    motd)
        local linux_version="GNU/Linux (${DISTRIB})"
        if [ -f "${CHROOT_DIR}/etc/os-release" ]
        then
            linux_version=$(. "${CHROOT_DIR}/etc/os-release"; echo ${PRETTY_NAME})
        elif [ -f "${CHROOT_DIR}/etc/arch-release" ]
        then
            linux_version="Arch Linux"
        elif [ -f "${CHROOT_DIR}/etc/gentoo-release" ]
        then
            linux_version=$(cat "${CHROOT_DIR}/etc/gentoo-release")
        elif [ -f "${CHROOT_DIR}/etc/fedora-release" ]
        then
            linux_version=$(cat "${CHROOT_DIR}/etc/fedora-release")
        elif [ -f "${CHROOT_DIR}/etc/redhat-release" ]
        then
            linux_version=$(cat "${CHROOT_DIR}/etc/redhat-release")
        elif [ -f "${CHROOT_DIR}/etc/centos-release" ]
        then
            linux_version=$(cat "${CHROOT_DIR}/etc/centos-release")
        elif [ -f "${CHROOT_DIR}/etc/debian_version" ]
        then
            linux_version=$(printf "Debian GNU/Linux " ; cat "${CHROOT_DIR}/etc/debian_version")
        fi
        local motd="${linux_version} [running on Android via Linux Deploy]"
        rm -f "${CHROOT_DIR}/etc/motd" || true
        echo ${motd} > "${CHROOT_DIR}/etc/motd"
    ;;
    hosts)
        if ! $(grep -q "^127.0.0.1" "${CHROOT_DIR}/etc/hosts"); then
            echo '127.0.0.1 localhost' >> "${CHROOT_DIR}/etc/hosts"
        fi
    ;;
    hostname)
        echo 'localhost' > "${CHROOT_DIR}/etc/hostname"
    ;;
    timezone)
        local timezone=$(getprop persist.sys.timezone || true)
        [ -z "${timezone}" ] && timezone=$(cat /etc/timezone)
        [ -z "${timezone}" ] && exit 1
        rm -f "${CHROOT_DIR}/etc/localtime" || true
        cp "${CHROOT_DIR}/usr/share/zoneinfo/${timezone}" "${CHROOT_DIR}/etc/localtime"
        echo ${timezone} > "${CHROOT_DIR}/etc/timezone"
    ;;
    su)
        case "${DISTRIB}" in
        fedora|opensuse)
            local pam_su="${CHROOT_DIR}/etc/pam.d/su-l"
        ;;
        *)
            local pam_su="${CHROOT_DIR}/etc/pam.d/su"
        ;;
        esac
        if [ -e "${pam_su}" ]; then
            if ! $(grep -q '^auth.*sufficient.*pam_succeed_if.so uid = 0 use_uid quiet$' "${pam_su}"); then
                sed -i '1,/^auth/s/^\(auth.*\)$/auth\tsufficient\tpam_succeed_if.so uid = 0 use_uid quiet\n\1/' "${pam_su}"
            fi
        fi
    ;;
    sudo)
        local sudo_str="${USER_NAME} ALL=(ALL:ALL) NOPASSWD:ALL"
        if ! $(grep -q "${sudo_str}" "${CHROOT_DIR}/etc/sudoers"); then
            echo ${sudo_str} >> "${CHROOT_DIR}/etc/sudoers"
        fi
        chmod 440 "${CHROOT_DIR}/etc/sudoers"
    ;;
    groups)
        local aids=$(cat "${ENV_DIR%/}/share/android-groups")
        for aid in ${aids}
        do
            local xname=$(echo ${aid} | awk -F: '{print $1}')
            local xid=$(echo ${aid} | awk -F: '{print $2}')
            sed -i "s|^${xname}:.*|${xname}:x:${xid}:${USER_NAME}|g" "${CHROOT_DIR}/etc/group" || true
            if ! $(grep -q "^${xname}:" "${CHROOT_DIR}/etc/group"); then
                echo "${xname}:x:${xid}:${USER_NAME}" >> "${CHROOT_DIR}/etc/group"
            fi
            if ! $(grep -q "^${xname}:" "${CHROOT_DIR}/etc/passwd"); then
                echo "${xname}:x:${xid}:${xid}::/:/bin/false" >> "${CHROOT_DIR}/etc/passwd"
            fi
        done
        # add users to aid_inet group
        local inet_users="root"
        case "${DISTRIB}" in
        debian|ubuntu|kalilinux)
            inet_users="${inet_users} messagebus www-data mysql postgres"
        ;;
        archlinux|fedora|centos)
            inet_users="${inet_users} dbus"
        ;;
        opensuse|gentoo|slackware)
            inet_users="${inet_users} messagebus"
        ;;
        esac
        for uid in ${inet_users}
        do
            [ -e "${CHROOT_DIR}/etc/group" ] || continue
            if ! $(grep -q "^aid_inet:.*${uid}" "${CHROOT_DIR}/etc/group"); then
                sed -i "s|^\(aid_inet:.*\)|\1,${uid}|g" "${CHROOT_DIR}/etc/group"
            fi
        done
    ;;
    locales)
        local inputfile=$(echo ${LOCALE} | awk -F. '{print $1}')
        local charmapfile=$(echo ${LOCALE} | awk -F. '{print $2}')
        chroot_exec localedef -i ${inputfile} -c -f ${charmapfile} ${LOCALE}
        case "${DISTRIB}" in
        debian|ubuntu|kalilinux)
            echo "LANG=${LOCALE}" > "${CHROOT_DIR}/etc/default/locale"
        ;;
        archlinux|centos)
            echo "LANG=${LOCALE}" > "${CHROOT_DIR}/etc/locale.conf"
        ;;
        fedora)
            echo "LANG=${LOCALE}" > "${CHROOT_DIR}/etc/sysconfig/i18n"
        ;;
        opensuse)
            echo "RC_LANG=${LOCALE}" > "${CHROOT_DIR}/etc/sysconfig/language"
            echo 'ROOT_USES_LANG="yes"' >> "${CHROOT_DIR}/etc/sysconfig/language"
        ;;
        slackware)
            sed -i "s|^export LANG=.*|export LANG=${LOCALE}|g" "${CHROOT_DIR}/etc/profile.d/lang.sh"
        ;;
        esac
    ;;
    repository)
        local platform=$(get_platform "${ARCH}")
        case "${DISTRIB}" in
        debian|ubuntu|kalilinux)
            if [ -e "${CHROOT_DIR}/etc/apt/sources.list" ]; then
                cp "${CHROOT_DIR}/etc/apt/sources.list" "${CHROOT_DIR}/etc/apt/sources.list.bak"
            fi
            if ! $(grep -q "${SOURCE_PATH}.*${SUITE}" "${CHROOT_DIR}/etc/apt/sources.list"); then
                case "${DISTRIB}" in
                debian|kalilinux)
                    echo "deb ${SOURCE_PATH} ${SUITE} main contrib non-free" > "${CHROOT_DIR}/etc/apt/sources.list"
                    echo "deb-src ${SOURCE_PATH} ${SUITE} main contrib non-free" >> "${CHROOT_DIR}/etc/apt/sources.list"
                ;;
                ubuntu)
                    echo "deb ${SOURCE_PATH} ${SUITE} main universe multiverse" > "${CHROOT_DIR}/etc/apt/sources.list"
                    echo "deb-src ${SOURCE_PATH} ${SUITE} main universe multiverse" >> "${CHROOT_DIR}/etc/apt/sources.list"
                ;;
                esac
            fi
        ;;
        archlinux)
            if [ "${platform}" = "intel" ]
            then local repo="${SOURCE_PATH%/}/\$repo/os/\$arch"
            else local repo="${SOURCE_PATH%/}/\$arch/\$repo"
            fi
            sed -i "s|^[[:space:]]*Architecture[[:space:]]*=.*$|Architecture = ${ARCH}|" "${CHROOT_DIR}/etc/pacman.conf"
            sed -i "s|^[[:space:]]*\(CheckSpace\)|#\1|" "${CHROOT_DIR}/etc/pacman.conf"
            sed -i "s|^[[:space:]]*SigLevel[[:space:]]*=.*$|SigLevel = Never|" "${CHROOT_DIR}/etc/pacman.conf"
            if $(grep -q '^[[:space:]]*Server' "${CHROOT_DIR}/etc/pacman.d/mirrorlist")
            then sed -i "s|^[[:space:]]*Server[[:space:]]*=.*|Server = ${repo}|" "${CHROOT_DIR}/etc/pacman.d/mirrorlist"
            else echo "Server = ${repo}" >> "${CHROOT_DIR}/etc/pacman.d/mirrorlist"
            fi
        ;;
        fedora)
            find "${CHROOT_DIR}/etc/yum.repos.d/" -name "*.repo" | while read f; do sed -i 's/^enabled=.*/enabled=0/g' "${f}"; done
            if [ "${platform}" = "intel" -o "${ARCH}" != "aarch64" -a "${SUITE}" -ge 20 ]
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
        ;;
        centos)
            chroot_exec -u root "yum-config-manager --disable '*'" >/dev/null
            local repo="${SOURCE_PATH%/}/${SUITE}/os/${ARCH}"
            local repo_file="${CHROOT_DIR}/etc/yum.repos.d/CentOS-${SUITE}-${ARCH}.repo"
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
            if [ "${platform}" = "intel" ]
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
            if ! $(grep -q '^aid_inet:.*,portage' "${CHROOT_DIR}/etc/group"); then
                sed -i "s|^\(aid_inet:.*\)|\1,portage|g" "${CHROOT_DIR}/etc/group"
            fi
            # set MAKEOPTS
            local ncpu=$(grep -c ^processor /proc/cpuinfo)
            let ncpu=${ncpu}+1
            if ! $(grep -q '^MAKEOPTS=' "${CHROOT_DIR}/etc/portage/make.conf"); then
                echo "MAKEOPTS=\"-j${ncpu}\"" >> "${CHROOT_DIR}/etc/portage/make.conf"
            fi
        ;;
        slackware)
            if [ -e "${CHROOT_DIR}/etc/slackpkg/mirrors" ]; then
                cp "${CHROOT_DIR}/etc/slackpkg/mirrors" "${CHROOT_DIR}/etc/slackpkg/mirrors.bak"
            fi
            echo ${SOURCE_PATH} > "${CHROOT_DIR}/etc/slackpkg/mirrors"
            chmod 644 "${CHROOT_DIR}/etc/slackpkg/mirrors"
            sed -i 's|^WGETFLAGS=.*|WGETFLAGS="--passive-ftp -q"|g' "${CHROOT_DIR}/etc/slackpkg/slackpkg.conf"
        ;;
        esac
    ;;
    profile)
        local reserved=$(echo ${USER_NAME} | grep ^aid_ || true)
        if [ -n "${reserved}" ]; then
            echo "Username ${USER_NAME} is reserved."
            exit 1
        fi
        sed -i 's|^UID_MIN.*|UID_MIN 5000|g' "${CHROOT_DIR}/etc/login.defs"
        sed -i 's|^GID_MIN.*|GID_MIN 5000|g' "${CHROOT_DIR}/etc/login.defs"
        # cli
        if [ "${USER_NAME}" != "root" ]; then
            chroot_exec groupadd ${USER_NAME} || true
            chroot_exec useradd -m -g ${USER_NAME} -s /bin/bash ${USER_NAME} || true
            chroot_exec usermod -g ${USER_NAME} ${USER_NAME} || true
        fi
        local user_home=$(grep -m1 "^${USER_NAME}:" "${CHROOT_DIR}/etc/passwd" | awk -F: '{print $6}')
        local user_id=$(grep -m1 "^${USER_NAME}:" "${CHROOT_DIR}/etc/passwd" | awk -F: '{print $3}')
        local group_id=$(grep -m1 "^${USER_NAME}:" "${CHROOT_DIR}/etc/passwd" | awk -F: '{print $4}')
        local path_str="PATH=${PATH}"
        if ! $(grep -q "${path_str}" "${CHROOT_DIR}${user_home}/.profile"); then
            echo ${path_str} >> "${CHROOT_DIR}${user_home}/.profile"
        fi
        # gui
        mkdir "${CHROOT_DIR}${user_home}/.vnc" || true
        echo 'XAUTHORITY=$HOME/.Xauthority' > "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        echo 'export XAUTHORITY' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        echo "LANG=$LOCALE" >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        echo 'export LANG' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        echo 'echo $$ > /tmp/xsession.pid' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        case "${DESKTOP_ENV}" in
        xterm)
            echo 'xterm -max' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        ;;
        lxde)
            echo 'startlxde' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        ;;
        xfce)
            echo 'startxfce4' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        ;;
        gnome)
            echo 'XKL_XMODMAP_DISABLE=1' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
            echo 'export XKL_XMODMAP_DISABLE' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
            echo 'if [ -n "`gnome-session -h | grep "\-\-session"`" ]; then' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
            echo '   gnome-session --session=gnome' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
            echo 'else' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
            echo '   gnome-session' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
            echo 'fi' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        ;;
        kde)
            echo 'startkde' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        ;;
        other)
            echo '# desktop environment' >> "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        ;;
        esac
        chmod 755 "${CHROOT_DIR}${user_home}/.vnc/xstartup"
        rm -f "${CHROOT_DIR}${user_home}/.xinitrc" || true
        ln -s "./.vnc/xstartup" "${CHROOT_DIR}${user_home}/.xinitrc"
        # set password for user
        echo ${USER_NAME}:${USER_PASSWORD} | chroot_exec chpasswd
        echo ${USER_PASSWORD} | chroot_exec vncpasswd -f > "${CHROOT_DIR}${user_home}/.vnc/passwd" ||
        echo "MPTcXfgXGiY=" | base64 -d > "${CHROOT_DIR}${user_home}/.vnc/passwd"
        chmod 600 "${CHROOT_DIR}${user_home}/.vnc/passwd"
        # set permissions
        chown -R ${user_id}:${group_id} "${CHROOT_DIR}${user_home}" || true
    ;;
    dbus)
        if [ -e "${CHROOT_DIR}/run" ]; then
            mkdir "${CHROOT_DIR}/run/dbus" || true
            chmod 755 "${CHROOT_DIR}/run/dbus"
        fi
        if [ -e "${CHROOT_DIR}/var/run" ]; then
            mkdir "${CHROOT_DIR}/var/run/dbus" || true
            chmod 755 "${CHROOT_DIR}/var/run/dbus"
        fi
        chroot_exec dbus-uuidgen > "${CHROOT_DIR}/etc/machine-id"
    ;;
    xorg)
        # Xwrapper.config
        mkdir -p "${CHROOT_DIR}/etc/X11"
        if $(grep -q '^allowed_users' "${CHROOT_DIR}/etc/X11/Xwrapper.config"); then
            sed -i 's/^allowed_users=.*/allowed_users=anybody/g' "${CHROOT_DIR}/etc/X11/Xwrapper.config"
        else
            echo "allowed_users=anybody" >> "${CHROOT_DIR}/etc/X11/Xwrapper.config"
        fi
        # xorg.conf
        mkdir -p "${CHROOT_DIR}/etc/X11"
        local xorg_conf="${CHROOT_DIR}/etc/X11/xorg.conf"
        [ -e "${xorg_conf}" ] && cp "${xorg_conf}" "${xorg_conf}.bak"
        cp "${ENV_DIR%/}/share/xorg.conf" "${xorg_conf}"
        chmod 644 "${xorg_conf}"
        # specific configuration
        case "${DISTRIB}" in
        gentoo)
            # set Xorg make configuration
            if $(grep -q '^INPUT_DEVICES=' "${CHROOT_DIR}/etc/portage/make.conf"); then
                sed -i 's|^\(INPUT_DEVICES=\).*|\1"evdev"|g' "${CHROOT_DIR}/etc/portage/make.conf"
            else
                echo 'INPUT_DEVICES="evdev"' >> "${CHROOT_DIR}/etc/portage/make.conf"
            fi
            if $(grep -q '^VIDEO_CARDS=' "${CHROOT_DIR}/etc/portage/make.conf"); then
                sed -i 's|^\(VIDEO_CARDS=\).*|\1"fbdev"|g' "${CHROOT_DIR}/etc/portage/make.conf"
            else
                echo 'VIDEO_CARDS="fbdev"' >> "${CHROOT_DIR}/etc/portage/make.conf"
            fi
        ;;
        opensuse)
            [ -e "${CHROOT_DIR}/usr/bin/Xorg" ] && chmod +s "${CHROOT_DIR}/usr/bin/Xorg"
        ;;
        esac
    ;;
    qemu)
        multiarch_support || exit 0
        local platform=$(get_platform)
        case "${platform}" in
        arm)
            local qemu_static="$(which qemu-i386-static)"
            local qemu_target="${CHROOT_DIR}/usr/local/bin/qemu-i386-static"
        ;;
        intel)
            local qemu_static="$(which qemu-arm-static)"
            local qemu_target="${CHROOT_DIR}/usr/local/bin/qemu-arm-static"
        ;;
        *)
            exit 0
        ;;
        esac
        [ -e "${qemu_target}" ] && exit 0
        [ -z "${qemu_static}" ] && exit 1
        mkdir -p "${CHROOT_DIR}/usr/local/bin" || true
        cp "${qemu_static}" "${qemu_target}"
        chown 0:0 "${qemu_target}"
        chmod 755 "${qemu_target}"
    ;;
    unchroot)
        local unchroot="${CHROOT_DIR}/bin/unchroot"
        echo '#!/bin/sh' > "${unchroot}"
        echo "PATH=$PATH" >> "${unchroot}"
        echo 'if [ $# -eq 0 ]; then' >> "${unchroot}"
        echo 'chroot /proc/1/cwd su -' >> "${unchroot}"
        echo 'else' >> "${unchroot}"
        echo 'chroot /proc/1/cwd $@' >> "${unchroot}"
        echo 'fi' >> "${unchroot}"
        chmod 755 "${unchroot}"
    ;;
    android)
        [ -e "/system" ] || exit 0
        [ ! -L "${CHROOT_DIR}/system" ] && ln -s "/mnt/system" "${CHROOT_DIR}/system"
        local reboot="$(which reboot)"
        if [ -n "${reboot}" ]; then
            rm -f "${CHROOT_DIR}/sbin/reboot" || true
            ln -s "${reboot}" "${CHROOT_DIR}/sbin/reboot"
        fi
        local shutdown="$(which shutdown)"
        if [ -n "${shutdown}" ]; then
            rm -f "${CHROOT_DIR}/sbin/shutdown" || true
            ln -s "${shutdown}" "${CHROOT_DIR}/sbin/shutdown"
        fi
    ;;
    misc)
        # Fix for upstart (Ubuntu)
        if [ -e "${CHROOT_DIR}/sbin/initctl" ]; then
            chroot_exec dpkg-divert --local --rename --add /sbin/initctl
            chroot_exec ln -s /bin/true /sbin/initctl
        fi
        # Fix for yum (Fedora)
        if [ -e "${CHROOT_DIR}/usr/bin/yum-deprecated" ]; then
            rm -f "${CHROOT_DIR}/usr/bin/yum" || true
            echo '#!/bin/sh' > "${CHROOT_DIR}/usr/bin/yum"
            echo '/usr/bin/yum-deprecated $*' >> "${CHROOT_DIR}/usr/bin/yum"
            chmod 755 "${CHROOT_DIR}/usr/bin/yum"
        fi
    ;;
    esac
exit 0)
[ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

return 0
}

container_configure()
{
if [ $# -eq 0 ]; then
    container_configure qemu dns mtab motd hosts hostname timezone su sudo groups locales repository profile dbus xorg unchroot android misc
    [ $? -ne 0 ] && return 1

    components_install
    [ $? -ne 0 ] && return 1
else 
    container_mounted || container_mount
    [ $? -ne 0 ] && return 1

    msg "Configuring the container: "
    for i in $*
    do
        configure_part $i
    done

    return 0
fi

return 0
}

components_install()
{
[ -z "${USE_COMPONENTS}" ] && return 1

msg "Installing additional components: "
(set -e
    case "${DISTRIB}" in
    debian|ubuntu|kalilinux)
        local pkgs=""
        selinux_support && pkgs="${pkgs} selinux-basics"
        for component in ${USE_COMPONENTS}
        do
            case "${component}" in
            desktop)
                pkgs="${pkgs} desktop-base x11-xserver-utils xfonts-base xfonts-utils"
                [ "${DISTRIB}" = "kalilinux" ] && pkgs="${pkgs} kali-defaults kali-menu"
                case "${DESKTOP_ENV}" in
                xterm)
                    pkgs="${pkgs} xterm"
                ;;
                lxde)
                    pkgs="${pkgs} lxde menu-xdg hicolor-icon-theme gtk2-engines"
                ;;
                xfce)
                    pkgs="${pkgs} xfce4 xfce4-terminal tango-icon-theme hicolor-icon-theme"
                ;;
                gnome)
                    pkgs="${pkgs} gnome-core"
                ;;
                kde)
                    pkgs="${pkgs} kde-standard"
                ;;
                esac
            ;;
            ssh)
                pkgs="${pkgs} openssh-server"
            ;;
            vnc)
                pkgs="${pkgs} tightvncserver"
            ;;
            xserver)
                pkgs="${pkgs} xinit xserver-xorg xserver-xorg-video-fbdev xserver-xorg-input-evdev florence"
            ;;
            kali-linux)
                pkgs="${pkgs} kali-linux-top10"
            ;;
            esac
        done
        [ -z "$pkgs" ] && return 1
        chroot_exec -u root "apt-get update -yq"
        chroot_exec -u root "DEBIAN_FRONTEND=noninteractive apt-get install -yf"
        chroot_exec -u root "DEBIAN_FRONTEND=noninteractive apt-get install ${pkgs} --no-install-recommends -yq"
        chroot_exec -u root "apt-get clean"
    ;;
    archlinux)
        local pkgs=""
        for component in ${USE_COMPONENTS}
        do
            case "${component}" in
            desktop)
                pkgs="${pkgs} xorg-utils xorg-fonts-misc ttf-dejavu"
                case "${DESKTOP_ENV}" in
                xterm)
                    pkgs="${pkgs} xterm"
                ;;
                lxde)
                    pkgs="${pkgs} lxde gtk-engines"
                ;;
                xfce)
                    pkgs="${pkgs} xfce4"
                ;;
                gnome)
                    pkgs="${pkgs} gnome"
                ;;
                kde)
                    pkgs="${pkgs} kdebase"
                ;;
                esac
            ;;
            ssh)
                pkgs="${pkgs} openssh"
            ;;
            vnc)
                pkgs="${pkgs} tigervnc"
            ;;
            xserver)
                pkgs="${pkgs} xorg-xinit xorg-server xf86-video-fbdev xf86-input-evdev"
            ;;
            esac
        done
        [ -z "${pkgs}" ] && return 1
        #rm -f ${CHROOT_DIR}/var/lib/pacman/db.lck || true
        chroot_exec -u root "pacman -Syq --noconfirm ${pkgs}"
        rm -f "${CHROOT_DIR}"/var/cache/pacman/pkg/* || true
    ;;
    fedora)
        local pkgs=""
        local igrp=""
        for component in ${USE_COMPONENTS}
        do
            case "${component}" in
            desktop)
                pkgs="${pkgs} xorg-x11-server-utils xorg-x11-fonts-misc dejavu-*"
                case "${DESKTOP_ENV}" in
                xterm)
                    pkgs="${pkgs} xterm"
                ;;
                lxde)
                    igrp="lxde-desktop"
                ;;
                xfce)
                    igrp="xfce-desktop"
                ;;
                gnome)
                    igrp="gnome-desktop"
                ;;
                kde)
                    igrp="kde-desktop"
                ;;
                esac
            ;;
            ssh)
                pkgs="${pkgs} openssh-server"
            ;;
            vnc)
                pkgs="${pkgs} tigervnc-server"
            ;;
            xserver)
                pkgs="${pkgs} xorg-x11-xinit xorg-x11-server-Xorg xorg-x11-drv-fbdev xorg-x11-drv-evdev"
            ;;
            esac
        done
        [ -z "${pkgs}" ] && return 1
        chroot_exec -u root "yum install ${pkgs} --nogpgcheck --skip-broken -y"
        [ -n "${igrp}" ] && chroot_exec -u root "yum groupinstall "${igrp}" --nogpgcheck --skip-broken -y"
        chroot_exec -u root "yum clean all"
    ;;
    centos)
        local pkgs=""
        local igrp=""
        for component in ${USE_COMPONENTS}
        do
            case "${component}" in
            desktop)
                pkgs="${pkgs} xorg-x11-server-utils xorg-x11-fonts-misc dejavu-*"
                case "${DESKTOP_ENV}" in
                xterm)
                    pkgs="${pkgs} xterm"
                ;;
                lxde)
                    igrp="lxde-desktop-environment"
                ;;
                xfce)
                    igrp="xfce-desktop-environment"
                ;;
                gnome)
                    igrp="gnome-desktop-environment"
                ;;
                kde)
                    igrp="kde-desktop-environment"
                ;;
                esac
            ;;
            ssh)
                pkgs="${pkgs} openssh-server"
            ;;
            vnc)
                pkgs="${pkgs} tigervnc-server"
            ;;
            xserver)
                pkgs="${pkgs} xorg-x11-xinit xorg-x11-server-Xorg xorg-x11-drv-fbdev xorg-x11-drv-evdev"
            ;;
            esac
        done
        [ -z "${pkgs}" ] && return 1
        chroot_exec -u root "yum install ${pkgs} --nogpgcheck --skip-broken -y"
        [ -n "${igrp}" ] && chroot_exec -u root "yum groupinstall "${igrp}" --nogpgcheck --skip-broken -y"
        chroot_exec -u root "yum clean all"
    ;;
    opensuse)
        local pkgs=""
        for component in ${USE_COMPONENTS}
        do
            case "${component}" in
            desktop)
                pkgs="${pkgs} xorg-x11-fonts-core dejavu-fonts xauth"
                case "${DESKTOP_ENV}" in
                xterm)
                    pkgs="${pkgs} xterm"
                ;;
                lxde)
                    pkgs="${pkgs} patterns-openSUSE-lxde"
                ;;
                xfce)
                    pkgs="${pkgs} patterns-openSUSE-xfce"
                ;;
                gnome)
                    pkgs="${pkgs} patterns-openSUSE-gnome"
                ;;
                kde)
                    pkgs="${pkgs} patterns-openSUSE-kde"
                ;;
                esac
            ;;
        ssh)
                pkgs="${pkgs} openssh"
            ;;
            vnc)
                pkgs="${pkgs} tightvnc"
            ;;
            xserver)
                pkgs="${pkgs} xinit xorg-x11-server xf86-video-fbdev xf86-input-evdev"
            ;;
            esac
        done
        [ -z "${pkgs}" ] && return 1
        chroot_exec -u root "zypper --no-gpg-checks --non-interactive install ${pkgs}"
        chroot_exec -u root "zypper clean"
    ;;
    gentoo)
        local pkgs=""
        for component in ${USE_COMPONENTS}
        do
            case "${component}" in
            desktop)
                pkgs="${pkgs} xauth"
                case "${DESKTOP_ENV}" in
                xterm)
                    pkgs="${pkgs} xterm"
                ;;
                lxde)
                    pkgs="${pkgs} lxde-meta gtk-engines"
                ;;
                xfce)
                    pkgs="${pkgs} xfce4-meta"
                ;;
                gnome)
                    pkgs="${pkgs} gnome"
                ;;
                kde)
                    pkgs="${pkgs} kde-meta"
                ;;
                esac
            ;;
            ssh)
                pkgs="${pkgs} openssh"
            ;;
            vnc)
                # set server USE flag for tightvnc
                if ! $(grep -q '^net-misc/tightvnc' "${CHROOT_DIR}/etc/portage/package.use"); then
                    echo "net-misc/tightvnc server" >> "${CHROOT_DIR}/etc/portage/package.use"
                fi
                if ! $(grep -q '^net-misc/tightvnc.*server' "${CHROOT_DIR}/etc/portage/package.use"); then
                    sed -i "s|^\(net-misc/tightvnc.*\)|\1 server|g" "${CHROOT_DIR}/etc/portage/package.use"
                fi
                pkgs="${pkgs} tightvnc"
            ;;
            xserver)
                pkgs="${pkgs} xinit xorg-server"
            ;;
            esac
        done
        [ -z "${pkgs}" ] && return 1
        chroot_exec -u root "emerge --autounmask-write ${pkgs}" || {
            mv "${CHROOT_DIR}/etc/portage/._cfg0000_package.use" "${CHROOT_DIR}/etc/portage/package.use"
            chroot_exec -u root "emerge ${pkgs}"
        }
    ;;
    slackware)
        local pkgs=""
        for component in ${USE_COMPONENTS}
        do
            case "${component}" in
            ssh)
                pkgs="${pkgs} openssh"
            ;;
            esac
        done
        [ -z "${pkgs}" ] && return 1
        chroot_exec -u root "slackpkg -checkgpg=off -batch=on -default_answer=y update" || true
        chroot_exec -u root "slackpkg -checkgpg=off -batch=on -default_answer=y install ${pkgs}"
    ;;
    *)
        msg " ...not supported"
        exit 1
    ;;
    esac
exit 0)
[ $? -ne 0 ] && return 1

return 0
}

container_install()
{
container_prepare
[ $? -ne 0 ] && return 1

container_mount root binfmt_misc
[ $? -ne 0 ] && return 1

case "${DISTRIB}" in
debian|ubuntu|kalilinux)
    msg "Installing Debian-based distribution: "

    local basic_packages="dbus,locales,sudo,man-db"

    (set -e
        DEBOOTSTRAP_DIR="${ENV_DIR%/}/share/debootstrap"
        . "${DEBOOTSTRAP_DIR}/debootstrap" --no-check-gpg --arch "${ARCH}" --foreign --extractor=ar --include="${basic_packages}" "${SUITE}" "${CHROOT_DIR}" "${SOURCE_PATH}"
    exit 0)
    [ $? -ne 0 ] && return 1

    container_configure qemu dns mtab

    unset DEBOOTSTRAP_DIR
    chroot_exec /debootstrap/debootstrap --second-stage
    [ $? -ne 0 ] && return 1

    container_mount
;;
archlinux)
    msg "Installing Arch Linux distribution: "

    local basic_packages="filesystem acl archlinux-keyring attr bash bzip2 ca-certificates coreutils cracklib curl db e2fsprogs expat findutils gawk gcc-libs gdbm glibc gmp gnupg gpgme grep keyutils krb5 libarchive libassuan libcap libgcrypt libgpg-error libgssglue libidn libksba libldap libsasl libssh2 libtirpc linux-api-headers lzo ncurses nettle openssl pacman pacman-mirrorlist pam pambase perl pinentry pth readline run-parts sed shadow sudo tzdata util-linux xz which zlib"

    local platform=$(get_platform ${ARCH})
    if [ "${platform}" = "intel" ]
    then local repo="${SOURCE_PATH%/}/core/os/${ARCH}"
    else local repo="${SOURCE_PATH%/}/${ARCH}/core"
    fi

    local cache_dir="${CHROOT_DIR}/var/cache/pacman/pkg"

    msg "Repository: ${repo}"

    msg -n "Preparing for deployment ... "
    (set -e
        cd "${CHROOT_DIR}"
        mkdir etc
        echo "root:x:0:0:root:/root:/bin/bash" > etc/passwd
        echo "root:x:0:" > etc/group
        touch etc/fstab
        mkdir tmp; chmod 01777 tmp
        mkdir -p "${cache_dir}"
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg -n "Retrieving packages list ... "
    local pkg_list=$(wget -q -O - "${repo}/" | sed -n '/<a / s/^.*<a [^>]*href="\([^\"]*\)".*$/\1/p' | awk -F'/' '{print $NF}' | sort -rn)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg "Retrieving base packages: "
    for package in ${basic_packages}
    do
        msg -n "${package} ... "
        local pkg_file=$(echo "${pkg_list}" | grep -m1 -e "^${package}-[[:digit:]].*\.xz$" -e "^${package}-[[:digit:]].*\.gz$")
        test "${pkg_file}" || { msg "fail"; return 1; }
        # download
        for i in 1 2 3
        do
            [ ${i} -gt 1 ] && sleep 30s
            wget -q -c -O "${cache_dir}/${pkg_file}" "${repo}/${pkg_file}"
            [ $? -eq 0 ] && break
        done
        # unpack
        case "${pkg_file}" in
        *.gz) tar xzf "${cache_dir}/${pkg_file}" -C "${CHROOT_DIR}";;
        *.xz) xz -dc "${cache_dir}/${pkg_file}" | tar x -C "${CHROOT_DIR}";;
        *) msg "fail"; return 1;;
        esac
        [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
    done

    container_mount

    container_configure qemu dns mtab groups repository

    msg "Installing base packages: "
    (set -e
        chroot_exec -u root "/usr/bin/pacman --noconfirm -Sy"
        extra_packages=$(chroot_exec -u root "/usr/bin/pacman --noconfirm -Sg base" | awk '{print $2}' | grep -v -e 'linux' -e 'kernel' | xargs)
        chroot_exec -u root "/usr/bin/pacman --noconfirm --force -Sq ${basic_packages} ${extra_packages}"
    exit 0)
    [ $? -ne 0 ] && return 1

    msg -n "Clearing cache ... "
    rm -f "${cache_dir}"/* "${CHROOT_DIR}/.INSTALL" "${CHROOT_DIR}/.MTREE" "${CHROOT_DIR}/.PKGINFO" $(find "${CHROOT_DIR}" -type f -name "*.pacorig")
    [ $? -eq 0 ] && msg "done" || msg "fail"
;;
fedora)
    msg "Installing Fedora distribution: "

    local basic_packages="filesystem audit-libs basesystem bash bzip2-libs ca-certificates chkconfig coreutils cpio cracklib cracklib-dicts crypto-policies cryptsetup-libs curl cyrus-sasl-lib dbus dbus-libs device-mapper device-mapper-libs diffutils elfutils-libelf elfutils-libs expat fedora-release fedora-repos file-libs fipscheck fipscheck-lib gamin gawk gdbm glib2 glibc glibc-common gmp gnupg2 gnutls gpgme grep gzip hwdata info keyutils-libs kmod kmod-libs krb5-libs libacl libarchive libassuan libattr libblkid libcap libcap-ng libcom_err libcurl libdb libdb4 libdb-utils libffi libgcc libgcrypt libgpg-error libidn libmetalink libmicrohttpd libmount libpwquality libseccomp libselinux libselinux-utils libsemanage libsepol libsmartcols libssh2 libstdc++ libtasn1 libuser libutempter libuuid libverto libxml2 lua lzo man-pages ncurses ncurses-base ncurses-libs nettle nspr nss nss-myhostname nss-softokn nss-softokn-freebl nss-sysinit nss-tools nss-util openldap openssl-libs p11-kit p11-kit-trust pam pcre pinentry pkgconfig policycoreutils popt pth pygpgme pyliblzma python python-chardet python-iniparse python-kitchen python-libs python-pycurl python-six python-urlgrabber pyxattr qrencode-libs readline rootfiles rpm rpm-build-libs rpm-libs rpm-plugin-selinux rpm-python sed selinux-policy setup shadow-utils shared-mime-info sqlite sudo systemd systemd-libs systemd-sysv tcp_wrappers-libs trousers tzdata ustr util-linux vim-minimal xz-libs yum yum-metadata-parser yum-utils which zlib"

    local platform=$(get_platform ${ARCH})
    if [ "${platform}" = "intel" -o "${ARCH}" != "aarch64" -a "${SUITE}" -ge 20 ]
    then local repo="${SOURCE_PATH%/}/fedora/linux/releases/${SUITE}/Everything/${ARCH}/os"
    else local repo="${SOURCE_PATH%/}/fedora-secondary/releases/${SUITE}/Everything/${ARCH}/os"
    fi

    msg "Repository: ${repo}"

    msg -n "Preparing for deployment ... "
    (set -e
        cd "${CHROOT_DIR}"
        mkdir etc
        echo "root:x:0:0:root:/root:/bin/bash" > etc/passwd
        echo "root:x:0:" > etc/group
        touch etc/fstab
        mkdir tmp; chmod 01777 tmp
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg -n "Retrieving packages list ... "
    local pkg_list="${CHROOT_DIR}/tmp/packages.list"
    (set -e
        repodata=$(wget -q -O - "${repo}/repodata/repomd.xml" | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\-primary\.xml\.gz\)".*$/\1/p')
        [ -z "${repodata}" ] && exit 1
        wget -q -O - "${repo}/${repodata}" | gzip -dc | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\)".*$/\1/p' > "${pkg_list}"
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg "Retrieving base packages: "
    for package in ${basic_packages}
    do
        msg -n "${package} ... "
        local pkg_url=$(grep -m1 -e "^.*/${package}-[0-9][0-9\.\-].*\.rpm$" "${pkg_list}")
        test "${pkg_url}" || { msg "skip"; continue; }
        local pkg_file=$(basename "${pkg_url}")
        # download
        for i in 1 2 3
        do
            [ ${i} -gt 1 ] && sleep 30s
            wget -q -c -O "${CHROOT_DIR}/tmp/${pkg_file}" "${repo}/${pkg_url}"
            [ $? -eq 0 ] && break
        done
        # unpack
        (cd "${CHROOT_DIR}"; rpm2cpio "${CHROOT_DIR}/tmp/${pkg_file}" | cpio -idmu)
        [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
    done

    container_configure qemu

    msg "Updating a packages database ... "
    chroot_exec /bin/rpm -iv --force --nosignature --nodeps --justdb /tmp/*.rpm
    [ $? -eq 0 ] && msg "done" || msg "fail"

    msg -n "Clearing cache ... "
    rm -rf "${CHROOT_DIR}"/tmp/*
    [ $? -eq 0 ] && msg "done" || msg "fail"

    container_mount

    container_configure dns mtab misc groups repository

    msg "Installing minimal environment: "
    (set -e
        chroot_exec -u root "yum groupinstall minimal-environment --nogpgcheck --skip-broken -y --exclude openssh-server"
        chroot_exec -u root "yum clean all"
    exit 0)
    [ $? -ne 0 ] && return 1
;;
centos)
    msg "Installing CentOS distribution: "

    local basic_packages="filesystem audit-libs basesystem bash bzip2-libs ca-certificates centos-release chkconfig coreutils cpio cracklib cracklib-dicts cryptsetup-libs curl cyrus-sasl-lib dbus dbus-libs device-mapper device-mapper-libs diffutils elfutils-libelf elfutils-libs expat file-libs gawk gdbm glib2 glibc glibc-common gmp gnupg2 gpgme grep gzip info keyutils-libs kmod kmod-libs krb5-libs libacl libassuan libattr libblkid libcap libcap-ng libcom_err libcurl libdb libdb-utils libffi libgcc libgcrypt libgpg-error libidn libmount libpwquality libselinux libsemanage libsepol libssh2 libstdc++ libtasn1 libuser libutempter libuuid libverto libxml2 lua ncurses ncurses-base ncurses-libs nspr nss nss-softokn nss-softokn-freebl nss-sysinit nss-tools nss-util openldap openssl-libs p11-kit p11-kit-trust pam pcre pinentry pkgconfig popt pth pygpgme pyliblzma python python-chardet python-iniparse python-kitchen python-libs python-pycurl python-urlgrabber pyxattr qrencode-libs readline rootfiles rpm rpm-build-libs rpm-libs rpm-python sed selinux-policy setup shadow-utils shared-mime-info sqlite sudo systemd systemd-libs tzdata ustr util-linux vim-minimal which xz-libs yum yum-metadata-parser yum-plugin-fastestmirror yum-utils zlib"

    local repo="${SOURCE_PATH%/}/${SUITE}/os/${ARCH}"

    msg "Repository: ${repo}"

    msg -n "Preparing for deployment ... "
    (set -e
        cd "${CHROOT_DIR}"
        mkdir etc
        echo "root:x:0:0:root:/root:/bin/bash" > etc/passwd
        echo "root:x:0:" > etc/group
        touch etc/fstab
        mkdir tmp; chmod 01777 tmp
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg -n "Retrieving packages list ... "
    local pkg_list="${CHROOT_DIR}/tmp/packages.list"
    (set -e
        repodata=$(wget -q -O - "${repo}/repodata/repomd.xml" | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\-primary\.xml\.gz\)".*$/\1/p')
        [ -z "${repodata}" ] && exit 1
        wget -q -O - "${repo}/${repodata}" | gzip -dc | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\)".*$/\1/p' > "${pkg_list}"
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg "Retrieving base packages: "
    for package in ${basic_packages}
    do
        msg -n "${package} ... "
        local pkg_url=$(grep -m1 -e "^.*/${package}-[0-9][0-9\.\-].*\.rpm$" "${pkg_list}")
        test "${pkg_url}" || { msg "skip"; continue; }
        local pkg_file=$(basename "${pkg_url}")
        # download
        for i in 1 2 3
        do
            [ ${i} -gt 1 ] && sleep 30s
            wget -q -c -O "${CHROOT_DIR}/tmp/${pkg_file}" "${repo}/${pkg_url}"
            [ $? -eq 0 ] && break
        done
        # unpack
        (cd "${CHROOT_DIR}"; rpm2cpio "${CHROOT_DIR}/tmp/${pkg_file}" | cpio -idmu)
        [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
    done

    container_configure qemu

    msg "Updating a packages database ... "
    chroot_exec /bin/rpm -iv --force --nosignature --nodeps --justdb /tmp/*.rpm
    [ $? -eq 0 ] && msg "done" || msg "fail"

    msg -n "Clearing cache ... "
    rm -rf "${CHROOT_DIR}"/tmp/*
    [ $? -eq 0 ] && msg "done" || msg "fail"

    container_mount

    container_configure dns mtab misc groups repository

    msg "Installing minimal environment: "
    (set -e
        chroot_exec -u root 'yum groupinstall "Minimal Install" --nogpgcheck --skip-broken -y --exclude openssh-server'
        chroot_exec -u root 'yum clean all'
    exit 0)
    [ $? -ne 0 ] && return 1
;;
opensuse)
    msg "Installing openSUSE distribution: "

    local basic_packages=""
    case "${SUITE}" in
    12.3) basic_packages="filesystem aaa_base aaa_base-extras autoyast2-installation bash bind-libs bind-utils branding-openSUSE bridge-utils bzip2 coreutils cpio cracklib cracklib-dict-full cron cronie cryptsetup curl cyrus-sasl dbus-1 dbus-1-x11 device-mapper dhcpcd diffutils dirmngr dmraid e2fsprogs elfutils file fillup findutils fontconfig gawk gio-branding-openSUSE glib2-tools glibc glibc-extra glibc-i18ndata glibc-locale gnu-unifont-bitmap-fonts-20080123 gpg2 grep groff gzip hwinfo ifplugd info initviocons iproute2 iputils-s20101006 kbd kpartx krb5 less-456 libX11-6 libX11-data libXau6 libXext6 libXft2 libXrender1 libacl1 libadns1 libaio1 libasm1 libassuan0 libattr1 libaudit1 libaugeas0 libblkid1 libbz2-1 libcairo2 libcap-ng0 libcap2 libcom_err2 libcrack2 libcryptsetup4 libcurl4 libdaemon0 libdb-4_8 libdbus-1-3 libdrm2 libdw1 libedit0 libelf0 libelf1 libestr0 libexpat1 libext2fs2 libffi4 libfreetype6 libgcc_s1 libgcrypt11 libgdbm4 libgio-2_0-0 libglib-2_0-0 libgmodule-2_0-0 libgmp10 libgnutls28 libgobject-2_0-0 libgpg-error0 libgssglue1 libharfbuzz0 libhogweed2 libicu49 libidn11 libiw30 libjson0 libkeyutils1 libkmod2-12 libksba8 libldap-2_4-2 liblua5_1 liblzma5 libmagic1 libmicrohttpd10 libmodman1 libmount1 libncurses5 libncurses6 libnettle4 libnl3-200 libopenssl1_0_0 libp11-kit0 libpango-1_0-0 libparted0 libpci3 libpcre1 libpipeline1 libpixman-1-0 libply-boot-client2 libply-splash-core2 libply-splash-graphics2 libply2 libpng15-15 libpolkit0 libpopt0 libprocps1 libproxy1 libpth20 libpython2_7-1_0 libqrencode3 libreadline6 libreiserfs-0_3-0 libselinux1 libsemanage1 libsepol1 libsolv-tools libssh2-1 libstdc++6 libstorage4 libtasn1 libtasn1-6 libtirpc1 libudev1-195 libusb-0_1-4 libusb-1_0-0 libustr-1_0-1 libuuid1 libwrap0 libxcb-render0 libxcb-shm0 libxcb1 libxml2-2 libxtables9 libyui-ncurses-pkg4 libyui-ncurses4 libyui4 libz1 libzio1 libzypp logrotate lsscsi lvm2 man man-pages mdadm mkinitrd module-init-tools multipath-tools ncurses-utils net-tools netcfg openSUSE-build-key openSUSE-release-12.3 openSUSE-release-ftp-12.3 openslp openssl pam pam-config pango-tools parted pciutils pciutils-ids perl perl-Bootloader perl-Config-Crontab perl-XML-Parser perl-XML-Simple perl-base perl-gettext permissions pinentry pkg-config polkit procps python-base rpcbind rpm rsyslog sed shadow shared-mime-info sudo suse-module-tools sysconfig sysfsutils syslog-service systemd-195 systemd-presets-branding-openSUSE systemd-sysvinit-195 sysvinit-tools tar tcpd terminfo-base timezone-2012j tunctl u-boot-tools udev-195 unzip update-alternatives util-linux vim vim-base vlan wallpaper-branding-openSUSE wireless-tools wpa_supplicant xz yast2 yast2-bootloader yast2-core yast2-country yast2-country-data yast2-firstboot yast2-hardware-detection yast2-installation yast2-packager yast2-perl-bindings yast2-pkg-bindings yast2-proxy yast2-slp yast2-storage yast2-trans-stats yast2-transfer yast2-update yast2-xml yast2-ycp-ui-bindings zypper"
    ;;
    13.2) basic_packages="filesystem aaa_base aaa_base-extras autoyast2-installation bash bind-libs bind-utils branding-openSUSE bridge-utils bzip2 coreutils cpio cracklib cracklib-dict-full cron cronie cryptsetup curl cyrus-sasl dbus-1 dbus-1-x11 device-mapper dhcpcd diffutils dirmngr dmraid e2fsprogs elfutils file fillup findutils fontconfig gawk gio-branding-openSUSE glib2-tools glibc glibc-extra glibc-i18ndata glibc-locale gnu-unifont-bitmap-fonts-20080123 gpg2 grep groff gzip hwinfo ifplugd info initviocons iproute2 iputils-s20121221 kbd kpartx krb5 less-458 libX11-6 libX11-data libXau6 libXext6 libXft2 libXrender1 libacl1 libadns1 libaio1 libasm1 libassuan0 libattr1 libaudit1 libaugeas0 libblkid1 libbz2-1 libcairo2 libcap-ng0 libcap2 libcom_err2 libcrack2 libcryptsetup4 libcurl4 libdaemon0 libdb-4_8 libdbus-1-3 libdrm2 libdw1 libedit0 libelf0 libelf1 libestr0 libexpat1 libext2fs2 libffi4 libfreetype6 libgcc_s1 libgcrypt20 libgdbm4 libgio-2_0-0 libglib-2_0-0 libgmodule-2_0-0 libgmp10 libgnutls28 libgobject-2_0-0 libgpg-error0 libgssglue1 libharfbuzz0 libhogweed2 libicu53_1 libidn11 libiw30 libjson-c2 libkeyutils1 libkmod2-18 libksba8 libldap-2_4-2 liblua5_1 liblua5_2 liblzma5 libmagic1 libmicrohttpd10 libmodman1 libmount1 libncurses5 libncurses6 libnettle4 libnl3-200 libopenssl1_0_0 libp11-kit0 libpango-1_0-0 libparted0 libpci3 libpcre1 libpipeline1 libpixman-1-0 libply-boot-client2 libply-splash-core2 libply-splash-graphics2 libply2 libpng16-16 libpolkit0 libpopt0 libprocps3 libproxy1 libpth20 libpython2_7-1_0 libqrencode3 libreadline6 libreiserfs-0_3-0 libsasl2-3 libselinux1 libsemanage1 libsepol1 libsolv-tools libssh2-1 libstdc++6 libstorage5 libtasn1 libtasn1-6 libtirpc1 libudev1-210 libusb-0_1-4 libusb-1_0-0 libustr-1_0-1 libuuid1 libwrap0 libxcb-render0 libxcb-shm0 libxcb1 libxml2-2 libxtables10 libyui-ncurses-pkg6 libyui-ncurses6 libyui6 libz1 libzio1 libzypp logrotate lsscsi lvm2 man man-pages mdadm multipath-tools ncurses-utils net-tools netcfg openSUSE-build-key openSUSE-release-13.2 openSUSE-release-ftp-13.2 openslp openssl pam pam-config pango-tools parted pciutils pciutils-ids perl perl-Bootloader perl-Config-Crontab perl-XML-Parser perl-XML-Simple perl-base perl-gettext permissions pinentry pkg-config polkit procps python-base rpcbind rpm rsyslog sed shadow shared-mime-info sudo suse-module-tools sysconfig sysfsutils syslog-service systemd-210 systemd-presets-branding-openSUSE systemd-sysvinit-210 sysvinit-tools tar tcpd terminfo-base timezone-2014h tunctl u-boot-tools udev-210 unzip update-alternatives util-linux vim vlan wallpaper-branding-openSUSE which wireless-tools wpa_supplicant xz yast2 yast2-bootloader yast2-core yast2-country yast2-country-data yast2-firstboot yast2-hardware-detection yast2-installation yast2-packager yast2-perl-bindings yast2-pkg-bindings yast2-proxy yast2-slp yast2-storage yast2-trans-stats yast2-transfer yast2-update yast2-xml yast2-ycp-ui-bindings zypper"
    ;;
    esac

    local platform=$(get_platform ${ARCH})
    if [ "${platform}" = "intel" ]
    then local repo="${SOURCE_PATH%/}/distribution/${SUITE}/repo/oss/suse"
    else local repo="${SOURCE_PATH%/}/${ARCH}/distribution/${SUITE}/repo/oss/suse"
    fi

    msg "Repository: ${repo}"

    msg -n "Preparing for deployment ... "
    (set -e
        cd "${CHROOT_DIR}"
        mkdir etc
        echo "root:x:0:0:root:/root:/bin/bash" > etc/passwd
        echo "root:x:0:" > etc/group
        touch etc/fstab
        mkdir tmp; chmod 01777 tmp
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg -n "Retrieving packages list ... "
    local pkg_list="$CHROOT_DIR/tmp/packages.list"
    (set -e
        repodata=$(wget -q -O - "${repo}/repodata/repomd.xml" | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\-primary\.xml\.gz\)".*$/\1/p')
        [ -z "${repodata}" ] && exit 1
        wget -q -O - "${repo}/${repodata}" | gzip -dc | sed -n '/<location / s/^.*<location [^>]*href="\([^\"]*\)".*$/\1/p' > "${pkg_list}"
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg "Retrieving base packages: "
    for package in ${basic_packages}
    do
        msg -n "${package} ... "
        local pkg_url=$(grep -e "^${ARCH}" -e "^noarch" "${pkg_list}" | grep -m1 -e "/${package}-[0-9]\{1,4\}\..*\.rpm$")
        test "${pkg_url}" || { msg "fail"; return 1; }
        local pkg_file=$(basename "${pkg_url}")
        # download
        for i in 1 2 3
        do
            [ ${i} -gt 1 ] && sleep 30s
            wget -q -c -O "${CHROOT_DIR}/tmp/${pkg_file}" "${repo}/${pkg_url}"
            [ $? -eq 0 ] && break
        done
        # unpack
        (cd "${CHROOT_DIR}"; rpm2cpio "${CHROOT_DIR}/tmp/${pkg_file}" | cpio -idmu)
        [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
    done

    container_configure qemu

    msg "Updating a packages database ... "
    chroot_exec /bin/rpm -iv --force --nosignature --nodeps /tmp/*.rpm
    [ $? -eq 0 ] && msg "done" || msg "fail"

    msg -n "Clearing cache ... "
    rm -rf "${CHROOT_DIR}"/tmp/*
    [ $? -eq 0 ] && msg "done" || msg "fail"

    container_mount
;;
gentoo)
    msg "Installing Gentoo distribution: "

    msg -n "Preparing for deployment ... "
    (set -e
        cd "${CHROOT_DIR}"
        mkdir tmp; chmod 01777 tmp
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg -n "Getting repository path ... "
    local repo="${SOURCE_PATH%/}/autobuilds"
    local stage3="${CHROOT_DIR}/tmp/latest-stage3.tar.bz2"
    local archive=$(wget -q -O - "${repo}/latest-stage3-${ARCH}.txt" | grep -v ^# | awk '{print $1}')
    [ -n "${archive}" ] && msg "done" || { msg "fail"; return 1; }

    msg -n "Retrieving stage3 archive ... "
    for i in 1 2 3
    do
        [ ${i} -gt 1 ] && sleep 30s
        wget -c -O "${stage3}" "${repo}/${archive}"
        [ $? -eq 0 ] && break
    done
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg -n "Unpacking stage3 archive ... "
    (set -e
        tar xjf "${stage3}" -C "${CHROOT_DIR}"
        rm -f "${stage3}"
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    container_mount

    container_configure qemu dns mtab groups repository

    msg -n "Updating portage tree ... "
    (set -e
        chroot_exec -u root "emerge --sync"
        chroot_exec -u root "eselect profile set 1"
    exit 0) 1>/dev/null
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg "Installing base packages: "
    (set -e
        chroot_exec -u root "emerge sudo"
    exit 0)
    [ $? -eq 0 ] || return 1

    msg -n "Updating configuration ... "
    find "${CHROOT_DIR}/" -name "._cfg0000_*" | while read f; do mv "${f}" "$(echo ${f} | sed 's/._cfg0000_//g')"; done
    [ $? -eq 0 ] && msg "done" || msg "skip"
;;
slackware)
    msg "Installing Slackware distribution: "

    local repo="${SOURCE_PATH%/}/slackware"
    local cache_dir="${CHROOT_DIR}/tmp"
    local extra_packages="l/glibc l/glibc-i18n l/libtermcap l/ncurses ap/diffutils ap/groff ap/man ap/nano ap/slackpkg ap/sudo n/gnupg n/wget"

    msg -n "Preparing for deployment ... "
    (set -e
        cd "${CHROOT_DIR}"
        mkdir etc
        touch etc/fstab
        mkdir tmp; chmod 01777 tmp
    exit 0)
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg -n "Retrieving packages list ... "
    local basic_packages=$(wget -q -O - "${repo}/a/tagfile" | grep -v -e 'kernel' -e 'efibootmgr' -e 'lilo' -e 'grub' | awk -F: '{if ($1!="") print "a/"$1}')
    local pkg_list="${cache_dir}/packages.list"
    wget -q -O - "${repo}/FILE_LIST" | grep -o -e '/.*\.\tgz$' -e '/.*\.\txz$' > "${pkg_list}"
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }

    msg "Retrieving base packages: "
    for package in ${basic_packages} ${extra_packages}
    do
        msg -n "${package} ... "
        local pkg_url=$(grep -m1 -e "/${package}\-" "${pkg_list}")
        test "${pkg_url}" || { msg "fail"; return 1; }
        local pkg_file=$(basename "${pkg_url}")
        # download
        for i in 1 2 3
        do
            [ ${i} -gt 1 ] && sleep 30s
            wget -q -c -O "${cache_dir}/${pkg_file}" "${repo}${pkg_url}"
                [ $? -eq 0 ] && break
            done
        # unpack
        case "${pkg_file}" in
        *gz) tar xzf "${cache_dir}/${pkg_file}" -C "${CHROOT_DIR}";;
        *xz) tar xJf "${cache_dir}/${pkg_file}" -C "${CHROOT_DIR}";;
        *) msg "fail"; return 1;;
        esac
        [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
        # install
        if [ -e "${CHROOT_DIR}/install/doinst.sh" ]; then
            (cd "${CHROOT_DIR}"; . ./install/doinst.sh)
        fi
        if [ -e "${CHROOT_DIR}/install" ]; then
            rm -rf "${CHROOT_DIR}/install"
        fi
    done

    msg -n "Clearing cache ... "
    rm -f "${cache_dir}"/*
    [ $? -eq 0 ] && msg "done" || msg "fail"

    container_mount
;;
rootfs)
    msg "Getting and unpacking rootfs archive: "
    if [ -n "$(echo ${SOURCE_PATH} | grep -i 'gz$')" ]; then
        if [ -e "${SOURCE_PATH}" ]; then
            (set -e
                tar xzvf "${SOURCE_PATH}" -C "${CHROOT_DIR}"
            exit 0)
            [ $? -eq 0 ] || return 1
        fi
        if [ -n "$(echo ${SOURCE_PATH} | grep -i '^http')" ]; then
            (set -e
                wget -q -O - "${SOURCE_PATH}" | tar xzv -C "${CHROOT_DIR}"
            exit 0)
            [ $? -eq 0 ] || return 1
        fi
    fi
    if [ -n "$(echo ${SOURCE_PATH} | grep -i 'bz2$')" ]; then
        if [ -e "${SOURCE_PATH}" ]; then
            (set -e
                tar xjvf "${SOURCE_PATH}" -C "${CHROOT_DIR}"
            exit 0)
            [ $? -eq 0 ] || return 1
        fi
        if [ -n "$(echo ${SOURCE_PATH} | grep -i '^http')" ]; then
            (set -e
                wget -q -O - "${SOURCE_PATH}" | tar xjv -C "${CHROOT_DIR}"
            exit 0)
            [ $? -eq 0 ] || return 1
        fi
    fi
    if [ -n "$(echo ${SOURCE_PATH} | grep -i 'xz$')" ]; then
        if [ -e "${SOURCE_PATH}" ]; then
            (set -e
                tar xJvf "${SOURCE_PATH}" -C "${CHROOT_DIR}"
            exit 0)
            [ $? -eq 0 ] || return 1
        fi
        if [ -n "$(echo ${SOURCE_PATH} | grep -i '^http')" ]; then
            (set -e
                wget -q -O - "${SOURCE_PATH}" | tar xJv -C "${CHROOT_DIR}"
            exit 0)
            [ $? -eq 0 ] || return 1
        fi
    fi
    [ $(ls "${CHROOT_DIR}" | wc -l) -le 1 ] && { msg " ...installation failed."; return 1; }

    container_mount
;;
*)
    msg "This Linux distribution is not supported."
    return 1
;;
esac

container_configure
[ $? -ne 0 ] && return 1

return 0
}

container_export()
{
local rootfs_archive="$1"
[ -z "${rootfs_archive}" ] && { msg "Incorrect export parameters."; return 1; }

container_mounted || container_mount root
[ $? -ne 0 ] && return 1

case "${rootfs_archive}" in
*gz)
    msg -n "Exporting rootfs as tar.gz archive ... "
    tar czvf "${rootfs_archive}" --one-file-system -C "${CHROOT_DIR}" .
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
;;
*bz2)
    msg -n "Exporting rootfs as tar.bz2 archive ... "
    tar cjvf "${rootfs_archive}" --one-file-system -C "${CHROOT_DIR}" .
    [ $? -eq 0 ] && msg "done" || { msg "fail"; return 1; }
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
msg $(cat "${ENV_DIR%/}/etc/version")

msg -n "BusyBox: "
msg $(busybox | busybox head -1 | busybox awk '{print $2}')

msg -n "Device: "
msg $(getprop ro.product.model || echo unknown)

msg -n "Android: "
msg $(getprop ro.build.version.release || echo unknown)

msg -n "Architecture: "
msg $(uname -m)

msg -n "Kernel: "
msg $(uname -r)

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

msg -n "Mounted system: "
local linux_version=$([ -r "${CHROOT_DIR}/etc/os-release" ] && . "${CHROOT_DIR}/etc/os-release"; [ -n "${PRETTY_NAME}" ] && echo "${PRETTY_NAME}" || echo "unknown")
msg "${linux_version}"

msg "Running services: "
msg -n "* SSH: "
ssh_started && msg "yes" || msg "no"
msg -n "* GUI: "
gui_started && msg "yes" || msg "no"

msg "Mounted parts on Linux: "
local is_mounted=0
for i in $(grep "${CHROOT_DIR}" /proc/mounts | awk '{print $2}' | sed "s|${CHROOT_DIR}/*|/|g")
do
    msg "* $i"
    local is_mounted=1
done
[ "${is_mounted}" -ne 1 ] && msg " ...nothing mounted"

msg "Available mount points: "
local is_mountpoints=0
for p in $(grep -v "${CHROOT_DIR}" /proc/mounts | grep ^/ | awk '{print $2":"$3}')
do
    local part=$(echo $p | awk -F: '{print $1}')
    local fstype=$(echo $p | awk -F: '{print $2}')
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
for i in /sys/block/*/dev
do
    if [ -f $i ]; then
        local devname=$(echo $i | sed -e 's@/dev@@' -e 's@.*/@@')
        [ -e "/dev/${devname}" ] && local devpath="/dev/${devname}"
        [ -e "/dev/block/${devname}" ] && local devpath="/dev/block/${devname}"
        [ -n "${devpath}" ] && local parts=$(fdisk -l ${devpath} | grep ^/dev/ | awk '{print $1}')
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
return 0
}

load_conf()
{
if [ -r "$1" ]; then
    . $1
    [ $? -ne 0 ] && exit 1
else
    echo "Configuration file not found."
    exit 1
fi
}

helper()
{
local version=$(cat "${ENV_DIR%/}/etc/version")

cat <<EOF
Linux Deploy ${version}
(c) 2012-2015 Anton Skshidlevsky, GPLv3

USAGE:
   linuxdeploy [OPTIONS] COMMAND ...

OPTIONS:
   -f FILE - configuration file
   -d - enable debug mode
   -t - enable trace mode

COMMANDS:
   prepare - create the disk image and make the file system
   install - begin a new installation of the distribution
   configure - configure or reconfigure a container
   mount - mount a container
   umount - unmount a container
   start - start services in the container
   stop - stop all services in the container
   shell [app] - execute application in the container, be default /bin/bash
   export <archive> - export the container as rootfs archive (tgz or tbz2)
   status - show information about the system

EOF
}

################################################################################

# init env
TERM="linux"
export PATH TERM
umask 0022

cd "${ENV_DIR}"

# load default config
CONF_FILE="${ENV_DIR%/}/etc/deploy.conf"
[ -e "${CONF_FILE}" ] && load_conf "${CONF_FILE}"

# parse options
while getopts :f:dt FLAG
do
    case ${FLAG} in
    f)
        CONF_FILE="${OPTARG}"
        load_conf "${CONF_FILE}"
    ;;
    d)
        DEBUG_MODE="1"
    ;;
    t)
        TRACE_MODE="1"
    ;;
    esac
done
shift $((OPTIND-1))

# exit if config not found
[ -e "${CONF_FILE}" ] || load_conf "${CONF_FILE}"

CHROOT_DIR="${CHROOT_DIR%/}"

# log level
[ "${DEBUG_MODE}" != "1" -a "${TRACE_MODE}" != "1" ] && { exec 2>/dev/null; }
[ "${TRACE_MODE}" = "1" ] && set -x

# exec command
case "$1" in
prepare)
    container_prepare
;;
install)
    container_install
;;
configure)
    container_configure
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
    container_stop
;;
shell)
    shift
    container_chroot "$@"
;;
export)
    container_export "$2"
;;
status)
    container_status
;;
*)
    helper
;;
esac
