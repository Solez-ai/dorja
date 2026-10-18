cmake_minimum_required(VERSION 3.22)
project(dorja_panorama CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# OPENCV_SDK_DIR is passed from app/build.gradle.kts (auto-downloaded SDK).
# It must point at <sdk-root>/OpenCV-android-sdk/sdk/native/jni
if(NOT DEFINED OPENCV_SDK_DIR)
    message(FATAL_ERROR "OPENCV_SDK_DIR not set. Gradle task downloadOpenCvSdk must run first.")
endif()

set(OpenCV_DIR "${OPENCV_SDK_DIR}")
find_package(OpenCV REQUIRED
    COMPONENTS core imgproc flann calib3d features2d stitching)

add_library(dorja_panorama SHARED panorama_stitcher.cpp)

target_link_libraries(dorja_panorama
    PRIVATE
    ${OpenCV_LIBS}
    log
    android)

# Size control: Oz + strip + gc-sections. Static OpenCV archives are big;
# the linker only pulls in what the stitch pipeline actually references.
target_compile_options(dorja_panorama PRIVATE -Oz -fvisibility=hidden)
target_link_options(dorja_panorama PRIVATE
    -Wl,--gc-sections
    -Wl,--exclude-libs,ALL)