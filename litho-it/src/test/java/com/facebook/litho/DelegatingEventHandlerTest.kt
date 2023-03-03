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
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(LithoTestRunner::class)
class DelegatingEventHandlerTest {

  private val hasEventDispatcher: HasEventDispatcher = mock()
  private val eventHandler1: EventHandler<Any> = EventHandler(hasEventDispatcher, 1)
  private val eventHandler2: EventHandler<Any> = EventHandler(hasEventDispatcher, 2)
  private val mockEventHandler1: EventHandler<Any> = mock()
  private val mockEventHandler2: EventHandler<Any> = mock()

  @Test
  fun testDispatchEvent() {
    val eventHandler: EventHandler<Any> =
        DelegatingEventHandler(mockEventHandler1, mockEventHandler2)
    val event = Any()
    eventHandler.dispatchEvent(event)
    verify(mockEventHandler1).dispatchEvent(event)
    verify(mockEventHandler2).dispatchEvent(event)
  }

  @Test
  fun testIsEquivalentToWithNull() {
    val eventHandler: EventHandler<Any> = DelegatingEventHandler(eventHandler1, eventHandler2)
    assertThat(eventHandler.isEquivalentTo(null)).isFalse
  }

  @Test
  fun testIsEquivalentToWithItself() {
    val eventHandler: EventHandler<Any> = DelegatingEventHandler(eventHandler1, eventHandler2)
    assertThat(eventHandler.isEquivalentTo(eventHandler)).isTrue
  }

  @Test
  fun testIsEquivalentToNormalEventHandler() {
    val eventHandler1: EventHandler<Any> = DelegatingEventHandler(eventHandler1, eventHandler2)
    assertThat(eventHandler1.isEquivalentTo(this.eventHandler1)).isFalse
    assertThat(this.eventHandler1.isEquivalentTo(eventHandler1)).isFalse
  }

  @Test
  fun testIsEquivalentToWithSameUnderlyingHandlers() {
    val eventHandler1: EventHandler<Any> = DelegatingEventHandler(eventHandler1, eventHandler2)
    val eventHandler2: EventHandler<Any> = DelegatingEventHandler(this.eventHandler1, eventHandler2)
    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isTrue
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isTrue
  }

  @Test
  fun testIsEquivalentToWithDifferentUnderlyingHandlers() {
    val eventHandler1: EventHandler<Any> = DelegatingEventHandler(eventHandler1, eventHandler2)
    val eventHandler2: EventHandler<Any> = DelegatingEventHandler(eventHandler2, this.eventHandler1)
    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse
  }
}
