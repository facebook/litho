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

package com.facebook.litho.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/** Test the thread local variable */
class LithoThreadLocalTest {

  @Test
  fun canCreate() {
    val local = LithoThreadLocal<Int>()
    assertThat(local).isNotNull()
  }

  @Test
  fun initalValueIsNull() {
    val local = LithoThreadLocal<Int>()
    assertThat(local.get()).isNull()
  }

  @Test
  fun canSetAndGetTheValue() {
    val local = LithoThreadLocal<Int>()
    local.set(100)
    assertThat(local.get()).isEqualTo(100)
  }
}
