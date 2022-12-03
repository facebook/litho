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

/**
 * Lazy representation of a single test node with necessary logic to materialize on-demand.
 *
 * Note: materalizing the [TestNodeSelection] should result in exactly **1** test node unless the
 * [assertDoesNotExist] method is used. In any case, an [IllegalStateException] will be thrown if
 * more than 1 test nodes are realized.
 */
class TestNodeSelection(
    private val testContext: TestContext,
    private val selector: TestNodeSelector
) {

  fun fetchTestNode(): TestNode {
    val result = fetchMatchingNodes()

    check(result.size == 1) { "Expected exactly 1 test node. Found ${result.size}" }

    return result.first()
  }

  fun assertExists() {
    val result = fetchMatchingNodes()

    val numResults = result.size

    if (numResults == 0) {
      throw AssertionError("There are no results for searched test node.")
    }

    if (numResults > 1) {
      throw AssertionError("There is more than one result for the matched test nodes.")
    }
  }

  fun assertDoesNotExist() {
    val result = fetchMatchingNodes()

    check(result.isEmpty()) {
      "Failed: assertDoesNotExist. Expected no match, but found ${result.size} test nodes"
    }
  }

  fun assert(matcher: TestNodeMatcher) {
    val testNode = fetchTestNode()

    if (!matcher.matches(testNode)) {
      throw AssertionError("Failed assertion: ${matcher.description}")
    }
  }

  private fun fetchMatchingNodes(): List<TestNode> {
    val nodes = testContext.provideAllTestNodes()
    return selector.map(nodes)
  }
}
