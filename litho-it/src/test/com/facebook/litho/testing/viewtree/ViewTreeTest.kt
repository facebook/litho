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

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.google.common.base.Function
import com.google.common.base.Predicates
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.LooperMode

/** Tests [ViewTree] */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ViewTreeTest {

  private lateinit var root: ViewGroup
  private lateinit var childLayout: ViewGroup
  private lateinit var child1: View
  private lateinit var grandchild1: View
  private lateinit var grandchild2: View
  private lateinit var tree: ViewTree

  @Before
  fun setUp() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    root = LinearLayout(activity)
    childLayout = LinearLayout(activity)
    child1 = View(activity)
    grandchild1 = View(activity)
    grandchild2 = View(activity)

    childLayout.addView(grandchild1)
    childLayout.addView(grandchild2)
    root.addView(child1)
    root.addView(childLayout)

    tree = ViewTree.of(root)
  }

  @Test
  fun testFindRoot() {
    assertThat(tree.findChild(Predicates.equalTo<View>(root))).containsExactly(root)
  }

  @Test
  fun testReturnNullIfCannotFind() {
    assertThat(tree.findChild(Predicates.isNull<View>())).isNull()
  }

  @Test
  fun testFindChild() {
    assertThat(tree.findChild(Predicates.equalTo<View>(childLayout)))
        .containsExactly(root, childLayout)
  }

  @Test
  fun testFindGrandchild() {
    assertThat(tree.findChild(Predicates.equalTo<View>(grandchild2)))
        .containsExactly(root, childLayout, grandchild2)
  }

  @Test
  fun testRespectShouldGoIntoChildren() {
    assertThat(
        tree.findChild(
            Predicates.equalTo<View>(grandchild2),
            Predicates.not(Predicates.equalTo(childLayout)),
        ),
    )
        .isNull()
  }

  @Test
  fun testGenerateString() {
    val expected =
        getString(root) +
            " (" +
            root.hashCode() +
            ")\n" +
            "  " +
            getString(child1) +
            " (" +
            child1.hashCode() +
            ")\n" +
            "  " +
            getString(childLayout) +
            " (" +
            childLayout.hashCode() +
            ")\n" +
            "    " +
            getString(grandchild1) +
            " (" +
            grandchild1.hashCode() +
            ")\n" +
            "    " +
            getString(grandchild2) +
            " (" +
            grandchild2.hashCode() +
            ")"

    assertThat(tree.makeString(Function<View, String> { input -> input.hashCode().toString() }))
        .isEqualTo(expected)
  }

  private fun getString(view: View): String {
    return removePrefix(removePrefix(view.toString(), "android.widget."), "android.view.")
  }

  private fun removePrefix(string: String, prefix: String): String {
    return if (string.startsWith(prefix)) string.substring(prefix.length) else string
  }
}
