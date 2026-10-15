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
import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.base.Predicates
import com.google.common.collect.ImmutableList

/**
 * This is a helper class to allow asserting on view trees and recursively verify predicates on its
 * nodes within the narrow abilities that Robolectric affords us.
 */
class ViewTree private constructor(private val view: View) {

  /** @return the view group used to generate this tree */
  val root: View
    get() = view

  /**
   * Find a view in the hierarchy for which the given predicate is true
   *
   * @param predicate the predicate to find a view upholding
   * @return null if no such view is found, or a list showing the path in the hierarchy to the view
   *   for which the predicate holds
   */
  fun findChild(predicate: Predicate<View>): ImmutableList<View>? {
    return findChild(view, predicate, Predicates.alwaysTrue<ViewGroup>())
  }

  /**
   * Find a view in the hierarchy for which the given predicate is true, while only check children
   * of nodes as directed by the additional shouldCheckChildren predicate
   *
   * @param predicate the predicate to find a view upholding
   * @param shouldCheckChildren a predicate to decide whether to
   * @return null if no such view is found, or a list showing the path in the hierarchy to the view
   *   for which the predicate holds
   */
  fun findChild(
      predicate: Predicate<View>,
      shouldCheckChildren: Predicate<in ViewGroup>,
  ): ImmutableList<View>? {
    return findChild(view, predicate, shouldCheckChildren)
  }

  /**
   * Generates a string describing the views tree using the views' toString methods and an extra
   * information function.
   *
   * The output is a string, with each view of the tree in its own line, indented according to its
   * depth in the tree, and then the extra information supplied by the function.
   *
   * This can be used, for example, to print all views and their respective text and is useful for
   * when assertions fail.
   *
   * @param extraTextFunction the function returning extra information to print per view, or null if
   *   not extra information should be printed
   * @return a string describing the tree
   */
  fun makeString(extraTextFunction: Function<View, out String?>?): String {
    return makeString(extraTextFunction, view, 0)
  }

  /**
   * Generates a string describing the views tree using the views' toString methods and an extra
   * information function with formatting starting at the given depth.
   *
   * The output is a string, with each view of the tree in its own line, indented according to its
   * depth in the tree, and then the extra information supplied by the function.
   *
   * This can be used, for example, to print all views and their respective text and is useful for
   * when assertions fail.
   *
   * @param startingDepth the starting depth of the tree for spacing
   * @return a string describing the tree
   */
  fun makeString(startingDepth: Int, extraTextFunction: Function<View, out String?>?): String {
    return makeString(extraTextFunction, view, startingDepth)
  }

  private fun makeString(
      extraTextFunction: Function<View, out String?>?,
      view: View,
      depth: Int,
  ): String {
    val builder = StringBuilder()
    if (depth > 0) {
      builder.append('\n')
    }
    repeat(depth) { builder.append("  ") }
    builder.append(getViewString(view))
    val extra = extraTextFunction?.apply(view)
    if (extra != null) {
      builder.append(" (")
      builder.append(extra)
      builder.append(")")
    }
    if (view is ViewGroup) {
      for (i in 0 until view.childCount) {
        val child = view.getChildAt(i)
        builder.append(makeString(extraTextFunction, child, depth + 1))
      }
    }

    return builder.toString()
  }

  private fun getViewString(view: View): String {
    val string = view.toString()
    return removePrefix(removePrefix(string, "android.widget."), "android.view.")
  }

  private fun findChild(
      root: View,
      predicate: Predicate<View>,
      shouldCheckChildren: Predicate<in ViewGroup>,
  ): ImmutableList<View>? {
    if (predicate.apply(root)) {
      return ImmutableList.of(root)
    }

    if (root is ViewGroup && shouldCheckChildren.apply(root)) {
      for (i in 0 until root.childCount) {
        val child = root.getChildAt(i)
        val result = findChild(child, predicate, shouldCheckChildren)
        if (result != null) {
          return ImmutableList.builder<View>().add(root).addAll(result).build()
        }
      }
    }

    return null
  }

  companion object {
    @JvmStatic
    fun of(view: View): ViewTree {
      return ViewTree(view)
    }

    private fun removePrefix(string: String, prefix: String): String {
      return if (string.startsWith(prefix)) string.substring(prefix.length) else string
    }
  }
}
