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

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.LifecycleStep
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LayoutSpec
object ComponentTreeTesterSpec {

  @PropDefault val unlockWaitingOnCreateLayout: CountDownLatch? = null

  @PropDefault val lockOnCreateLayoutFinish: CountDownLatch? = null

  @PropDefault val lifecycleSteps: List<LifecycleStep>? = null

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(optional = true) unlockWaitingOnCreateLayout: CountDownLatch?,
      @Prop(optional = true) lockOnCreateLayoutFinish: CountDownLatch?,
      @Prop(optional = true) lifecycleSteps: MutableList<LifecycleStep>?
  ): Component {
    unlockWaitingOnCreateLayout?.countDown()
    if (lockOnCreateLayoutFinish != null) {
      try {
        lockOnCreateLayoutFinish.await(5, TimeUnit.SECONDS)
      } catch (e: InterruptedException) {
        e.printStackTrace()
      }
    }
    if (lifecycleSteps != null && !lifecycleSteps.contains(LifecycleStep.ON_CREATE_LAYOUT)) {
      lifecycleSteps.add(LifecycleStep.ON_CREATE_LAYOUT)
    }
    return Column.create(c).build()
  }
}
