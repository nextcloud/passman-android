#!/bin/bash

OPENSSL_SUBMODULE_PATH="app/src/main/cpplibs/openssl"

if [ -f "openssl.conf" ] ; then
    . openssl.conf
elif [ -f "openssl.conf.example" ]; then
    echo "Could not found openssl.conf!"
    echo "Try it with openssl.conf.example ..."
    . openssl.conf.example
else
    echo "You need to create a openssl.conf or have at least the default openssl.conf.example in this folder!"
    exit 1
fi

if [ ! -d "$OPENSSL_SUBMODULE_PATH" ]; then
    echo "Could not find openssl at $OPENSSL_SUBMODULE_PATH. Seems the submodules are not initialized yet."
    exit 1
fi


cd $OPENSSL_SUBMODULE_PATH

export ANDROID_NDK_ROOT="$ANDROID_NDK_HOME"
export TOOLCHAIN=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG
PATH=$TOOLCHAIN/bin:$PATH

# OpenSSL needs -D__ANDROID_API__=N to select the NDK clang wrapper (e.g. aarch64-linux-android21-clang).
# NDK r26+ already defines that macro as __ANDROID_MIN_SDK_VERSION__, so without -Wno-macro-redefined
# every translation unit emits a redefinition warning on stderr.
# See https://developer.android.com/ndk/guides/sdk-versions#minsdkversion
# Well, basically we need this only if our minSdkVersion is different from the one in <NDK>/meta/platforms.json,
# but let's keep it to ensure we won't get problems when upgrading the NDK or changing our minSdkVersion next time.
OPENSSL_ANDROID_API_CFLAGS="-D__ANDROID_API__=$MIN_SDK_VERSION -Wno-macro-redefined"

# arm64
export TARGET_HOST=aarch64-linux-android
./Configure android-arm64 no-shared \
 $OPENSSL_ANDROID_API_CFLAGS \
 --prefix=$PWD/build/arm64-v8a

make -j"$THREADS"
make install_sw
make clean

# arm
export TARGET_HOST=armv7a-linux-androideabi
./Configure android-arm no-shared \
 $OPENSSL_ANDROID_API_CFLAGS \
 --prefix=$PWD/build/armeabi-v7a

make -j"$THREADS"
make install_sw
make clean

# x86
export TARGET_HOST=i686-linux-android
./Configure android-x86 no-shared \
 $OPENSSL_ANDROID_API_CFLAGS \
 --prefix=$PWD/build/x86

make -j"$THREADS"
make install_sw
make clean

# x64
export TARGET_HOST=x86_64-linux-android
./Configure android-x86_64 no-shared \
 $OPENSSL_ANDROID_API_CFLAGS \
 --prefix=$PWD/build/x86_64

make -j"$THREADS"
make install_sw
make clean
