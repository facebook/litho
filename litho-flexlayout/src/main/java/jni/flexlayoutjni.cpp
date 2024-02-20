// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#include "FlexLayoutJNIVanilla.h"
#include "common.h"
#include "corefunctions.h"

using namespace facebook::flexlayout;

jint JNI_OnLoad(JavaVM* vm, void*) {
  JNIEnv* env;
  jint ret = jni::ensureInitialized(&env, vm);
  FlexLayoutJNIVanilla::registerNatives(env);
  return ret;
}
