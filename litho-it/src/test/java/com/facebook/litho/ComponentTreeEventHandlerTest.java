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

package com.facebook.litho;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.util.Pair;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class ComponentTreeEventHandlerTest {

  private ComponentTree mComponentTree;
  private ComponentContext mContext;

  @Before
  public void setup() {
    mComponentTree = ComponentTree.create(new ComponentContext(getApplicationContext())).build();
    mContext = mComponentTree.getContext();
  }

  @Test
  public void testAllEventHandlersForEventDispatchInfoAreUpdated() {
    Component component = mock(Component.class);
    final String componentGlobalKey = "component1";
    ComponentContext scopedContext =
        ComponentContext.withComponentScope(mContext, component, componentGlobalKey);

    EventHandlersController eventHandlersController = mComponentTree.getEventHandlersController();

    EventHandler eventHandler1 =
        Component.newEventHandler(component.getClass(), "TestComponent", scopedContext, 1, null);

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(0);

    EventHandler eventHandler2 =
        Component.newEventHandler(component.getClass(), "TestComponent", scopedContext, 1, null);

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(0);
    assertThat(eventHandler1.dispatchInfo).isNotSameAs(eventHandler2.dispatchInfo);

    final ArrayList<Pair<String, EventHandler>> eventHandlers = new ArrayList<>();
    eventHandlers.add(new Pair<>(componentGlobalKey, eventHandler1));
    eventHandlers.add(new Pair<>(componentGlobalKey, eventHandler2));

    eventHandlersController.canonicalizeEventDispatchInfos(eventHandlers);

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(1);
    assertThat(eventHandler1.dispatchInfo).isSameAs(eventHandler2.dispatchInfo);

    Component newComponent = mock(Component.class);
    ComponentContext newScopedContext =
        ComponentContext.withComponentScope(mContext, newComponent, componentGlobalKey);
    eventHandlersController.updateEventDispatchInfoForGlobalKey(
        newScopedContext, newComponent, componentGlobalKey);
    eventHandlersController.clearUnusedEventDispatchInfos();

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(1);
    assertThat(eventHandler1.dispatchInfo.componentContext).isSameAs(newScopedContext);
    assertThat(eventHandler2.dispatchInfo.componentContext).isSameAs(newScopedContext);
    assertThat(eventHandler1.dispatchInfo.hasEventDispatcher).isSameAs(newComponent);
    assertThat(eventHandler2.dispatchInfo.hasEventDispatcher).isSameAs(newComponent);
  }

  @Test
  public void testClearUnusedEntries() {
    Component component = mock(Component.class);
    Component component2 = mock(Component.class);
    Component component2_2 = mock(Component.class);
    final String componentGlobalKey1 = "component1";
    final String componentGlobalKey2 = "component2";
    ComponentContext scopedContext =
        ComponentContext.withComponentScope(mContext, component, componentGlobalKey1);
    ComponentContext scopedContext2 =
        ComponentContext.withComponentScope(mContext, component2, componentGlobalKey2);
    ComponentContext scopedContext2_2 =
        ComponentContext.withComponentScope(mContext, component2_2, componentGlobalKey2);

    EventHandlersController eventHandlersController = mComponentTree.getEventHandlersController();

    EventHandler eventHandler1 =
        Component.newEventHandler(component.getClass(), "TestComponent", scopedContext, 1, null);
    EventHandler eventHandler2 =
        Component.newEventHandler(component2.getClass(), "TestComponent2", scopedContext2, 1, null);

    final ArrayList<Pair<String, EventHandler>> eventHandlers = new ArrayList<>();
    eventHandlers.add(new Pair<>(componentGlobalKey1, eventHandler1));
    eventHandlers.add(new Pair<>(componentGlobalKey2, eventHandler2));

    eventHandlersController.canonicalizeEventDispatchInfos(eventHandlers);

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(2);
    assertThat(eventHandler1.dispatchInfo.componentContext).isSameAs(scopedContext);
    assertThat(eventHandler2.dispatchInfo.componentContext).isSameAs(scopedContext2);
    assertThat(eventHandler1.dispatchInfo.hasEventDispatcher).isSameAs(component);
    assertThat(eventHandler2.dispatchInfo.hasEventDispatcher).isSameAs(component2);

    eventHandlersController.updateEventDispatchInfoForGlobalKey(
        scopedContext2_2, component2_2, componentGlobalKey2);
    eventHandlersController.clearUnusedEventDispatchInfos();

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(1);
    assertThat(eventHandlersController.getDispatchInfos().get(componentGlobalKey2)).isNotNull();

    eventHandlersController.clearUnusedEventDispatchInfos();
    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(0);

    assertThat(eventHandler1.dispatchInfo.componentContext).isSameAs(scopedContext);
    assertThat(eventHandler2.dispatchInfo.componentContext).isSameAs(scopedContext2_2);
    assertThat(eventHandler1.dispatchInfo.hasEventDispatcher).isSameAs(component);
    assertThat(eventHandler2.dispatchInfo.hasEventDispatcher).isSameAs(component2_2);
  }
}
