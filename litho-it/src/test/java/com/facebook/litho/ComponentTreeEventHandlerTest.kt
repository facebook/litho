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
import android.util.Pair
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(LithoTestRunner::class)
class ComponentTreeEventHandlerTest {

  private lateinit var componentTree: ComponentTree
  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    componentTree =
        ComponentTree.create(ComponentContext(ApplicationProvider.getApplicationContext<Context>()))
            .build()
    context = componentTree.context
  }

  @Test
  fun testAllEventHandlersForEventDispatchInfoAreUpdated() {
    val component = mock<SpecGeneratedComponent>()
    val componentGlobalKey = "component1"
    val scopedContext = ComponentContext.withComponentScope(context, component, componentGlobalKey)
    val eventHandlersController = componentTree.eventHandlersController
    val eventHandler1: EventHandler<*> =
        Component.newEventHandler<Any>(
            component.javaClass, "TestComponent", scopedContext, 1, emptyArray())
    assertThat(eventHandlersController.dispatchInfos.size).isEqualTo(0)
    val eventHandler2: EventHandler<*> =
        Component.newEventHandler<Any>(
            component.javaClass, "TestComponent", scopedContext, 1, emptyArray())
    assertThat(eventHandlersController.dispatchInfos.size).isEqualTo(0)
    assertThat(eventHandler1.dispatchInfo).isNotSameAs(eventHandler2.dispatchInfo)
    val eventHandlers = ArrayList<Pair<String, EventHandler<*>>>()
    eventHandlers.add(Pair(componentGlobalKey, eventHandler1))
    eventHandlers.add(Pair(componentGlobalKey, eventHandler2))
    eventHandlersController.canonicalizeEventDispatchInfos(eventHandlers)
    assertThat(eventHandlersController.dispatchInfos.size).isEqualTo(1)
    assertThat(eventHandler1.dispatchInfo).isSameAs(eventHandler2.dispatchInfo)
    val newComponent = mock<SpecGeneratedComponent>()
    val newScopedContext =
        ComponentContext.withComponentScope(context, newComponent, componentGlobalKey)
    eventHandlersController.updateEventDispatchInfoForGlobalKey(
        newScopedContext, newComponent, componentGlobalKey)
    eventHandlersController.clearUnusedEventDispatchInfos()
    assertThat(eventHandlersController.dispatchInfos.size).isEqualTo(1)
    assertThat(eventHandler1.dispatchInfo.componentContext).isSameAs(newScopedContext)
    assertThat(eventHandler2.dispatchInfo.componentContext).isSameAs(newScopedContext)
    assertThat(eventHandler1.dispatchInfo.hasEventDispatcher).isSameAs(newComponent)
    assertThat(eventHandler2.dispatchInfo.hasEventDispatcher).isSameAs(newComponent)
  }

  @Test
  fun testClearUnusedEntries() {
    val component = mock<SpecGeneratedComponent>()
    val component2 = mock<SpecGeneratedComponent>()
    val component2_2 = mock<SpecGeneratedComponent>()
    val componentGlobalKey1 = "component1"
    val componentGlobalKey2 = "component2"
    val scopedContext = ComponentContext.withComponentScope(context, component, componentGlobalKey1)
    val scopedContext2 =
        ComponentContext.withComponentScope(context, component2, componentGlobalKey2)
    val scopedContext2_2 =
        ComponentContext.withComponentScope(context, component2_2, componentGlobalKey2)
    val eventHandlersController = componentTree.eventHandlersController
    val eventHandler1: EventHandler<*> =
        Component.newEventHandler<Any>(
            component.javaClass, "TestComponent", scopedContext, 1, emptyArray())
    val eventHandler2: EventHandler<*> =
        Component.newEventHandler<Any>(
            component2.javaClass, "TestComponent2", scopedContext2, 1, emptyArray())
    val eventHandlers = ArrayList<Pair<String, EventHandler<*>>>()
    eventHandlers.add(Pair(componentGlobalKey1, eventHandler1))
    eventHandlers.add(Pair(componentGlobalKey2, eventHandler2))
    eventHandlersController.canonicalizeEventDispatchInfos(eventHandlers)
    assertThat(eventHandlersController.dispatchInfos.size).isEqualTo(2)
    assertThat(eventHandler1.dispatchInfo.componentContext).isSameAs(scopedContext)
    assertThat(eventHandler2.dispatchInfo.componentContext).isSameAs(scopedContext2)
    assertThat(eventHandler1.dispatchInfo.hasEventDispatcher).isSameAs(component)
    assertThat(eventHandler2.dispatchInfo.hasEventDispatcher).isSameAs(component2)
    eventHandlersController.updateEventDispatchInfoForGlobalKey(
        scopedContext2_2, component2_2, componentGlobalKey2)
    eventHandlersController.clearUnusedEventDispatchInfos()
    assertThat(eventHandlersController.dispatchInfos.size).isEqualTo(1)
    assertThat(eventHandlersController.dispatchInfos[componentGlobalKey2]).isNotNull
    eventHandlersController.clearUnusedEventDispatchInfos()
    assertThat(eventHandlersController.dispatchInfos.size).isEqualTo(0)
    assertThat(eventHandler1.dispatchInfo.componentContext).isSameAs(scopedContext)
    assertThat(eventHandler2.dispatchInfo.componentContext).isSameAs(scopedContext2_2)
    assertThat(eventHandler1.dispatchInfo.hasEventDispatcher).isSameAs(component)
    assertThat(eventHandler2.dispatchInfo.hasEventDispatcher).isSameAs(component2_2)
  }
}
