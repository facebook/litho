/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#pragma once
#include <functional>
#include <string>
#include <jni.h>

#include <fb/visibility.h>

namespace facebook {
namespace jni {

// Keeps a thread-local reference to the current thread's JNIEnv.
struct Environment {
  // May be null if this thread isn't attached to the JVM
  FBEXPORT static JNIEnv* current();
  static void initialize(JavaVM* vm);

  // There are subtle issues with calling the next functions directly. It is
  // much better to always use a ThreadScope to manage attaching/detaching for
  // you.
  FBEXPORT static JNIEnv* ensureCurrentThreadIsAttached();
  FBEXPORT static void detachCurrentThread();
};

/**
