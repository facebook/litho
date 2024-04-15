// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

/**
 * This is a modified version of Android's ScopedLocalRef class that can be
 * found in the Android's JNI code.
 */
#pragma once

#include <cstddef>
#include <type_traits>
#include "jni.h"

namespace facebook {
namespace flexlayout {
namespace jni {

/**
 * ScopedLocalRef is a sort of smart reference that allows us to control the
 * lifespan of a JNI local reference.
 *
 * This class is designed so that when a ScopedLocalRef goes out of scope, its
 * destructor will delete -JNIEnv->DeleteLocalRef()- the underlying JNI
 * reference.
 *
 * This class should be used to wrap all the local references that JNI
 * gives us other than those that are passed to native methods at
 * invocation time. The idea behind this is that in JNI we should be very
 * explicit about the lifespan of local references. Local references can quickly
 * get out of control, and the developer should always be very aware of the
 * lifespan of each local reference that is created in JNI so that leaks are
 * prevented.
 *
 * This class is very explicit in its behavior, and it does not allow to perform
 * unexpected conversions or unexpected ownership transfer. In practice, this
 * class acts as a unique pointer where the underying JNI reference can have one
 * and just one owner. Transfering ownership is allowed but it is an explicit
 * operation (implemneted via move semantics and also via explicity API calls).
 *
 * As with standard JNI local references it is not a valid operation to keep a
 * reference around between different native method calls.
 */
template <typename T>
class ScopedLocalRef {
  static_assert(
      (std::is_same<T, jclass>::value || std::is_same<T, jobject>::value ||
       std::is_same<T, jstring>::value || std::is_same<T, jthrowable>::value ||
       std::is_same<T, jbyteArray>::value ||
       std::is_same<T, jintArray>::value ||
       std::is_same<T, jshortArray>::value ||
       std::is_same<T, jcharArray>::value ||
       std::is_same<T, jlongArray>::value ||
       std::is_same<T, jfloatArray>::value ||
       std::is_same<T, jdoubleArray>::value ||
       std::is_same<T, jobjectArray>::value ||
       std::is_same<T, jbooleanArray>::value),
      "ScopedLocalRef instantiated for invalid type");

 public:
  /**
   * Constructs a ScopedLocalRef with a JNI local reference.
   *
   * @param localRef the local reference to wrap. Can be NULL.
   */
  ScopedLocalRef(JNIEnv* env, T localRef) : mEnv(env), mLocalRef(localRef) {}

  /**
   * Equivalent to ScopedLocalRef(NULL, NULL)
   */
  explicit ScopedLocalRef() : mEnv(NULL), mLocalRef(NULL) {}

  /**
   * Move construction is allowed.
   */
  ScopedLocalRef(ScopedLocalRef&& s) noexcept
      : mEnv(s.mEnv), mLocalRef(s.release()) {}

  /**
   * Move assignment is allowed.
   */
  auto operator=(ScopedLocalRef&& s) noexcept -> ScopedLocalRef& {
    reset(s.release());
    mEnv = s.mEnv;
    return *this;
  }

  ~ScopedLocalRef() {
    reset();
  }

  /**
   * Deletes the currently held reference and reassigns a new one to the
   * ScopedLocalRef.
   */
  void reset(T ptr = NULL) {
    if (ptr != mLocalRef) {
      if (mLocalRef != NULL) {
        mEnv->DeleteLocalRef(mLocalRef);
      }
      mLocalRef = ptr;
    }
  }

  /**
   * Makes this ScopedLocalRef not own the underlying JNI local reference. After
   * calling this method, the ScopedLocalRef will not delete the JNI local
   * reference when the ScopedLocalRef goes out of scope.
   */
  auto release() -> T {
    T localRef = mLocalRef;
    mLocalRef = NULL;
    return localRef;
  }

  /**
   * Returns the underlying JNI local reference.
   */
  auto get() const -> T {
    return mLocalRef;
  }

  /**
   * Returns true if the underlying JNI reference is not NULL.
   */
  operator bool() const {
    return mLocalRef != NULL;
  }

  ScopedLocalRef(const ScopedLocalRef& ref) = delete;
  auto operator=(const ScopedLocalRef& other) -> ScopedLocalRef& = delete;

 private:
  JNIEnv* mEnv;
  T mLocalRef;
};

template <typename T>
auto make_local_ref(JNIEnv* env, T localRef) -> ScopedLocalRef<T> {
  return ScopedLocalRef<T>(env, localRef);
}

template <typename T>
auto make_local_ref_from_unowned(JNIEnv* env, T aliasRef) -> ScopedLocalRef<T> {
  return make_local_ref(env, static_cast<T>(env->NewLocalRef(aliasRef)));
}

} // namespace jni
} // namespace flexlayout
} // namespace facebook
