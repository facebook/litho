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
import com.google.common.base.Function

/**
 * Helper object for accessibility-related testing utilities in Litho component hierarchies.
 *
 * This object provides accessibility-specific predicates, extractors, and assertions built on top
 * of the generic [NodeInfoTestHelper]. Use this for testing accessibility properties like
 * accessibility roles, focus states, and other accessibility-related NodeInfo fields.
 *
 * Usage example:
 * ```
 * // Assert that a view tree contains a view with a specific accessibility role
 * AccessibilityTestHelper.assertHasAccessibilityRole(viewTree, "android.widget.Button")
 *
 * // Use the extractor function for debug output
 * val extractor = AccessibilityTestHelper.GET_ACCESSIBILITY_ROLE_FUNCTION
 * ```
 */
object AccessibilityTestHelper {

  /**
   * A function that extracts accessibility role information from a View and formats it for debug
   * output.
   *
   * This can be used with [ViewTree.makeString] to generate human-readable debug output showing the
   * accessibility roles in a view hierarchy.
   */
  @JvmField
  val GET_ACCESSIBILITY_ROLE_FUNCTION: Function<View, String> =
      NodeInfoTestHelper.createPropertyExtractor<String>(
          "accessibility role",
          { it.accessibilityRole },
      )

  /**
   * Asserts that a ViewTree contains a view with a NodeInfo that has the specified accessibility
   * role.
   *
   * @param viewTree the ViewTree to search
   * @param accessibilityRole the accessibility role to search for
   * @throws AssertionError if no view with the specified accessibility role is found
   */
  @JvmStatic
  fun assertHasAccessibilityRole(viewTree: ViewTree, accessibilityRole: String) {
    NodeInfoTestHelper.assertHasProperty(
        viewTree,
        "accessibility role",
        accessibilityRole,
        { it.accessibilityRole },
        GET_ACCESSIBILITY_ROLE_FUNCTION,
    )
  }
}
