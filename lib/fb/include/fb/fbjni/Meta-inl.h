/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#pragma once

#include <jni.h>

#include "Common.h"
#include "Exceptions.h"
#include "MetaConvert.h"
#include "References.h"
#include "Boxed.h"

#if defined(__ANDROID__)
#include <sys/system_properties.h>
#endif

namespace facebook {
namespace jni {

// JMethod /////////////////////////////////////////////////////////////////////////////////////////

inline JMethodBase::JMethodBase(jmethodID method_id) noexcept
  : method_id_{method_id}
{}

inline JMethodBase::operator bool() const noexcept {
  return method_id_ != nullptr;
}

inline jmethodID JMethodBase::getId() const noexcept {
  return method_id_;
}

namespace {

template <int idx, typename... Args>
struct ArgsArraySetter;

template <int idx, typename Arg, typename... Args>
struct ArgsArraySetter<idx, Arg, Args...> {
  static void set(alias_ref<JArrayClass<jobject>::javaobject> array, Arg arg0, Args... args) {
    // TODO(xxxxxxxx): Use Convert<Args>... to do conversions like the fast path.
    (*array)[idx] = autobox(arg0);
    ArgsArraySetter<idx + 1, Args...>::set(array, args...);
  }
};

template <int idx>
struct ArgsArraySetter<idx> {
  static void set(alias_ref<JArrayClass<jobject>::javaobject> array) {
  }
};

template <typename... Args>
local_ref<JArrayClass<jobject>::javaobject> makeArgsArray(Args... args) {
  auto arr = JArrayClass<jobject>::newArray(sizeof...(args));
  ArgsArraySetter<0, Args...>::set(arr, args...);
  return arr;
}


inline bool needsSlowPath(alias_ref<jobject> obj) {
