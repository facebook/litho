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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.facebook.litho.debug.LithoDebugEvent.LayoutCommitted
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ComponentTreeDebugEventListenerTest {

  @get:Rule val rule = LithoViewRule()

  @Test
  fun `should process events if associated with a ComponentTree`() {
    val listener = TestListener()

    val componentContext = ComponentContext(getApplicationContext() as Context)
    val componentTree =
        ComponentTree.create(componentContext, EmptyComponent())
            .withComponentTreeDebugEventListener(listener)
            .build()

    rule.render(componentTree = componentTree) { MyComponent() }
    rule.idle()

    assertThat(listener.observedEvents).hasSize(1)
    assertThat(listener.observedEvents.first().type).isEqualTo(LayoutCommitted)
  }

  @Test
  fun `should not process events if not associated with a ComponentTree`() {
    val listener = TestListener()

    val componentContext = ComponentContext(getApplicationContext() as Context)
    val componentTree =
        ComponentTree.create(componentContext, EmptyComponent())
            .withComponentTreeDebugEventListener(null)
            .build()

    rule.render(componentTree = componentTree) { MyComponent() }
    rule.idle()

    assertThat(listener.observedEvents).hasSize(0)
  }

  @Test
  fun `should not process any component tree event once released`() {
    val listener = TestListener()

    val componentContext = ComponentContext(getApplicationContext() as Context)
    val componentTree =
        ComponentTree.create(componentContext, EmptyComponent())
            .withComponentTreeDebugEventListener(listener)
            .build()

    rule.render(componentTree = componentTree) { MyComponent() }
    rule.idle()

    assertThat(listener.observedEvents).hasSize(1)
    assertThat(listener.observedEvents.last().type).isEqualTo(LayoutCommitted)

    /* if we dispatch the event we are looking for we will process it */
    DebugEventDispatcher.dispatch(
        type = LayoutCommitted, renderStateId = { componentTree.mId.toString() })
    assertThat(listener.observedEvents).hasSize(2)
    assertThat(listener.observedEvents.last().type).isEqualTo(LayoutCommitted)

    /* if we dispatch the event we are looking for after the Component Tree is released, it will not
    be processed */
    componentTree.release()
    DebugEventDispatcher.dispatch(
        type = LayoutCommitted, renderStateId = { componentTree.mId.toString() })
    assertThat(listener.observedEvents).hasSize(2)
  }

  private class TestListener : ComponentTreeDebugEventListener {

    val observedEvents: List<DebugEvent>
      get() = _observedEvents

    private val _observedEvents = mutableListOf<DebugEvent>()

    override fun onEvent(debugEvent: DebugEvent) {
      _observedEvents.add(debugEvent)
    }

    override val events: Set<String> = setOf(LayoutCommitted)
  }

  private class MyComponent : KComponent() {
    override fun ComponentScope.render(): Component = Text("Hello")
  }
}
