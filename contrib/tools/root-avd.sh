#!/bin/sh
alias adb=$HOME/android-sdk-linux/platform-tools/adb
adb shell "mount -o rw,remount /system"
adb shell "rm -rf /system/media/audio"
adb push ./SuperSU-v2.0.1-x86 /system/bin/su
adb shell "chmod 6755 /system/bin/su"
