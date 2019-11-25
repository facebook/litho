// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

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

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class NoOpEventHandlerTest {
  @Test
  public void testGetNoOpEventHandler() {
    EventHandler eventHandler = NoOpEventHandler.getNoOpEventHandler();
    assertThat(eventHandler.isEquivalentTo(null)).isFalse();
    EventHandler eventHandler1 = NoOpEventHandler.getNoOpEventHandler();
    assertThat(eventHandler.isEquivalentTo(eventHandler1)).isTrue();
  }

  @Test
  public void testHasEventDispatcherNotNull() {
    NoOpEventHandler eventHandler = NoOpEventHandler.getNoOpEventHandler();
    assertThat(eventHandler.isEquivalentTo(null)).isFalse();
    assertThat(NoOpEventHandler.sNoOpEventHandler != null).isTrue();
    assertThat(NoOpEventHandler.sNoOpHasEventDispatcher != null).isTrue();
  }

  @Test(expected = RuntimeException.class)
  public void testComponentContextThrowsExceptionWithoutComponentScope() {
    ComponentContext componentContext = new ComponentContext(RuntimeEnvironment.application);
    assertThat(
            componentContext
                .newEventHandler(1, new Object[1])
                .isEquivalentTo(NoOpEventHandler.getNoOpEventHandler()))
        .isTrue();
  }

  @Test(expected = RuntimeException.class)
  public void testComponentLifeCycleThrowsExceptionWithoutComponentScope() {
    ComponentContext componentContext = new ComponentContext(RuntimeEnvironment.application);
    assertThat(
            ComponentLifecycle.newEventHandler(componentContext, 1, new Object[1])
                .isEquivalentTo(NoOpEventHandler.getNoOpEventHandler()))
        .isTrue();
  }
}
