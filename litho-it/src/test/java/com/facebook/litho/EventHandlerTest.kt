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

@RunWith(LithoTestRunner::class)
class EventHandlerTest {

  private val hasEventDispatcher = mock<HasEventDispatcher>()

  @Test
  fun testIsEquivalentToWithNullHandler() {
    val eventHandler: EventHandler<*> = EventHandler<Any?>(hasEventDispatcher, 1)
    assertThat(eventHandler.isEquivalentTo(null)).isFalse
  }

  @Test
  fun testIsEquivalentToWithSameHandler() {
    val eventHandler: EventHandler<*> = EventHandler<Any?>(hasEventDispatcher, 1)
    assertThat(eventHandler.isEquivalentTo(eventHandler)).isTrue
  }

  @Test
  fun testIsEquivalentToWithDifferentIds() {
    val eventHandler1: EventHandler<*> = EventHandler<Any?>(hasEventDispatcher, 1)
    val eventHandler2: EventHandler<*> = EventHandler<Any?>(hasEventDispatcher, 2)
    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse
  }

  @Test
  fun testIsEquivalentToWithOneNullParams() {
    val eventHandler1: EventHandler<*> = EventHandler<Any?>(hasEventDispatcher, 1)
    val eventHandler2: EventHandler<*> = EventHandler<Any?>(hasEventDispatcher, 1, arrayOfNulls(0))
    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse
  }

  @Test
  fun testIsEquivalentToWithBothNullParams() {
    val eventHandler1: EventHandler<*> = EventHandler<Any?>(hasEventDispatcher, 1)
    val eventHandler2: EventHandler<*> = EventHandler<Any?>(hasEventDispatcher, 1)
    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isTrue
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isTrue
  }

  @Test
  fun testIsEquivalentToWithDifferentLengthParams() {
    val eventHandler1: EventHandler<*> =
        EventHandler<Any?>(hasEventDispatcher, 1, arrayOf<Any>(1, 2, 3))
    val eventHandler2: EventHandler<*> =
        EventHandler<Any?>(hasEventDispatcher, 1, arrayOf<Any>(1, 2))
    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse
  }

  @Test
  fun testIsEquivalentToWithDifferentParams() {
    val eventHandler1: EventHandler<*> =
        EventHandler<Any?>(hasEventDispatcher, 1, arrayOf<Any>(1, 2, 3))
    val eventHandler2: EventHandler<*> =
        EventHandler<Any?>(hasEventDispatcher, 1, arrayOf<Any>(1, 3, 3))
    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse
  }

  @Test
  fun testIsEquivalentToWithFirstParamDifferent() {
    val eventHandler1: EventHandler<*> =
        EventHandler<Any?>(hasEventDispatcher, 1, arrayOf<Any>(1, 2, 3))
    val eventHandler2: EventHandler<*> =
        EventHandler<Any?>(hasEventDispatcher, 1, arrayOf<Any>(2, 2, 3))
    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isFalse
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isFalse
  }

  @Test
  fun testIsEquivalentToWithDifferentHasEventDispatchInfos() {
    val eventHandler1: EventHandler<*> =
        EventHandler<Any?>(1, EventDispatchInfo(mock(), mock()), arrayOf<Any>(1, 2, 3))
    val eventHandler2: EventHandler<*> =
        EventHandler<Any?>(1, EventDispatchInfo(mock(), mock()), arrayOf<Any>(1, 2, 3))
    assertThat(eventHandler1.isEquivalentTo(eventHandler2)).isTrue
    assertThat(eventHandler2.isEquivalentTo(eventHandler1)).isTrue
  }
}
