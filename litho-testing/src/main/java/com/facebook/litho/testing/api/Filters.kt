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

import com.facebook.litho.AttributeKey
import com.facebook.litho.testing.api.TestNodeAttributes.ContentDescription
import com.facebook.litho.testing.api.TestNodeAttributes.Enabled
import com.facebook.litho.testing.api.TestNodeAttributes.TestKey
import com.facebook.litho.widget.WidgetAttributes

/**
 * This filter will match all test nodes that represent a [Component] with the given class type
 * [componentClass].
 *
 * An example usage: `hasType(CounterComponent::class.java)`
 */
fun hasType(type: Class<*>): TestNodeMatcher =
    TestNodeMatcher("is a component of type ${type.canonicalName}") { testNode ->
      testNode.componentType == type
    }

inline fun <reified T> hasType(): TestNodeMatcher = hasType(T::class.java)

fun isEnabled(): TestNodeMatcher =
    TestNodeMatcher("is enabled") { node -> node.getAttribute(Enabled) == true }

fun isNotEnabled(): TestNodeMatcher =
    TestNodeMatcher("is not enabled") { node -> node.getAttribute(Enabled) != true }

fun hasTestKey(key: String): TestNodeMatcher =
    TestNodeMatcher("has test key \"$key\"") { node -> node.getAttribute(TestKey) == key }

/**
 * Returns a [TestNodeMatcher] that verifies if the given node text matches the exact [text]
 *
 * It uses the attribute [WidgetAttributes.Text] to look for the text value. One example of Litho
 * widget that relies on this attribute is the [TextSpec]. This means that you can use this matcher
 * to verify the text used with any instance of Litho's Text.
 */
fun hasText(text: CharSequence): TestNodeMatcher {
  return TestNodeMatcher("has text \"$text\"") { node ->
    val textValues = node.getAttribute(WidgetAttributes.Text)
    textValues?.any { it == text } ?: false
  }
}

/** Returns a [TestNodeMatcher] that verifies if the given node contains the given [text] */
fun hasTextContaining(text: CharSequence): TestNodeMatcher {
  return TestNodeMatcher("has text containing \"$text\"") { node ->
    val textValues = node.getAttribute(WidgetAttributes.Text)
    textValues?.any { it.contains(text) } ?: false
  }
}

/**
 * Returns a [TestNodeMatcher] that verifies if the given node has a content description which is an
 * exact match with [description].
 */
fun hasContentDescription(description: CharSequence): TestNodeMatcher {
  return TestNodeMatcher("has contentDescription \"$description\"") { node ->
    node.getAttribute(ContentDescription) == description
  }
}

/**
 * Generic purpose test node filters based on the attributes system. This filter will match all
 * [TestNode] whose [Component] has set [value] for the attribute with key [key]
 */
fun <T> hasAttribute(key: AttributeKey<T>, value: T): TestNodeMatcher {
  return TestNodeMatcher("${key.description} = $value") { testNode: TestNode ->
    testNode.getAttribute(key) == value
  }
}

/**
 * Returns a [TestNodeMatcher] that verifies if a [TestNode] is the root of a component tree.
 *
 * A root node is the node that has no associated parent.
 */
fun isRoot(): TestNodeMatcher {
  return TestNodeMatcher("is root") { node -> node.parent == null }
}

/**
 * Returns a [TestNodeMatcher] that verifies if a [TestNode] has a parent [TestNode] that matches
 * the given [matcher].
 *
 * If the [TestNode] has no parent (it is the root) then the returned [TestNodeMatcher] will never
 * be matched.
 */
fun hasParent(matcher: TestNodeMatcher): TestNodeMatcher {
  return TestNodeMatcher("has parent that ${matcher.description}") { testNode: TestNode ->
    val parent = testNode.parent
    if (parent == null) {
      false
    } else {
      matcher.matches(parent)
    }
  }
}

/**
 * Returns a [TestNodeMatcher] that will match [TestNode]s which have an ancestor that match with
 * the given [matcher].
 *
 * We consider an ancestor to be any [TestNode] in the direct line of parents. This is the direct
 * parent, the parent of the parent, and so on...
 */
fun hasAncestor(matcher: TestNodeMatcher): TestNodeMatcher {
  return TestNodeMatcher("has ancestor that ${matcher.description}") { testNode: TestNode ->
    testNode.ancestors.any { ancestor -> matcher.matches(ancestor) }
  }
}

/**
 * Returns a [TestNodeMatcher] that will match [TestNode]s which have one child that matches with
 * the given [matcher].
 */
fun hasChild(matcher: TestNodeMatcher): TestNodeMatcher {
  return TestNodeMatcher("has child that ${matcher.description}") { testNode: TestNode ->
    testNode.children.any { child -> matcher.matches(child) }
  }
}

/**
 * Returns a [TestNodeMatcher] that will match [TestNode]s which have one descendant that matches
 * the given [matcher].
 *
 * We consider a descendant of a [TestNode] to be any node that is part of its subtree.
 */
fun hasDescendant(matcher: TestNodeMatcher): TestNodeMatcher {
  fun checkHasDescendant(node: TestNode, matcher: TestNodeMatcher): Boolean {
    if (node.children.any(matcher::matches)) return true

    return node.children.any { child -> checkHasDescendant(child, matcher) }
  }

  return TestNodeMatcher("has descendant that ${matcher.description}") { testNode: TestNode ->
    checkHasDescendant(testNode, matcher)
  }
}

private val TestNode.ancestors: Iterable<TestNode>
  get() =
      object : Iterable<TestNode> {
        override fun iterator(): Iterator<TestNode> =
            object : Iterator<TestNode> {

              private var next = parent

              override fun hasNext(): Boolean = next != null

              override fun next(): TestNode = next!!.also { next = it.parent }
            }
      }
