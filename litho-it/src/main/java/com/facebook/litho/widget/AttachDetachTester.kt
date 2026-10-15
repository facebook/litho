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

package com.facebook.litho.widget

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ThreadUtils
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AttachDetachTester(
    private val steps: MutableList<String>,
    private val name: String,
    private val children: List<Component>? = null,
    private val extraThreadInfo: ConcurrentHashMap<String, Any?>? = null,
    private val releaseLatch: CountDownLatch? = null,
    private val countdownLatch: CountDownLatch? = null,
) : KComponent() {

  companion object {
    const val ON_ATTACHED = "ON_ATTACHED"
    const val ON_DETACHED = "ON_DETACHED"
    const val IS_MAIN_THREAD_ON_ATTACHED = "IS_MAIN_THREAD_ATTACHED"
    const val IS_MAIN_THREAD_LAYOUT = "IS_MAIN_THREAD_LAYOUT"
  }

  override fun ComponentScope.render(): Component {
    if (extraThreadInfo != null) {
      extraThreadInfo[IS_MAIN_THREAD_LAYOUT] = ThreadUtils.isMainThread
    }

    useEffect(name, steps, releaseLatch, countdownLatch, extraThreadInfo) {
      releaseLatch?.countDown()
      if (countdownLatch != null) {
        try {
          countdownLatch.await(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
          e.printStackTrace()
        }
      }
      steps.add("${name}:${ON_ATTACHED}")
      if (extraThreadInfo != null) {
        extraThreadInfo[IS_MAIN_THREAD_ON_ATTACHED] = ThreadUtils.isMainThread
      }

      onCleanup { steps.add("${name}:${ON_DETACHED}") }
    }

    return Row { this@AttachDetachTester.children?.forEach { child -> child(child) } }
  }
}
