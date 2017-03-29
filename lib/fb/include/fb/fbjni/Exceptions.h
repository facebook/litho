/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

/**
 * @file Exceptions.h
 *
 * After invoking a JNI function that can throw a Java exception, the macro
 * @ref FACEBOOK_JNI_THROW_PENDING_EXCEPTION() or @ref FACEBOOK_JNI_THROW_EXCEPTION_IF()
 * should be invoked.
 *
 * IMPORTANT! IMPORTANT! IMPORTANT! IMPORTANT! IMPORTANT! IMPORTANT! IMPORTANT! IMPORTANT!
 * To use these methods you MUST call initExceptionHelpers() when your library is loaded.
 */

#pragma once

#include <alloca.h>
#include <stdexcept>
#include <string>

#include <jni.h>

#include <fb/visibility.h>

#include "Common.h"
#include "References.h"
#include "CoreClasses.h"

namespace facebook {
namespace jni {

class JThrowable;

class JCppException : public JavaClass<JCppException, JThrowable> {
 public:
  static auto constexpr kJavaDescriptor = "Lcom/facebook/jni/CppException;";

  static local_ref<JCppException> create(const char* str) {
    return newInstance(make_jstring(str));
  }

  static local_ref<JCppException> create(const std::exception& ex) {
    return newInstance(make_jstring(ex.what()));
  }
};
