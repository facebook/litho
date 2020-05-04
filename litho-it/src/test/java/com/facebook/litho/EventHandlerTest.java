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

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class EventHandlerTest {
  private final HasEventDispatcher mHasEventDispatcher = mock(HasEventDispatcher.class);

  @Test
  public void testIsEquivalentToWithNullHandler() {
    EventHandler eventHandler = new EventHandler(mHasEventDispatcher, 1);

    assertThat(eventHandler.isEquivalentTo(null)).isFalse();
  }

  @Test
  public void testIsEquivalentToWithSameHandler() {
    EventHandler eventHandler = new EventHandler(mHasEventDispatcher, 1);

    assertThat(eventHandler.isEquivalentTo(eventHandler)).isTrue();
  }

  @Test
  public void testIsEquivalentToWithDifferentIds() {
    EventHandler eventHandler1 = new EventHandler(mHasEventDispatcher, 1);
    EventHandler eventHandler2 = new EventHandler(mHasEventDispatcher, 2);

    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse();
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse();
  }

  @Test
  public void testIsEquivalentToWithOneNullParams() {
    EventHandler eventHandler1 = new EventHandler(mHasEventDispatcher, 1);
    EventHandler eventHandler2 = new EventHandler(mHasEventDispatcher, 1, new Object[0]);

    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse();
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse();
  }

  @Test
  public void testIsEquivalentToWithBothNullParams() {
    EventHandler eventHandler1 = new EventHandler(mHasEventDispatcher, 1);
    EventHandler eventHandler2 = new EventHandler(mHasEventDispatcher, 1);

    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isTrue();
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isTrue();
  }

  @Test
  public void testIsEquivalentToWithDifferentLengthParams() {
    EventHandler eventHandler1 = new EventHandler(mHasEventDispatcher, 1, new Object[] {1, 2, 3});
    EventHandler eventHandler2 = new EventHandler(mHasEventDispatcher, 1, new Object[] {1, 2});

    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse();
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse();
  }

  @Test
  public void testIsEquivalentToWithDifferentParams() {
    EventHandler eventHandler1 = new EventHandler(mHasEventDispatcher, 1, new Object[] {1, 2, 3});
    EventHandler eventHandler2 = new EventHandler(mHasEventDispatcher, 1, new Object[] {1, 3, 3});

    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse();
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse();
  }

  @Test
  public void testIsEquivalentToFirstParamCanBeDifferent() {
    EventHandler eventHandler1 = new EventHandler(mHasEventDispatcher, 1, new Object[] {1, 2, 3});
    EventHandler eventHandler2 = new EventHandler(mHasEventDispatcher, 1, new Object[] {2, 2, 3});

    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isTrue();
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isTrue();
  }
}
