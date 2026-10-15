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

package com.facebook.litho.testing.viewtree

import android.view.View
import android.view.ViewGroup
import com.facebook.litho.ComponentHost
import com.facebook.litho.NodeInfo
import com.google.common.base.Function
import com.google.common.base.Predicate
import org.assertj.core.api.Assertions.assertThat

/**
 * Helper class for extracting NodeInfo from Views and creating predicates/extractors for testing
 * NodeInfo properties.
 *
 * This class provides utilities for testing accessibility properties and other NodeInfo fields in
 * Litho component hierarchies. It centralizes the logic for extracting NodeInfo from views via the
 * COMPONENT_NODE_INFO_ID tag.
 *
 * Usage example:
 * ```
 * // Get NodeInfo from a view
 * val nodeInfo = NodeInfoTestHelper.getNodeInfo(view)
 *
 * // Create a predicate to check for a specific accessibility role
 * val predicate = NodeInfoTestHelper.createPropertyPredicate(
 *     "accessibilityRole",
 *     "android.widget.Button"
 * ) { it.accessibilityRole }
 *
 * // Create an extractor for debug output
 * val extractor = NodeInfoTestHelper.createPropertyExtractor(
 *     "accessibilityRole"
 * ) { it.accessibilityRole }
 * ```
 */
object NodeInfoTestHelper {

  /** Functional interface for extracting a property from NodeInfo. */
  fun interface PropertyExtractor<T> {
    fun extract(nodeInfo: NodeInfo): T?
  }

  /** Functional interface for formatting property values to readable strings. */
  fun interface PropertyFormatter<T> {
    fun format(value: T): String
  }

  /**
   * Extracts NodeInfo from a View if it has one attached via the COMPONENT_NODE_INFO_ID tag.
   *
   * @param view the view to extract NodeInfo from
   * @return the NodeInfo attached to the view, or null if none exists
   */
  @JvmStatic
  fun getNodeInfo(view: View?): NodeInfo? {
    if (view == null) {
      return null
    }
    val tag = view.getTag(ComponentHost.COMPONENT_NODE_INFO_ID)
    return tag as? NodeInfo
  }

  /**
   * Creates a predicate that checks if a view has a NodeInfo with a property matching the given
   * value.
   *
   * @param propertyName the name of the property (used for debugging)
   * @param expectedValue the expected value of the property
   * @param extractor function to extract the property from NodeInfo
   * @return a predicate that returns true if the view has the expected property value
   */
  @JvmStatic
  fun <T> createPropertyPredicate(
      propertyName: String,
      expectedValue: T,
      extractor: PropertyExtractor<T>,
  ): Predicate<View> {
    return object : Predicate<View> {
      override fun apply(input: View): Boolean {
        val nodeInfo = getNodeInfo(input)
        if (nodeInfo != null) {
          val actualValue = extractor.extract(nodeInfo)
          return expectedValue == actualValue
        }
        return false
      }
    }
  }

  /**
   * Creates an extractor function that extracts a property from NodeInfo and formats it for debug
   * output.
   *
   * @param propertyName the name of the property (used in output messages)
   * @param extractor function to extract the property from NodeInfo
   * @param formatter optional function to format the value for display (defaults to toString())
   * @return a function that returns a debug string describing the property value
   */
  @JvmStatic
  @JvmOverloads
  fun <T> createPropertyExtractor(
      propertyName: String,
      extractor: PropertyExtractor<T>,
      formatter: PropertyFormatter<T> = PropertyFormatter { it.toString() },
  ): Function<View, String> {
    return Function { input ->
      val nodeInfo = getNodeInfo(input)
      if (nodeInfo != null) {
        val value = extractor.extract(nodeInfo)
        if (value != null) {
          val formattedValue = formatter.format(value)
          return@Function "Found $propertyName: $formattedValue, view is ${getVisibilityString(input.visibility)}"
        }
      }

      "No $propertyName found, view is ${getVisibilityString(input.visibility)}"
    }
  }

  private fun <T> collectPropertyValuesRecursive(
      view: View,
      extractor: PropertyExtractor<T>,
      values: MutableList<T>,
  ) {
    val nodeInfo = getNodeInfo(view)
    if (nodeInfo != null) {
      val value = extractor.extract(nodeInfo)
      if (value != null) {
        values.add(value)
      }
    }

    if (view is ViewGroup) {
      for (i in 0 until view.childCount) {
        collectPropertyValuesRecursive(view.getChildAt(i), extractor, values)
      }
    }
  }

  private fun getVisibilityString(visibility: Int): String =
      if (visibility == View.VISIBLE) "visible" else "not visible"

  /**
   * Asserts that a ViewTree contains a view with a NodeInfo that has the specified property value.
   *
   * @param viewTree the ViewTree to search
   * @param propertyName the name of the property (used in error messages)
   * @param expectedValue the expected value of the property
   * @param extractor function to extract the property from NodeInfo
   * @param extractorFunction function to generate debug output for error messages
   * @throws AssertionError if no view with the specified property value is found
   */
  @JvmStatic
  fun <T> assertHasProperty(
      viewTree: ViewTree,
      propertyName: String,
      expectedValue: T,
      extractor: PropertyExtractor<T>,
      extractorFunction: Function<View, String>,
  ) {
    val predicate = createPropertyPredicate(propertyName, expectedValue, extractor)
    val path = viewTree.findChild(predicate)
    assertThat(path)
        .overridingErrorMessage(
            "Cannot find %s \"%s\" in view hierarchy:%n%s",
            propertyName,
            expectedValue,
            viewTree.makeString(extractorFunction),
        )
        .isNotNull()
  }
}
