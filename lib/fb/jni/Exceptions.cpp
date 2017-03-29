/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <fb/fbjni/CoreClasses.h>

#include <fb/assert.h>
#include <fb/log.h>

#include <alloca.h>
#include <cstdlib>
#include <ios>
#include <stdexcept>
#include <stdio.h>
#include <string>
#include <system_error>

#include <jni.h>


namespace facebook {
namespace jni {

namespace {
class JRuntimeException : public JavaClass<JRuntimeException, JThrowable> {
 public:
  static auto constexpr kJavaDescriptor = "Ljava/lang/RuntimeException;";

  static local_ref<JRuntimeException> create(const char* str) {
    return newInstance(make_jstring(str));
  }

  static local_ref<JRuntimeException> create() {
    return newInstance();
  }
};

class JIOException : public JavaClass<JIOException, JThrowable> {
 public:
  static auto constexpr kJavaDescriptor = "Ljava/io/IOException;";

  static local_ref<JIOException> create(const char* str) {
    return newInstance(make_jstring(str));
  }
};

class JOutOfMemoryError : public JavaClass<JOutOfMemoryError, JThrowable> {
 public:
  static auto constexpr kJavaDescriptor = "Ljava/lang/OutOfMemoryError;";

  static local_ref<JOutOfMemoryError> create(const char* str) {
    return newInstance(make_jstring(str));
  }
};

