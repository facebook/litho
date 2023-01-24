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

internal fun throwCountMismatchError(
    failedAssertionMessage: String,
    selector: TestNodeSelector,
    expectedCount: Int,
    matchedNodes: List<TestNode>
): Nothing {
  throw AssertionError(
      buildCountMismatchError(failedAssertionMessage, selector, expectedCount, matchedNodes))
}

internal fun buildCountMismatchError(
    failedAssertionMessage: String,
    selector: TestNodeSelector,
    expectedCount: Int,
    matchedNodes: List<TestNode>
): String = buildString {
  appendLine(failedAssertionMessage)
  appendLine("Reason: Expected exactly $expectedCount node(s), but found ${matchedNodes.size}")
  if (matchedNodes.isNotEmpty()) {
    appendLine("Node(s) found:")
    matchedNodes.printTo(this)
  }
  appendLine("Selector used: ${selector.description}")
}

internal fun throwNoMatchingNodeForSelectionError(
    failedAssertionMessage: String,
    selector: TestNodeSelector
): Nothing {
  throw AssertionError(buildNoMatchingNodeForSelectionError(failedAssertionMessage, selector))
}

internal fun buildNoMatchingNodeForSelectionError(
    failedAssertionMessage: String,
    selector: TestNodeSelector,
    nodes: List<TestNode> = emptyList()
): String = buildString {
  appendLine(failedAssertionMessage)
  appendLine("Reason: Could not find any matching node for selection")
  if (nodes.isNotEmpty()) {
    appendLine("Node(s) found:")
    nodes.printTo(this)
  }
  appendLine("Selector used: ${selector.description}")
}

internal fun throwGeneralError(
    failedAssertionMessage: String,
    selector: TestNodeSelector,
    node: TestNode
): Nothing {
  val message = buildString {
    appendLine(failedAssertionMessage)
    node.printTo(this)
    appendLine("Selector used: ${selector.description}")
  }
  throw AssertionError(message)
}

internal fun buildInvalidIndexError(
    selector: TestNodeSelector,
    index: Int,
    nodes: List<TestNode>
): String = buildString {
  appendLine("Failed selection: index is out of bounds")
  append("Reason: Requested a node at index $index, ")
  if (nodes.isEmpty()) appendLine("but there are no nodes available")
  else if (nodes.size == 1) appendLine("but only 1 node is available")
  else appendLine("but only ${nodes.size} nodes are available")
  if (nodes.isNotEmpty()) {
    appendLine("Node(s) found:")
    nodes.printTo(this)
  }
  appendLine("Selector used: ${selector.description}")
}

internal fun throwAssertAnyFailError(
    matcher: TestNodeMatcher,
    selector: TestNodeSelector,
    nodes: List<TestNode>
): Nothing {
  val message = buildString {
    appendLine("Failed: assertAny(${matcher.description})")
    if (nodes.isEmpty()) {
      appendLine("Reason: Could not find any matching node for selection")
    } else {
      appendLine("Reason: None of the selected nodes match the expected condition")
      appendLine("Node(s) found:")
      nodes.printTo(this)
    }
    appendLine("Selector used: ${selector.description}")
  }
  throw AssertionError(message)
}

internal fun throwAssertAllFailError(
    matcher: TestNodeMatcher,
    selector: TestNodeSelector,
    failedNodes: List<TestNode>
): Nothing {
  val message = buildString {
    appendLine("Failed: assertAll(${matcher.description})")
    appendLine("Reason: The following nodes do not match the expected condition:")
    failedNodes.printTo(this)
    appendLine("Selector used: ${selector.description}")
  }
  throw AssertionError(message)
}
