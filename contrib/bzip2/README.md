bzip2 Build Guide
==============================

bzip2 - a block-sorting file compressor.

#### Build instruction ####

1) Prepare chroot environment (QEMU, Debian 7.0, armel).

2) Get bzip2:

    apt-get build-dep bzip2
    apt-get source bzip2

3) Build bzip2:

    cd ./bzip2-1.0.6
    make CFLAGS=-static LDFLAGS=-static
    strip -s bzip2

4) Copy bzip2 binary to assets directory.
