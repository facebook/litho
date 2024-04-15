# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

# configure import libs
set(flexlayoutcore_DIR ${CMAKE_SOURCE_DIR}/../cpp)

set(build_DIR ${CMAKE_SOURCE_DIR}/build)

set(flexlayoutcore_build_DIR ${build_DIR}/flexlayoutcore/${ANDROID_ABI})

file(MAKE_DIRECTORY ${build_DIR})

add_subdirectory(${flexlayoutcore_DIR} ${flexlayoutcore_build_DIR})

add_compile_options(
    -fno-omit-frame-pointer
    -fexceptions
    -Wall
    -O3
    -std=c++20)

file(GLOB jni_SRC
    jni/*.cpp)

add_library(flexlayout SHARED ${jni_SRC})

target_include_directories(flexlayout PRIVATE
    ${flexlayoutcore_DIR})

target_link_libraries(flexlayout flexlayoutcore)
