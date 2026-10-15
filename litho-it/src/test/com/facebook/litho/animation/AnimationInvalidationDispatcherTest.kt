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

package com.facebook.litho.animation

import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [AnimationInvalidationDispatcher]'s single-level batching contract. */
@RunWith(LithoTestRunner::class)
class AnimationInvalidationDispatcherTest {

  @Test
  fun batch_reentrant_throws() {
    // A nested batch would flush the outer batch early and desync isBatching, so it must throw.
    assertThatThrownBy {
          AnimationInvalidationDispatcher.batch { AnimationInvalidationDispatcher.batch {} }
        }
        .isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun clear_duringBatch_throws() {
    // clear() is a test-only reset and must never run while a batch is collecting.
    assertThatThrownBy {
          AnimationInvalidationDispatcher.batch { AnimationInvalidationDispatcher.clear() }
        }
        .isInstanceOf(IllegalStateException::class.java)
  }
}
