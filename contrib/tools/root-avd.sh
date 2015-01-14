#!/bin/sh
[ -z "$1" ] && { echo "Usage: $0 <path/to/su>"; exit 1; }
alias adb=$HOME/android-sdk-linux/platform-tools/adb
adb shell "mount -o rw,remount /system"
adb shell "rm -rf /system/media/audio"
adb push "$1" /system/bin/su
adb shell "chown 0:0 /system/bin/su"
adb shell "chmod 6755 /system/bin/su"
