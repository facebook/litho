/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.facebook.litho.ComponentsLifecycles.LeakDetector
import com.facebook.rendercore.MountItemsPool
import java.lang.RuntimeException
import java.util.WeakHashMap

/**
 * Callbacks that must be invoked to avoid leaking memory if using Components below ICS (API level
 * 14).
 */
object ComponentsLifecycles {

  private var trackedContexts: WeakHashMap<Context, LeakDetector>? = null

  @JvmStatic
  fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    onContextCreated(activity)
  }

  @JvmStatic
  fun onActivityDestroyed(activity: Activity) {
    onContextDestroyed(activity)
  }

  @JvmStatic
  fun onContextCreated(activity: Activity) {
    setManualCallbacksEnabledForPool()
    onContextCreated((activity as Context))
  }

  @JvmStatic
  fun onContextCreated(context: Context) {
    if (trackedContexts == null) {
      trackedContexts = WeakHashMap()
    }

    val old = trackedContexts!!.put(context, LeakDetector(context))
    if (old != null) {
      throw RuntimeException("Duplicate onContextCreated call for: $context")
    }
    onContextCreatedForPool(context)
  }

  @JvmStatic
  fun onContextDestroyed(context: Context) {
    val removed =
        trackedContexts?.remove(context)
            ?: error("onContextDestroyed called without onContextCreated for :$context")
    removed.clear()

    onContextDestroyedForPool(context)
  }

  private fun setManualCallbacksEnabledForPool() {
    MountItemsPool.sIsManualCallbacks = true
  }

  private fun onContextCreatedForPool(context: Context) {
    MountItemsPool.onContextCreated(context)
  }

  private fun onContextDestroyedForPool(context: Context) {
    MountItemsPool.onContextDestroyed(context)
  }

  private class LeakDetector(private var context: Context?) {
    fun clear() {
      context = null
    }

    fun finalize() {
      context?.let {
        // Post the error to the main thread to bring down the process -
        // exceptions on finalizer threads are ignored by default.
        Handler(Looper.getMainLooper()).post {
          throw RuntimeException("onContextDestroyed method not called for: $it")
        }
      }
    }
  }
}
