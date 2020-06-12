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

package com.facebook.litho

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Unit tests for [KEventHandler]. */
@RunWith(JUnit4::class)
class KEventHandlerTest {

  @Test
  fun isEquivalentTo_handlersWithDifferentLambdas_returnsFalse() {
    val eh1 = KEventHandler<Any> { println("A") }
    val eh2 = KEventHandler<Any> { println("B") }

    assertThat(eh1.isEquivalentTo(eh2)).isFalse()
  }

  @Test
  fun isEquivalentTo_handlersWithSameLambda_returnsTrue() {
    val onEvent: (Any) -> Unit = { println("A") }

    val eh1 = KEventHandler(onEvent)
    val eh2 = KEventHandler(onEvent)

    assertThat(eh1.isEquivalentTo(eh2)).isTrue()
  }

  private fun onEventMethod(e: Any) {
    println("A")
  }

  @Test
  fun isEquivalentTo_handlersWithSameMethod_returnsTrue() {
    val eh1 = KEventHandler(::onEventMethod)
    val eh2 = KEventHandler(::onEventMethod)

    assertThat(eh1.isEquivalentTo(eh2)).isTrue()
  }
}
