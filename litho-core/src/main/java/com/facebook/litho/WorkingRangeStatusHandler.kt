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

package com.facebook.litho

import androidx.annotation.IntDef
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableObjectIntMap
import java.util.HashMap

/** A handler that stores the range status of components with given working range. */
class WorkingRangeStatusHandler {
  @IntDef(STATUS_UNINITIALIZED, STATUS_IN_RANGE, STATUS_OUT_OF_RANGE)
  annotation class WorkingRangeStatus

  internal data class WorkingRangeKey(val name: String, val globalKey: GlobalKey)

  /**
   * Stores the status of components with a given working range. The key of the container is a
   * [WorkingRangeKey] combining the component's global key and the working range name.
   *
   * The global key guarantees the uniqueness of the component in a ComponentTree, and it's
   * consistent across different [LayoutState]s. The working range name is used to find the specific
   * working range since a component can have several working ranges. The value is an integer
   * presenting the status.
   */
  private var _status: MutableObjectIntMap<WorkingRangeKey>? = null

  private fun getOrCreateStatus(): MutableObjectIntMap<WorkingRangeKey> {
    return _status ?: MutableObjectIntMap<WorkingRangeKey>().also { _status = it }
  }

  fun isInRange(name: String, globalKey: GlobalKey): Boolean =
      getStatus(name, globalKey) == STATUS_IN_RANGE

  /** Components in the collection share same status, we can only check the first component. */
  @WorkingRangeStatus
  private fun getStatus(name: String, componentGlobalKey: GlobalKey): Int {
    val key = WorkingRangeKey(name, componentGlobalKey)
    // STATUS_UNINITIALIZED == 0 is also the getOrDefault fallback for a missing key, so this
    // preserves the existing semantics.
    return _status?.getOrDefault(key, STATUS_UNINITIALIZED) ?: STATUS_UNINITIALIZED
  }

  fun setEnteredRangeStatus(name: String, globalKey: GlobalKey) {
    setStatus(name, globalKey, STATUS_IN_RANGE)
  }

  fun setExitedRangeStatus(name: String, globalKey: GlobalKey) {
    setStatus(name, globalKey, STATUS_OUT_OF_RANGE)
  }

  fun clear() {
    _status = null
  }

  @get:VisibleForTesting
  internal val status: Map<WorkingRangeKey, Int>?
    get() {
      // MutableObjectIntMap doesn't implement Map; synthesize a snapshot for the test getter only.
      // Not on any hot path.
      return _status?.let { map ->
        HashMap<WorkingRangeKey, Int>(map.size).also { hashMap ->
          map.forEach { key, value -> hashMap[key] = value }
        }
      }
    }

  @VisibleForTesting
  fun setStatus(name: String, componentGlobalKey: GlobalKey, @WorkingRangeStatus status: Int) {
    val key = WorkingRangeKey(name, componentGlobalKey)
    getOrCreateStatus()[key] = status
  }

  companion object {
    const val STATUS_UNINITIALIZED = 0
    const val STATUS_IN_RANGE = 1
    const val STATUS_OUT_OF_RANGE = 2
  }
}
