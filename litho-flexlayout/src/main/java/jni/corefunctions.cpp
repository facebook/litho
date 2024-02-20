// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#include "corefunctions.h"
#include "FlexLayoutJniException.h"
#include "macros.h"

namespace facebook {
namespace flexlayout {
namespace jni {

namespace {
JavaVM* globalVm = nullptr;
struct JavaVMInitializer {
  JavaVMInitializer(JavaVM* vm) {
    globalVm = vm;
  }
};
} // namespace

auto ensureInitialized(JNIEnv** env, JavaVM* vm) -> jint {
  static JavaVMInitializer init(vm);

  if (env == nullptr) {
    logErrorMessageAndDie(
        "Need to pass a valid JNIEnv pointer to vanillajni initialization "
        "routine");
  }

  if (vm->GetEnv(reinterpret_cast<void**>(env), JNI_VERSION_1_6) != JNI_OK) {
    logErrorMessageAndDie(
        "Error retrieving JNIEnv during initialization of vanillajni");
  }

  return JNI_VERSION_1_6;
}

// TODO T67644702 why we need JNIEXPORT for getCurrentEnv ?
JNIEXPORT auto getCurrentEnv() -> JNIEnv* {
  JNIEnv* env;
  jint ret = globalVm->GetEnv((void**)&env, JNI_VERSION_1_6);
  if (ret != JNI_OK) {
    logErrorMessageAndDie(
        "There was an error retrieving the current JNIEnv. Make sure the "
        "current thread is attached");
  }
  return env;
}

void logErrorMessageAndDie(const char* message) {
  logError("Aborting due to error detected in native code: %s", message);
}

static auto throwableDescription(jthrowable throwable) -> std::string {
  const auto env = getCurrentEnv();
  const auto class_Throwable =
      ScopedLocalRef(env, env->FindClass("java/lang/Throwable"));
  static const auto method_Throwable_toString = getMethodId(
      env, class_Throwable.get(), "toString", "()Ljava/lang/String;");
  const auto descriptionJStr = ScopedLocalRef(
      env, callMethod<jstring>(env, throwable, method_Throwable_toString));
  const auto descriptionCStr =
      env->GetStringUTFChars(descriptionJStr.get(), nullptr);
  auto description = std::string(descriptionCStr);
  env->ReleaseStringUTFChars(descriptionJStr.get(), descriptionCStr);

  return description;
}

void assertNoPendingJniException(JNIEnv* env) {
  if (env->ExceptionCheck() == JNI_FALSE) {
    return;
  }

  auto throwable = make_local_ref(env, env->ExceptionOccurred());
  if (!throwable) {
    logErrorMessageAndDie("Unable to get pending JNI exception.");
  }
  env->ExceptionClear();

  auto description = throwableDescription(throwable.get());
  logError("Rethrowing Java exception as native: %s", description.c_str());
  throw FlexLayoutJniException(throwable.get(), std::move(description));
}

void assertNoPendingJniExceptionIf(JNIEnv* env, bool condition) {
  if (!condition) {
    return;
  }

  if (env->ExceptionCheck() == JNI_TRUE) {
    assertNoPendingJniException(env);
    return;
  }

  throw FlexLayoutJniException();
}

} // namespace jni
} // namespace flexlayout
} // namespace facebook
