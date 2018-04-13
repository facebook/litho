/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeEventHandlerTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testNoDuplicateWhenEventHandlerIsReplacedInEventHandlerWrapper() {
    Component component = mock(Component.class);
    ComponentTree componentTree = ComponentTree.create(mContext, component).build();

    EventHandler eventHandler1 = mContext.newEventHandler(1);
    when(component.getGlobalKey()).thenReturn("component1");

    componentTree.recordEventHandler(component, eventHandler1);
    componentTree.bindEventHandler(component);
    componentTree.clearUnusedEventHandlers();

    assertThat(componentTree.getEventHandlers().size()).isEqualTo(1);

    EventHandlersWrapper eventHandlersWrapper =
        componentTree.getEventHandlers().values().iterator().next();

    assertThat(eventHandlersWrapper.eventHandlers.size()).isEqualTo(1);

    EventHandler eventHandler2 = mContext.newEventHandler(1);

    componentTree.recordEventHandler(component, eventHandler2);
    componentTree.bindEventHandler(component);
    componentTree.clearUnusedEventHandlers();

    assertThat(componentTree.getEventHandlers().size()).isEqualTo(1);

    eventHandlersWrapper = componentTree.getEventHandlers().values().iterator().next();
    assertThat(eventHandlersWrapper.eventHandlers.size()).isEqualTo(1);
  }
}
