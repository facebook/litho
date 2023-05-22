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
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.ViewGroupWithLithoViewChildren
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.ComponentWrapperTester
import com.facebook.litho.widget.HorizontalScroll
import com.facebook.litho.widget.LayoutSpecLifecycleTester
import com.facebook.litho.widget.LayoutSpecVisibilityEventTester
import com.facebook.litho.widget.TextDrawable
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.visibility.VisibilityItem
import com.facebook.rendercore.visibility.VisibilityMountExtension
import com.facebook.rendercore.visibility.VisibilityMountExtension.VisibilityMountExtensionState
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class VisibilityEventsTest {

  private val left = 0
  private val right = 15

  private lateinit var context: ComponentContext
  private lateinit var lithoView: LithoView
  private lateinit var parent: FrameLayout

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  @Before
  fun setup() {
    context = legacyLithoViewRule.context
    lithoView = LithoView(context)
    legacyLithoViewRule.useLithoView(lithoView)
    parent =
        FrameLayout(context.androidContext).apply {
          left = 0
          top = 0
          right = 15
          bottom = 15
          addView(lithoView)
        }
  }

  @Test
  fun testVisibleEvent() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(5))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 20, 20), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun testNoVisibleEventWhenNotProcessingVisibleOutputs() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(5))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)

    // invoke notifyVisibleBoundsChanged, but pass 'false' to process visibility outputs.
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 20, 20), false)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should still not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun testVisibleEventWithHeightRatio() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .visibleHeightRatio(0.4f)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(5))
        .measure()
        .layout()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 10, 1), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 10, 2), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 10, 3), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 10, 4), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 10, 5), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 10, 6), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 10, 7), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun testVisibleEventWithWidthRatio() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .visibleWidthRatio(0.4f)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(5))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 3, 10), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 5, 10), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun testVisibleEventWithHeightAndWidthRatio() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .visibleWidthRatio(0.4f)
            .visibleHeightRatio(0.4f)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(5))
        .measure()
        .layout()

    // Neither width or height are in visible range
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 3, 6), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)

    // Width but not height are in visible range
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 5, 6), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)

    // Height but not width are in visible range
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 3, 8), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)

    // Height and width are both in visible range
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 5, 8), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun testFocusedOccupiesHalfViewport() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c).steps(steps).widthPx(10).heightPx(10).build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Focused visible event should be dispatched")
        .contains(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
  }

  @Test
  fun testFocusedOccupiesLessThanHalfViewport() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component = LayoutSpecLifecycleTester.create(c).steps(steps).widthPx(10).heightPx(3).build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Focused visible event should be dispatched")
        .contains(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
  }

  @Test
  fun testMultipleFocusAndUnfocusEvents() {
    val content = TestViewComponent.create(context).build()
    val focusedHandler = EventHandler<FocusedVisibleEvent>(content, 2)
    val unfocusedHandler = EventHandler<UnfocusedVisibleEvent>(content, 3)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .focusedHandler(focusedHandler)
                    .unfocusedHandler(unfocusedHandler)
                    .widthPx(15)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 8))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 0, 0), true)

    // Mount test view in the middle of the view port (focused)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 8, right, 13), true)
    assertThat(content.dispatchedEventHandlers).containsOnly(focusedHandler)

    // Mount test view on the edge of the viewport (not focused)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 14, right, 19), true)
    assertThat(content.dispatchedEventHandlers).containsOnly(unfocusedHandler)

    // Mount test view in the middle of the view port (focused)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 8, right, 14), true)
    assertThat(content.dispatchedEventHandlers).containsOnly(focusedHandler)

    // Mount test view on the edge of the viewport (not focused)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 1, right, 6), true)
    assertThat(content.dispatchedEventHandlers).containsOnly(unfocusedHandler)
  }

  @Test
  fun testFullImpressionEvent() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Full Impression visible event should be dispatched")
        .contains(LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT)
  }

  @Test
  fun testVisibility1fTop() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .visibleHeightRatio(1f)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun testVisibility1fBottom() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .visibleHeightRatio(1f)
            .widthPx(10)
            .heightPx(5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun testInvisibleEvent() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 5), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
  }

  @Test
  fun testVisibleRectChangedEventItemVisible() {
    val content = TestViewComponent.create(context).build()
    val visibilityChangedHandler = EventHandler<VisibilityChangedEvent>(content, 3)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    var visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(10)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(100f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 4), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(4)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(40f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
    lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 5), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(5)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(50f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
  }

  @Test
  fun testVisibleRectChangedEventItemNotVisible() {
    val content = TestViewComponent.create(context).build()
    val visibilityChangedHandler = EventHandler<VisibilityChangedEvent>(content, 3)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 5), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    val visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(0)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(0)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(0.0f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(0.0f)
  }

  @Test
  fun whenItemIsFullyVisible_VisibleTopAndLeftShouldBe0() {
    val content = TestViewComponent.create(context).build()
    val visibilityChangedHandler = EventHandler<VisibilityChangedEvent>(content, 3)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(1_000))
        .measure()
        .layout()
    val visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleTop).isEqualTo(0)
    assertThat(visibilityChangedEvent?.visibleLeft).isEqualTo(0)
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(10)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(100f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
  }

  @Test
  fun testVisibleRectChangedEventLargeView() {
    val content = TestViewComponent.create(context).build()
    val visibilityChangedHandler = EventHandler<VisibilityChangedEvent>(content, 3)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(1_000))
        .measure()
        .layout()
    var visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleTop).isEqualTo(0)
    assertThat(visibilityChangedEvent?.visibleLeft).isEqualTo(0)
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(10)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(100f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 4), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(4)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(40f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
    lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 5), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(5)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(50f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
    lithoView.notifyVisibleBoundsChanged(Rect(5, 5, 10, 10), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleTop).isEqualTo(5)
    assertThat(visibilityChangedEvent?.visibleLeft).isEqualTo(5)
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(5)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(5)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(50f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(50f)
  }

  @Test
  fun testVisibleAndInvisibleEvents() {
    val c = legacyLithoViewRule.context
    val steps: MutableList<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c)
            .steps(steps)
            .widthPx(10)
            .heightPx(5)
            .marginPx(YogaEdge.TOP, 5)
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event should be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    steps.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 5), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    steps.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 3, right, 9), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    steps.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 10, right, 15), true)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun testMultipleVisibleEvents() {
    val c = legacyLithoViewRule.context
    val steps1: MutableList<StepInfo> = mutableListOf()
    val steps2: MutableList<StepInfo> = mutableListOf()
    val steps3: MutableList<StepInfo> = mutableListOf()
    val component1 =
        LayoutSpecLifecycleTester.create(c).steps(steps1).widthPx(10).heightPx(5).build()
    val component2 =
        LayoutSpecLifecycleTester.create(c).steps(steps2).widthPx(10).heightPx(5).build()
    val component3 =
        LayoutSpecLifecycleTester.create(c).steps(steps3).widthPx(10).heightPx(5).build()
    val root =
        Column.create(context)
            .key("root")
            .child(component1)
            .child(component2)
            .child(component3)
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_VISIBILITY_CHANGED)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 0), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_VISIBILITY_CHANGED)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 3), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_VISIBILITY_CHANGED)
    steps1.clear()
    steps2.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 3, right, 11), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_VISIBILITY_CHANGED)
  }

  @Test
  fun testMultipleVisibleAndInvisibleEvents() {
    val c = legacyLithoViewRule.context
    val steps1: MutableList<StepInfo> = mutableListOf()
    val steps2: MutableList<StepInfo> = mutableListOf()
    val steps3: MutableList<StepInfo> = mutableListOf()
    val component1 =
        LayoutSpecLifecycleTester.create(c).steps(steps1).widthPx(10).heightPx(5).build()
    val component2 =
        LayoutSpecLifecycleTester.create(c).steps(steps2).widthPx(10).heightPx(5).build()
    val component3 =
        LayoutSpecLifecycleTester.create(c).steps(steps3).widthPx(10).heightPx(5).build()
    val root =
        Column.create(context)
            .key("root")
            .child(component1)
            .child(component2)
            .child(component3)
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(15), exactly(15))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 15), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 0), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 0), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 3), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 3, right, 11), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 5, right, 11), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
  }

  @Test
  fun testSkipFullyVisible() {
    val c = legacyLithoViewRule.context
    val steps1: MutableList<StepInfo> = mutableListOf()
    val steps2: MutableList<StepInfo> = mutableListOf()
    val steps3: MutableList<StepInfo> = mutableListOf()
    val component1 =
        LayoutSpecLifecycleTester.create(c).steps(steps1).widthPx(10).heightPx(5).build()
    val component2 =
        LayoutSpecLifecycleTester.create(c).steps(steps2).widthPx(10).heightPx(5).build()
    val component3 =
        LayoutSpecLifecycleTester.create(c).steps(steps3).widthPx(10).heightPx(5).build()
    val root =
        Column.create(context)
            .key("root")
            .child(component1)
            .child(component2)
            .child(component3)
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(15), exactly(15))
        .measure()
        .layout()
    var visibilityItemMap: Map<String?, VisibilityItem?>? = getCurrentVisibilityToItemMap()
    visibilityItemMap?.keys?.forEach { key ->
      val item = visibilityItemMap?.get(key)
      assertThat(item?.wasFullyVisible()).isTrue
    }
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 15), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    //
    visibilityItemMap = getCurrentVisibilityToItemMap()
    assertThat(visibilityItemMap?.size).isEqualTo(3)
    visibilityItemMap?.keys?.forEach { key ->
      val item = visibilityItemMap?.get(key)
      assertThat(item?.wasFullyVisible()).isTrue
    }
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 3, right, 12), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    visibilityItemMap = getCurrentVisibilityToItemMap()
    assertThat(visibilityItemMap?.size).isEqualTo(3)
    var fullyVisibleCount = 0
    visibilityItemMap?.keys?.forEach { key ->
      val item = visibilityItemMap?.get(key)
      fullyVisibleCount += if (item?.wasFullyVisible() == true) 1 else 0
    }
    assertThat(fullyVisibleCount).isEqualTo(1)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 0), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    visibilityItemMap = getCurrentVisibilityToItemMap()
    assertThat(visibilityItemMap?.size).isEqualTo(0)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 0), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 3, right, 12), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    visibilityItemMap = getCurrentVisibilityToItemMap()
    assertThat(visibilityItemMap?.size).isEqualTo(3)
    fullyVisibleCount = 0
    visibilityItemMap?.keys?.forEach { key ->
      val item = visibilityItemMap?.get(key)
      fullyVisibleCount += if (item?.wasFullyVisible() == true) 1 else 0
    }
    assertThat(fullyVisibleCount).isEqualTo(1)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 15), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Invisible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_INVISIBLE)
    visibilityItemMap = getCurrentVisibilityToItemMap()
    assertThat(visibilityItemMap?.size).isEqualTo(3)
    visibilityItemMap?.keys?.forEach { key ->
      val item = visibilityItemMap?.get(key)
      assertThat(item?.wasFullyVisible()).isTrue
    }
  }

  @Test
  fun testDispatchFocusedHandler() {
    val c = legacyLithoViewRule.context
    val steps1: MutableList<StepInfo> = mutableListOf()
    val steps2: MutableList<StepInfo> = mutableListOf()
    val steps3: MutableList<StepInfo> = mutableListOf()
    val component1 =
        LayoutSpecLifecycleTester.create(c).steps(steps1).widthPx(10).heightPx(5).build()
    val component2 =
        LayoutSpecLifecycleTester.create(c).steps(steps2).widthPx(10).heightPx(5).build()
    val component3 =
        LayoutSpecLifecycleTester.create(c).steps(steps3).widthPx(10).heightPx(5).build()
    val root =
        Column.create(context)
            .key("root")
            .child(component1)
            .child(component2)
            .child(component3)
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(15))
        .measure()
        .layout()
    val visibilityItemLongSparseArray: Map<String?, VisibilityItem?>? =
        getCurrentVisibilityToItemMap()
    visibilityItemLongSparseArray?.keys?.forEach { key ->
      val item = visibilityItemLongSparseArray?.get(key)
      assertThat(item?.wasFullyVisible()).isTrue
    }
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Focused visible event should be dispatched")
        .contains(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Focused visible event should be dispatched")
        .contains(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Focused visible event should be dispatched")
        .contains(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Unfocused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Unfocused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Unfocused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 4, right, 15), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Focused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Focused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Focused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Unfocused visible event should be dispatched")
        .contains(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Unfocused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Unfocused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    steps1.clear()
    steps2.clear()
    steps3.clear()
    legacyLithoViewRule.lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 15), true)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Focused visible event should be dispatched")
        .contains(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Focused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Focused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps1))
        .describedAs("Unfocused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps2))
        .describedAs("Unfocused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(steps3))
        .describedAs("Unfocused visible event should not be dispatched")
        .doesNotContain(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
  }

  @Test
  fun testDetachWithReleasedTreeTriggersInvisibilityItems() {
    val content = TestViewComponent.create(context).build()
    val invisibleEventHandler = EventHandler<InvisibleEvent>(content, 2)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 10), true)
    lithoView.release()
    assertThat(content.dispatchedEventHandlers).contains(invisibleEventHandler)
  }

  @Test
  fun testSetComponentWithDifferentKeyGeneratesVisibilityEvents() {
    val component1 = TestViewComponent.create(context).key("component1").build()
    val visibleEventHandler1 = EventHandler<VisibleEvent>(component1, 1)
    val invisibleEventHandler1 = EventHandler<InvisibleEvent>(component1, 2)
    val focusedEventHandler1 = EventHandler<FocusedVisibleEvent>(component1, 3)
    val unfocusedEventHandler1 = EventHandler<UnfocusedVisibleEvent>(component1, 4)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(component1)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .focusedHandler(focusedEventHandler1)
                    .unfocusedHandler(unfocusedEventHandler1)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    assertThat(component1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(component1.dispatchedEventHandlers).contains(focusedEventHandler1)
    val component2 = TestViewComponent.create(context).key("component2").build()
    val visibleEventHandler2 = EventHandler<VisibleEvent>(component2, 3)
    val newRoot =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(component2)
                    .visibleHandler(visibleEventHandler2)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule.setRoot(newRoot)
    ComponentTestHelper.measureAndLayout(lithoView)
    assertThat(component1.dispatchedEventHandlers).contains(invisibleEventHandler1)
    assertThat(component1.dispatchedEventHandlers).contains(unfocusedEventHandler1)
    assertThat(component2.dispatchedEventHandlers).contains(visibleEventHandler2)
  }

  @Test
  fun testSetDifferentComponentTreeWithSameKeysStillCallsInvisibleAndVisibleEvents() {
    val firstComponent = TestViewComponent.create(context).build()
    val secondComponent = TestViewComponent.create(context).build()
    val visibleEventHandler1 = EventHandler<VisibleEvent>(firstComponent, 1)
    val invisibleEventHandler1 = EventHandler<InvisibleEvent>(firstComponent, 2)
    val visibleEventHandler2 = EventHandler<VisibleEvent>(secondComponent, 1)
    val invisibleEventHandler2 = EventHandler<InvisibleEvent>(secondComponent, 2)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(firstComponent)
                    .visibleHandler(visibleEventHandler1)
                    .invisibleHandler(invisibleEventHandler1)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    assertThat(firstComponent.dispatchedEventHandlers).containsExactly(visibleEventHandler1)
    firstComponent.dispatchedEventHandlers.clear()
    val newRoot =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(secondComponent)
                    .visibleHandler(visibleEventHandler2)
                    .invisibleHandler(invisibleEventHandler2)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).build())
        .setRoot(newRoot)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    assertThat(firstComponent.dispatchedEventHandlers).containsExactly(invisibleEventHandler1)
    assertThat(secondComponent.dispatchedEventHandlers).containsExactly(visibleEventHandler2)
  }

  @Test
  fun testSetComponentTreeToNullDispatchesInvisibilityEvents() {
    val component = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(component, 1)
    val invisibleEventHandler = EventHandler<InvisibleEvent>(component, 2)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(component)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    assertThat(component.dispatchedEventHandlers).containsExactly(visibleEventHandler)
    component.dispatchedEventHandlers.clear()
    legacyLithoViewRule.useComponentTree(null)
    assertThat(component.dispatchedEventHandlers).containsExactly(invisibleEventHandler)
  }

  @Test
  fun testTransientStateDoesNotTriggerVisibilityEvents() {
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    lithoView.notifyVisibleBoundsChanged(Rect(0, -10, 10, -5), true)
    content.dispatchedEventHandlers.clear()
    lithoView.setHasTransientState(true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.setMountStateDirty()
    lithoView.notifyVisibleBoundsChanged(Rect(0, -10, 10, -5), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.setHasTransientState(false)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
  }

  @Test
  fun visibilityOutputs_setTransientStateFalse_parentInTransientState_processVisibilityOutputs() {
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    val view = lithoView.parent as View
    view.setHasTransientState(true)
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    lithoView.notifyVisibleBoundsChanged(Rect(0, -10, 10, -5), true)
    content.dispatchedEventHandlers.clear()
    lithoView.setHasTransientState(true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.setMountStateDirty()
    lithoView.notifyVisibleBoundsChanged(Rect(0, -10, 10, -5), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.setHasTransientState(false)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
    view.setHasTransientState(false)
  }

  @Test
  fun testRemovingComponentTriggersInvisible() {
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 1)
    val invisibleEventHandler = EventHandler<InvisibleEvent>(content, 2)
    val wrappedContent =
        Wrapper.create(context)
            .delegate(content)
            .widthPx(10)
            .heightPx(5)
            .visibleHandler(visibleEventHandler)
            .invisibleHandler(invisibleEventHandler)
            .build()
    val root = Column.create(context).child(wrappedContent).build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
    assertThat(content.dispatchedEventHandlers).doesNotContain(invisibleEventHandler)
    content.dispatchedEventHandlers.clear()
    lithoView.setComponent(Column.create(context).build())
    assertThat(content.dispatchedEventHandlers).contains(invisibleEventHandler)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    content.dispatchedEventHandlers.clear()
    lithoView.setComponent(Column.create(context).child(wrappedContent).build())
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
    assertThat(content.dispatchedEventHandlers).doesNotContain(invisibleEventHandler)
  }

  @Test
  fun testMultipleVisibilityEventsOnSameNode() {
    val context = legacyLithoViewRule.context
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val lifecycleTracker3 = LifecycleTracker()
    val content1 =
        ComponentWrapperTester.create(context)
            .content(Column.create(context).build())
            .lifecycleTracker(lifecycleTracker1)
            .build()
    val content2 =
        ComponentWrapperTester.create(context)
            .content(content1)
            .lifecycleTracker(lifecycleTracker2)
            .build()
    val content3 =
        ComponentWrapperTester.create(context)
            .content(content2)
            .lifecycleTracker(lifecycleTracker3)
            .build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(this.context).build())
        .setRoot(content3)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(lifecycleTracker1.steps)
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(lifecycleTracker2.steps)
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(lifecycleTracker3.steps)
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(lifecycleTracker1.steps)
        .describedAs("Focus event should be dispatched")
        .contains(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(lifecycleTracker2.steps)
        .describedAs("Focus event should be dispatched")
        .contains(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(lifecycleTracker3.steps)
        .describedAs("Focus event should be dispatched")
        .contains(LifecycleStep.ON_FOCUSED_EVENT_VISIBLE)
    assertThat(lifecycleTracker1.steps)
        .describedAs("FullImpressionVisible event should be dispatched")
        .contains(LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT)
    assertThat(lifecycleTracker2.steps)
        .describedAs("FullImpressionVisible event should be dispatched")
        .contains(LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT)
    assertThat(lifecycleTracker3.steps)
        .describedAs("FullImpressionVisible event should be dispatched")
        .contains(LifecycleStep.ON_FULL_IMPRESSION_VISIBLE_EVENT)
    lifecycleTracker1.reset()
    lifecycleTracker2.reset()
    lifecycleTracker3.reset()
    legacyLithoViewRule.lithoView.unbind()
    assertThat(lifecycleTracker1.steps)
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(lifecycleTracker2.steps)
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(lifecycleTracker3.steps)
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
    assertThat(lifecycleTracker1.steps)
        .describedAs("Unfocus event should be dispatched")
        .contains(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    assertThat(lifecycleTracker2.steps)
        .describedAs("Unfocus event should be dispatched")
        .contains(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
    assertThat(lifecycleTracker3.steps)
        .describedAs("Unfocus event should be dispatched")
        .contains(LifecycleStep.ON_UNFOCUSED_EVENT_VISIBLE)
  }

  @Test
  fun testSetVisibilityHint() {
    val component = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(component, 1)
    val invisibleEventHandler = EventHandler<InvisibleEvent>(component, 2)
    val focusedEventHandler = EventHandler<FocusedVisibleEvent>(component, 3)
    val unfocusedEventHandler = EventHandler<UnfocusedVisibleEvent>(component, 4)
    val fullImpressionHandler = EventHandler<FullImpressionVisibleEvent>(component, 5)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(component)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .focusedHandler(focusedEventHandler)
                    .unfocusedHandler(unfocusedEventHandler)
                    .fullImpressionHandler(fullImpressionHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    assertThat(component.dispatchedEventHandlers).contains(visibleEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(focusedEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(fullImpressionHandler)
    component.dispatchedEventHandlers.clear()
    lithoView.setVisibilityHint(false, false)
    assertThat(component.dispatchedEventHandlers).contains(invisibleEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(unfocusedEventHandler)
    component.dispatchedEventHandlers.clear()
    lithoView.setVisibilityHint(true, false)
    assertThat(component.dispatchedEventHandlers).contains(visibleEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(focusedEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(fullImpressionHandler)
  }

  @Test
  fun testSetVisibilityHintRecursive() {
    // TODO(festevezga, T68365308) - replace with SimpleMountSpecTesterSpec
    val testComponentInner = TestDrawableComponent.create(context).build()
    val visibleEventHandlerInner = EventHandler<VisibleEvent>(testComponentInner, 1)
    val invisibleEventHandlerInner = EventHandler<InvisibleEvent>(testComponentInner, 2)
    val mountedTestComponentInner =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(testComponentInner)
                    .visibleHandler(visibleEventHandlerInner)
                    .invisibleHandler(invisibleEventHandlerInner)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).build())
        .setRoot(mountedTestComponentInner)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    val child = ComponentTestHelper.mountComponent(context, mountedTestComponentInner, true, true)
    assertThat(testComponentInner.dispatchedEventHandlers.size).isEqualTo(1)
    assertThat(testComponentInner.dispatchedEventHandlers.contains(visibleEventHandlerInner))
    testComponentInner.dispatchedEventHandlers.clear()
    val viewGroup = ViewGroupWithLithoViewChildren(context.androidContext)
    val parent = child.parent as ViewGroup
    parent.removeView(child)
    viewGroup.addView(child)
    val parentView =
        ComponentTestHelper.mountComponent(
            context, TestViewComponent.create(context).testView(viewGroup).build(), true, true)
    parentView.setVisibilityHint(false, false)
    assertThat(testComponentInner.dispatchedEventHandlers.size).isEqualTo(1)
    assertThat(testComponentInner.dispatchedEventHandlers.contains(invisibleEventHandlerInner))
    testComponentInner.dispatchedEventHandlers.clear()
    parentView.setVisibilityHint(true, false)
    assertThat(testComponentInner.dispatchedEventHandlers.size).isEqualTo(1)
    assertThat(testComponentInner.dispatchedEventHandlers.contains(visibleEventHandlerInner))
  }

  @Test
  fun testMultipleVisibleEventsIncrementalMountDisabled() {
    val content1 = TestViewComponent.create(context).build()
    val content2 = TestViewComponent.create(context).build()
    val content3 = TestViewComponent.create(context).build()
    val visibleEventHandler1 = EventHandler<VisibleEvent>(content1, 1)
    val visibleEventHandler2 = EventHandler<VisibleEvent>(content2, 2)
    val visibleEventHandler3 = EventHandler<VisibleEvent>(content3, 3)
    val visibilityChangedHandler = EventHandler<VisibilityChangedEvent>(content3, 4)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content1)
                    .visibleHandler(visibleEventHandler1)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(context)
                    .delegate(content2)
                    .visibleHandler(visibleEventHandler2)
                    .widthPx(10)
                    .heightPx(5))
            .child(
                Wrapper.create(context)
                    .delegate(content3)
                    .visibleHandler(visibleEventHandler3)
                    .visibilityChangedHandler(visibilityChangedHandler)
                    .widthPx(10)
                    .heightPx(5))
            .build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).incrementalMount(false).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).contains(visibilityChangedHandler)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 0), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).contains(visibilityChangedHandler)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 0, right, 3), true)
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibilityChangedHandler)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(left, 3, right, 11), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).contains(visibilityChangedHandler)
  }

  @Test
  fun testSetVisibilityHintIncrementalMountDisabled() {
    val component = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(component, 1)
    val invisibleEventHandler = EventHandler<InvisibleEvent>(component, 2)
    val focusedEventHandler = EventHandler<FocusedVisibleEvent>(component, 3)
    val unfocusedEventHandler = EventHandler<UnfocusedVisibleEvent>(component, 4)
    val fullImpressionHandler = EventHandler<FullImpressionVisibleEvent>(component, 5)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(component)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .focusedHandler(focusedEventHandler)
                    .unfocusedHandler(unfocusedEventHandler)
                    .fullImpressionHandler(fullImpressionHandler)
                    .widthPx(10)
                    .heightPx(10))
            .build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).incrementalMount(false).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    assertThat(component.dispatchedEventHandlers).contains(visibleEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(focusedEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(fullImpressionHandler)
    component.dispatchedEventHandlers.clear()
    lithoView.setVisibilityHint(false, false)
    assertThat(component.dispatchedEventHandlers).contains(invisibleEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(unfocusedEventHandler)
    component.dispatchedEventHandlers.clear()
    lithoView.setVisibilityHint(true, false)
    assertThat(component.dispatchedEventHandlers).contains(visibleEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(focusedEventHandler)
    assertThat(component.dispatchedEventHandlers).contains(fullImpressionHandler)
  }

  @Test
  fun testVisibilityProcessingNoScrollChange() {

    // TODO(T118124771): Test failure because of incorrect visible bounds
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build()
    legacyLithoViewRule
        .useComponentTree(ComponentTree.create(context).incrementalMount(false).build())
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(5))
        .measure()
        .layout()
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.bottom = 10
    lithoView.performLayout(true, 0, 0, right, 10)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
  }

  @Test
  fun setNewComponentTree_noMount_noVisibilityEventsDispatched() {
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val invisibleEventHandler = EventHandler<InvisibleEvent>(content, 1)
    val root =
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibleHandler(visibleEventHandler)
                    .invisibleHandler(invisibleEventHandler)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build()
    val componentTree = ComponentTree.create(context, root).build()
    lithoView.componentTree = componentTree
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    content.dispatchedEventHandlers.clear()
    val newComponentTree = ComponentTree.create(context, root).build()
    lithoView.componentTree = newComponentTree
    assertThat(content.dispatchedEventHandlers).doesNotContain(invisibleEventHandler)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
  }

  @Test
  fun processVisibility_componentIsMounted() {
    val textDrawableOutput = Output<Any>()
    val viewOutput = Output<Any>()
    val nullOutput = Output<Any>()
    val root: Component =
        Column.create(context)
            .child(
                LayoutSpecVisibilityEventTester.create(context)
                    .textOutput(textDrawableOutput)
                    .viewOutput(viewOutput)
                    .nullOutput(nullOutput))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(100), exactly(100))
        .measure()
        .layout()
    assertThat(textDrawableOutput.get()).isNotNull
    assertThat(textDrawableOutput.get()).isInstanceOf(TextDrawable::class.java)
    assertThat(viewOutput.get()).isNotNull
    assertThat(viewOutput.get()).isInstanceOf(ComponentHost::class.java)
    assertThat(nullOutput.get()).isNull()
  }

  private fun getCurrentVisibilityToItemMap(): Map<String?, VisibilityItem?>? {
    val lithoHostListenerCoordinator: LithoHostListenerCoordinator? =
        Whitebox.getInternalState(lithoView, "mLithoHostListenerCoordinator")
    val extensionState: ExtensionState<VisibilityMountExtensionState>? =
        Whitebox.getInternalState(lithoHostListenerCoordinator, "mVisibilityExtensionState")
    return if (extensionState != null) {
      VisibilityMountExtension.getVisibilityIdToItemMap(extensionState)
    } else {
      null
    }
  }

  @Test
  fun testVisibleEventHorizontalScroll() {

    // TODO(T118124771): Test failure because of incorrect visible bounds
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val c = legacyLithoViewRule.context
    val stepsList: MutableList<List<StepInfo>> = mutableListOf()
    val numberOfItems = 2
    val component =
        HorizontalScroll.create(c)
            .incrementalMountEnabled(true)
            .contentProps(createHorizontalScrollChildren(c, numberOfItems, stepsList))
            .build()
    legacyLithoViewRule
        .setRoot(component)
        .attachToWindow()
        .setSizeSpecs(exactly(10), exactly(10))
        .measure()
        .layout()
    assertThat(LifecycleStep.getSteps(stepsList[0]))
        .describedAs("Visible event should not be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
    assertThat(LifecycleStep.getSteps(stepsList[1]))
        .describedAs("Visible event should be dispatched")
        .doesNotContain(LifecycleStep.ON_EVENT_VISIBLE)
    val lScrollView =
        legacyLithoViewRule.lithoView.getMountItemAt(0).getContent() as HorizontalScrollView
    ReflectionHelpers.setField(lScrollView, "mScrollX", 10)
    ReflectionHelpers.setField(lScrollView, "mScrollY", 0)
    lScrollView.scrollBy(10, 0)
    legacyLithoViewRule.dispatchGlobalLayout()
    assertThat(LifecycleStep.getSteps(stepsList[1]))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun testOnInvisibleWhenVisibleRectBecomesEmpty() {
    // Test only relevant when IM continues when the visible rect is empty.
    if (!ComponentsConfiguration.shouldContinueIncrementalMountWhenVisibileRectIsEmpty) {
      return
    }
    val c = legacyLithoViewRule.context
    val steps: MutableList<StepInfo> = mutableListOf()
    val component =
        LayoutSpecLifecycleTester.create(c).steps(steps).widthPx(10).heightPx(10).build()

    // Set root with non-empty size specs.
    legacyLithoViewRule
        .setRoot(component)
        .setSizeSpecs(exactly(10), exactly(10))
        .attachToWindow()
        .measure()
        .layout()

    // Ensure onVisible is fired.
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Visible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_VISIBLE)

    // Clear the steps.
    steps.clear()

    // Keep the same root, but now set the width size specs to 0 and remeasure / layout.
    legacyLithoViewRule.setSizeSpecs(exactly(0), exactly(10)).measure().layout()

    // Ensure onInvisible is now fired.
    assertThat(LifecycleStep.getSteps(steps))
        .describedAs("Invisible event should be dispatched")
        .contains(LifecycleStep.ON_EVENT_INVISIBLE)
  }

  private fun createHorizontalScrollChildren(
      c: ComponentContext,
      numberOfItems: Int,
      stepsList: MutableList<List<StepInfo>>
  ): Component {
    val rowBuilder = Row.create(c)
    for (i in 0 until numberOfItems) {
      val steps: List<StepInfo> = mutableListOf()
      val component =
          LayoutSpecLifecycleTester.create(c)
              .steps(steps)
              .widthPx(10)
              .heightPx(5)
              .marginPx(YogaEdge.TOP, 5)
              .build()
      rowBuilder.child(component)
      stepsList.add(steps)
    }
    return rowBuilder.build()
  }
}
