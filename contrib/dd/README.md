dd Build Guide
==============================

dd - convert and copy a file.

#### Build instruction ####

1) Prepare chroot environment (QEMU, Debian 7.0, armel).

2) Get coreutils (includes dd):

    apt-get build-dep coreutils
    apt-get source coreutils

3) Build dd:

    cd ./coreutils
    export CFLAGS=-static
    export LDLAGS=-static
    ./configure
    cd ./lib
    make
    cd ../src
    make version.h
    make dd

4) Remove all symbol and relocation information:

    strip -s dd

5) Copy dd binary to linuxdeploy directory:

    cp ./dd <LinuxDeploy>/assets/home/bin/dd
