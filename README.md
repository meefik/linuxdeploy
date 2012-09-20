Linux Deploy (English)
=====================

Copyright (C) 2012  Anton Skshidlevsky

Licensed under the [GPL version 3](http://www.gnu.org/licenses/) or later.

This application is open source software for quick and easy installation of the operating system (OS) GNU / Linux on your Android device.

The application creates a disk image on the flash card, mount it and install there OS distribution. Applications of the new system are run in a chroot environment and working in parallel with platform Android. All changes made on the device are reversible, ie the application and components can be removed completely. Installation of a distribution kit is carried out on a network from the official mirrors online. The application are required superuser rights (ROOT).

The program interface supports English and Russian. Through the interface you can manage the process of installing the OS, and after installation, you can start and stop services of the new system (there is support for running your scripts). The installation process is displayed as text reports in the main application window. During the installation, the program will adjust the work environment, which includes the base system, SSH server, VNC server and desktop environment LXDE. Also through the program interface to manage server settings SSH and VNC.

Installing a new operating system takes about 30 minutes. The recommended minimum size of a disk image 1024 MB, without a GUI - 512 MB. After the initial setup a default password for SSH and VNC - changeme. The password can be changed standard tools OS.

#### Features: ####
* Supported OS: Debian GNU / Linux
* Version: stable, testing, unstable (squeeze, wheezy, sid)
* Architecture: armel, armhf
* Installation type: loop device, SD card
* Supported file systems: ext2, ext4
* Control Interface: SSH, VNC
* Supported languages: English, Russian

#### Install ####
You can install the app from Google play: <https://play.google.com/store/apps/details?id=ru.meefik.linuxdeploy>.

#### Update ####
To perform after update: Settings -> Update ENV.

#### Source code ####
Source code: <https://github.com/meefik/linuxdeploy>. This source code are written with use Android SDK/NDK and Eclipse ADT plugin.

#### Donate ####
Donations: <http://meefik.github.com/linuxdeploy>


Linux Deploy (Русский)
======================

Copyright (C) 2012  Антон Скшидлевский

Лицензировано под [GPL версии 3](http://www.gnu.org/licenses/) или более поздней.

Это приложение с открытым исходным кодом для простой и быстрой установки операционной системы (ОС) GNU/Linux на Android устройство.

Приложение создает образ диска на флеш-карте, монтирует его и устанавливает туда дистрибутив ОС. Приложения из новой системы запускаются в chroot окружении параллельно со штатной работой платформы Android. Все вносимые изменения на устройстве обратимы, т.е. приложение и созданные им компоненты можно полностью удалить. Установка дистрибутива ОС осуществляется по сети с официальных зеркал в Интернете. Для работы приложению требуются привилегии суперпользователя (ROOT).

Интерфейс программы поддерживает английский и русский языки. Через интерфейс программы можно управлять процессом установки ОС, а после установки можно запускать и останавливать службы новой системы (есть поддержка запуска своих сценариев). Процесс установки отображается в виде текстовых отчетов в главном окне приложения. Во время установки программа сама настраивает рабочее окружение, которое включает в себя базовую систему, сервер SSH, сервер VNC и графическую среду LXDE. Также через интерфейс программы можно управлять параметрами сервера SSH и VNC.

Установка новой ОС занимает около 30 минут. Рекомендованный минимальный размер образа диска без графического интерфейса - 512 МБ, а с графическим интерфейсом - 1024 МБ. После начальной установки пароль для доступа по SSH и VNC назначается как "changeme", который можно сменить стандартными средствами ОС.

#### Характеристики: ####
* Поддерживаемая ОС: Debian GNU/Linux
* Версии дистрибутива: stable, testing, unstable (squeeze, wheezy, sid)
* Сборка под архитектуру: armel, armhf
* Тип установки: loop-файл, SD карта
* Поддерживаемые файловые системы: ext2, ext4
* Интерфейс управления: SSH, VNC
* Поддерживаемые языки: русский, английский

#### Установка ####

Вы можете установить это приложение из Google play: <https://play.google.com/store/apps/details?id=ru.meefik.linuxdeploy>.

#### Обновление ####

После обновления необходимо выполнить: Настройки -> Обновить окружение.

#### Исходный код ####

Исходный код доступен по адресу: <https://github.com/meefik/linuxdeploy>. Этот исходный код написан с использованием Android SDK/NDK и Eclipse ADT plugin.

#### Помощь проекту ####

Помочь проекту можно здесь: <http://meefik.github.com/linuxdeploy>.

