#!/bin/sh

HOME_DIR=`dirname $0`
. $HOME_DIR/deploy.conf

debian_install()
{
	debootstrap --no-check-gpg --arch $ARCH --foreign --extractor=dpkg-deb --include=locales $SUITE $CHROOT_DIR $MIRROR
	mkdir -p $CHROOT_DIR/usr/local/bin
	qemu_static=$(which qemu-arm-static)
	cp $qemu_static $CHROOT_DIR/usr/local/bin
	chroot $CHROOT_DIR /debootstrap/debootstrap --second-stage
}

debian_config()
{
	cp /etc/resolv.conf $CHROOT_DIR/etc/resolv.conf
	echo "deb $MIRROR $SUITE main contrib non-free" > $CHROOT_DIR/etc/apt/sources.list
	echo "deb-src $MIRROR $SUITE main contrib non-free" >> $CHROOT_DIR/etc/apt/sources.list
	[ -z "$(grep "^127.0.0.1" $CHROOT_DIR/etc/hosts)" ] && echo '127.0.0.1 localhost' >> $CHROOT_DIR/etc/hosts
	cp /etc/mtab $CHROOT_DIR/etc/mtab
	echo "en_US.UTF-8 UTF-8" > $CHROOT_DIR/etc/locale.gen
	echo "ru_RU.UTF-8 UTF-8" >> $CHROOT_DIR/etc/locale.gen
	chroot $CHROOT_DIR locale-gen en_US.UTF-8 ru_RU.UTF-8
	echo "LANG=en_US.UTF-8" > $CHROOT_DIR/etc/default/locale
	TZ=`cat /etc/timezone`
	echo "$TZ" > $CHROOT_DIR/etc/timezone
	cp $CHROOT_DIR/usr/share/zoneinfo/$TZ $CHROOT_DIR/etc/localtime
}

debian_mount()
{
	[ ! -d "$CHROOT_DIR" ] && mkdir $CHROOT_DIR
	[ -z "$(grep /proc/sys/fs/binfmt_misc /proc/mounts)" ] && mount binfmt_misc -t binfmt_misc /proc/sys/fs/binfmt_misc
	echo ':arm:M::\x7fELF\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x02\x00\x28\x00:\xff\xff\xff\xff\xff\xff\xff\x00\xff\xff\xff\xff\xff\xff\xff\xff\xfe\xff\xff\xff:/usr/local/bin/qemu-arm-static:' > /proc/sys/fs/binfmt_misc/register
	[ -z "$(grep ${CHROOT_DIR}/proc /proc/mounts)" ] && { [ ! -d ${CHROOT_DIR}/proc ] && mkdir ${CHROOT_DIR}/proc; mount -t proc none ${CHROOT_DIR}/proc; }
	[ -z "$(grep ${CHROOT_DIR}/dev /proc/mounts)" ] && { [ ! -d ${CHROOT_DIR}/dev ] && mkdir ${CHROOT_DIR}/dev; mount -o bind /dev ${CHROOT_DIR}/dev; }
	[ -z "$(grep ${CHROOT_DIR}/dev/pts /proc/mounts)" ] && { mount -o bind /dev/pts ${CHROOT_DIR}/dev/pts; }
	[ -z "$(grep ${CHROOT_DIR}/sys /proc/mounts)" ] && { [ ! -d ${CHROOT_DIR}/sys ] && mkdir ${CHROOT_DIR}/sys; mount -o bind /sys ${CHROOT_DIR}/sys; }
}

debian_stop()
{
	for i in 1 2 3 4
	do
		[ "$i" -gt "3" ] && exit 1
		pids=`lsof 2>/dev/null | grep $CHROOT_DIR | awk '{print $1}' | uniq || true`
		if [ -n "$pids" ]; then
			kill -9 $pids || true
			sleep 1
		else
			break
		fi
	done
}

debian_umount()
{
	umount ${CHROOT_DIR}/dev/pts
	umount ${CHROOT_DIR}/dev
	umount ${CHROOT_DIR}/sys
	umount ${CHROOT_DIR}/proc
	echo -1 > /proc/sys/fs/binfmt_misc/arm
	umount /proc/sys/fs/binfmt_misc
}

case "$1" in
deploy)
	echo ">>> deploy Debian $SUITE/$ARCH to $CHROOT_DIR ... "
	debian_mount
	debian_install
	debian_config
	debian_stop
	debian_umount
;;
chroot)
	echo ">>> chroot $CHROOT_DIR"
	debian_mount
	chroot ${CHROOT_DIR} /bin/bash
	debian_stop
	debian_umount
;;
*)
	echo "Usage: $0 {deploy|chroot}"
	exit 1
;;
esac

