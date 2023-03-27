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

import androidx.core.view.ViewCompat
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.LifecycleStep.getSteps
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.LayoutWithSizeSpecLifecycleTester
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester
import com.facebook.litho.widget.Text
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LayoutWithSizeSpecLifecycleTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  @Test
  fun onSetRootWithoutLayoutWithSizeSpec_shouldNotCallLifecycleMethods() {
    val info: List<StepInfo> = ArrayList()
    val component: Component =
        LayoutWithSizeSpecLifecycleTester.create(legacyLithoViewRule.context).steps(info).build()
    legacyLithoViewRule.setRoot(component).idle()
    assertThat(getSteps(info))
        .describedAs("No lifecycle methods should be called")
        .containsExactly(LifecycleStep.ON_CREATE_INITIAL_STATE)
  }

  @Test
  fun onSetRootWithLayoutWithSizeSpecAtRoot_shouldCallLifecycleMethods() {
    val info: List<StepInfo> = ArrayList()
    val component: Component =
        LayoutWithSizeSpecLifecycleTester.create(legacyLithoViewRule.context).steps(info).build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
  }

  @Test
  fun onSetRootWithLayoutWithSizeSpecWhichDoesNotRemeasure_shouldCallLifecycleMethods() {
    val info: List<StepInfo> = ArrayList()
    val c = legacyLithoViewRule.context
    val component: Component =
        Column.create(c)
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c)
                    .steps(info)
                    .importantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES))
            .build()
    legacyLithoViewRule.setRoot(component)
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
  }

  @Test
  fun onSetRootWithLayoutWithSizeSpecWhichRemeasure_shouldCallLifecycleMethods() {
    val info: List<StepInfo> = ArrayList()
    val c = legacyLithoViewRule.context
    val tracker = LifecycleTracker()
    val mountable: Component =
        MountSpecPureRenderLifecycleTester.create(c).lifecycleTracker(tracker).build()
    val component: Component =
        Row.create(c)
            .heightPx(100) // Ensures that nested tree is resolved only twice
            .child(LayoutWithSizeSpecLifecycleTester.create(c).steps(info).body(mountable))
            .child(Text.create(c).text("Hello World"))
            .build()
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
  }
}
