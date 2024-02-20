// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#pragma once

#ifdef __ANDROID__
#include <android/log.h>
#else
#include <cstdio>
#endif

namespace facebook {
namespace flexlayout {
namespace jni {

template <typename... Ts>
void logError(const char* format, Ts... args) {
#ifdef __ANDROID__
  __android_log_print(ANDROID_LOG_ERROR, "FlexLayoutJNI", format, args...);
#else
  std::fprintf(stderr, format, args...);
#endif
}
} // namespace jni
} // namespace flexlayout
} // namespace facebook
