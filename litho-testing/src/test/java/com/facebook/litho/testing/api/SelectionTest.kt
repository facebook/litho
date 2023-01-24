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

package com.facebook.litho.testing.api

import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.kotlin.widget.Text
import org.assertj.core.api.Assertions
import org.junit.Test

class SelectionTest {

  private val identitySelector = TestNodeSelector("identity") { SelectionResult(it) }

  @Test
  fun `materializing exactly 1 match completes successfully`() {
    val testContext = TestTestContext(listOf(TestNode(SimpleComponent())))
    val selection = TestNodeSelection(testContext, identitySelector)

    runCatching { selection.fetchTestNode() }
        .onFailure { Assertions.fail("Excepted success. Found $it") }
  }

  @Test
  fun `assertDoesNotExist passes successfully when no match is found`() {
    val testContext = TestTestContext(emptyList())
    val selection = TestNodeSelection(testContext, identitySelector)

    runCatching { selection.assertDoesNotExist() }
        .onFailure { Assertions.fail("Excepted success. Found $it") }
  }

  @Test
  fun `materializing an empty match throws exception`() {
    val testContext = TestTestContext(emptyList())
    val selection = TestNodeSelection(testContext, identitySelector)

    Assertions.assertThatThrownBy { selection.fetchTestNode() }
        .isInstanceOf(AssertionError::class.java)
  }

  @Test
  fun `materializing a selection with more than 1 match throws exception`() {
    val testNode = TestNode(SimpleComponent())
    val testContext = TestTestContext(listOf(testNode, testNode))
    val selection = TestNodeSelection(testContext, identitySelector)

    Assertions.assertThatThrownBy { selection.fetchTestNode() }
        .isInstanceOf(AssertionError::class.java)
  }
}

internal class TestTestContext(private val nodes: List<TestNode>) : TestContext {
  override fun provideAllTestNodes(): List<TestNode> = nodes
}

internal class SimpleComponent : KComponent() {
  override fun ComponentScope.render() = Text("Hello")
}
