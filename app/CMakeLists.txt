# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

add_compile_options(
    -UANDROID
)

add_library( # Sets the name of the library.
     passman-lib

     # Sets the library as a shared library.
     SHARED

     # Provides a relative path to your source file(s).
     # Associated headers in the same location as their source
     # file are automatically included.
     src/main/cpp/passman-lib.cpp
)

add_library(
    base64-lib
    STATIC
    src/main/cpp/base64.cpp
)

add_library(
    sjcl-lib
    STATIC
    src/main/cpp/SJCL.cpp
)

add_library(
    json-lib
    STATIC
    src/main/cpplibs/SimpleJSON/src/JSON.cpp
    src/main/cpplibs/SimpleJSON/src/JSONValue.cpp
)

include_directories (
    src/main/cpplibs/openssl/include/
    src/main/cpplibs/SimpleJSON/src/
)

add_library(
    openssl-lib
    STATIC
    IMPORTED
)
set_target_properties(
    openssl-lib
    #PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpplibs/openssl/libssl.a
    PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpplibs/openssl/build/${CMAKE_ANDROID_ARCH_ABI}/lib/libssl.a
)

add_library(
    opensslcrypto-lib
    STATIC
    IMPORTED
)
set_target_properties(
    opensslcrypto-lib
    #PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpplibs/openssl/libcrypto.a
    PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpplibs/openssl/build/${CMAKE_ANDROID_ARCH_ABI}/lib/libcrypto.a
)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because system libraries are included in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       passman-lib
                       base64-lib
                       sjcl-lib
                       json-lib

                       openssl-lib
                       opensslcrypto-lib
                       #src/main/cpplib/openssl/libcrypto.so.1.0.0

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
