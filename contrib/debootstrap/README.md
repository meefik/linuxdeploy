debootstrap Build Guide
=======================

debootstrap - bootstrap a basic Debian system.

#### Build instruction ####

1) Get debootstrap: 

    wget http://ftp.ru.debian.org/debian/pool/main/d/debootstrap/debootstrap_1.0.48+deb7u1_all.deb
    ar p debootstrap_1.0.48+deb7u1_all.deb data.tar.gz | tar xzf -

2) Apply patches:

    cd ./usr/sbin/debootstrap
    patch -Np1 ./usr/sbin/debootstrap ../linuxdeploy/contrib/debootstrap/debootstrap.diff

3) Copy debootstrap to linuxdeploy directory:

    cp ./usr/sbin/debootstrap ../linuxdeploy/assets/home/bin/debootstrap
    cp -r ./usr/share/debootstrap ../linuxdeploy/assets/home/deploy/debootstrap
    cd ../linuxdeploy/assets/home/deploy/debootstrap/ && mv devices.tar.gz devices.tgz

