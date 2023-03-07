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

import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class EventTriggersContainerTest {

  @Mock lateinit var TEST_HANDLE_1: Handle

  @Mock lateinit var TEST_HANDLE_2: Handle

  @Mock lateinit var mockEventTriggerWithKey: EventTrigger<*>

  @Mock lateinit var mockEventTriggerWithHandle: EventTrigger<*>

  @Mock lateinit var mockEventTriggerWithKeyAndHandle: EventTrigger<*>
  private lateinit var eventTriggersContainer: EventTriggersContainer

  @Before
  fun setUp() {
    TEST_HANDLE_1 = mock()
    TEST_HANDLE_2 = mock()
    mockEventTriggerWithKey = mock()
    whenever(mockEventTriggerWithKey.key).thenReturn(TEST_KEY_1)
    mockEventTriggerWithHandle = mock()
    whenever(mockEventTriggerWithHandle.handle).thenReturn(TEST_HANDLE_1)
    whenever(mockEventTriggerWithHandle.id).thenReturn(METHOD_ID_1)
    mockEventTriggerWithKeyAndHandle = mock()
    whenever(mockEventTriggerWithKeyAndHandle.key).thenReturn(TEST_KEY_2)
    whenever(mockEventTriggerWithKeyAndHandle.handle).thenReturn(TEST_HANDLE_2)
    whenever(mockEventTriggerWithKeyAndHandle.id).thenReturn(METHOD_ID_2)
    eventTriggersContainer = EventTriggersContainer()
  }

  @Test
  fun testKeyAndHandleStorage() {
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithKey)
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithHandle)
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithKeyAndHandle)
    assertThat(eventTriggersContainer.getEventTrigger(TEST_KEY_1))
        .isEqualTo(mockEventTriggerWithKey)
    assertThat(eventTriggersContainer.getEventTrigger(TEST_HANDLE_1, METHOD_ID_1))
        .isEqualTo(mockEventTriggerWithHandle)
    assertThat(eventTriggersContainer.getEventTrigger(TEST_KEY_2))
        .isEqualTo(mockEventTriggerWithKeyAndHandle)
    assertThat(eventTriggersContainer.getEventTrigger(TEST_HANDLE_2, METHOD_ID_2))
        .isEqualTo(mockEventTriggerWithKeyAndHandle)
  }

  @Test
  fun testInvalidStates() {
    eventTriggersContainer.recordEventTrigger(null)
    assertThat(eventTriggersContainer.getEventTrigger("invalid")).isNull()
    assertThat(eventTriggersContainer.getEventTrigger(Handle(), 0)).isNull()
  }

  @Test
  fun testClear() {
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithKey)
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithHandle)
    eventTriggersContainer.recordEventTrigger(mockEventTriggerWithKeyAndHandle)
    eventTriggersContainer.clear()
    assertThat(eventTriggersContainer.getEventTrigger(TEST_KEY_1)).isNull()
    assertThat(eventTriggersContainer.getEventTrigger(TEST_HANDLE_1, METHOD_ID_1)).isNull()
    assertThat(eventTriggersContainer.getEventTrigger(TEST_KEY_2)).isNull()
    assertThat(eventTriggersContainer.getEventTrigger(TEST_HANDLE_2, METHOD_ID_2)).isNull()
  }

  companion object {
    private const val TEST_KEY_1 = "test_key_1"
    private const val TEST_KEY_2 = "test_key_2"
    private const val METHOD_ID_1 = 1
    private const val METHOD_ID_2 = 2
  }
}
