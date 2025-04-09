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

import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.LayoutSpecLifecycleTester
import com.facebook.litho.widget.LayoutSpecLifecycleTesterSpec
import com.facebook.litho.widget.events.EventWithoutAnnotation
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LayoutSpecLifecycleStatelessTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  @JvmField @Rule var backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  @Test
  fun lifecycle_onSetRootWithoutLayout_shouldNotCallLifecycleMethods() {
    val info: List<StepInfo> = ArrayList()
    val component = LayoutSpecLifecycleTester.create(lithoTestRule.context).steps(info).build()
    val testLithoView = lithoTestRule.createTestLithoView()
    testLithoView.lithoView.setComponent(component)
    lithoTestRule.idle()
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Only render lifecycle methods should be called")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun lifecycle_onSetRootWithLayout_shouldCallLifecycleMethods() {
    ComponentsConfiguration.isAnimationDisabled = false
    val info: List<StepInfo> = ArrayList()
    val component = LayoutSpecLifecycleTester.create(lithoTestRule.context).steps(info).build()
    lithoTestRule.render { component }
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_CREATE_TRANSITION,
            LifecycleStep.ON_ATTACHED)
    ComponentsConfiguration.isAnimationDisabled = true
  }

  @Test
  fun lifecycle_release_shouldCallLifecycleMethodOnDetach() {
    val info: MutableList<StepInfo> = ArrayList()
    val component = LayoutSpecLifecycleTester.create(lithoTestRule.context).steps(info).build()
    val testLithoView = lithoTestRule.render { component }
    info.clear()
    testLithoView.release()
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call onDetached")
        .containsExactly(LifecycleStep.ON_DETACHED)
  }

  @Test
  fun lifecycle_subsequentSetRoot_shouldCallLifecycleMethod() {
    val info: MutableList<StepInfo> = ArrayList()
    val component = LayoutSpecLifecycleTester.create(lithoTestRule.context).steps(info).build()
    val testLithoView = lithoTestRule.render { component }
    info.clear()
    val newComponent = LayoutSpecLifecycleTester.create(lithoTestRule.context).steps(info).build()
    testLithoView.setRoot(newComponent)
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun lifecycle_updateState_shouldCallLifecycleMethod() {
    val info: MutableList<StepInfo> = ArrayList()
    val caller = LayoutSpecLifecycleTesterSpec.Caller()
    val component =
        LayoutSpecLifecycleTester.create(lithoTestRule.context).steps(info).caller(caller).build()
    lithoTestRule.render { component }
    info.clear()
    caller.updateStateSync()
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_UPDATE_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun lifecycle_updateStateWithTransition_shouldCallLifecycleMethod() {
    val info: MutableList<StepInfo> = ArrayList()
    val caller = LayoutSpecLifecycleTesterSpec.Caller()
    val component =
        LayoutSpecLifecycleTester.create(lithoTestRule.context).steps(info).caller(caller).build()
    lithoTestRule.render { component }
    info.clear()
    caller.updateStateWithTransition()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    assertThat(LifecycleStep.getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_UPDATE_STATE_WITH_TRANSITION,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun lifecycle_dispatchEventWithoutAnnotation_shouldCallOnEventWithoutAnnotation() {
    val info: MutableList<StepInfo> = ArrayList()
    val caller = LayoutSpecLifecycleTesterSpec.Caller()
    val component =
        LayoutSpecLifecycleTester.create(lithoTestRule.context).steps(info).caller(caller).build()
    lithoTestRule.render { component }
    info.clear()
    val eventDispatched = EventWithoutAnnotation(1, true, "hello")
    caller.dispatchEventWithoutAnnotation(eventDispatched)
    val eventReceived = caller.eventWithoutAnnotation
    assertThat(eventReceived).isNotNull
    assertThat(eventDispatched.count).isSameAs(eventReceived?.count)
    assertThat(eventDispatched.isDirty).isSameAs(eventReceived?.isDirty)
    assertThat(eventDispatched.message).isSameAs(eventReceived?.message)
  }
}
