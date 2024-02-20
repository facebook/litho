// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#include <stdexcept>
#include <string>
#include "ScopedGlobalRef.h"
#include "ScopedLocalRef.h"
#include "common.h"
namespace facebook {
namespace flexlayout {
namespace jni {
/**
 * This class wraps a Java exception (jthrowable) into a C++ exception; A global
 * reference to Java exception (jthrowable) is made so that the exception object
 * does not gets cleared before jni call completion
 */
class FlexLayoutJniException : public std::exception {
 public:
  FlexLayoutJniException();
  ~FlexLayoutJniException() override;

  explicit FlexLayoutJniException(jthrowable throwable, std::string reason);

  FlexLayoutJniException(FlexLayoutJniException&& rhs) noexcept;

  FlexLayoutJniException(const FlexLayoutJniException& rhs);

  auto getThrowable() const noexcept -> ScopedLocalRef<jthrowable>;

  auto what() const noexcept -> const char* override;

 private:
  ScopedGlobalRef<jthrowable> throwable_;
  std::string reason_;
};
} // namespace jni
} // namespace flexlayout
} // namespace facebook
