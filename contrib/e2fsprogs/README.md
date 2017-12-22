e2fsprogs Build Guide
=====================

e2fsprogs - ext2/ext3/ext4 file system utilities.

#### Build instruction

1) Prepare chroot environment (QEMU, Debian 7.0, armel).

2) Get e2fsprogs (includes mke2fs and e2fsck):

```
apt-get build-dep e2fsprogs
apt-get source e2fsprogs
```

3) Apply patch:

```
patch ./e2fsprogs-1.42.5/lib/ext2fs/ismounted.c ./ismounted.patch
```

4) Build e2fsprogs:

```
cd ./e2fsprogs-1.42.5
./configure
make CFLAGS=-static LDFLAGS=-static
strip -s ./misc/mke2fs
strip -s ./e2fsck/e2fsck
```

5) Copy mke2fs and e2fsck binary to assets directory.

