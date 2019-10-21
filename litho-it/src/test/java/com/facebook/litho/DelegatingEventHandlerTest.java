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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DelegatingEventHandlerTest {
  private final HasEventDispatcher mHasEventDispatcher = mock(HasEventDispatcher.class);
  private final EventHandler<Object> mEventHandler1 = new EventHandler<>(mHasEventDispatcher, 1);
  private final EventHandler<Object> mEventHandler2 = new EventHandler<>(mHasEventDispatcher, 2);
  private final EventHandler<Object> mMockEventHandler1 = mock(EventHandler.class);
  private final EventHandler<Object> mMockEventHandler2 = mock(EventHandler.class);

  @Test
  public void testDispatchEvent() {
    EventHandler<Object> eventHandler =
        new DelegatingEventHandler<>(mMockEventHandler1, mMockEventHandler2);

    Object event = new Object();
    eventHandler.dispatchEvent(event);

    verify(mMockEventHandler1).dispatchEvent(event);
    verify(mMockEventHandler2).dispatchEvent(event);
  }

  @Test
  public void testIsEquivalentToWithNull() {
    EventHandler<Object> eventHandler =
        new DelegatingEventHandler<>(mEventHandler1, mEventHandler2);

    assertThat(eventHandler.isEquivalentTo(null)).isFalse();
  }

  @Test
  public void testIsEquivalentToWithItself() {
    EventHandler<Object> eventHandler =
        new DelegatingEventHandler<>(mEventHandler1, mEventHandler2);

    assertThat(eventHandler.isEquivalentTo(eventHandler)).isTrue();
  }

  @Test
  public void testIsEquivalentToNormalEventHandler() {
    EventHandler<Object> eventHandler1 =
        new DelegatingEventHandler<>(mEventHandler1, mEventHandler2);

    assertThat(eventHandler1.isEquivalentTo(mEventHandler1)).isFalse();
    assertThat(mEventHandler1.isEquivalentTo(eventHandler1)).isFalse();
  }

  @Test
  public void testIsEquivalentToWithSameUnderlyingHandlers() {
    EventHandler<Object> eventHandler1 =
        new DelegatingEventHandler<>(mEventHandler1, mEventHandler2);
    EventHandler<Object> eventHandler2 =
        new DelegatingEventHandler<>(mEventHandler1, mEventHandler2);

    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isTrue();
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isTrue();
  }

  @Test
  public void testIsEquivalentToWithDifferentUnderlyingHandlers() {
    EventHandler<Object> eventHandler1 =
        new DelegatingEventHandler<>(mEventHandler1, mEventHandler2);
    EventHandler<Object> eventHandler2 =
        new DelegatingEventHandler<>(mEventHandler2, mEventHandler1);

    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse();
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse();
  }
}
