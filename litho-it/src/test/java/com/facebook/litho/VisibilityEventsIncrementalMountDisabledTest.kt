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

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.ViewGroupWithLithoViewChildren
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.visibility.VisibilityItem
import com.facebook.rendercore.visibility.VisibilityMountExtension
import com.facebook.rendercore.visibility.VisibilityMountExtension.VisibilityMountExtensionState
import com.facebook.yoga.YogaEdge
import java.lang.Exception
import java.lang.RuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class VisibilityEventsIncrementalMountDisabledTest {

  private lateinit var context: ComponentContext
  private lateinit var lithoView: LithoView
  private lateinit var parent: FrameLayout

  @Before
  fun setup() {
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    lithoView = LithoView(context)
    parent =
        FrameLayout(context.androidContext).apply {
          left = 0
          top = 0
          right = 10
          bottom = 10
          addView(lithoView)
        }
  }

  @Test
  fun testVisibleEvent() {
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .visibleHandler(visibleEventHandler)
                        .widthPx(10)
                        .heightPx(5)
                        .marginPx(YogaEdge.TOP, 5))
                .build(),
            false,
            true,
            10,
            5)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 10), true)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
  }

  @Test
  fun testVisibleEventWithHeightRatio() {
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .visibleHeightRatio(0.4f)
                        .visibleHandler(visibleEventHandler)
                        .widthPx(10)
                        .heightPx(5)
                        .marginPx(YogaEdge.TOP, 5))
                .build(),
            false,
            true,
            10,
            10)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 1), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 2), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 3), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 4), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 5), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 6), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 7), true)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
  }

  @Test
  fun testVisibleEventWithWidthRatio() {
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .visibleWidthRatio(0.4f)
                        .visibleHandler(visibleEventHandler)
                        .widthPx(10)
                        .heightPx(5)
                        .marginPx(YogaEdge.TOP, 5))
                .build(),
            false,
            true,
            10,
            5)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, 3, 10), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, 5, 10), true)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
  }

  @Test
  fun testVisibleEventWithHeightAndWidthRatio() {
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 2)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .visibleWidthRatio(0.4f)
                        .visibleHeightRatio(0.4f)
                        .visibleHandler(visibleEventHandler)
                        .widthPx(10)
                        .heightPx(5)
                        .marginPx(YogaEdge.TOP, 5))
                .build(),
            false,
            true,
            10,
            10)
    content.dispatchedEventHandlers.clear()

    // Neither width or height are in visible range
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, 3, 6), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)

    // Width but not height are in visible range
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, 5, 6), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)

    // Height but not width are in visible range
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, 3, 8), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)

    // Height and width are both in visible range
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, 5, 8), true)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
  }

  @Test
  fun testFocusedOccupiesHalfViewport() {
    val content = TestViewComponent.create(context).build()
    val focusedEventHandler = EventHandler<FocusedVisibleEvent>(content, 2)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .focusedHandler(focusedEventHandler)
                        .widthPx(10)
                        .heightPx(10))
                .build(),
            false,
            true,
            10,
            10)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 4), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(focusedEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 5), true)
    assertThat(content.dispatchedEventHandlers).contains(focusedEventHandler)
  }

  @Test
  fun testFocusedOccupiesLessThanHalfViewport() {
    val content = TestViewComponent.create(context).build()
    val focusedEventHandler = EventHandler<FocusedVisibleEvent>(content, 2)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .focusedHandler(focusedEventHandler)
                        .widthPx(10)
                        .heightPx(3))
                .build(),
            false,
            true,
            10,
            10)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 2), true)
    assertThat(content.dispatchedEventHandlers).doesNotContain(focusedEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 3), true)
    assertThat(content.dispatchedEventHandlers).contains(focusedEventHandler)
  }

  @Test
  fun testMultipleFocusAndUnfocusEvents() {
    val content = TestViewComponent.create(context).build()
    val focusedHandler = EventHandler<FocusedVisibleEvent>(content, 2)
    val unfocusedHandler = EventHandler<UnfocusedVisibleEvent>(content, 3)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .focusedHandler(focusedHandler)
                        .unfocusedHandler(unfocusedHandler)
                        .widthPx(10)
                        .heightPx(7)
                        .marginPx(YogaEdge.TOP, 3))
                .build(),
            false,
            true,
            100,
            100)
    lithoView.notifyVisibleBoundsChanged(Rect(0, 0, 0, 0), true)

    // Mount test view in the middle of the view port (focused)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 4, RIGHT, 10), true)
    assertThat(content.dispatchedEventHandlers).containsOnly(focusedHandler)

    // Mount test view on the edge of the viewport (not focused)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 9, RIGHT, 14), true)
    assertThat(content.dispatchedEventHandlers).containsOnly(unfocusedHandler)

    // Mount test view in the middle of the view port (focused)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 3, RIGHT, 9), true)
    assertThat(content.dispatchedEventHandlers).containsOnly(focusedHandler)

    // Mount test view on the edge of the viewport (not focused)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 1, RIGHT, 6), true)
    assertThat(content.dispatchedEventHandlers).containsOnly(unfocusedHandler)
  }

  @Test
  fun testFullImpressionEvent() {
    val content = TestViewComponent.create(context).build()
    val fullImpressionVisibleEvent = EventHandler<FullImpressionVisibleEvent>(content, 2)
    ComponentTestHelper.mountComponent(
        context,
        lithoView,
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .fullImpressionHandler(fullImpressionVisibleEvent)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build(),
        false,
        true,
        10,
        10)
    assertThat(content.dispatchedEventHandlers).contains(fullImpressionVisibleEvent)
  }

  @Test
  fun testVisibility1fTop() {
    val content = TestViewComponent.create(context).build()
    val visibleEvent = EventHandler<VisibleEvent>(content, 2)
    ComponentTestHelper.mountComponent(
        context,
        lithoView,
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibleHandler(visibleEvent)
                    .visibleHeightRatio(1f)
                    .widthPx(10)
                    .heightPx(5)
                    .marginPx(YogaEdge.TOP, 5))
            .build(),
        false,
        true,
        10,
        10)
    assertThat(content.dispatchedEventHandlers).contains(visibleEvent)
  }

  @Test
  fun testVisibility1fBottom() {
    val content = TestViewComponent.create(context).build()
    val visibleEvent = EventHandler<VisibleEvent>(content, 2)
    ComponentTestHelper.mountComponent(
        context,
        lithoView,
        Column.create(context)
            .child(
                Wrapper.create(context)
                    .delegate(content)
                    .visibleHandler(visibleEvent)
                    .visibleHeightRatio(1f)
                    .widthPx(10)
                    .heightPx(5))
            .build(),
        false,
        true,
        10,
        10)
    assertThat(content.dispatchedEventHandlers).contains(visibleEvent)
  }

  @Test
  fun testInvisibleEvent() {
    val content = TestViewComponent.create(context).build()
    val invisibleEventHandler = EventHandler<InvisibleEvent>(content, 2)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .invisibleHandler(invisibleEventHandler)
                        .widthPx(10)
                        .heightPx(5)
                        .marginPx(YogaEdge.TOP, 5))
                .build(),
            false,
            true,
            10,
            10)
    assertThat(content.dispatchedEventHandlers).doesNotContain(invisibleEventHandler)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 5), true)
    assertThat(content.dispatchedEventHandlers).contains(invisibleEventHandler)
  }

  @Test
  fun testVisibleRectChangedEventItemVisible() {
    val content = TestViewComponent.create(context).build()
    val visibilityChangedHandler = EventHandler<VisibilityChangedEvent>(content, 3)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .visibilityChangedHandler(visibilityChangedHandler)
                        .widthPx(10)
                        .heightPx(10))
                .build(),
            false,
            true,
            10,
            10)
    var visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(10)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(100f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 4), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(4)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(40f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 5), true)
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
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .visibilityChangedHandler(visibilityChangedHandler)
                        .widthPx(10)
                        .heightPx(5)
                        .marginPx(YogaEdge.TOP, 5))
                .build(),
            false,
            true,
            10,
            10)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 5), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    val visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(0)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(0)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(0.0f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(0.0f)
  }

  @Test
  fun testVisibleRectChangedEventLargeView() {
    val content = TestViewComponent.create(context).build()
    val visibilityChangedHandler = EventHandler<VisibilityChangedEvent>(content, 3)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .visibilityChangedHandler(visibilityChangedHandler)
                        .widthPx(10)
                        .heightPx(10))
                .build(),
            false,
            true,
            10,
            1_000)
    var visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(10)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(100f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 4), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(4)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(40f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 5), true)
    assertThat(content.dispatchedEventHandlers).contains(visibilityChangedHandler)
    visibilityChangedEvent =
        content.getEventState(visibilityChangedHandler) as VisibilityChangedEvent?
    assertThat(visibilityChangedEvent?.visibleHeight).isEqualTo(5)
    assertThat(visibilityChangedEvent?.visibleWidth).isEqualTo(10)
    assertThat(visibilityChangedEvent?.percentVisibleHeight).isEqualTo(50f)
    assertThat(visibilityChangedEvent?.percentVisibleWidth).isEqualTo(100f)
  }

  @Test
  fun testVisibleAndInvisibleEvents() {
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(content, 1)
    val invisibleEventHandler = EventHandler<InvisibleEvent>(content, 2)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .visibleHandler(visibleEventHandler)
                        .invisibleHandler(invisibleEventHandler)
                        .widthPx(10)
                        .heightPx(5)
                        .marginPx(YogaEdge.TOP, 5))
                .build(),
            false,
            true,
            10,
            10)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
    assertThat(content.dispatchedEventHandlers).doesNotContain(invisibleEventHandler)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 5), true)
    assertThat(content.dispatchedEventHandlers).contains(invisibleEventHandler)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 3, RIGHT, 9), true)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
    assertThat(content.dispatchedEventHandlers).doesNotContain(invisibleEventHandler)
    content.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 10, RIGHT, 15), true)
    assertThat(content.dispatchedEventHandlers).contains(invisibleEventHandler)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
  }

  @Test
  fun testMultipleVisibleEvents() {
    val content1 = TestViewComponent.create(context).build()
    val content2 = TestViewComponent.create(context).build()
    val content3 = TestViewComponent.create(context).build()
    val visibleEventHandler1 = EventHandler<VisibleEvent>(content1, 1)
    val visibleEventHandler2 = EventHandler<VisibleEvent>(content2, 2)
    val visibleEventHandler3 = EventHandler<VisibleEvent>(content3, 3)
    val visibilityChangedHandler = EventHandler<VisibilityChangedEvent>(content3, 4)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
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
                .build(),
            false,
            true,
            10,
            10)
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).contains(visibilityChangedHandler)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 0), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).contains(visibilityChangedHandler)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 3), true)
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibilityChangedHandler)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 3, RIGHT, 11), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).contains(visibilityChangedHandler)
  }

  @Test
  fun testMultipleVisibleAndInvisibleEvents() {
    val content1 = TestViewComponent.create(context).build()
    val content2 = TestViewComponent.create(context).build()
    val content3 = TestViewComponent.create(context).build()
    val visibleEventHandler1 = EventHandler<VisibleEvent>(content1, 1)
    val visibleEventHandler2 = EventHandler<VisibleEvent>(content2, 2)
    val visibleEventHandler3 = EventHandler<VisibleEvent>(content3, 3)
    val invisibleEventHandler1 = EventHandler<InvisibleEvent>(content1, 1)
    val invisibleEventHandler2 = EventHandler<InvisibleEvent>(content2, 2)
    val invisibleEventHandler3 = EventHandler<InvisibleEvent>(content3, 3)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content1)
                        .visibleHandler(visibleEventHandler1)
                        .invisibleHandler(invisibleEventHandler1)
                        .widthPx(10)
                        .heightPx(3))
                .child(
                    Wrapper.create(context)
                        .delegate(content2)
                        .visibleHandler(visibleEventHandler2)
                        .invisibleHandler(invisibleEventHandler2)
                        .widthPx(10)
                        .heightPx(3))
                .child(
                    Wrapper.create(context)
                        .delegate(content3)
                        .visibleHandler(visibleEventHandler3)
                        .invisibleHandler(invisibleEventHandler3)
                        .widthPx(10)
                        .heightPx(3))
                .build(),
            false,
            true,
            15,
            15)
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 9), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 0), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).contains(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(invisibleEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 0), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 1), true)
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 1, RIGHT, 8), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 4, RIGHT, 8), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).contains(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
  }

  @Test
  fun testSkipFullyVisible() {
    val content1 = TestViewComponent.create(context).key("tc1").build()
    val content2 = TestViewComponent.create(context).key("tc2").build()
    val content3 = TestViewComponent.create(context).key("tc3").build()
    val visibleEventHandler1 = EventHandler<VisibleEvent>(content1, 1)
    val visibleEventHandler2 = EventHandler<VisibleEvent>(content2, 2)
    val visibleEventHandler3 = EventHandler<VisibleEvent>(content3, 3)
    val invisibleEventHandler1 = EventHandler<InvisibleEvent>(content1, 1)
    val invisibleEventHandler2 = EventHandler<InvisibleEvent>(content2, 2)
    val invisibleEventHandler3 = EventHandler<InvisibleEvent>(content3, 3)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .key("root")
                .child(
                    Wrapper.create(context)
                        .key("child1")
                        .delegate(content1)
                        .visibleHandler(visibleEventHandler1)
                        .invisibleHandler(invisibleEventHandler1)
                        .widthPx(10)
                        .heightPx(3))
                .child(
                    Wrapper.create(context)
                        .key("child2")
                        .delegate(content2)
                        .visibleHandler(visibleEventHandler2)
                        .invisibleHandler(invisibleEventHandler2)
                        .widthPx(10)
                        .heightPx(3))
                .child(
                    Wrapper.create(context)
                        .key("child3")
                        .delegate(content3)
                        .visibleHandler(visibleEventHandler3)
                        .invisibleHandler(invisibleEventHandler3)
                        .widthPx(10)
                        .heightPx(3))
                .build(),
            false,
            true,
            15,
            9)
    var visibilityItemMap = getVisibilityIdToItemMap(lithoView)
    visibilityItemMap?.keys?.forEach { key ->
      val item = visibilityItemMap?.get(key)
      assertThat(item?.wasFullyVisible()).isTrue
    }
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 10), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    visibilityItemMap = getVisibilityIdToItemMap(lithoView)
    assertThat(visibilityItemMap?.size).isEqualTo(3)
    visibilityItemMap?.keys?.forEach { key ->
      val item = visibilityItemMap?.get(key)
      assertThat(item?.wasFullyVisible()).isTrue
    }
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 1, RIGHT, 8), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    visibilityItemMap = getVisibilityIdToItemMap(lithoView)
    assertThat(visibilityItemMap?.size).isEqualTo(3)
    assertThat(
            visibilityItemMap
                ?.get(ComponentKeyUtils.getKeyWithSeparator("root", "child1", "tc1"))
                ?.wasFullyVisible())
        .isFalse
    assertThat(
            visibilityItemMap
                ?.get(ComponentKeyUtils.getKeyWithSeparator("root", "child2", "tc2"))
                ?.wasFullyVisible())
        .isTrue
    assertThat(
            visibilityItemMap
                ?.get(ComponentKeyUtils.getKeyWithSeparator("root", "child3", "tc3"))
                ?.wasFullyVisible())
        .isFalse
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 0), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).contains(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(invisibleEventHandler3)
    visibilityItemMap = getVisibilityIdToItemMap(lithoView)
    assertThat(visibilityItemMap?.size).isEqualTo(0)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 0), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 1, RIGHT, 8), true)
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    visibilityItemMap = getVisibilityIdToItemMap(lithoView)
    assertThat(visibilityItemMap?.size).isEqualTo(3)
    assertThat(
            visibilityItemMap
                ?.get(ComponentKeyUtils.getKeyWithSeparator("root", "child1", "tc1"))
                ?.wasFullyVisible())
        .isFalse
    assertThat(
            visibilityItemMap
                ?.get(ComponentKeyUtils.getKeyWithSeparator("root", "child2", "tc2"))
                ?.wasFullyVisible())
        .isTrue
    assertThat(
            visibilityItemMap
                ?.get(ComponentKeyUtils.getKeyWithSeparator("root", "child3", "tc3"))
                ?.wasFullyVisible())
        .isFalse
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 9), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(invisibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(invisibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(invisibleEventHandler3)
    visibilityItemMap = getVisibilityIdToItemMap(lithoView)
    assertThat(visibilityItemMap?.size).isEqualTo(3)
    assertThat(
            visibilityItemMap
                ?.get(ComponentKeyUtils.getKeyWithSeparator("root", "child1", "tc1"))
                ?.wasFullyVisible())
        .isTrue
    assertThat(
            visibilityItemMap
                ?.get(ComponentKeyUtils.getKeyWithSeparator("root", "child2", "tc2"))
                ?.wasFullyVisible())
        .isTrue
    assertThat(
            visibilityItemMap
                ?.get(ComponentKeyUtils.getKeyWithSeparator("root", "child3", "tc3"))
                ?.wasFullyVisible())
        .isTrue
  }

  @Test
  fun testDispatchFocusedHandler() {
    val content1 = TestViewComponent.create(context).key("tc1").build()
    val content2 = TestViewComponent.create(context).key("tc2").build()
    val content3 = TestViewComponent.create(context).key("tc3").build()
    val focusedEventHandler1 = EventHandler<FocusedVisibleEvent>(content1, 1)
    val focusedEventHandler2 = EventHandler<FocusedVisibleEvent>(content2, 2)
    val focusedEventHandler3 = EventHandler<FocusedVisibleEvent>(content3, 3)
    val unfocusedEventHandler1 = EventHandler<UnfocusedVisibleEvent>(content1, 4)
    val unfocusedEventHandler2 = EventHandler<UnfocusedVisibleEvent>(content2, 5)
    val unfocusedEventHandler3 = EventHandler<UnfocusedVisibleEvent>(content3, 6)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .key("root")
                .child(
                    Wrapper.create(context)
                        .key("child1")
                        .delegate(content1)
                        .focusedHandler(focusedEventHandler1)
                        .unfocusedHandler(unfocusedEventHandler1)
                        .widthPx(10)
                        .heightPx(3))
                .child(
                    Wrapper.create(context)
                        .key("child2")
                        .delegate(content2)
                        .focusedHandler(focusedEventHandler2)
                        .unfocusedHandler(unfocusedEventHandler2)
                        .widthPx(10)
                        .heightPx(3))
                .child(
                    Wrapper.create(context)
                        .key("child3")
                        .delegate(content3)
                        .focusedHandler(focusedEventHandler3)
                        .unfocusedHandler(unfocusedEventHandler3)
                        .widthPx(10)
                        .heightPx(3))
                .build(),
            false,
            true,
            10,
            9)
    val visibilityItemLongSparseArray = getVisibilityIdToItemMap(lithoView)
    visibilityItemLongSparseArray?.keys?.forEach { key ->
      val item = visibilityItemLongSparseArray?.get(key)
      assertThat(item?.wasFullyVisible()).isTrue
    }
    assertThat(content1.dispatchedEventHandlers).contains(focusedEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(focusedEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(focusedEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(unfocusedEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(unfocusedEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(unfocusedEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 2, RIGHT, 9), true)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(focusedEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(focusedEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(focusedEventHandler3)
    assertThat(content1.dispatchedEventHandlers).contains(unfocusedEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(unfocusedEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(unfocusedEventHandler3)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 9), true)
    assertThat(content1.dispatchedEventHandlers).contains(focusedEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(focusedEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(focusedEventHandler3)
    assertThat(content1.dispatchedEventHandlers).doesNotContain(unfocusedEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(unfocusedEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(unfocusedEventHandler3)
  }

  @Test
  fun testDetachWithReleasedTreeTriggersInvisibilityItems() {
    val content = TestViewComponent.create(context).build()
    val invisibleEventHandler = EventHandler<InvisibleEvent>(content, 2)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .invisibleHandler(invisibleEventHandler)
                        .widthPx(10)
                        .heightPx(10))
                .build(),
            false,
            true)
    lithoView.notifyVisibleBoundsChanged(Rect(LEFT, 0, RIGHT, 10), true)
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
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
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
                .build(),
            false,
            true)
    assertThat(component1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(component1.dispatchedEventHandlers).contains(focusedEventHandler1)
    val component2 = TestViewComponent.create(context).key("component2").build()
    val visibleEventHandler2 = EventHandler<VisibleEvent>(component2, 3)
    lithoView.componentTree =
        ComponentTree.create(
                context,
                Column.create(context)
                    .child(
                        Wrapper.create(context)
                            .delegate(component2)
                            .visibleHandler(visibleEventHandler2)
                            .widthPx(10)
                            .heightPx(10))
                    .build())
            .build()
    ComponentTestHelper.measureAndLayout(lithoView)
    assertThat(component1.dispatchedEventHandlers).contains(invisibleEventHandler1)
    assertThat(component1.dispatchedEventHandlers).contains(unfocusedEventHandler1)
    assertThat(component2.dispatchedEventHandlers).contains(visibleEventHandler2)
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
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context).child(wrappedContent).build(),
            false,
            true,
            10,
            10)
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
    val content = TestViewComponent.create(context).build()
    val visibleEventHandler1 = EventHandler<VisibleEvent>(content, 1)
    val visibleEventHandler2 = EventHandler<VisibleEvent>(content, 2)
    val visibleEventHandler3 = EventHandler<VisibleEvent>(content, 3)
    val invisibleEventHandler1 = EventHandler<InvisibleEvent>(content, 4)
    val invisibleEventHandler2 = EventHandler<InvisibleEvent>(content, 5)
    val invisibleEventHandler3 = EventHandler<InvisibleEvent>(content, 6)
    val focusedEventHandler1 = EventHandler<FocusedVisibleEvent>(content, 7)
    val focusedEventHandler2 = EventHandler<FocusedVisibleEvent>(content, 8)
    val focusedEventHandler3 = EventHandler<FocusedVisibleEvent>(content, 9)
    val unfocusedEventHandler1 = EventHandler<UnfocusedVisibleEvent>(content, 10)
    val unfocusedEventHandler2 = EventHandler<UnfocusedVisibleEvent>(content, 11)
    val unfocusedEventHandler3 = EventHandler<UnfocusedVisibleEvent>(content, 12)
    val fullImpressionVisibleEventHandler1 = EventHandler<FullImpressionVisibleEvent>(content, 13)
    val fullImpressionVisibleEventHandler2 = EventHandler<FullImpressionVisibleEvent>(content, 14)
    val fullImpressionVisibleEventHandler3 = EventHandler<FullImpressionVisibleEvent>(content, 15)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(
                            Wrapper.create(context)
                                .delegate(
                                    Wrapper.create(context)
                                        .delegate(content)
                                        .visibleHandler(visibleEventHandler1)
                                        .invisibleHandler(invisibleEventHandler1)
                                        .focusedHandler(focusedEventHandler1)
                                        .unfocusedHandler(unfocusedEventHandler1)
                                        .fullImpressionHandler(fullImpressionVisibleEventHandler1)
                                        .widthPx(10)
                                        .heightPx(5)
                                        .build())
                                .visibleHandler(visibleEventHandler2)
                                .invisibleHandler(invisibleEventHandler2)
                                .focusedHandler(focusedEventHandler2)
                                .unfocusedHandler(unfocusedEventHandler2)
                                .fullImpressionHandler(fullImpressionVisibleEventHandler2)
                                .build())
                        .visibleHandler(visibleEventHandler3)
                        .invisibleHandler(invisibleEventHandler3)
                        .focusedHandler(focusedEventHandler3)
                        .unfocusedHandler(unfocusedEventHandler3)
                        .fullImpressionHandler(fullImpressionVisibleEventHandler3))
                .build(),
            false,
            true,
            10,
            10)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content.dispatchedEventHandlers).contains(focusedEventHandler1)
    assertThat(content.dispatchedEventHandlers).contains(focusedEventHandler2)
    assertThat(content.dispatchedEventHandlers).contains(focusedEventHandler3)
    assertThat(content.dispatchedEventHandlers).contains(fullImpressionVisibleEventHandler1)
    assertThat(content.dispatchedEventHandlers).contains(fullImpressionVisibleEventHandler2)
    assertThat(content.dispatchedEventHandlers).contains(fullImpressionVisibleEventHandler3)
    content.dispatchedEventHandlers.clear()
    ComponentTestHelper.unbindComponent(lithoView)
    assertThat(content.dispatchedEventHandlers).contains(invisibleEventHandler1)
    assertThat(content.dispatchedEventHandlers).contains(invisibleEventHandler2)
    assertThat(content.dispatchedEventHandlers).contains(invisibleEventHandler3)
    assertThat(content.dispatchedEventHandlers).contains(unfocusedEventHandler1)
    assertThat(content.dispatchedEventHandlers).contains(unfocusedEventHandler2)
    assertThat(content.dispatchedEventHandlers).contains(unfocusedEventHandler3)
  }

  @Test
  fun testSetVisibilityHint() {
    val component = TestViewComponent.create(context).build()
    val visibleEventHandler = EventHandler<VisibleEvent>(component, 1)
    val invisibleEventHandler = EventHandler<InvisibleEvent>(component, 2)
    val focusedEventHandler = EventHandler<FocusedVisibleEvent>(component, 3)
    val unfocusedEventHandler = EventHandler<UnfocusedVisibleEvent>(component, 4)
    val fullImpressionHandler = EventHandler<FullImpressionVisibleEvent>(component, 5)
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
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
                .build(),
            false,
            true)
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
    val mountedTestComponentInner: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Wrapper.create(c)
                          .delegate(testComponentInner)
                          .visibleHandler(visibleEventHandlerInner)
                          .invisibleHandler(invisibleEventHandlerInner)
                          .widthPx(10)
                          .heightPx(10))
                  .build()
        }
    val child = ComponentTestHelper.mountComponent(context, mountedTestComponentInner, false, true)
    assertThat(testComponentInner.dispatchedEventHandlers.size).isEqualTo(1)
    assertThat(testComponentInner.dispatchedEventHandlers.contains(visibleEventHandlerInner)).isTrue
    testComponentInner.dispatchedEventHandlers.clear()
    val viewGroup = ViewGroupWithLithoViewChildren(context.androidContext)
    val parent = child.parent as ViewGroup
    parent.removeView(child)
    viewGroup.addView(child)
    val parentView =
        ComponentTestHelper.mountComponent(
            context, TestViewComponent.create(context).testView(viewGroup).build(), false, true)
    parentView.setVisibilityHint(false, false)
    assertThat(testComponentInner.dispatchedEventHandlers.size).isEqualTo(1)
    assertThat(testComponentInner.dispatchedEventHandlers.contains(invisibleEventHandlerInner))
        .isTrue
    testComponentInner.dispatchedEventHandlers.clear()
    parentView.setVisibilityHint(true, false)
    assertThat(testComponentInner.dispatchedEventHandlers.size).isEqualTo(1)
    assertThat(testComponentInner.dispatchedEventHandlers.contains(visibleEventHandlerInner)).isTrue
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
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
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
                .build(),
            false,
            true,
            10,
            10)
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).contains(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).contains(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).contains(visibilityChangedHandler)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.processVisibilityOutputs(Rect(LEFT, 0, RIGHT, 0))
    assertThat(content1.dispatchedEventHandlers).doesNotContain(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).contains(visibilityChangedHandler)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    content3.dispatchedEventHandlers.clear()
    lithoView.processVisibilityOutputs(Rect(LEFT, 0, RIGHT, 3))
    assertThat(content1.dispatchedEventHandlers).contains(visibleEventHandler1)
    assertThat(content2.dispatchedEventHandlers).doesNotContain(visibleEventHandler2)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibleEventHandler3)
    assertThat(content3.dispatchedEventHandlers).doesNotContain(visibilityChangedHandler)
    content1.dispatchedEventHandlers.clear()
    content2.dispatchedEventHandlers.clear()
    lithoView.processVisibilityOutputs(Rect(LEFT, 3, RIGHT, 11))
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
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
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
                .build(),
            false,
            true)
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
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            lithoView,
            Column.create(context)
                .child(
                    Wrapper.create(context)
                        .delegate(content)
                        .visibleHandler(visibleEventHandler)
                        .widthPx(10)
                        .heightPx(5)
                        .marginPx(YogaEdge.TOP, 5))
                .build(),
            false,
            true,
            10,
            5)
    assertThat(content.dispatchedEventHandlers).doesNotContain(visibleEventHandler)
    lithoView.bottom = 10
    lithoView.performLayout(true, 0, 0, RIGHT, 10)
    assertThat(content.dispatchedEventHandlers).contains(visibleEventHandler)
  }

  @Test
  fun visibilityProcessing_WhenViewIsFullyTranslatedOntoScreen_DispatchesFullImpressionEvent() {

    // TODO(T118124771): Test failure because of incorrect visible bounds
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val content = TestViewComponent.create(context).build()
    val fullImpressionHandler = EventHandler<FullImpressionVisibleEvent>(content, 2)
    val parent = FrameLayout(context.androidContext)
    val lithoView = LithoView(context)
    val componentTree =
        ComponentTree.create(
                context,
                Column.create(context)
                    .child(
                        Wrapper.create(context)
                            .delegate(content)
                            .fullImpressionHandler(fullImpressionHandler)
                            .widthPx(100)
                            .heightPx(100))
                    .build())
            .incrementalMount(false)
            .layoutDiffing(false)
            .visibilityProcessing(true)
            .build()
    lithoView.componentTree = componentTree
    parent.addView(
        lithoView,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    lithoView.translationY = -10f
    parent.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY))
    parent.layout(0, 0, 100, 100)
    try {
      Whitebox.invokeMethod<Any>(lithoView, "onAttach")
    } catch (e: Exception) {
      throw RuntimeException(e)
    }

    // When self managing, LithoViews ignore translation when calculating the visible rect.
    if (!ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      assertThat(content.dispatchedEventHandlers).doesNotContain(fullImpressionHandler)
      lithoView.translationY = -5f
      assertThat(content.dispatchedEventHandlers).doesNotContain(fullImpressionHandler)

      // Note: there seems to be some bug where the local visible rect is off by 1px when using
      // translation, thus this check will not work with -1
      lithoView.translationY = -2f
      assertThat(content.dispatchedEventHandlers).doesNotContain(fullImpressionHandler)
    }
    lithoView.translationY = 0f
    assertThat(content.dispatchedEventHandlers).contains(fullImpressionHandler)
  }

  private fun getVisibilityIdToItemMap(lithoView: LithoView): Map<String?, VisibilityItem>? {
    val lithoHostListenerCoordinator =
        Whitebox.getInternalState<LithoHostListenerCoordinator>(
            lithoView, "mLithoHostListenerCoordinator")
    val extensionState =
        Whitebox.getInternalState<ExtensionState<VisibilityMountExtensionState>>(
            lithoHostListenerCoordinator, "mVisibilityExtensionState")
    return if (extensionState != null) {
      VisibilityMountExtension.getVisibilityIdToItemMap(extensionState)
    } else {
      null
    }
  }

  companion object {
    private const val LEFT = 0
    private const val RIGHT = 10
  }
}
