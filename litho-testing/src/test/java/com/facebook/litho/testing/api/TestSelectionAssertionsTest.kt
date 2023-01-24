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
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions
import org.junit.Test

class TestSelectionAssertionsTest {

  private val identitySelector = TestNodeSelector("identity") { SelectionResult(it) }

  @Test
  fun `if assertion succeeds it should not throw an assertion error`() {
    val testContext = FakeTestContext(listOf(TestNode(SimpleComponent())))
    val testNodeSelection = TestNodeSelection(testContext, identitySelector)

    runCatching { testNodeSelection.assert(hasType(SimpleComponent::class.java)) }
        .onFailure { Assertions.fail("Should not throw an assertion: $it") }
  }

  @Test
  fun `if assertion fails it should throw an assertion error with the matcher description`() {
    val testContext = FakeTestContext(listOf(TestNode(SimpleComponent())))
    val testNodeSelection = TestNodeSelection(testContext, identitySelector)

    val error = Assertions.catchThrowable { testNodeSelection.assert(hasType(Text::class.java)) }

    Assertions.assertThat(error)
        .isInstanceOf(AssertionError::class.java)
        .hasMessageStartingWith(
            "Failed assertion: is a component of type com.facebook.litho.widget.Text")
  }

  private class FakeTestContext(private val nodes: List<TestNode>) : TestContext {
    override fun provideAllTestNodes(): List<TestNode> = nodes
  }

  private class SimpleComponent : KComponent() {
    override fun ComponentScope.render() = Text("Hello")
  }
}
