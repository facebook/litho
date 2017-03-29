/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <pthread.h>
#include <fb/log.h>
#include <fb/StaticInitialized.h>
#include <fb/ThreadLocal.h>
#include <fb/Environment.h>
#include <fb/fbjni/CoreClasses.h>
#include <fb/fbjni/NativeRunnable.h>

#include <functional>

namespace facebook {
namespace jni {

namespace {
StaticInitialized<ThreadLocal<JNIEnv>> g_env;
JavaVM* g_vm = nullptr;

struct JThreadScopeSupport : JavaClass<JThreadScopeSupport> {
  static auto constexpr kJavaDescriptor = "Lcom/facebook/jni/ThreadScopeSupport;";

  // These reinterpret_casts are a totally dangerous pattern. Don't use them. Use HybridData instead.
  static void runStdFunction(std::function<void()>&& func) {
    static auto method = javaClassStatic()->getStaticMethod<void(jlong)>("runStdFunction");
    method(javaClassStatic(), reinterpret_cast<jlong>(&func));
  }

  static void runStdFunctionImpl(alias_ref<JClass>, jlong ptr) {
    (*reinterpret_cast<std::function<void()>*>(ptr))();
  }

  static void OnLoad() {
    // We need the javaClassStatic so that the class lookup is cached and that
    // runStdFunction can be called from a ThreadScope-attached thread.
    javaClassStatic()->registerNatives({
        makeNativeMethod("runStdFunctionImpl", runStdFunctionImpl),
      });
  }
};
}
