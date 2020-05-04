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
import static org.mockito.Mockito.when;

import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(LithoTestRunner.class)
public class EventTriggersContainerTest {

  private static final String TEST_KEY_1 = "test_key_1";
  private static final String TEST_KEY_2 = "test_key_2";
  private static final Handle TEST_HANDLE_1 = new Handle();
  private static final Handle TEST_HANDLE_2 = new Handle();
  private static final int METHOD_ID_1 = 1;
  private static final int METHOD_ID_2 = 2;

  @Mock EventTrigger mockEventTriggerWithKey;
  @Mock EventTrigger mockEventTriggerWithHandle;
  @Mock EventTrigger mockEventTriggerWithKeyAndHandle;

  private EventTriggersContainer eventTriggersContainer;

  @Before
  public void setUp() {
    mockEventTriggerWithKey = mock(EventTrigger.class);
    when(mockEventTriggerWithKey.getKey()).thenReturn(TEST_KEY_1);

    mockEventTriggerWithHandle = mock(EventTrigger.class);
    when(mockEventTriggerWithHandle.getHandle()).thenReturn(TEST_HANDLE_1);
    when(mockEventTriggerWithHandle.getId()).thenReturn(METHOD_ID_1);

    mockEventTriggerWithKeyAndHandle = mock(EventTrigger.class);
    when(mockEventTriggerWithKeyAndHandle.getKey()).thenReturn(TEST_KEY_2);
    when(mockEventTriggerWithKeyAndHandle.getHandle()).thenReturn(TEST_HANDLE_2);
    when(mockEventTriggerWithKeyAndHandle.getId()).thenReturn(METHOD_ID_2);

    eventTriggersContainer = new EventTriggersContainer();
  }

  @Test
  public void testKeyAndHandleStorage() {
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithKey);
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithHandle);
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithKeyAndHandle);

    assertThat(eventTriggersContainer.getEventTrigger(TEST_KEY_1))
        .isEqualTo(mockEventTriggerWithKey);
    assertThat(eventTriggersContainer.getEventTrigger(TEST_HANDLE_1, METHOD_ID_1))
        .isEqualTo(mockEventTriggerWithHandle);
    assertThat(eventTriggersContainer.getEventTrigger(TEST_KEY_2))
        .isEqualTo(mockEventTriggerWithKeyAndHandle);
    assertThat(eventTriggersContainer.getEventTrigger(TEST_HANDLE_2, METHOD_ID_2))
        .isEqualTo(mockEventTriggerWithKeyAndHandle);
  }

  @Test
  public void testInvalidStates() {
    eventTriggersContainer.recordEventTrigger(null);
    assertThat(eventTriggersContainer.getEventTrigger("invalid")).isNull();
    assertThat(eventTriggersContainer.getEventTrigger(new Handle(), 0)).isNull();
  }

  @Test
  public void testClear() {
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithKey);
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithHandle);
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithKeyAndHandle);
    eventTriggersContainer.clear();
    assertThat(eventTriggersContainer.getEventTrigger(TEST_KEY_1)).isNull();
    assertThat(eventTriggersContainer.getEventTrigger(TEST_HANDLE_1, METHOD_ID_1)).isNull();
    assertThat(eventTriggersContainer.getEventTrigger(TEST_KEY_2)).isNull();
    assertThat(eventTriggersContainer.getEventTrigger(TEST_HANDLE_2, METHOD_ID_2)).isNull();
  }
}
