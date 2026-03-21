# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to your build.

cmake_minimum_required(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/native-lib.cpp )

# Add KissFFT
add_library(
    kissfft
    STATIC
    src/main/cpp/kissfft-131.2.0/kiss_fft.c
)

target_include_directories(
    kissfft
    PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/kissfft-131.2.0
)

# Link KissFFT with your native lib
target_link_libraries(
    native-lib
    kissfft
    log
)