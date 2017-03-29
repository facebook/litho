/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#pragma once

#include <string>

#include <jni.h>

#include <fb/visibility.h>

namespace facebook {
namespace jni {

namespace detail {

void utf8ToModifiedUTF8(const uint8_t* bytes, size_t len, uint8_t* modified, size_t modifiedLength);
size_t modifiedLength(const std::string& str);
size_t modifiedLength(const uint8_t* str, size_t* length);
std::string modifiedUTF8ToUTF8(const uint8_t* modified, size_t len) noexcept;
std::string utf16toUTF8(const uint16_t* utf16Bytes, size_t len) noexcept;
