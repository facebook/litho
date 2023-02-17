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

import com.facebook.litho.ActionAttributeKey
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
  fetchTestNode("Failed: printNode").printTo(appendable, maxDepth)
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
  nodes.printTo(appendable, maxDepth)
}

internal fun List<TestNode>.printTo(appendable: Appendable, maxDepth: Int = 0) {
  forEach { node -> node.printTo(appendable, maxDepth) }
}

internal fun TestNode.printTo(appendable: Appendable, maxDepth: Int = Int.MAX_VALUE) {
  printTo(appendable, indentLevel = 0, maxIndentLevel = maxDepth, hasNextSibling = false)
}

private fun TestNode.printTo(
    appendable: Appendable,
    indentLevel: Int,
    maxIndentLevel: Int,
    hasNextSibling: Boolean
) {
  require(maxIndentLevel >= indentLevel) { "maxIndentLevel cannot be less than indentLevel" }

  val levelPrefix = if (indentLevel == 0) "" else " ".repeat(indentLevel)

  // 1. Set node prefix
  val nodePrefix = buildString {
    append(levelPrefix)
    if (indentLevel != 0) {
      append("|")
    }
    append("-")
  }

  appendable.append(nodePrefix)

  // 2. Append name
  val normalizedComponentName = componentType.simpleName
  appendable.append(normalizedComponentName)

  // 3. Append meta properties
  val metaProps = listOfNotNull(Pair("children", children.size).takeIf { children.isNotEmpty() })
  if (metaProps.isNotEmpty()) {
    appendable.appendLine("(${metaProps.joinToString(", ") { (k, v) -> "$k=$v"}})")
  } else {
    appendable.appendLine()
  }

  // 4. Append attributes
  val nonActionAttributes =
      attributes
          .filter { (key, _) -> key !is ActionAttributeKey }
          .mapNotNull { (key, value) ->
            value?.let { Pair(key.description, formatPrettyValue(value)) }
          }

  val attributePrefix = buildString {
    append(levelPrefix)
    append(if (hasNextSibling) "|  " else "   ")
  }

  nonActionAttributes.forEach { (key, value) ->
    appendable.append(attributePrefix)
    appendable.appendLine("$key = $value")
  }

  // 5. Append Action attributes
  val actionAttributes =
      attributes.filter { (key, value) -> key is ActionAttributeKey && value != null }

  if (actionAttributes.isNotEmpty()) {
    appendable.append(attributePrefix)
    appendable.append("Actions = ")
    appendable.appendLine(
        actionAttributes.joinToString(separator = " , ", prefix = "[", postfix = "]") {
          it.first.description
        })
  }

  if (indentLevel == maxIndentLevel) return

  children.forEachIndexed { index, child ->
    child.printTo(
        appendable = appendable,
        indentLevel = indentLevel + 1,
        maxIndentLevel = maxIndentLevel,
        hasNextSibling = children.lastIndex != index)
  }
}

/**
 * This helps to have less clutter when printing values.
 *
 * For example, if the given [value] is a Collection of a single value, it will only print the first
 * value, instead of the whole array representation.
 */
private fun formatPrettyValue(value: Any?): String? {
  return if (value is Collection<*>) {
    if (value.size > 1) value.toString() else value.first()?.toString()
  } else {
    value?.toString()
  }
}
