Linux Deploy (English)
=====================

Copyright (C) 2012-2015  Anton Skshidlevsky

Licensed under the [GPL version 3](http://www.gnu.org/licenses/) or later.

This application is open source software for quick and easy installation of the operating system (OS) GNU / Linux on your Android device.

The application creates a disk image on a flash card, mounts it and installs an OS distribution. Applications of the new system are run in a chroot environment and working together with the Android platform. All changes made on the device are reversible, i.e. the application and components can be removed completely. Installation of a distribution is done by downloading files from official mirrors online over the internet. The application requires superuser rights (ROOT).

The program interface supports English and Russian. You can manage the process of installing the OS, and after installation, you can start and stop services of the new system (there is support for running your scripts) through the UI. The installation process is reported as text in the main application window. During the installation, the program will adjust the environment, which includes the base system, SSH server, VNC server and desktop environment. The program interface can also manage SSH and VNC settings.

Installing a new operating system takes about 30 minutes. The recommended minimum size of a disk image is 1024 MB (with LXDE), and without a GUI - 512 MB. When you install Linux on the flash card with the FAT32 file system, the image size should not exceed 4095 MB! After the initial setup the default password for SSH and VNC is changeme. The password can be changed through standard OS tools.

#### Features: ####
* Supported distributions: Debian, Ubuntu, Kali Linux, Arch Linux, Fedora, Gentoo, openSUSE, Slackware, RootFS (tgz, tbz2, txz)
* Installation type: image file, disk partition, RAM, directory
* Supported file systems: ext2, ext3, ext4
* Supported architectures: ARM, ARM64, x86, x86_64, emulation mode (ARM ~ x86)
* Control interface: CLI, SSH, VNC, X, framebuffer
* Desktop environment: XTerm, LXDE, Xfce, GNOME, KDE, other (manual configuration)
* Supported languages: English, Russian, German, French, Italian, Spanish, Chinese, Vietnamese, Slovak

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
<http://meefik.github.io/donate.html>


Linux Deploy (Русский)
======================

Copyright (C) 2012-2015  Антон Скшидлевский

Лицензировано под [GPL версии 3](http://www.gnu.org/licenses/) или более поздней.

Это приложение с открытым исходным кодом для простой и быстрой установки операционной системы (ОС) GNU/Linux на Android устройство.

Приложение создает образ диска на флеш-карте, монтирует его и устанавливает туда дистрибутив ОС. Приложения из новой системы запускаются в chroot окружении параллельно со штатной работой платформы Android. Все вносимые изменения на устройстве обратимы, т.е. приложение и созданные им компоненты можно полностью удалить. Установка дистрибутива ОС осуществляется по сети с официальных зеркал в Интернете. Для работы приложению требуются привилегии суперпользователя (ROOT).

Интерфейс программы поддерживает русский и английский языки. Через интерфейс программы можно управлять процессом установки ОС, а после установки можно запускать и останавливать службы новой системы (есть поддержка запуска своих сценариев). Процесс установки отображается в виде текстовых отчетов в главном окне приложения. Во время установки программа сама настраивает рабочее окружение, которое включает в себя базовую систему, сервер SSH, сервер VNC и графическую среду на выбор. Также через интерфейс программы можно управлять параметрами сервера SSH и VNC.

Установка новой ОС занимает около 30 минут. Рекомендованный минимальный размер образа диска без графического интерфейса - 512 МБ, а с графическим интерфейсом - 1024 МБ (для LXDE). При установке ОС в образ на карту памяти с файловой системой FAT32 размер образа не должен превышать 4095 МБ! После начальной установки пароль для доступа по SSH и VNC назначается как "changeme", который можно сменить стандартными средствами ОС.

#### Характеристики: ####
* Поддерживаемые дистрибутивы: Debian, Ubuntu, Kali Linux, Arch Linux, Fedora, Gentoo, openSUSE, Slackware, RootFS (tgz, tbz2, txz)
* Тип установки: файл образа, раздел диска, оперативная память, директория
* Поддерживаемые файловые системы: ext2, ext3, ext4
* Поддерживаемые архитектуры: ARM, ARM64, x86, x86_64, режим эмуляции (ARM ~ x86)
* Интерфейс управления: CLI, SSH, VNC, X, фрейм-буфер
* Окружение рабочего стола: XTerm, LXDE, Xfce, GNOME, KDE, другое (ручная настройка)
* Поддерживаемые языки: русский, английский, немецкий, французский, итальянский, испанский, китайский, вьетнамский, словацкий

#### Установка ####
Вы можете установить это приложение из Google play: <https://play.google.com/store/apps/details?id=ru.meefik.linuxdeploy>.

#### Обновление ####
После обновления необходимо выполнить: Настройки -> Обновить окружение.

#### Производительность ####
Скорость чтения/записи SD карты (10 класс) на Android (Samsung Galaxy S II) для файловых систем vfat, ext2, ext4:
* **vfat**: скорость чтения 14.1 МБ/с; скорость записи 12.0 МБ/с
* **ext2**: скорость чтения 14.9 МБ/с; скорость записи 3.9 МБ/с
* **ext4**: скорость чтения 14.9 МБ/с; скорость записи 16.6 МБ/с
* **ext2 (loop)**: скорость чтения 17.0 МБ/с; скорость записи 7.4 МБ/с
* **ext4 (loop)**: скорость чтения 17.2 МБ/с; скорость записи 8.8 МБ/с

Время установки и занимаемое место на диске (Debian wheezy/armhf на Samsung Galaxy S II):
* **Без графики** ~ 0:12 / 260 МБ
* **XTerm** ~ 0:14 / 290 МБ
* **LXDE** ~ 0:19 / 450 МБ
* **XFCE** ~ 0:20 / 495 МБ
* **GNOME** ~ 0:55 / 1.3 ГБ
* **KDE** ~ 1:20 / 1.3 ГБ

#### Исходный код ####
Исходный код доступен по адресу: <https://github.com/meefik/linuxdeploy>. Этот исходный код написан с использованием Android SDK/NDK и Eclipse ADT plugin.

#### Поддержать проект ####
<http://meefik.github.io/donate.html>
