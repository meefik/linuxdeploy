ar Build Guide
==============================

ar - create, modify, and extract from archives.

#### Build instruction ####

1) Prepare chroot environment (QEMU, Debian 7.0, armel).

2) Get binutils (includes ar):

    apt-get build-dep binutils
    apt-get source binutils

3) Build ar:

    cd ./binutils-2.22
    ./configure
    make CFLAGS=-static LDFLAGS=-static
    cd ./binutils
    gcc -W -Wall -Wstrict-prototypes -Wmissing-prototypes -Wshadow -Werror -o ar arparse.o arlex.o ar.o not-ranlib.o arsup.o rename.o binemul.o emul_vanilla.o bucomm.o version.o filemode.o  ../bfd/.libs/libbfd.a ../libiberty/libiberty.a -lz -static
    strip -s ar

4) Copy ar binary to assets directory.

