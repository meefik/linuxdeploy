BusyBox Build Guide
===================

BusyBox provides several stripped-down Unix tools in a single executable file.

#### Required ####
* BusyBox 1.23.0
* Android NDK r10d

#### Features ####
* special defconfig
* rpm2cpio lzma support
* fix mntent_r.c (mntent errors, undefined reference) 
* add missing_syscalls.c

#### Build instruction ####

Use script bb-build.sh, for example:

    export ANDROID_NDK_ROOT="$HOME/android-ndk-r10d"
    ./bb-build.sh arm

#### Development ####

Create patch command for developers:

    cd busybox-1.23.0
    diff -urN ../busybox-1.23.0.orig/ . > ../busybox-1.23.0.patch

