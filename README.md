Linux Deploy
============

Copyright (C) 2012-2015  Anton Skshidlevsky, [GPLv3](http://opensource.org/licenses/gpl-3.0.html)

This application is open source software for quick and easy installation of the operating system (OS) GNU/Linux on your Android device.

The application creates a disk image on a flash card, mounts it and installs an OS distribution. Applications of the new system are run in a chroot environment and working together with the Android platform. All changes made on the device are reversible, i.e. the application and components can be removed completely. Installation of a distribution is done by downloading files from official mirrors online over the internet. The application requires superuser rights (ROOT).

The program supports multi language interface. You can manage the process of installing the OS, and after installation, you can start and stop services of the new system (there is support for running your scripts) through the UI. The installation process is reported as text in the main application window. During the installation, the program will adjust the environment, which includes the base system, SSH server, VNC server and desktop environment. The program interface can also manage SSH and VNC settings.

Installing a new operating system takes about 30 minutes. The recommended minimum size of a disk image is 1024 MB (with LXDE), and without a GUI - 512 MB. When you install Linux on the flash card with the FAT32 file system, the image size should not exceed 4095 MB! After the initial setup the default password for SSH and VNC is changeme. The password can be changed through standard OS tools.

#### Features: ####
* Supported distributions: Debian, Ubuntu, Kali Linux, Arch Linux, Fedora, CentOS, Gentoo, openSUSE, Slackware, RootFS (tgz, tbz2, txz)
* Installation type: image file, disk partition, RAM, directory
* Supported file systems: ext2, ext3, ext4
* Supported architectures: ARM, ARM64, x86, x86_64, emulation mode (ARM ~ x86)
* Control interface: CLI, SSH, VNC, X, framebuffer
* Desktop environment: XTerm, LXDE, Xfce, GNOME, KDE, other (manual configuration)
* Supported languages: English, Russian, German, French, Italian, Spanish, Chinese, Vietnamese, Slovak, Portuguese

#### Install ####
You can install the app from Google Play: <https://play.google.com/store/apps/details?id=ru.meefik.linuxdeploy>.

#### Update ####
After updating the program you must perform: Settings -> Update ENV.

#### Performance ####
SD card read / write speed (10 class) on Android (Samsung Galaxy S II) for file systems vfat, ext2, ext4:
* **vfat**: read speed 14.1 MB/s; write speed 12.0 MB/s
* **ext2**: read speed 14.9 MB/s; write speed 3.9 MB/s
* **ext4**: read speed 14.9 MB/s; write speed 16.6 MB/s
* **ext2 (loop)**: read speed 17.0 MB/s; write speed 7.4 MB/s
* **ext4 (loop)**: read speed 17.2 MB/s; write speed 8.8 MB/s

Installation time and use space on disk (Debian wheezy/armhf on Samsung Galaxy S II):
* **Without GUI** ~ 0:12 / 260 MB
* **XTerm** ~ 0:14 / 290 MB
* **LXDE** ~ 0:19 / 450 MB
* **XFCE** ~ 0:20 / 495 MB
* **GNOME** ~ 0:55 / 1.3 GB
* **KDE** ~ 1:20 / 1.3 GB

#### Source code ####
Source code: <https://github.com/meefik/linuxdeploy>. The source code is written using the Android SDK/NDK and the Eclipse ADT plugin.

#### Donations ####
<http://meefik.github.io/donate>
