// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#pragma once

#include <cstddef>
#include "ScopedGlobalRef.h"
#include "jni.h"

namespace facebook {
namespace flexlayout {
namespace jni {

/**
 * Find class by its name. Aborts if not found.
 */
jclass findClass(JNIEnv* env, const char* className);

/**
 * Registers a set of methods for a JNI class. Aborts if registration fails.
 */
void registerNatives(
    JNIEnv* env,
    const char* className,
    const JNINativeMethod methods[],
    jint numMethods);

/**
 * Returns a class non-static field ID. Aborts if any error happens.
 */
auto getFieldId(
    JNIEnv* env,
    jclass clazz,
    const char* fieldName,
    const char* fieldSignature) -> jfieldID;

/**
 * Returns a jmethodID for a class non-static method. Aborts if any error
 * happens.
 */
auto getMethodId(
    JNIEnv* env,
    jclass clazz,
    const char* methodName,
    const char* methodDescriptor) -> jmethodID;

// Calls a non-static method on an object depending on the
// return type. Will abort the execution if an error
// (such as a Java pending exception) is detected after invoking the
// Java method.
template <typename Result, typename... Args>
auto callMethod(JNIEnv* env, jobject obj, jmethodID methodId, Args... args)
    -> Result {
  const auto result = [&]() -> Result {
    if constexpr (std::is_same_v<Result, jfloat>) {
      return env->CallFloatMethod(obj, methodId, args...);
    } else {
      return static_cast<Result>(env->CallObjectMethod(obj, methodId, args...));
    }
  }();
  assertNoPendingJniException(env);
  return result;
}

auto newGlobalRef(JNIEnv* env, jobject obj) -> ScopedGlobalRef<jobject>;

auto newGlobalRef(JNIEnv* env, jthrowable obj) -> ScopedGlobalRef<jthrowable>;
} // namespace jni
} // namespace flexlayout
} // namespace facebook
