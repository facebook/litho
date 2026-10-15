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
import com.facebook.litho.LifecycleStep
import com.facebook.litho.layout.flexbox.Column
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ComponentTreeTester(
    private val unlockWaitingOnCreateLayout: CountDownLatch? = null,
    private val lockOnCreateLayoutFinish: CountDownLatch? = null,
    private val lifecycleSteps: MutableList<LifecycleStep>? = null,
) : KComponent() {

  override fun ComponentScope.render(): Component {

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
    return Column {}
  }
}
