/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class ComponentTreeEventHandlerTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    mContext.setLayoutStateContextForTesting();
  }

  @Test
  public void testNoDuplicateWhenEventHandlerIsReplacedInEventHandlerWrapper() {
    Component component = mock(Component.class);
    final String componentGlobalKey = "component1";
    ComponentContext scopedContext =
        ComponentContext.withComponentScope(mContext, component, componentGlobalKey);

    when(component.getScopedContext(any(), eq(componentGlobalKey))).thenReturn(scopedContext);

    Whitebox.setInternalState(component, "mGlobalKey", componentGlobalKey);

    ComponentTree componentTree = ComponentTree.create(mContext, component).build();
    EventHandlersController eventHandlersController = componentTree.getEventHandlersController();

    EventHandler eventHandler1 = scopedContext.newEventHandler(1);

    componentTree.recordEventHandler(scopedContext, eventHandler1);
    eventHandlersController.bindEventHandlers(scopedContext, component, componentGlobalKey);
    eventHandlersController.clearUnusedEventHandlers();

    assertThat(eventHandlersController.getEventHandlers().size()).isEqualTo(1);

    EventHandlersController.EventHandlersWrapper eventHandlersWrapper =
        eventHandlersController.getEventHandlers().values().iterator().next();

    assertThat(eventHandlersWrapper.getEventHandlers().size()).isEqualTo(1);

    EventHandler eventHandler2 = scopedContext.newEventHandler(1);

    componentTree.recordEventHandler(scopedContext, eventHandler2);
    eventHandlersController.bindEventHandlers(scopedContext, component, componentGlobalKey);
    eventHandlersController.clearUnusedEventHandlers();

    assertThat(eventHandlersController.getEventHandlers().size()).isEqualTo(1);

    eventHandlersWrapper = eventHandlersController.getEventHandlers().values().iterator().next();
    assertThat(eventHandlersWrapper.getEventHandlers().size()).isEqualTo(1);
  }

  @Test
  public void testClearUnusedEntries() {

    Component component = mock(Component.class);
    final String componentGlobalKey1 = "component1";
    final String componentGlobalKey2 = "component2";
    ComponentContext scopedContext =
        ComponentContext.withComponentScope(mContext, component, componentGlobalKey1);
    Whitebox.setInternalState(component, "mGlobalKey", componentGlobalKey1);
    when(component.getScopedContext(any(), eq(componentGlobalKey1))).thenReturn(scopedContext);

    ComponentTree componentTree = ComponentTree.create(mContext, component).build();
    EventHandlersController eventHandlersController = componentTree.getEventHandlersController();

    EventHandler eventHandler1 = scopedContext.newEventHandler(1);

    componentTree.recordEventHandler(scopedContext, eventHandler1);
    eventHandlersController.bindEventHandlers(scopedContext, component, componentGlobalKey1);
    eventHandlersController.clearUnusedEventHandlers();

    assertThat(eventHandlersController.getEventHandlers().size()).isEqualTo(1);

    Whitebox.setInternalState(component, "mGlobalKey", componentGlobalKey2);
    ComponentContext scopedContext2 =
        ComponentContext.withComponentScope(mContext, component, componentGlobalKey2);
    when(component.getScopedContext(any(), eq(componentGlobalKey2))).thenReturn(scopedContext2);

    componentTree.setRoot(component);
    componentTree.recordEventHandler(scopedContext2, eventHandler1);
    eventHandlersController.bindEventHandlers(scopedContext2, component, componentGlobalKey2);
    eventHandlersController.clearUnusedEventHandlers();

    assertThat(eventHandlersController.getEventHandlers().size()).isEqualTo(1);
  }
}
