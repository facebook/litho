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

import androidx.collection.MutableScatterSet
import androidx.collection.ScatterMap
import androidx.collection.ScatterSet
import androidx.collection.emptyScatterSet
import com.facebook.litho.GlobalKey

fun getDirtyScopes(
    updatedStates: Set<StateId>,
    stateReads: Map<StateId, ScatterSet<GlobalKey>>?,
    isComponentScope: Boolean = true,
): Set<GlobalKey> {
  if (updatedStates.isEmpty()) {
    return emptySet()
  }
  var result: HashSet<GlobalKey>? = null
  for (state in updatedStates) {
    if (state.isSpecState && isComponentScope) {
      // Since we can't track who is reading spec states, we treat such mutations
      // as if the whole sub-tree may be dirty (just like without read tracking)
      (result ?: HashSet<GlobalKey>().also { result = it }).add(state.globalKey)
    }
    val readers = stateReads?.get(state) ?: continue
    val dirty = result ?: HashSet<GlobalKey>().also { result = it }
    readers.forEach { dirty.add(it) }
  }
  return result ?: emptySet()
}

/**
 * Allocation-free boolean counterpart of [getDirtyScopes]: whether any of [updatedStates] is dirty
 * for [stateReads] (a spec state in component scope, or read by at least one scope). Early-exits on
 * the first match; use when only the presence of dirty scopes matters, not the scope set.
 */
fun hasDirtyScopes(
    updatedStates: Set<StateId>,
    stateReads: Map<StateId, ScatterSet<GlobalKey>>?,
    isComponentScope: Boolean = true,
): Boolean = hasDirtyScopes(updatedStates, stateReads, isComponentScope) { false }

/** [hasDirtyScopes] over only the states [shouldSkip] rejects. Inline so the predicate is free. */
inline fun hasDirtyScopes(
    updatedStates: Set<StateId>,
    stateReads: Map<StateId, ScatterSet<GlobalKey>>?,
    isComponentScope: Boolean = true,
    shouldSkip: (StateId) -> Boolean,
): Boolean {
  for (state in updatedStates) {
    if (shouldSkip(state)) continue
    if (state.isSpecState && isComponentScope) return true
    if (stateReads?.get(state)?.isEmpty() == false) return true
  }
  return false
}

/**
 * Variant of [getDirtyScopes] for effect scope tracking where uniqueIds may be [Any] (e.g.
 * [com.facebook.litho.HookGlobalKey]) rather than [String].
 */
fun getEffectDirtyScopes(
    updatedStates: Set<StateId>,
    stateReads: Map<StateId, ScatterSet<Any>>?,
): ScatterSet<Any> {
  if (updatedStates.isEmpty() || stateReads.isNullOrEmpty()) {
    return emptyScatterSet()
  }
  var expectedReaderCount = 0
  for (state in updatedStates) {
    expectedReaderCount += stateReads[state]?.size ?: 0
  }
  if (expectedReaderCount == 0) {
    return emptyScatterSet()
  }
  val result = MutableScatterSet<Any>(expectedReaderCount)
  for (state in updatedStates) {
    stateReads[state]?.let { result.addAll(it) }
  }
  return result
}

/**
 * ScatterMap-backed variant of [getDirtyScopes] for effect scope tracking where uniqueIds may be
 * [Any] (e.g. [com.facebook.litho.HookGlobalKey]) rather than [String].
 */
fun getEffectDirtyScopes(
    updatedStates: Set<StateId>,
    stateReads: ScatterMap<StateId, out ScatterSet<Any>>?,
): ScatterSet<Any> {
  if (updatedStates.isEmpty() || stateReads == null || stateReads.isEmpty()) {
    return emptyScatterSet()
  }
  var expectedReaderCount = 0
  for (state in updatedStates) {
    expectedReaderCount += stateReads[state]?.size ?: 0
  }
  if (expectedReaderCount == 0) {
    return emptyScatterSet()
  }
  val result = MutableScatterSet<Any>(expectedReaderCount)
  for (state in updatedStates) {
    stateReads[state]?.let { result.addAll(it) }
  }
  return result
}
