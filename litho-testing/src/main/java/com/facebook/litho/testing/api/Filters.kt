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

fun isEnabled(): TestNodeMatcher = TestNodeMatcher("is enabled") { node -> node.isEnabled }

fun isNotEnabled(): TestNodeMatcher = TestNodeMatcher("is not enabled") { node -> !node.isEnabled }

fun hasTestKey(key: String): TestNodeMatcher =
    TestNodeMatcher("has test key \"$key\"") { node -> node.testKey == key }

/**
 * Returns a [TestNodeMatcher] that verifies if the given node text matches the exact [text]
 *
 * It uses the attribute [WidgetAttributes.Text] to look for the text value. One example of Litho
 * widget that relies on this attribute is the [TextSpec]. This means that you can use this matcher
 * to verify the text used with any instance of Litho's Text.
 */
fun hasText(text: String): TestNodeMatcher {
  return hasAttribute(WidgetAttributes.Text, text)
}

/** Returns a [TestNodeMatcher] that verifies if the given node contains the given [text] */
fun hasTextContaining(text: CharSequence): TestNodeMatcher {
  return TestNodeMatcher("has text containing \"$text\"") { node ->
    node.getAttribute(WidgetAttributes.Text)?.contains(text) ?: false
  }
}

/**
 * Returns a [TestNodeMatcher] that verifies if the given node has a content description which is an
 * exact match with [description].
 */
fun hasContentDescription(description: CharSequence): TestNodeMatcher {
  return TestNodeMatcher("has contentDescription \"$description\"") { node ->
    node.contentDescription == description
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
