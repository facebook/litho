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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.testing.LithoViewRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/** Unit tests for [ComponentTreeScope]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ComponentTreeScopeTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  private val testDispatcher = TestCoroutineDispatcher()

  @Before
  fun setUp() {
    testDispatcher.pauseDispatcher()
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun `componentTreeScope can start when LithoView is attached`() {
    val events = mutableListOf<String>()

    val tree = ComponentTree.create(mock()).build()

    tree.componentTreeScope.launch { events += "launch" }
    testDispatcher.runCurrent()

    assertThat(events).containsExactly("launch")
  }

  @Test
  fun `componentTreeScope is canceled when LithoView is detached`() {
    val events = mutableListOf<String>()

    val tree = ComponentTree.create(mock()).build()

    tree.componentTreeScope.launch {
      try {
        awaitCancellation()
      } finally {
        events += "cancel"
      }
    }
    testDispatcher.runCurrent()

    assertThat(events).isEmpty()

    tree.release()
    testDispatcher.runCurrent()

    assertThat(events).containsExactly("cancel")
  }

  @Test
  fun `componentTreeScope starts as canceled if view is detached`() {
    val tree = ComponentTree.create(mock()).build()
    tree.release()

    assertThat(tree.componentTreeScope.coroutineContext.job.isCancelled).isTrue
  }
}
