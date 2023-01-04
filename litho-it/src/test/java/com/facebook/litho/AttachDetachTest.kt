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

import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.HorizontalScroll
import com.facebook.litho.widget.MountSpecLifecycleTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class AttachDetachTest {
  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule()

  @Test
  fun horizontalScroll_releaseTree_childComponentShouldCallOnDetached() {
    val c = legacyLithoViewRule.context
    val lifecycleTracker = LifecycleTracker()
    legacyLithoViewRule.setRoot(
        HorizontalScroll.create(c)
            .contentProps(
                MountSpecLifecycleTester.create(c)
                    .intrinsicSize(Size(800, 600))
                    .lifecycleTracker(lifecycleTracker)
                    .build())
            .build())
    legacyLithoViewRule.attachToWindow().measure().layout()
    legacyLithoViewRule.release()
    assertThat(lifecycleTracker.steps)
        .describedAs(
            "Child component should call @OnDetached when parent HorizontalScroll is detached")
        .contains(LifecycleStep.ON_DETACHED)
  }
}
