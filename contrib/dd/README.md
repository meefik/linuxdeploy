dd Build Guide
==============================

dd - convert and copy a file.

#### Build instruction ####

1) Prepare chroot environment (QEMU, Debian 7.0, armel).

2) Get coreutils (includes dd):

    apt-get build-dep coreutils
    apt-get source coreutils

3) Build dd:

    cd ./coreutils-8.13
    export CFLAGS=-static
    export LDLAGS=-static
    ./configure
    cd ./lib
    make
    cd ../src
    make version.h
    make dd
    strip -s dd

4) Copy dd binary to assets directory.

