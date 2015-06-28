tar Build Guide
==============================

tar - the GNU version of the tar archiving utility.

#### Build instruction ####

1) Prepare chroot environment (QEMU, Debian 8.0, armel).

2) Get tar:

    apt-get build-dep tar
    apt-get source tar

3) Build tar:

    cd ./tar-1.27.1
    ./configure --without-posix-acls
    make CFLAGS=-static LDFLAGS=-static
    strip -s ./src/tar

4) Copy tar binary to assets directory.
