/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
