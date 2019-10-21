# Copyright (c) 2017-present, Facebook, Inc.
#
# This source code is licensed under the Apache 2.0 license found in the
# LICENSE file in the root directory of this source tree.

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

# configure import libs
set(libfb_DIR ${CMAKE_SOURCE_DIR}/../../../../fbjni/src/main/cpp)
set(yogacore_DIR ${CMAKE_SOURCE_DIR}/../../../../yoga/src/main/cpp)

set(build_DIR ${CMAKE_SOURCE_DIR}/build)

set(libfb_build_DIR ${build_DIR}/libfb/${ANDROID_ABI})
set(yogacore_build_DIR ${build_DIR}/yogacore/${ANDROID_ABI})

file(MAKE_DIRECTORY ${build_DIR})

add_subdirectory(${libfb_DIR} ${libfb_build_DIR})
add_subdirectory(${yogacore_DIR} ${yogacore_build_DIR})

add_compile_options(
    -fno-omit-frame-pointer
    -fexceptions
    -Wall
    -O3
    -std=c++11)

file(GLOB jni_SRC
    jni/*.cpp)

add_library(yoga SHARED ${jni_SRC})

target_include_directories(yoga PRIVATE
    ${libfb_DIR}
    ${yogacore_DIR})

target_link_libraries(yoga yogacore fb)
