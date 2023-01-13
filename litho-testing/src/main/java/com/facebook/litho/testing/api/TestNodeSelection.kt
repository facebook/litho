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

    if (result.size != 1) {
      val errorMessage = "Failed: expected exactly 1 test node. Found ${result.size}"
      throw AssertionError(errorMessage)
    }

    return result.first()
  }

  fun assertExists(): TestNodeSelection {
    val result = fetchMatchingNodes()

    val numResults = result.size

    if (numResults == 0) {
      throw AssertionError("There are no results for searched test node.")
    }

    if (numResults > 1) {
      throw AssertionError("There is more than one result for the matched test nodes.")
    }

    return this
  }

  fun assertDoesNotExist(): TestNodeSelection {
    val result = fetchMatchingNodes()

    check(result.isEmpty()) {
      "Failed: assertDoesNotExist. Expected no match, but found ${result.size} test nodes"
    }

    return this
  }

  fun assert(matcher: TestNodeMatcher): TestNodeSelection {
    val testNode = fetchTestNode()

    if (!matcher.matches(testNode)) {
      throw AssertionError("Failed assertion: ${matcher.description}")
    }

    return this
  }

  /**
   * Returns a [TestNodeSelection] that further filters the current one, by selecting the child
   * [TestNode] at [index].
   *
   * This assumes that there is order in the `Component`s children. For example, the first one
   * defined inside a `Column` or `Row` will be the one at index 0.
   *
   * If the current [TestNodeSelection] has no children or a child at the given [index] then it will
   * throw an [AssertionError].
   */
  fun selectChildAt(index: Int): TestNodeSelection {
    return TestNodeSelection(
        testContext,
        TestNodeSelector { nodes ->
          val selectedNodes = selector.map(nodes)
          if (selectedNodes.size != 1) {
            throw AssertionError("selectChildAt: expected selection to be a single node")
          }

          val selectedNode = selectedNodes.first()
          val child =
              selectedNode.children.getOrNull(index)
                  ?: throw AssertionError("selectChildAt: expected child at $index to not be null")

          listOf(child)
        })
  }

  /**
   * Returns a [TestNodeCollectionSelection] that further defines the current one, by defining a
   * collection of [TestNode] composed by its children.
   *
   * The usage of this API assumes that the previous selection materializes into a single
   * [TestNode], so that the children can be extracted from.
   *
   * An example usage:
   * ```
   * rule.selectNode(hasType<Column>())
   *  .selectChildren()
   *  .assertCount(2)
   * ```
   */
  fun selectChildren(): TestNodeCollectionSelection {
    return TestNodeCollectionSelection(
        testContext,
        TestNodeSelector { nodes ->
          val selectedNodes = selector.map(nodes)
          if (selectedNodes.size != 1) {
            throw AssertionError("selectChildren: expected selection to be a single node")
          }

          val selectedNode = selectedNodes.first()
          selectedNode.children
        })
  }

  private fun fetchMatchingNodes(): List<TestNode> {
    val nodes = testContext.provideAllTestNodes()
    return selector.map(nodes)
  }
}

/**
 * Lazy representation of a collection of test nodes with necessary logic to materialize on-demand.
 *
 * Note: materializing the [TestNodeSelection] should result a collection of test node unless the
 * [assertDoesNotExist] method is used.
 */
class TestNodeCollectionSelection(
    private val testContext: TestContext,
    private val selector: TestNodeSelector
) {

  fun assertExists(): TestNodeCollectionSelection {
    val result = fetchMatchingNodes()

    val numResults = result.size

    if (numResults == 0) {
      throw AssertionError("Failed: assertExists. There are no results for searched test nodes.")
    }

    return this
  }

  fun assertDoesNotExist(): TestNodeCollectionSelection {
    val result = fetchMatchingNodes()

    if (result.isNotEmpty()) {
      throw AssertionError(
          "Failed: assertDoesNotExist. Expected no match, but found ${result.size} test nodes")
    }

    return this
  }

  /**
   * Asserts that this selection is defined by [count] elements.
   *
   * It will throw an [AssertionError] if the number of elements is lower or higher than [count]
   */
  fun assertCount(count: Int): TestNodeCollectionSelection {
    val result = fetchMatchingNodes()

    if (result.size != count) {
      throw AssertionError(
          "Failed: assertHasCount. Expected $count components, but found ${result.size}")
    }

    return this
  }

  fun selectAtIndex(index: Int): TestNodeSelection {
    return TestNodeSelection(
        testContext,
        TestNodeSelector { nodes ->
          val selectedNodes = selector.map(nodes)
          listOfNotNull(selectedNodes.getOrNull(index))
        })
  }

  fun fetchMatchingNodes(): List<TestNode> {
    val nodes = testContext.provideAllTestNodes()
    return selector.map(nodes)
  }
}
