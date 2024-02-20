// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#include "FlexLayoutJniException.h"
#include <stdexcept>
#include <string>
#include "common.h"

namespace facebook {
namespace flexlayout {
namespace jni {

FlexLayoutJniException::FlexLayoutJniException() {
  jclass cl = getCurrentEnv()->FindClass("Ljava/lang/RuntimeException;");
  static const jmethodID methodId = facebook::flexlayout::jni::getMethodId(
      getCurrentEnv(), cl, "<init>", "()V");
  auto* throwable = getCurrentEnv()->NewObject(cl, methodId);
  throwable_ =
      newGlobalRef(getCurrentEnv(), static_cast<jthrowable>(throwable));
}

FlexLayoutJniException::FlexLayoutJniException(
    jthrowable throwable,
    std::string reason) {
  throwable_ = newGlobalRef(getCurrentEnv(), throwable);
  reason_ = std::move(reason);
}

FlexLayoutJniException::FlexLayoutJniException(
    FlexLayoutJniException&& rhs) noexcept
    : throwable_(std::move(rhs.throwable_)) {}

FlexLayoutJniException::FlexLayoutJniException(
    const FlexLayoutJniException& rhs) {
  throwable_ = newGlobalRef(getCurrentEnv(), rhs.throwable_.get());
}

auto FlexLayoutJniException::what() const noexcept -> const char* {
  return reason_.c_str();
}

FlexLayoutJniException::~FlexLayoutJniException() {
  try {
    throwable_.reset();
  } catch (...) {
    std::terminate();
  }
}

auto FlexLayoutJniException::getThrowable() const noexcept
    -> ScopedLocalRef<jthrowable> {
  return make_local_ref_from_unowned(getCurrentEnv(), throwable_.get());
}
} // namespace jni
} // namespace flexlayout
} // namespace facebook
