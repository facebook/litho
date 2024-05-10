// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#include "common.h"
#include "corefunctions.h"

namespace facebook {
namespace flexlayout {
namespace jni {

jclass findClass(JNIEnv* env, const char* className) {
  jclass clazz = env->FindClass(className);
  assertNoPendingJniExceptionIf(env, clazz == nullptr);

  return clazz;
}

void registerNatives(
    JNIEnv* env,
    const char* className,
    const JNINativeMethod methods[],
    jint numMethods) {
  jclass clazz = findClass(env, className);

  auto result = env->RegisterNatives(clazz, methods, numMethods);

  assertNoPendingJniExceptionIf(env, result != JNI_OK);
}

auto getFieldId(
    JNIEnv* env,
    jclass clazz,
    const char* fieldName,
    const char* fieldSignature) -> jfieldID {
  jfieldID fieldId = env->GetFieldID(clazz, fieldName, fieldSignature);
  assertNoPendingJniExceptionIf(env, fieldId == nullptr);
  return fieldId;
}

auto getMethodId(
    JNIEnv* env,
    jclass clazz,
    const char* methodName,
    const char* methodDescriptor) -> jmethodID {
  jmethodID methodId = env->GetMethodID(clazz, methodName, methodDescriptor);
  assertNoPendingJniExceptionIf(env, methodId == nullptr);
  return methodId;
}

auto newGlobalRef(JNIEnv* env, jobject obj) -> ScopedGlobalRef<jobject> {
  jobject result = env->NewGlobalRef(obj);

  if (result == nullptr) {
    logErrorMessageAndDie("Could not obtain global reference from object");
  }

  return make_global_ref(result);
}

auto newGlobalRef(JNIEnv* env, jthrowable obj) -> ScopedGlobalRef<jthrowable> {
  auto* result = static_cast<jthrowable>(env->NewGlobalRef(obj));

  if (result == nullptr) {
    logErrorMessageAndDie("Could not obtain global reference from object");
  }

  return make_global_ref(result);
}
} // namespace jni
} // namespace flexlayout
} // namespace facebook
