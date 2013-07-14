bzip2 Build Guide
==============================

bzip2 - a block-sorting file compressor.

#### Build instruction ####

1) Prepare chroot environment (QEMU, Debian 7.0, armel).

2) Get bzip2:

    apt-get build-dep bzip2
    apt-get source bzip2

3) Build bzip2:

    cd ./bzip2
    export CFLAGS=-static
    export LDLAGS=-static
    ./configure
    make

4) Copy bzip2 binary to linuxdeploy directory:

    cp ./bzip2 <LinuxDeploy>/assets/home/bin/bzip2-full
