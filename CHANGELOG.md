# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.6.0] - 2020-02-01
### Changed
- Set target version of SDK to 28 for binary execution in Android Q
- UI refactoring (@Iscle)
- Minimum Android API set to 21

## [2.5.1] - 2019-11-04
### Fixed
- Fixed Arch Linux package management

### Changed
- Changed a format for privileged users to UID:GID

## [2.5.0] - 2019-11-04
### Added
- Added compatibility with Android 10
- Added support for Docker images

### Fixed
- Fixed URL of Ubuntu repository
- Fixed Arch Linux deprecations

### Changed
- Updated built-in busybox to v1.31.1

### Removed
- Removed PRoot support
- Removed installing symlink to /system/bin

## [2.4.1] - 2019-10-20
### Added
- Added power trigger

## [2.4.0] - 2019-08-12
### Added
- Added support Alpine Linux
- Added support for Slackware x86_64
- Added built-in binaries for 64-bit architecture
- Added NET_TRIGGER in the properties dialog

### Changed
- Added executable permission for application directory
- Updated built-in busybox to v1.30.1

### Removed
- End of support for Gentoo

## [2.3.1] - 2019-07-29
### Added
- Added support Debian 10 (buster)

### Fixed
- Fixed problem with network trigger on Android 7+

## [2.3.0] - 2019-03-02
### Changed
- Code refactoring and migrated to AndroidX (issue #1058)

### Fixed
- Fixed start on boot on Android 8.1 (issue #1041)
- Fixed notification channels (issue #1059)
- Fixed Arch Linux bootstrap (issue #1055)

## [2.2.2] - 2018-11-25
### Changed
- Have separate source/target inputs for mounts (issue #1019)
- Replaced by dbus-run-session to dbus-launch

### Fixed
- Fixed problem with running "am" via unchroot (issue #987)
- Fixed problem with color in the list of mount points (issue #1018)

## [2.2.1] - 2018-10-29
### Changed
- Updated built-in busybox to v1.29.3

### Fixed
- Added package zstd for Arch Linux bootstrap

## [2.2.0] - 2018-08-25
### Changed
- All CLI files are moved to the environment directory
- Automatic start of httpd and telnetd after Android reboot
- Automatically change an interpreter in scripts after changing the environment directory
- Updated built-in busybox to v1.29.2
- Improved Arch Linux bootstrap
- The web terminal now opens in the Chrome custom tab (increases API to 15)

### Fixed
- Fixed problem with UID and GID numeration
- Fixed problem with lxpolkit (issue #978)
- Fixed problem with randomly disconnecting web terminal
- Access rules for web terminal by default "A:127.0.0.1 D:*"

## [2.1.1] - 2018-08-20
### Added
- Added support Fedora 28

### Changed
- Improved web terminal

### Removed
- End of support for Fedora 27 and below

### Fixed
- Fixed support Arch Linux i686 (http://mirror.archlinux32.org)
- Fixed problem with initial installation of Arch Linux (issue #986)
- Fixed CentOS bootstrap (issues #972)
- Fixed problem with NSS and systemd (issue #971)
- Fixed problem with unchroot am

## [2.1.0] - 2018-05-07
### Added
- Added support Ubuntu 18.04 LTS (bionic)

### Removed
- Disabled auto changing SELinux mode
- End of support for Android is below version 4.0

## [2.0.7] - 2018-04-25
### Changed
- Improved Gentoo bootstrap

### Fixed
- Fixed problem with seccomp (issue #869)

## [2.0.6] - 2018-02-17
### Added
- Added support for PulseAudio

### Changed
- Improved mount and unmount functions
- Force make a filesystem

## [2.0.5] - 2017-12-23
### Added
- Enabled journal for FS ext3/4, ext2 without journal
- Added an attempt to re-unmount in case of failure
- The e2fsck utility is included in the assembly
- START and STOP buttons in notification
- Show app after five taps on notification in stealth mode

### Fixed
- Fixed problem with running dbus in Fedora
- Fixed issue of creating a FS (mke2fs) when using busybox from system (issue #891)

## [2.0.4] - 2017-11-29
### Added
- Added support Fedora 26 and 27

### Changed
- Updated Arch Linux bootstrap

### Removed
- openSUSE end of support

## [2.0.3] - 2017-10-05
### Added
- Added GNU dd utility (issue #729)
- Added support for arm64 architecture in Gentoo
- Translated into Indonesian (issue #851)

### Fixed
- Fixed problem with set the variable name using the "config" command

## [2.0.2] - 2017-05-10
### Added
- Added file system check (issue #785)
- Added custom target type

### Changed
- Updated Arch Linux bootstrap (+libnghttp2, -xorg-utils)

### Fixed
- Fixed an architecture emulation via binfmt_misc
- Fixed issue of launching Xfce and Mate (issue #807)
- Fixed problem with showing menu button (issue #693)

## [2.0.1] - 2017-01-23
### Added
- Added Fedora 25 (issue #683)

### Changed
- Updated Arch Linux bootstrap (issue #699)

### Fixed
- Fixed Fedora and CentOS bootstrap (issue #646)

## [2.0.0] - 2016-08-17
### Added
- Added support Debian 9 (stretch), Fedora 24 and Ubuntu 16.10 (yakkety)
- Added support for work without root permissions (based on PRoot)
- Added interface for managing from TELNET (based on telnetd)
- Added web interface (based on httpd, websocket.sh and xterm.js)
- Added synchronization of the operating environment with server or other device
- Added function to generate a random password for user (instead changeme)
- Added function to track changes in the network and update the current container
- Added ability to add users to groups aid_ * from the application interface
- Added support for initialization systems run-parts and sysv
- Added an option to keep CPU running to do work while the screen is off
- Added an option to change the delay of start after opening XServer XSDL
- Added stealth mode
- Added ability to send notifications from a container through am
- Added MATE desktop environment
- Added autostart delay
- Created a repository of containers

### Changed
- The application interface is rewritten with Material Design
- Completed transition to the new CLI
- Extended syntax description of mount points, you can now specify to mount /from/dir:/to/dir
- Make an unified container profiles for CLI and the application interface
- Restore Android after closing Xorg session in framebuffer mode

### Removed
- Removed GNOME and KDE desktop environments

## [1.5.6] - 2016-02-16
### Added
- Added support kali-rolling in Kali Linux
- Added target type "Auto" for manual prepare target path
- Added timestamp support to log filename (${TIMESTAMP} variable)
- Added support /dev/net/tun (for OpenVPN)

### Changed
- Automatically switch to root (unchroot)
- Mount root with options "exec,suid,dev"

### Removed
- End of support Debian Squeeze and Kali Moto

### Fixed
- Fixed Arch Linux packages (close #529)

## [1.5.5] - 2015-12-26
### Added
- Added support CentOS
- Open a shell from Android Terminal Emulator
- Translated into Korean

## [1.5.4] - 2015-10-10
### Added
- Added support system BusyBox, a built-in is no longer supported
- Added option to display a time stamp
- Translated into Portuguese

### Changed
- Improved support for framebuffer

### Fixed
- Fixed a problem with user password that contains uppercase letters

## [1.5.3] - 2015-09-11
### Added
- Added an option to change the chroot directory
- Added unchroot script
- Implemented integration with Android
- Implemented integration with XServer XSDL
- Translated into Slovak and French
- Automatically select the language

## [1.5.2] - 2015-08-18
### Added
- Added support Slackware
- Added support Kali Linux 2.0 (sana)
- Added support tar.xz for RootFS installation
- Added support x86_64 (amd64) distributions
- Added export a container as rootfs archive (tgz or tbz2)
- Added ability to change password from GUI
- Added support aarch64 for Arch Linux
- Translated into Vietnamese

### Changed
- Modified update ENV mechanism
- Updated command line interface
- Updated root checker

### Fixed
- Fixed autostart problem

## [1.5.1] - 2015-05-20
### Added
- Added support for installation type in RAM
- Added fix for tty0 (use X options: "-sharevts vt0")
- Added handling for shutdown event
- Added support Ubuntu 15.10 (Wily Werewolf)
- Added support Fedora 22 (armhfp only)
- Added more locales
- Translated into German, Italian, Spanish and Chinese

### Changed
- Updated command "status"
- Updated security groups

## [1.5.0] - 2015-01-10
### Added
- Added support for devices with an Intel architecture
- Added support for cross-architecture installation x86 <-> ARM based on QEMU (binfmt_misc require support in the kernel)
- Added support Ubuntu 15.04 (Vivid Vervet)
- Added suits kali-dev and kali-rolling in Kali Linux
- Added support for third-party BusyBox (required /system/xbin/ash)
- Added support Ubuntu 14.10 (Utopic Unicorn)
- Added support arm64/aarch64 for some distributions

### Changed
- Updated built-in BusyBox to version 1.23.0 (builds with PIE and without PIE)
- Updated linuxdeploy shell

### Fixed
- Bugfix "Couldn't find these debs: 0"
- Fixed Gentoo installation problems

## [1.4.8] - 2014-12-09
### Added
- Added support Android 5 (PIE support)
- Added support Fedora 21 (armhfp only)

### Fixed
- Fixed LD_PRELOAD errors

## [1.4.7] - 2014-11-22
### Fixed
- Fixed packages list of Arch Linux (issue #161)
- Fixed error with VNC_ARGS (issue #153)

### Changed
- Added root to the group aid_inet be default (ticket #135)
- Updated openSUSE bootstrap (ticket #154)
- Save and restore SELinux state (ticket #167)

## [1.4.6] - 2014-06-22
### Added
- Added option to disable the built-in shell
- Added sync in framebuffer
- Added ACCESS_SUPERUSER permission

### Changed
- Operating environment by default: /data/data/ru.meefik.linuxdeploy/linux

### Fixed
- Fixed packages list of Arch Linux (issue #136)
- Fixed packages list of openSUSE

## [1.4.5] - 2014-04-21
### Added
- Added support SELinux
- Added support Ubuntu 14.04 (Trusty Tahr)

### Changed
- BusyBox dynamically linked (fixed DNS resolver)

### Fixed
- Fixed X Window System startup
- Fixed dbus configuration

## [1.4.4] - 2014-01-16
### Added 
- Added support Fedora 20 (armhfp)
- Added VNC options

### Fixed
- Fixed broken packages in Arch Linux
- Bug fixes

## [1.4.3] - 2013-10-31
### Added
- Added support Ubuntu 13.10 (Saucy Salamander)
- Added three modes Android UI freeze
- Added autostart dbus in X-server mode and framebuffer
- Added an property "X options" for framebuffer

### Changed
- Change tightvnc to tigervnc in Arch Linux

### Fixed
- Fixed list of packages openSUSE installation

## [1.4.2] - 2013-09-11
### Added
- Added support Debian 8.0 (jessie)
- Added ability to choose components to install

### Changed
- Interface improvements

## [1.4.1] - 2013-06-26
### Added
- Added support for installation from rootfs archives (tgz, tbz2)
- Added custom installation type (install without make new image file and filesystem)

### Changed
- Improved mechanism of disk image file creation (fast re-creation)
- Improved framebuffer support

### Fixed
- Changing font size

## [1.4.0] - 2013-06-17
### Added
- Added function of import/export profiles
- Added support Fedora 19
- Added support Android 2.1 (API 7)

### Changed
- Updated list of packages for openSUSE (armv7hl)
- Improved command line utility linuxdeploy
- Automatic detection of screen resolution (for VNC)

## [1.3.9] - 2013-06-12
### Added
- Added support Gentoo (installation requires at least 4 GB of free space)

### Changed
- Limitation of scroll buffer size
- Automatic mount /dev/shm
- Updated debootstrap up to 1.0.48
- Support for multiple input devices in framebuffer mode (in xorg.conf changed lines are marked as #linuxdeploy)
- Hide icon in notification bar after you exit the application
- Solved problem with running SSH in Arch Linux
- Updated Arch Linux bootstrap

### Fixed
- Fixed conflict with SSH applications in Android

## [1.3.8]
### Added
- Added support Ubuntu 13.04 Raring Ringtail
- Added option for stop Android UI in framebuffer mode
- Added Wi-Fi lock
- Added support armv5tel architecture for openSUSE (VNC now works)

### Fixed
- Fixed problem with autostart

## [1.3.7]
### Added
- Added support Kali Linux
- Added ability to display GUI through framebuffer

### Fixed
- Fixed list of packages openSUSE installation
- Fixed pacman error "invalid option"

## [1.3.6]
### Added
- Added support Fedora 18
- Added application icon in notification bar

### Changed
- Updated openSUSE bootstrap
- Updated Arch Linux bootstrap

### Fixed
- Dbus error in Arch Linux and Fedora
- Wrong mount mode for /dev/pts

## [1.3.5]
### Added
- Added check superuser permissions (root)

### Changed
- Redesigned interface (include ActionBarSherlock)
- Improved autostart feature
- Automatic update operating environment
- Double tap for select profile

### Fixed
- Fixed incorrect use of default parameters

## [1.3.4]
### Added
- Added command status to linuxdeploy script
- Added function startup GNU/Linux when running Android (Settings -> Autostart)
- Added DPI property for VNC

### Changed
- Excluded lost package insserv form openSUSE installation
- Improved algorithm displaying reports

## [1.3.3]
### Added
- Added xorg-fonts-misc package in Arch Linux GUI installation

### Fixed
- Dbus error in Debian/Ubuntu
- Problem with update configuration file after change profile

## [1.3.2]
### Added
- Added support openSUSE installation

### Changed
- Improved Arch Linux mirrors settings
- Updated built-in BusyBox
- Configuration file is updated only when the parameters are updated
- Script linuxdeploy can now be run from Android console
- Now can create a symbolic link to linuxdeploy in /system/bin (Settings -> Create symlink)
- Notification of need to update the operating environment

### Fixed
- Wrong information size of image file in automatic calculation mode
- Crashes when VNC display not integer number

## [1.3.1]
### Added
- Added support Fedora installation

### Changed
- Improved Arch Linux locale support

## [1.3.0]
### Added
- Added support Arch Linux installation
- Added automatic calculation of image size (90% of free space, but not more than 4095 MB)
- Added subitem desktop environment "Other" (for manual settings)

### Changed
- Interface improvements

## [1.2.9]
### Added
- Added support ext3 file system

### Changed
- Improved preferences organization
- Improved automatic detection of file system
- Optimize of making disk image file

### Fixed
- sudo, upstart, gnome-session, XKL_XMODMAP_DISABLE

## [1.2.8]
### Added
- Added support installation in directory
- Added checking mount points before installation

### Changed
- Rebuilding image file at change the size
- Improved custom mounting

### Fixed
- Fixed problem with installing BusyBox on some devices

## [1.2.7]
### Added
- Added support X Window System (startup GUI over X server)
- Added automatic DNS configuration
- Added support Unity desktop environment

### Fixed
- Fixed incorrect startup GNOME session on Ubuntu

## [1.2.6]
### Added
- Added support Ubuntu installation
- Added information of active Linux system in SysInfo

### Changed
- Improved wake lock

### Fixed
- Fixed incorrect profile management

## [1.2.5]
### Added
- Added support for profiles
- Added select for file system
- Added separate localization settings

### Changed
- Change TWM to XTerm

## [1.2.4]
### Added
- Added information of available mount points and partitions in SysInfo
- Added ability to select and copy text in the main window (Android 3+)

## [1.2.3]
### Added
- Added themes support (dark and light)

## [1.2.2]
### Changed
- Improved resources release function
- Improved auto-scrolling

## [1.2.1]
### Changed
- Default user name changed to "android"

### Fixed
- Fixed error of set user password on third-party images

## [1.2.0]
### Changed
- Improved function of the system configuration for third-party images

## [1.1.9]
### Added
- Added ability to select a desktop environment (TWM, LXDE, XFCE, GNOME, KDE)
- Added support for startup as root
- Added check for reserved usernames
- Added ability to change DNS server

## [1.1.8]
### Added
- Support mounting custom partitions from /dev (e.g. /dev/block/sda1)

### Changed
- Interface improvements

## [1.1.7]
### Added
- Support for architectures ARMv5TE and above
- Added feature mounting custom partitions
- Added feature to run multiple custom scripts

## [1.1.6]
### Added
- Added tracing mode
- Added feature save logs to file

### Fixed
- Fixed error of starting custom script

## [1.1.5]
### Changed
- Updated built-in BusyBox up to v1.21.0

### Fixed
- Fixed conflict with existing BusyBox

## [1.1.4]
### Changed
- Improved WiFi control
- Interface improvements

## [1.1.3]
### Fixed
- Fixed error of updating the operating environment on some devices

## [1.1.2]
### Added
- Added support for ext4 file system

## [1.1.1]
### Fixed
- Fixed error of making new user during the installation of Debian Squeeze (it also error starting VNC server)

## [1.1.0]
### Added
- Added ability to install OS on SD card (without loop device)
- Added debug mode

## [1.0.9]
### Changed
- Language of OS matched the language of interface

## [1.0.8]
### Fixed
- Fixed error of make a disk image larger than 1 GB

## [1.0.7]
### Added
- Added script <INSTALL_DIR>/bin/linuxchroot
- Added information of mounted parts in SysInfo

## [1.0.6]
### Added
- Added information of storages in SysInfo

## [1.0.5]
### Added
- ARMEL/ARMHF support for SFTP (sftp-server)

## [1.0.4]
### Fixed
- Incomplete removal

### Changed
- Interface improvements

## [1.0.3]
### Added
- Added function to reconfigure the system

### Changed
- Improved backlight control

### Fixed
- Fixed problem with permissions via SFTP from Linux

## [1.0.2]
### Fixed
- Check SSH is running in SysInfo

## [1.0.1]
### Added
- Added information about ports on the title bar
 
### Changed
- Updated packages list of base system installation
 
### Fixed
- Fixed error of disk image creation
