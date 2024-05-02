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

import android.graphics.Rect
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.LayoutSpecLifecycleTester
import com.facebook.litho.widget.MountSpecLifecycleTester
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class LithoVisibilityEventsControllerTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()
  private lateinit var lithoView: LithoView
  private lateinit var component: LayoutSpecLifecycleTester
  private lateinit var mountableComponent: MountSpecLifecycleTester
  private lateinit var lithoLifecycleProviderDelegate: LithoVisibilityEventsControllerDelegate
  private lateinit var steps: MutableList<LifecycleStep.StepInfo>
  private lateinit var lifecycleTracker: LifecycleTracker

  @Before
  fun setup() {
    val c = legacyLithoViewRule.context
    lithoLifecycleProviderDelegate = LithoVisibilityEventsControllerDelegate()
    steps = ArrayList<LifecycleStep.StepInfo>()
    lifecycleTracker = LifecycleTracker()
    component = LayoutSpecLifecycleTester.create(c).widthPx(10).heightPx(5).steps(steps).build()
    mountableComponent =
        MountSpecLifecycleTester.create(c)
            .widthPx(10)
            .heightPx(5)
            .lifecycleTracker(lifecycleTracker)
            .build()
    lithoView = LithoView.create(c, Column.create(c).build(), lithoLifecycleProviderDelegate)
    legacyLithoViewRule.useLithoView(lithoView)
  }

  @After
  fun resetViews() {
    steps.clear()
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.DESTROYED)
  }

  @Test
  fun lithoLifecycleProviderDelegateInvisibleToVisibleTest() {
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(5))
        .measure()
        .layout()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 10, 10), true)
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event is expected to be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    steps.clear()
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event is expected to be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun lithoLifecycleProviderDelegateInvisibleToInvisibleTest() {
    legacyLithoViewRule.setRoot(component).setSizeSpecs(exactly(10), exactly(5))
    legacyLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10)
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event is expected to be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    steps.clear()
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event is expected to be dispatched")
        .isEmpty()
  }

  @Test
  fun lithoLifecycleProviderDelegateVisibleToVisibleTest() {
    legacyLithoViewRule.setRoot(component).setSizeSpecs(exactly(10), exactly(5))
    legacyLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10)
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event is expected to be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    steps.clear()
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event is expected to be dispatched")
        .isEmpty()
  }

  @Test
  fun lithoLifecycleProviderDelegateVisibleToDestroyedTest() {
    legacyLithoViewRule.setRoot(mountableComponent).setSizeSpecs(exactly(10), exactly(5))
    legacyLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10)
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_VISIBLE)
    assertThat(lifecycleTracker.steps)
        .describedAs("Visible event is expected to be dispatched")
        .contains(LifecycleStep.ON_MOUNT)
    lifecycleTracker.reset()
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.DESTROYED)
    assertThat(lifecycleTracker.steps)
        .describedAs("Visible event is expected to be dispatched")
        .contains(LifecycleStep.ON_UNMOUNT)
  }

  @Test
  fun lithoLifecycleProviderComponentTreeResetVisibilityFlags() {
    // In the new implementation, `setVisibilityHintNonRecursive` is always called in
    // `setLithoView`, so mHasVisibilityHint will be still true after set new Component Tree
    legacyLithoViewRule.setRoot(component).setSizeSpecs(exactly(10), exactly(5))
    legacyLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10)
    lithoLifecycleProviderDelegate.moveToVisibilityState(
        LithoVisibilityEventsController.LithoVisibilityState.HINT_INVISIBLE)
    var hasVisibilityHint: Boolean =
        Whitebox.getInternalState<Boolean>(legacyLithoViewRule.lithoView, "mHasVisibilityHint")
    var pauseMountingWhileVisibilityHintFalse: Boolean =
        Whitebox.getInternalState<Boolean>(
            legacyLithoViewRule.lithoView, "mPauseMountingWhileVisibilityHintFalse")
    assertThat(hasVisibilityHint).isTrue
    assertThat(pauseMountingWhileVisibilityHintFalse).isTrue

    legacyLithoViewRule.useComponentTree(ComponentTree.create(legacyLithoViewRule.context).build())
    hasVisibilityHint =
        Whitebox.getInternalState<Boolean>(legacyLithoViewRule.lithoView, "mHasVisibilityHint")
    pauseMountingWhileVisibilityHintFalse =
        Whitebox.getInternalState<Boolean>(
            legacyLithoViewRule.lithoView, "mPauseMountingWhileVisibilityHintFalse")
    assertThat(hasVisibilityHint).isFalse
    assertThat(pauseMountingWhileVisibilityHintFalse).isFalse
  }
}
