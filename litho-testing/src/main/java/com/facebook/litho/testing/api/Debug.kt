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

import com.facebook.litho.widget.WidgetAttributes
import java.lang.Appendable

/**
 * Prints all the available node information into a string.
 *
 * [maxDepth] can be used to configure how deep into the hierarchy to print. **0** will print just
 * this node, and [Int.MAX_VALUE] effectively prints the whole hierarchy. By default, the whole
 * hierarchy will be printed.
 */
fun TestNodeSelection.printToString(maxDepth: Int = Int.MAX_VALUE): String {
  return buildString { printTo(this, maxDepth) }
}

/**
 * Prints all the matching nodes in the collection into a string.
 *
 * [maxDepth] can be used to configure how deep into the hierarchy of each node to print. **0** will
 * print just the node, and [Int.MAX_VALUE] effectively prints the whole hierarchy. By default, this
 * does not print the sub-hierarchy of the matching modes.
 *
 * @see [TestNodeSelection.printToString]
 */
fun TestNodeCollectionSelection.printToString(maxDepth: Int = 0): String {
  return buildString { printTo(this, maxDepth) }
}

/**
 * Prints all the available node information into the given [appendable].
 *
 * A typical use-case for this API could be printing the node hierarchy into a stream or file, as
 * the case may be, using the [java.io.OutputStreamWriter] or [java.io.FileWriter] APIs accordingly
 *
 * [maxDepth] can be used to configure how deep into the hierarchy to print. **0** will print just
 * this node, and [Int.MAX_VALUE] effectively prints the whole hierarchy. By default, the whole
 * hierarchy will be printed.
 *
 * @see [TestNodeSelection.printToString]
 */
fun TestNodeSelection.printTo(appendable: Appendable, maxDepth: Int = Int.MAX_VALUE) {
  fetchTestNode()
      .printTo(
          appendable,
          indentLevel = 0,
          indentLevelPrefix = "",
          maxIndentLevel = maxDepth,
          hasNextSibling = false)
}

/**
 * Prints all the matching nodes in the collection into the given [appendable].
 *
 * [maxDepth] can be used to configure how deep into the hierarchy of each node to print. **0** will
 * print just the node, and [Int.MAX_VALUE] effectively prints the whole hierarchy. By default, this
 * does not print the sub-hierarchy of the matching modes.
 *
 * @see [TestNodeSelection.printTo]
 * @see [TestNodeCollectionSelection.printToString]
 */
fun TestNodeCollectionSelection.printTo(appendable: Appendable, maxDepth: Int = 0) {
  val nodes = fetchMatchingNodes()
  appendable.appendLine("Found ${nodes.size} matching node(s)")
  nodes.forEach { node ->
    node.printTo(
        appendable,
        indentLevel = 0,
        indentLevelPrefix = "",
        maxIndentLevel = maxDepth,
        hasNextSibling = false)
  }
}

private fun TestNode.printTo(
    appendable: Appendable,
    indentLevel: Int,
    indentLevelPrefix: String,
    maxIndentLevel: Int,
    hasNextSibling: Boolean
) {
  require(maxIndentLevel >= indentLevel) { "maxIndentLevel cannot be less than indentLevel" }

  fun newPrefix(isAddendum: Boolean = false) = buildString {
    append(indentLevelPrefix)
    when {
      isAddendum -> if (indentLevel == 0 || !hasNextSibling) append("  ") else append("| ")
      indentLevel == 0 -> append("- ")
      else -> append("|-")
    }
  }

  val basicProps =
      listOfNotNull(
          Pair("componentType", componentType.simpleName),
          testKey?.let { Pair("testKey", it) },
          contentDescription?.let { Pair("contentDescription", "'$it'") },
          Pair("isEnabled", isEnabled),
          Pair("children", children.size).takeIf { children.isNotEmpty() },
      )
  val newPrefix = newPrefix()
  appendable.append(newPrefix)
  appendable.appendLine("Node(${basicProps.joinToString(", ") { (k, v) -> "$k=$v"}})")

  val newPrefixForAddendum = newPrefix(isAddendum = true)
  val attrs =
      listOfNotNull(
          getAttribute(WidgetAttributes.Text)?.let { Pair("text", "'$it'") },
          // Add more attributes here
      )
  if (attrs.isNotEmpty()) {
    appendable.append(newPrefixForAddendum)
    appendable.appendLine("Attrs: [${attrs.joinToString(", ") { (k, v) -> "$k=$v"}}]")
  }

  val actions =
      listOfNotNull(
          clickHandler?.let { "click" },
          // Add more actions here
      )
  if (actions.isNotEmpty()) {
    appendable.append(newPrefixForAddendum)
    appendable.appendLine("Actns: [${actions.joinToString(", ")}]")
  }

  if (indentLevel == maxIndentLevel) return

  children.forEachIndexed { index, child ->
    child.printTo(
        appendable = appendable,
        indentLevel = indentLevel + 1,
        indentLevelPrefix = newPrefixForAddendum,
        maxIndentLevel = maxIndentLevel,
        hasNextSibling = index != children.lastIndex)
  }
}
