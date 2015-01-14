#!/bin/sh

BB_VERSION="1.23.0"
ANDROID_NATIVE_API_LEVEL="android-9"
MARCH="$1"
PIE="$2"
[ -z "$MARCH" ] && { echo "Usage: $0 <arm|intel|mips> [pie]"; exit 1; }
[ -z "$ANDROID_NDK_ROOT" ] && ANDROID_NDK_ROOT="$HOME/android-ndk-r10d"
HOST_ARCH=$(uname -m)
NCPU=$(grep -ci processor /proc/cpuinfo)
PREFIX="../bb-$MARCH"
[ -n "$PIE" ] && PREFIX="$PREFIX-pie"

pkg="busybox-$BB_VERSION"

echo ">>> download"
[ -e "$pkg.tar.bz2" ] || wget --progress=dot http://busybox.net/downloads/$pkg.tar.bz2 || exit 1

echo ">>> unpack"
[ -d "$pkg" ] && rm -rf $pkg
tar jvxf $pkg.tar.bz2 || exit 1

echo ">>> patch"
cd $pkg
patch -b -p0 < ../$pkg.patch || exit 1

echo ">>> config"
case "$MARCH" in
arm)
	CONFIG_CROSS_COMPILER_PREFIX="$ANDROID_NDK_ROOT/toolchains/arm-linux-androideabi-4.6/prebuilt/linux-$HOST_ARCH/bin/arm-linux-androideabi-"
	CONFIG_SYSROOT="$ANDROID_NDK_ROOT/platforms/$ANDROID_NATIVE_API_LEVEL/arch-arm"
	CONFIG_EXTRA_CFLAGS="-fsigned-char -march=armv5te -mtune=xscale -msoft-float -fdata-sections -ffunction-sections -fexceptions -mthumb -fPIC -Wno-psabi -DANDROID -D__ARM_ARCH_5__ -D__ARM_ARCH_5T__ -D__ARM_ARCH_5E__ -D__ARM_ARCH_5TE__ -fomit-frame-pointer $CFLAGS"
	CONFIG_EXTRA_LDFLAGS="-Xlinker -z -Xlinker muldefs -nostdlib -Bdynamic -Xlinker -dynamic-linker -Xlinker /system/bin/linker -Xlinker -z -Xlinker nocopyreloc -Xlinker --no-undefined \${SYSROOT}/usr/lib/crtbegin_dynamic.o \${SYSROOT}/usr/lib/crtend_android.o $LDFLAGS"
	CONFIG_EXTRA_LDLIBS="m c gcc"
;;
intel)
	CONFIG_CROSS_COMPILER_PREFIX="$ANDROID_NDK_ROOT/toolchains/x86-4.6/prebuilt/linux-$HOST_ARCH/bin/i686-linux-android-"
	CONFIG_SYSROOT="$ANDROID_NDK_ROOT/platforms/$ANDROID_NATIVE_API_LEVEL/arch-x86"
	CONFIG_EXTRA_CFLAGS="-march=i686 -mtune=atom -DANDROID -fPIC -mandroid -mstackrealign -msse3 -mfpmath=sse -m32 -fno-short-enums -ffunction-sections -funwind-tables -fomit-frame-pointer -fstrict-aliasing -funswitch-loops $CFLAGS"
	CONFIG_EXTRA_LDFLAGS="-Xlinker -z -Xlinker muldefs -nostdlib -Bdynamic -Xlinker -dynamic-linker -Xlinker /system/bin/linker -Xlinker -z -Xlinker nocopyreloc -Xlinker --no-undefined \${SYSROOT}/usr/lib/crtbegin_dynamic.o \${SYSROOT}/usr/lib/crtend_android.o $LDFLAGS"
	CONFIG_EXTRA_LDLIBS="m c gcc"
;;
mips)
	CONFIG_CROSS_COMPILER_PREFIX="$ANDROID_NDK_ROOT/toolchains/mipsel-linux-android-4.6/prebuilt/linux-$HOST_ARCH/bin/mipsel-linux-android-"
	CONFIG_SYSROOT="$ANDROID_NDK_ROOT/platforms/$ANDROID_NATIVE_API_LEVEL/arch-mips"
	CONFIG_EXTRA_CFLAGS="-fPIC -Wno-psabi -DANDROID -fomit-frame-pointer -fno-strict-aliasing -finline-functions -ffunction-sections -funwind-tables -fmessage-length=0 -fno-inline-functions-called-once -fgcse-after-reload -frerun-cse-after-loop -frename-registers $CFLAGS"
	CONFIG_EXTRA_LDFLAGS="-Xlinker -z -Xlinker muldefs -nostdlib -Bdynamic -Xlinker -dynamic-linker -Xlinker /system/bin/linker -Xlinker -z -Xlinker nocopyreloc -Xlinker --no-undefined \${SYSROOT}/usr/lib/crtbegin_dynamic.o \${SYSROOT}/usr/lib/crtend_android.o $LDFLAGS"
	CONFIG_EXTRA_LDLIBS="m c gcc"
;;
esac

unset CFLAGS LDFLAGS
export CONFIG_CROSS_COMPILER_PREFIX CONFIG_SYSROOT CONFIG_EXTRA_CFLAGS CONFIG_EXTRA_LDFLAGS CONFIG_EXTRA_LDLIBS

defconfig="linuxdeploy_defconfig"
sed -i "s|CONFIG_CROSS_COMPILER_PREFIX_TEMPLATE|$CONFIG_CROSS_COMPILER_PREFIX|" ./configs/$defconfig
sed -i "s|CONFIG_SYSROOT_TEMPLATE|$CONFIG_SYSROOT|" ./configs/$defconfig
sed -i "s|CONFIG_EXTRA_CFLAGS_TEMPLATE|$CONFIG_EXTRA_CFLAGS|" ./configs/$defconfig
sed -i "s|CONFIG_EXTRA_LDFLAGS_TEMPLATE|$CONFIG_EXTRA_LDFLAGS|" ./configs/$defconfig
sed -i "s|CONFIG_EXTRA_LDLIBS_TEMPLATE|$CONFIG_EXTRA_LDLIBS|" ./configs/$defconfig
if [ -n "$PIE" ]
then sed -i "s|^# CONFIG_PIE.*|CONFIG_PIE=y|" ./configs/$defconfig
else sed -i "s|^CONFIG_PIE.*|# CONFIG_PIE is not set|" ./configs/$defconfig
fi
make $defconfig || exit 1

echo ">>> make"
make -j$NCPU || exit 1

echo ">>> install"
make CONFIG_PREFIX=$PREFIX install || exit 1
