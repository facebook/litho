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

package com.facebook.litho.testing.helper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.EventHandler
import com.facebook.litho.FocusedVisibleEvent
import com.facebook.litho.FullImpressionVisibleEvent
import com.facebook.litho.InvisibleEvent
import com.facebook.litho.Row
import com.facebook.litho.UnfocusedVisibleEvent
import com.facebook.litho.VisibleEvent
import com.facebook.litho.testing.TestLayoutComponent
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/** Tests [VisibilityEventsHelper] */
@RunWith(LithoTestRunner::class)
class VisibilityEventsHelperTest {

  @Mock private val visibleEventEventHandler: EventHandler<VisibleEvent> = mock()

  @Mock private val invisibleEventEventHandler: EventHandler<InvisibleEvent> = mock()

  @Mock private val focusedVisibleEventEventHandler: EventHandler<FocusedVisibleEvent> = mock()

  @Mock private val unfocusedVisibleEventEventHandler: EventHandler<UnfocusedVisibleEvent> = mock()

  @Mock
  private val fullImpressionEventEventHandler: EventHandler<FullImpressionVisibleEvent> = mock()
  private lateinit var context: ComponentContext
  private lateinit var _componentTree: ComponentTree

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    _componentTree = componentTreeWithHandlers
    reset(visibleEventEventHandler)
    reset(invisibleEventEventHandler)
    reset(focusedVisibleEventEventHandler)
    reset(unfocusedVisibleEventEventHandler)
    reset(fullImpressionEventEventHandler)
  }

  @Test
  fun triggerVisibleEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(_componentTree, VisibleEvent::class.java))
        .isTrue
    verify(visibleEventEventHandler).call(any<VisibleEvent>())
  }

  @Test
  fun triggerInvisibleEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                _componentTree, InvisibleEvent::class.java))
        .isTrue
    verify(invisibleEventEventHandler).call(any<InvisibleEvent>())
  }

  @Test
  fun triggerFocusedEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                _componentTree, FocusedVisibleEvent::class.java))
        .isTrue
    verify(focusedVisibleEventEventHandler).call(any<FocusedVisibleEvent>())
  }

  @Test
  fun triggerUnfocusedEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                _componentTree, UnfocusedVisibleEvent::class.java))
        .isTrue
    verify(unfocusedVisibleEventEventHandler).call(any<UnfocusedVisibleEvent>())
  }

  @Test
  fun triggerFullImpressionEventForEventsShouldDispatchHandler() {
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(
                _componentTree, FullImpressionVisibleEvent::class.java))
        .isTrue
    verify(fullImpressionEventEventHandler).call(any<FullImpressionVisibleEvent>())
  }

  @Test
  fun triggerEventWithoutHandlerShouldNotDispatchHandler() {
    val component = TestLayoutComponent.create(context).build()
    val componentTree = getComponentTree(component)
    reset(visibleEventEventHandler)
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(componentTree, VisibleEvent::class.java))
        .isFalse
  }

  @Test
  fun triggerEventShouldFindFirstHandler() {
    val component =
        Row.create(context)
            .child(Row.create(context).build())
            .child(Row.create(context).visibleHandler(visibleEventEventHandler).build())
            .build()
    val componentTree = getComponentTree(component)
    reset(visibleEventEventHandler)
    assertThat(
            VisibilityEventsHelper.triggerVisibilityEvent(componentTree, VisibleEvent::class.java))
        .isTrue
    verify(visibleEventEventHandler).call(any<VisibleEvent>())
  }

  private val componentWithHandlers: Component
    get() =
        Row.create(context)
            .visibleHandler(visibleEventEventHandler)
            .invisibleHandler(invisibleEventEventHandler)
            .focusedHandler(focusedVisibleEventEventHandler)
            .unfocusedHandler(unfocusedVisibleEventEventHandler)
            .fullImpressionHandler(fullImpressionEventEventHandler)
            .build()

  private fun getComponentTree(component: Component): ComponentTree =
      ComponentTestHelper.mountComponent(context, component).componentTree!!

  private val componentTreeWithHandlers: ComponentTree
    get() = getComponentTree(componentWithHandlers)
}
