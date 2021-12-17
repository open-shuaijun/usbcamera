cmake_minimum_required(VERSION 3.10.2)

project("uvccamera")

file(GLOB native-src
        _onload.cpp
        utilbase.cpp
        UVCCamera.cpp
        UVCPreview.cpp
        UVCButtonCallback.cpp
        UVCStatusCallback.cpp
        Parameters.cpp
        serenegiant_usb_UVCCamera.cpp
        AvcEncoder.cpp
        )

add_library(uvccamera SHARED ${native-src})


target_include_directories(
        uvccamera
        PRIVATE
        ${CMAKE_SOURCE_DIR}/include
        ${CMAKE_SOURCE_DIR}/include/libusb
        ${CMAKE_SOURCE_DIR}/include/libuvc
        ${CMAKE_SOURCE_DIR}/include/librapidjson
        ${CMAKE_SOURCE_DIR}/UVCCamera.h
        ${CMAKE_SOURCE_DIR}/AvcQueue.h
        ${CMAKE_SOURCE_DIR}/AvcArgs.h
        ${CMAKE_SOURCE_DIR}/Avc.h
)


add_library(jpeg-turbo1500 SHARED IMPORTED)
add_library(usb100 SHARED IMPORTED)
add_library(uvc SHARED IMPORTED)

set_target_properties(
        jpeg-turbo1500
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libjpeg-turbo1500.so
)

set_target_properties(
        usb100
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}/libusb100.so
)

set_target_properties(
        uvc
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}/libuvc.so
)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library(log-lib log)
find_library(android-lib android)
find_library(amedia-lib mediandk)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        uvccamera
        jpeg-turbo1500
        usb100
        uvc
        ${android-lib}
        ${amedia-lib}
        ${log-lib})

