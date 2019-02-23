# Linux Deploy

Copyright (C) 2012-2019  Anton Skshidlevsky, [GPLv3](https://github.com/meefik/linuxdeploy/blob/master/LICENSE)

This application is open source software for quick and easy installation of the operating system (OS) GNU/Linux on your Android device.

The application creates a disk image or a dictory on a flash card or uses a partition or RAM, mounts it and installs an OS distribution. Applications of the new system are run in a chroot environment and working together with the Android platform. All changes made on the device are reversible, i.e. the application and components can be removed completely. Installation of a distribution is done by downloading files from official mirrors online over the internet. The application can run better with superuser rights (ROOT).

The program supports multi language interface. You can manage the process of installing the OS, and after installation, you can start and stop services of the new system (there is support for running your scripts) through the UI. The installation process is reported as text in the main application window. During the installation, the program will adjust the environment, which includes the base system, SSH server, VNC server and desktop environment. The program interface can also manage SSH and VNC settings.

Installing a new operating system takes about 15 minutes. The recommended minimum size of a disk image is 1024 MB (with LXDE), and without a GUI - 512 MB. When you install Linux on the flash card with the FAT32 file system, the image size should not exceed 4095 MB! After the initial setup the password for SSH and VNC generated automatically. The password can be changed through "Properties -> User password" or standard OS tools (passwd, vncpasswd).

The app is available for download in Google Play and GitHub.

<a href="https://play.google.com/store/apps/details?id=ru.meefik.linuxdeploy"><img src="https://gist.githubusercontent.com/meefik/54a54afa7cc1dc600bdb855cb7895a4a/raw/ad617c006a1ac28d067c9a87cec60199ca8fef7c/get-it-on-google-play.png" alt="Get it on Google Play"></a>
<a href="https://github.com/meefik/linuxdeploy/releases/latest"><img src="https://gist.githubusercontent.com/meefik/54a54afa7cc1dc600bdb855cb7895a4a/raw/ad617c006a1ac28d067c9a87cec60199ca8fef7c/get-apk-from-github.png" alt="Get it on Github"></a>

## Features

- Supported distributions: Debian, Ubuntu, Kali Linux, Arch Linux, Fedora, CentOS, Gentoo, Slackware, RootFS (tgz, tbz2, txz)
- Installation type: image file, directory, disk partition, RAM
- Supported file systems: ext2, ext3, ext4
- Supported architectures: ARM, ARM64, x86, x86_64, emulation mode (ARM ~ x86)
- Control interface: CLI, SSH, VNC, X11, Framebuffer
- Desktop environment: XTerm, LXDE, Xfce, MATE, other (manual configuration)
- Supported languages: multilingual interface

## FAQ

> Do not work update operating environment or errors appear in debug mode: "Permission denied", "Socket operation on non-socket" or other.

Install compatible [BusyBox](https://github.com/meefik/busybox/releases) in /system/xbin, add path /system/xbin in "Settings -> PATH variable", update the operating environment "Settings -> Update ENV". Before upgrading the environment, it is desirable restart the device. After that, the container options must be selected "Properties -> File system -> Auto" and "Propetries -> Image size (MB) -> 2000", because "busybox mke2fs" is not supperted an option "-t" to specify type of file system and not supperted image greater 2 GB. Now you can start a new installation "Menu -> Install".

> Making an image on sdcard return an error "Read-only file system".

If you are using SuperSU utility you need to uncheck "mount namespace separation" in SuperSU settings. See [documentation](https://su.chainfire.eu/#how-mount).

> Installing an application on Google Play fails with the message "Unknown error code during application installation: -24".

You need to remove the application directory: /data/data/ru.meefik.linuxdeploy

## Performance

SD card read / write speed (10 class) on Android (Samsung Galaxy S II) for file systems vfat, ext2, ext4:
- **vfat**: read speed 14.1 MB/s; write speed 12.0 MB/s
- **ext2**: read speed 14.9 MB/s; write speed 3.9 MB/s
- **ext4**: read speed 14.9 MB/s; write speed 16.6 MB/s
- **ext2 (loop)**: read speed 17.0 MB/s; write speed 7.4 MB/s
- **ext4 (loop)**: read speed 17.2 MB/s; write speed 8.8 MB/s

Installation time and use space on disk (Debian wheezy/armhf on Samsung Galaxy S II):
- **Without GUI** ~ 0:12 / 260 MB
- **XTerm** ~ 0:14 / 290 MB
- **LXDE** ~ 0:19 / 450 MB
- **XFCE** ~ 0:20 / 495 MB
- **GNOME** ~ 0:55 / 1.3 GB
- **KDE** ~ 1:20 / 1.3 GB

## Links

Source code: 

- Linux Deploy App: <https://github.com/meefik/linuxdeploy>
- Linux Deploy CLI: <https://github.com/meefik/linuxdeploy-cli>

Donations:

- E-Money: <http://meefik.github.io/donate>
- Google Play: <https://play.google.com/store/apps/details?id=ru.meefik.donate>
