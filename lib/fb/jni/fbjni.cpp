/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <fb/fbjni.h>

#include <mutex>
#include <vector>
#include <jni/LocalString.h>
#include <fb/log.h>

namespace facebook {
namespace jni {

jint initialize(JavaVM* vm, std::function<void()>&& init_fn) noexcept {
  static std::once_flag flag{};
  // TODO (t7832883): DTRT when we have exception pointers
  static auto error_msg = std::string{"Failed to initialize fbjni"};
  static auto error_occured = false;

  std::call_once(flag, [vm] {
    try {
      Environment::initialize(vm);
    } catch (std::exception& ex) {
      error_occured = true;
      try {
        error_msg = std::string{"Failed to initialize fbjni: "} + ex.what();
      } catch (...) {
        // Ignore, we already have a fall back message
      }
    } catch (...) {
      error_occured = true;
    }
  });

  try {
    if (error_occured) {
      throw std::runtime_error(error_msg);
    }

    init_fn();
  } catch (const std::exception& e) {
    FBLOGE("error %s", e.what());
    translatePendingCppExceptionToJavaException();
  } catch (...) {
    translatePendingCppExceptionToJavaException();
    // So Java will handle the translated exception, fall through and
    // return a good version number.
  }
  return JNI_VERSION_1_6;
}

alias_ref<JClass> findClassStatic(const char* name) {
  const auto env = internal::getEnv();
  if (!env) {
    throw std::runtime_error("Unable to retrieve JNIEnv*.");
  }
  auto cls = env->FindClass(name);
  FACEBOOK_JNI_THROW_EXCEPTION_IF(!cls);
  auto leaking_ref = (jclass)env->NewGlobalRef(cls);
  FACEBOOK_JNI_THROW_EXCEPTION_IF(!leaking_ref);
  return wrap_alias(leaking_ref);
}

local_ref<JClass> findClassLocal(const char* name) {
  const auto env = internal::getEnv();
