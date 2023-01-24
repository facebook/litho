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

/** Transforms a given set of nodes into a new list of [TestNode]. */
class TestNodeSelector(
    val description: String,
    private val parentSelector: TestNodeSelector? = null,
    private val throwOnSelectionError: Boolean = false,
    private val selector: TestNodeSelector.(List<TestNode>) -> SelectionResult
) {

  fun map(nodes: List<TestNode>): SelectionResult {
    val parentResult = parentSelector?.map(nodes)?.throwIfNeeded()

    val inputNodes = parentResult?.nodes ?: nodes
    return selector(inputNodes).throwIfNeeded()
  }

  private fun SelectionResult.throwIfNeeded(): SelectionResult {
    if (throwOnSelectionError && selectionError != null) throw AssertionError(selectionError)
    return this
  }
}

class SelectionResult(val nodes: List<TestNode>, internal val selectionError: String? = null)

internal fun testNodeSelector(matcher: TestNodeMatcher): TestNodeSelector {
  return TestNodeSelector(matcher.description) { nodes ->
    val filteredNodes = nodes.filter(matcher::matches)
    SelectionResult(filteredNodes)
  }
}

/**
 * Should be used for secondary selectors of a [TestNodeSelection] as they would typically expect a
 * single node as their input.
 */
internal fun TestNodeSelector.plusSingleToManySelector(
    description: String,
    newSelector: (TestNode) -> List<TestNode>
): TestNodeSelector {
  return TestNodeSelector(
      description = "${this.description} => $description",
      parentSelector = this,
      throwOnSelectionError = true,
      selector = { nodes ->
        if (nodes.size == 1) SelectionResult(newSelector(nodes[0]))
        else
            SelectionResult(
                emptyList(),
                buildCountMismatchError(
                    failedAssertionMessage = "Failed selection",
                    // Using parent selector to build this message since the bad input was from
                    // there and not really a result of the current selector
                    selector = this@plusSingleToManySelector,
                    expectedCount = 1,
                    matchedNodes = nodes),
            )
      })
}

/**
 * Should be used for secondary selectors of a [TestNodeCollectionSelection] as they would typically
 * expect a collection of test nodes as their input.
 */
internal fun TestNodeSelector.plusManyToSingleSelector(
    description: String,
    newSelector: (List<TestNode>) -> TestNode?
): TestNodeSelector {
  return TestNodeSelector(
      description = "${this.description} => $description",
      parentSelector = this,
      throwOnSelectionError = false,
      selector = { nodes ->
        when (val node = newSelector(nodes)) {
          null ->
              SelectionResult(
                  emptyList(),
                  buildNoMatchingNodeForSelectionError("Failed selection", this, nodes))
          else -> SelectionResult(listOf(node))
        }
      })
}

/**
 * Should be used for secondary selectors of a [TestNodeCollectionSelection] as they would typically
 * expect a collection of test nodes as their input.
 */
internal fun TestNodeSelector.plusManyToManySelector(matcher: TestNodeMatcher): TestNodeSelector {
  return TestNodeSelector(
      description = "${this.description} => ${matcher.description}",
      parentSelector = this,
      throwOnSelectionError = false,
      selector = { nodes ->
        val filteredNodes = nodes.filter { matcher.matches(it) }
        SelectionResult(filteredNodes)
      })
}

/**
 * Should be used for secondary selectors of a [TestNodeCollectionSelection] specifically for cases
 * where we simply need to pick a node at the given index in the collection.
 */
internal fun TestNodeSelector.plusIndexNodeSelector(
    index: Int,
): TestNodeSelector {
  return TestNodeSelector(
      description = "${this.description} => node at index $index",
      parentSelector = this,
      throwOnSelectionError = false,
      selector = { nodes ->
        when (val node = nodes.getOrNull(index)) {
          null -> SelectionResult(emptyList(), buildInvalidIndexError(this, index, nodes))
          else -> SelectionResult(listOf(node))
        }
      })
}
