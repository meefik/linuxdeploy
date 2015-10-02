tar Build Guide
==============================

tar - the GNU version of the tar archiving utility.

#### Build instruction ####

1) Prepare chroot environment (QEMU, Debian 7.0, armel).

2) Get tar:

    apt-get build-dep tar
    apt-get source tar

3) Build tar:

    cd ./tar-1.26
    export FORCE_UNSAFE_CONFIGURE=1
    ./configure
    make CFLAGS=-static LDFLAGS=-static
    strip -s ./src/tar

4) Copy tar binary to assets directory.
