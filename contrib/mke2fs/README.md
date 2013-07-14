mke2fs Build Guide
==============================

mke2fs - create an ext2/3 filesystem.

#### Build instruction ####

1) Prepare chroot environment (QEMU, Debian 7.0, armel).

2) Get e2fsprogs (includes mke2fs):

    apt-get build-dep e2fsprogs
    apt-get source e2fsprogs

3) Build mke2fs:

    cd ./e2fsprogs-1.42.5
    export CFLAGS=-static
    export LDLAGS=-static
    ./configure
    make

4) Copy mke2fs binary to linuxdeploy directory:

    cp ./misc/mke2fs <LinuxDeploy>/assets/home/bin/mke2fs

