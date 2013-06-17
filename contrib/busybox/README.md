BusyBox Build Guide
===================

BusyBox provides several stripped-down Unix tools in a single executable file.

#### Required ####
* BusyBox 1.20.2
* Android NDK r8b

#### Features ####
* special defconfig
* rpm2cpio lzma support
* fix mntent_r.c (mntent errors, undefined reference) 
* fix stat.c
* add missing_syscalls.c

#### Build instruction ####

1) Get BusyBox:

    wget http://www.busybox.net/downloads/busybox-1.20.2.tar.bz2
    tar xjf busybox-1.20.2.tar.bz2

2) Apply patches:

    cd busybox-1.20.2
    patch -Np1 < ../linuxdeploy/contrib/busybox/linuxdeploy.diff

3) Build BusyBox:

    export ANDROID_NDK_ROOT=~/android-ndk-r8b
    make linuxdeploy_defconfig && make

4) Copy busybox binary to linuxdeploy directory:

    cp ./busybox ../linuxdeploy/assets/home/bin/busybox


#### Development ####

Create patch command for developers:

    cd busybox
    diff -urN ../busybox-1.20.2.orig/ . > ~/tmp/linuxdeploy/contrib/busybox/linuxdeploy.diff

