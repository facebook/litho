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

package com.facebook.litho.state

import com.facebook.litho.GlobalKey
import com.facebook.litho.StringGlobalKey
import java.util.NavigableSet
import java.util.TreeSet

/**
 * Pre-computed index over a set of dirty scope keys for O(1) descendant lookups.
 *
 * Built once per resolve/layout pass from the dirty scope keys, then queried O(tree_size) times
 * during reconciliation and layout. Exploits the structural parent pointers in
 * [com.facebook.litho.DefaultGlobalKey] and [com.facebook.litho.ManualGlobalKey] to pre-compute the
 * set of all ancestors of all dirty scopes. A query "does any dirty scope descend from this key?"
 * becomes a single hash lookup into the ancestor set.
 *
 * For [StringGlobalKey] (legacy, flag-off path) which has no parent pointer, falls back to
 * string-prefix matching.
 */
class DirtyScopeIndex
private constructor(
    private val directKeys: Set<GlobalKey>,
    private val ancestors: Set<GlobalKey>,
    private val stringKeys: NavigableSet<String>,
) {

  /** Returns true if [key] is directly in the dirty scope set. */
  operator fun contains(key: GlobalKey): Boolean = key in directKeys

  /**
   * Returns true if any dirty scope is a strict descendant of [key].
   *
   * For structural keys (DefaultGlobalKey, ManualGlobalKey) this is an O(1) hash lookup. For
   * StringGlobalKey dirty scopes, falls back to O(log n) NavigableSet lookup via string-prefix
   * comparison.
   */
  fun hasDescendant(key: GlobalKey): Boolean {
    if (key in ancestors) return true
    if (stringKeys.isEmpty()) return false
    val keyStr = key.toString()
    val higher = stringKeys.higher(keyStr)
    return higher != null && higher.startsWith(keyStr)
  }

  /** Returns true if [key] is dirty or has a dirty descendant. */
  fun containsOrHasDescendant(key: GlobalKey): Boolean = contains(key) || hasDescendant(key)

  val size: Int
    get() = directKeys.size

  fun isEmpty(): Boolean = directKeys.isEmpty()

  companion object {
    private val EMPTY_NAVIGABLE_SET: NavigableSet<String> = TreeSet()
    val EMPTY = DirtyScopeIndex(emptySet(), emptySet(), EMPTY_NAVIGABLE_SET)

    /**
     * Builds an index from a set of dirty scope keys.
     *
     * For each key with parent pointers (DefaultGlobalKey, ManualGlobalKey), walks up the parent
     * chain and adds each ancestor to the ancestor set. Shared ancestor chains are only walked once
     * (early break on [MutableSet.add] returning false).
     *
     * For StringGlobalKey (no parent pointer), stores the key string in a NavigableSet for O(log n)
     * prefix lookup via [NavigableSet.higher].
     */
    fun of(keys: Set<GlobalKey>): DirtyScopeIndex {
      if (keys.isEmpty()) return EMPTY
      val ancestors = hashSetOf<GlobalKey>()
      val stringKeys: NavigableSet<String> = TreeSet()
      for (key in keys) {
        if (key is StringGlobalKey) {
          stringKeys.add(key.key)
        } else {
          var current: GlobalKey? = key.parent
          while (current != null) {
            if (!ancestors.add(current)) break
            current = current.parent
          }
        }
      }
      return DirtyScopeIndex(keys, ancestors, stringKeys)
    }
  }
}
