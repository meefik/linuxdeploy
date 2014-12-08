# Setup
See https://github.com/Efreak/apt-cacher-ng for info on apt-cacher-ng and link to original source.
Install cygwin. You need automake, libtool, gcc, cmake, zlib, liblzma, libbz2, openssl dev, make, gcc, etc.
As you can see, I'm just symlinking the /usr/lib/apt-cacher-ng directory; if you wish to configure it properly, go ahead. I didn't bother.

# Compiling
````bash
wget --content-disposition https://github.com/Efreak/apt-cacher-ng/archive/upstream/0.8.0_rc4.tar.gz
tar -xzvf apt-cacher-ng-upstream-0.8.0_rc4.tar.gz
cd apt-cacher-ng-upstream-0.8.0_rc4
make
ln -s /etc/apt-cacher-ng /usr/lib/apt-cacher-ng
````

# Configuring:
## Main Config File
edit /etc/apt-cacher-ng/acng.conf and change/set the following settings (leave the others as-is)
````
BindAddress: 0.0.0.0
Remap-debrep: file:deb_mirror*.gz /debian ; file:backends_debian # Debian Archives
Remap-uburep: file:ubuntu_mirrors /ubuntu ; file:backends_ubuntu # Ubuntu Archives
Remap-armdebrep: file:armdeb_mirror*.gz /armdebian ; file:backends_armdebian # Debian archives for armhf
Remap-armuburep: file:armubuntu_mirrors /armubuntu ; file:backends_armubuntu # ubuntu archives for armhf
Remap-armkali: file:armkali_mirrors /armkali ; file:backends_armkali # kali archives for armhf

PrecacheFor: debrep/dists/wheezy/*/binary-armhf/Packages* armkali/dists/kali*/*/binary-armhf/Packages* armubuntu/dists/trusty/*/binary-armhf/Packages* armubuntu/dists/saucy/*/binary-armhf/Packages*
````
You probably wish to change the PrecacheFor line.

## Backends and mirrors
#### Contents of backends_armdebian:
I tested the list of debian mirrors for armhf packages. Those listed below should work.
````
ftp://ftp.us.debian.org/debian/
http://ftp.us.debian.org/debian/
ftp://ftp-mirror.internap.com/pub/debian/
http://ftp-mirror.internap.com/pub/debian/
ftp://mirrors.kernel.org/debian/
http://mirrors.kernel.org/debian/
ftp://debian.csail.mit.edu/debian/
http://debian.csail.mit.edu/debian/
ftp://debian.osuosl.org/debian/
http://debian.osuosl.org/debian
````
###Contents of armdeb_mirrors.gz
````
ftp://ftp.us.debian.org/debian/
http://ftp.us.debian.org/debian/
ftp://ftp-mirror.internap.com/pub/debian/
http://ftp-mirror.internap.com/pub/debian/
ftp://mirrors.kernel.org/debian/
http://mirrors.kernel.org/debian/
ftp://debian.csail.mit.edu/debian/
http://debian.csail.mit.edu/debian/
ftp://debian.osuosl.org/debian/
http://debian.osuosl.org/debian/
````
### Contents of armkali_mirrors
````
http://http.kali.org/kali
````
### Contents of backends_armkali:
````
http://http.kali.org/kali
````
### Contents of backends_armkali.default:
````
http://http.kali.org/kali
I tried testing standard ubuntu mirrors for armhf binaries, but I don't remember the results, and it took too long to do it again.
````
### Contents of backends_armubuntu:
````
http://ports.ubuntu.com
````
### Contents of armubuntu_mirrors:
````
http://ports.ubuntu.com
````
### Contents of backends_armubuntu.default:
````
http://ports.ubuntu.com
````
#Running apt-cacher-ng
`apt-cacher-ng -c /etc/apt-cacher-ng`
