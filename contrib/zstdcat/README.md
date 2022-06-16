zstdcat Build Guide
===================

Zstandard file compression utility. This is required for newer version of Ubuntu, and not included before Android 12.

## Build instructions

In a work directory of your choice...

### Download/Install Android SDK:

```
$ mkdir sdk/
$ cd sdk/
$ mkdir cmdline-tools/
$ cd cmdline-tools/
```

Go to https://developer.android.com/studio/
Download after agreeing to EULA: `commandlinetools-linux-xxx_latest.zip` into above path.
(In my case it was `commandlinetools-linux-8512546_latest.zip`.)

```
$ unzip commandlinetools-linux-8512546_latest.zip
```

Now install CMake and the NDK, I used the version below:

```
$ cmdline-tools/bin/sdkmanager --install "cmake;3.10.2.4988404"
$ cmdline-tools/bin/sdkmanager --install "ndk;24.0.8215888"
$ cd ../..
```

### Clone and build zstdcat

Preparation:

```
$ git clone https://github.com/facebook/zstd.git
$ cd zstd/build/cmake/
$ mkdir builddir/
$ cd builddir/
$ export ANDROID_SDK=../../../../sdk
```

#### Build for armeabi

```
$ $ANDROID_SDK/cmake/3.10.2.4988404/bin/cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_SDK/ndk/24.0.8215888/build/cmake/android.toolchain.cmake -DANDROID_ABI=armeabi-v7a ..
$ make -j 8
```

Copy out (and rename) from zstd ``programs/zstd`` to linuxdeploy as ``app/src/main/assets/bin/arm/zstdcat``

#### Build for arm64-v8a

```
$ rm -rf *
$ $ANDROID_SDK/cmake/3.10.2.4988404/bin/cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_SDK/ndk/24.0.8215888/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a ..
$ make -j 8
```

Copy out (and rename) from zstd ``programs/zstd`` to linuxdeploy as ``app/src/main/assets/bin/arm_64/zstdcat``

#### Build for x86

```
$ rm -rf *
$ $ANDROID_SDK/cmake/3.10.2.4988404/bin/cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_SDK/ndk/24.0.8215888/build/cmake/android.toolchain.cmake -DANDROID_ABI=x86 ..
$ make -j 8
```

Copy out (and rename) from zstd ``programs/zstd`` to linuxdeploy as ``app/src/main/assets/bin/x86/zstdcat``

#### Build for x86_64

```
$ rm -rf *
$ $ANDROID_SDK/cmake/3.10.2.4988404/bin/cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_SDK/ndk/24.0.8215888/build/cmake/android.toolchain.cmake -DANDROID_ABI=x86_64 ..
$ make -j 8
```

Copy out (and rename) from zstd ``programs/zstd`` to linuxdeploy as ``app/src/main/assets/bin/x86_64/zstdcat``
