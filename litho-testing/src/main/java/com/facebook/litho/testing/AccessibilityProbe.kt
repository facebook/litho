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

package com.facebook.litho.testing

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider

/**
 * Reads a rendered tree the way an accessibility service does: as nodes, not as Views.
 *
 * A service never sees a `ComponentHost`. It sees a tree of [AccessibilityNodeInfo], some nodes
 * backed by a View and some virtual, and it acts on them by id. A test that reaches for a
 * `ComponentHost` is asserting on where the node happens to come from today rather than on what a
 * service observes, so it has to be rewritten the moment components stop mounting a View of their
 * own. Locate targets through this instead, and the same test covers either implementation.
 *
 * Nothing is cached: every lookup re-walks the tree and re-reads each node, so a probe held across
 * a re-render reports the new state.
 *
 * The tree must be attached to a real window — see [LithoTestRule.attachToRealWindow].
 */
class AccessibilityProbe(private val root: View) {

  /**
   * Every node a service could reach from the root, in the order of the node tree the platform
   * hands a service: a View's children as [View.addChildrenForAccessibility] lists them, with
   * unimportant Views flattened out and siblings sorted by position on screen, and a virtual node's
   * children in id order. That is not quite reading order: a service also applies the
   * `traversalBefore`/`traversalAfter` hints `Style.focusOrder` sets, and this does not.
   *
   * Only what is shown is listed, so a subtree that is invisible or not attached is left out.
   */
  fun nodes(): List<AccessibilityTarget> {
    // Detached, the platform lists no children at all, and an assertion that a node is absent
    // would pass without looking at anything.
    check(root.windowToken != null) {
      "The tree is not attached to a window; see LithoTestRule.attachToRealWindow."
    }
    val found = mutableListOf<AccessibilityTarget>()
    collect(root, found)
    return found
  }

  fun findAll(matching: (AccessibilityNodeInfo) -> Boolean): List<AccessibilityTarget> =
      nodes().filter { matching(it.info()) }

  fun find(matching: (AccessibilityNodeInfo) -> Boolean): AccessibilityTarget? =
      nodes().firstOrNull { matching(it.info()) }

  fun require(matching: (AccessibilityNodeInfo) -> Boolean): AccessibilityTarget =
      checkNotNull(find(matching)) { "No node matched. Reachable:\n${describeReachable()}" }

  private fun describeReachable(): String =
      nodes().joinToString(separator = "\n") { "  ${it.describe()}" }

  private fun collect(view: View, found: MutableList<AccessibilityTarget>) {
    if (view.isImportantForAccessibility) {
      found.add(AccessibilityTarget(view, AccessibilityNodeProvider.HOST_VIEW_ID))
    }
    val provider = view.accessibilityNodeProvider
    if (provider != null) {
      // Virtual ids are assumed to be 0 until childCount: that is what ExploreByTouchHelper clients
      // assign, Litho's ComponentAccessibilityDelegate included. A provider free to pick sparse ids
      // would leave some of its nodes unreachable here, so ids with no node are skipped rather than
      // treated as an error.
      val host = provider.createAccessibilityNodeInfo(AccessibilityNodeProvider.HOST_VIEW_ID)
      for (id in 0 until (host?.childCount ?: 0)) {
        if (provider.createAccessibilityNodeInfo(id) != null) {
          found.add(AccessibilityTarget(view, id))
        }
      }
    }
    if (view is ViewGroup) {
      val children = ArrayList<View>()
      view.addChildrenForAccessibility(children)
      for (child in children) {
        collect(child, found)
      }
    }
  }
}

/**
 * One node in the tree a service walks, whether it is backed by a View or is virtual. Which of the
 * two it is deliberately does not change how a test reads or acts on it.
 */
class AccessibilityTarget
internal constructor(private val owner: View, private val virtualId: Int) {

  val isVirtual: Boolean
    get() = virtualId != AccessibilityNodeProvider.HOST_VIEW_ID

  /** What a service reads for this node, as of now. */
  fun info(): AccessibilityNodeInfo {
    val provider = owner.accessibilityNodeProvider
    val node =
        if (provider != null) {
          provider.createAccessibilityNodeInfo(virtualId)
        } else {
          owner.createAccessibilityNodeInfo()
        }
    return checkNotNull(node) { "No node $virtualId on $owner." }
  }

  /** Activates this node, as a service does on behalf of the user. */
  @JvmOverloads
  fun performAction(action: Int, arguments: Bundle? = null): Boolean {
    val provider = owner.accessibilityNodeProvider
    return provider?.performAction(virtualId, action, arguments)
        ?: owner.performAccessibilityAction(action, arguments)
  }

  /**
   * Gives this node's owner input focus, as keyboard or D-pad navigation does, and reports whether
   * focus moved.
   *
   * Requires the test to have left touch mode
   * (`InstrumentationRegistry.getInstrumentation().setInTouchMode(false)`), which is the state a
   * device is in whenever focus is being moved by keys, and the component to have declared itself
   * focusable. Returns false otherwise, exactly as the framework would.
   */
  fun requestKeyboardFocus(): Boolean = owner.requestFocus()

  internal fun describe(): String {
    val node = info()
    return "virtual=$isVirtual id=$virtualId class=${node.className} " +
        "cd=${node.contentDescription} clickable=${node.isClickable}"
  }
}

/** An [AccessibilityProbe] over the rendered `LithoView`. */
fun TestLithoView.accessibilityProbe(): AccessibilityProbe = AccessibilityProbe(lithoView)
