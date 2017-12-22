dd Build Guide
==============

dd - convert and copy a file.

#### Build instruction

1) Prepare chroot environment (QEMU, Debian 7.0, armel).

2) Get coreutils (includes dd):
```
apt-get build-dep coreutils
apt-get source coreutils
```

3) Build coreutils:
```
cd ./coreutils-8.13
./configure
make CFLAGS=-static LDFLAGS=-static
strip -s ./src/dd
```

4) Copy dd binary to assets directory.

