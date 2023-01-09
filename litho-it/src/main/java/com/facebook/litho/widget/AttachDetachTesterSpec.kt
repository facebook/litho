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
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.ThreadUtils
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnAttached
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.Prop
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LayoutSpec
object AttachDetachTesterSpec {

  const val ON_ATTACHED = "ON_ATTACHED"
  const val ON_DETACHED = "ON_DETACHED"
  const val IS_MAIN_THREAD_ON_ATTACHED = "IS_MAIN_THREAD_ATTACHED"
  const val IS_MAIN_THREAD_LAYOUT = "IS_MAIN_THREAD_LAYOUT"

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop steps: List<String>,
      @Prop(optional = true) children: List<@JvmWildcard Component.Builder<*>>?,
      @Prop(optional = true) extraThreadInfo: ConcurrentHashMap<String, Any?>?
  ): Component {
    val containerBuilder = Row.create(c)
    if (children != null) {
      for (child in children) {
        if (child is AttachDetachTester.Builder) {
          containerBuilder.child(child.steps(steps))
        } else {
          containerBuilder.child(child)
        }
      }
    }
    if (extraThreadInfo != null) {
      extraThreadInfo[IS_MAIN_THREAD_LAYOUT] = ThreadUtils.isMainThread()
    }
    return containerBuilder.build()
  }

  @JvmStatic
  @OnAttached
  fun onAttached(
      c: ComponentContext,
      @Prop name: String,
      @Prop steps: MutableList<String>,
      @Prop(optional = true) releaseLatch: CountDownLatch?,
      @Prop(optional = true) countdownLatch: CountDownLatch?,
      @Prop(optional = true) extraThreadInfo: ConcurrentHashMap<String, Any?>?
  ) {
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
      extraThreadInfo[IS_MAIN_THREAD_ON_ATTACHED] = ThreadUtils.isMainThread()
    }
  }

  @JvmStatic
  @OnDetached
  fun onDetached(c: ComponentContext, @Prop name: String, @Prop steps: MutableList<String>) {
    steps.add("${name}:${ON_DETACHED}")
  }
}
