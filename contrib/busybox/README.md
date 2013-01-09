BusyBox Build Guide
===================

#### Features: ####
* special defconfig
* fix mntent errors (undefined reference) 
* rpm2cpio lzma support

#### Build instruction ####

1) Get BusyBox:

    git clone git://busybox.net/busybox.git

2) Apply patches:

    cd busybox
    patch -Np1 < ../linuxdeploy/contrib/busybox/linuxdeploy.path

3) Build BusyBox:

    export ANDROID_NDK_ROOT=~/android-ndk-r8d
    make linuxdeploy_defconfig && make

4) Copy busybox binary to linuxdeploy directory:

	cp ./busybox ../linuxdeploy/assets/home/bin/busybox


#### Development ####

Create patch command for developers:

    cd busybox
    diff -urN ../busybox.orig/ . > ../linuxdeploy/contrib/busybox/linuxdeploy.path
