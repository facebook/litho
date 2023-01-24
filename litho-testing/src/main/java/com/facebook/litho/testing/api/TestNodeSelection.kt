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
    internal val selector: TestNodeSelector
) {

  fun fetchTestNode(errorOnFail: String = "Failed"): TestNode {
    val result = fetchMatchingNodes()

    if (result.nodes.size != 1)
        when {
          result.selectionError != null ->
              throw AssertionError("$errorOnFail\n${result.selectionError}")
          result.nodes.isEmpty() -> throwNoMatchingNodeForSelectionError(errorOnFail, selector)
          else -> throwCountMismatchError(errorOnFail, selector, 1, result.nodes)
        }

    return result.nodes.first()
  }

  fun assertExists(): TestNodeSelection {
    fetchTestNode("Failed: assertExists")

    return this
  }

  fun assertDoesNotExist(): TestNodeSelection {
    val result = fetchMatchingNodes().nodes

    if (result.isNotEmpty()) {
      throwCountMismatchError("Failed: assertDoesNotExist", selector, 0, result)
    }

    return this
  }

  fun assert(matcher: TestNodeMatcher): TestNodeSelection {
    val testNode = fetchTestNode("Failed assertion: ${matcher.description}")

    if (!matcher.matches(testNode)) {
      throwGeneralError("Failed assertion: ${matcher.description}", selector, testNode)
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
   * If the current [TestNodeSelection] has no children or a child at the given [index] then
   * assertions (other than assertDoesNotExist) on the result will throw an [AssertionError].
   */
  fun selectChildAt(index: Int): TestNodeSelection {
    return selectChildren().selectAtIndex(index)
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
        testContext, selector.plusSingleToManySelector("children") { it.children })
  }

  private fun fetchMatchingNodes(): SelectionResult {
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

    if (result.isEmpty()) {
      throwNoMatchingNodeForSelectionError("Failed: assertExists", selector)
    }

    return this
  }

  fun assertDoesNotExist(): TestNodeCollectionSelection {
    val result = fetchMatchingNodes()

    if (result.isNotEmpty()) {
      throwCountMismatchError("Failed: assertDoesNotExist", selector, 0, result)
    }

    return this
  }

  /**
   * Asserts that this selection is defined by [count] elements.
   *
   * It will throw an [AssertionError] if the number of elements is not equal to [count]
   */
  fun assertCount(count: Int): TestNodeCollectionSelection {
    val result = fetchMatchingNodes()

    if (result.size != count) {
      throwCountMismatchError("Failed: assertCount", selector, count, result)
    }

    return this
  }

  /**
   * Asserts that all the nodes in the collection are matching the given [matcher].
   *
   * It will throw an [AssertionError] if there is at least one node that does not match.
   *
   * Note: An empty collection with pass this assertion because it technically doesn't have any node
   * that fails the condition.
   */
  fun assertAll(matcher: TestNodeMatcher): TestNodeCollectionSelection {
    val result = fetchMatchingNodes()
    val failedNodes = result.filterNot { matcher.matches(it) }
    if (failedNodes.isNotEmpty()) {
      throwAssertAllFailError(matcher, selector, failedNodes)
    }

    return this
  }

  /**
   * Asserts that there is at least one node in the selection that matches [matcher].
   *
   * It will throw an [AssertionError] if there are no matching nodes.
   *
   * Note: An empty collection will also result in [AssertionError] since it technically doesn't
   * have any matching node
   */
  fun assertAny(matcher: TestNodeMatcher): TestNodeCollectionSelection {
    val result = fetchMatchingNodes()
    if (!result.any(matcher::matches)) {
      throwAssertAnyFailError(matcher, selector, result)
    }

    return this
  }

  fun selectAtIndex(index: Int): TestNodeSelection {
    return TestNodeSelection(testContext, selector.plusIndexNodeSelector(index))
  }

  fun selectFirst(): TestNodeSelection {
    return TestNodeSelection(
        testContext, selector.plusManyToSingleSelector("first node") { it.firstOrNull() })
  }

  fun selectLast(): TestNodeSelection {
    return TestNodeSelection(
        testContext, selector.plusManyToSingleSelector("last node") { it.lastOrNull() })
  }

  /**
   * Attempts to select all subsequent nodes that match the given [matcher].
   *
   * @return a new [TestNodeCollectionSelection]
   */
  fun selectAll(matcher: TestNodeMatcher): TestNodeCollectionSelection {
    return TestNodeCollectionSelection(testContext, selector.plusManyToManySelector(matcher))
  }

  /**
   * Attempts to select the only node that matches the given [matcher].
   *
   * This method returns a [TestNodeSelection] so it expects to find exactly 1 match. Materializing
   * without this condition holding true will result in an [AssertionError] unless asserted via
   * [assertDoesNotExist]
   */
  fun select(matcher: TestNodeMatcher): TestNodeSelection {
    return TestNodeSelection(
        testContext,
        // Using many to many here even though we're returning a TestNodeSelection (and not a
        // collection) because we want the result to be exactly 1. In the case where this doesn't
        // happen, the error trigger is delegated to TestNodeSelection which already handles 0 and
        // more results correctly
        selector.plusManyToManySelector(matcher))
  }

  fun fetchMatchingNodes(): List<TestNode> {
    val nodes = testContext.provideAllTestNodes()
    return selector.map(nodes).nodes
  }
}
