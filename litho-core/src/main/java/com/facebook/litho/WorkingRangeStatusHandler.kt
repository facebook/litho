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
import androidx.collection.SimpleArrayMap
import java.util.HashMap

/** A handler that stores the range status of components with given working range. */
class WorkingRangeStatusHandler {
  @IntDef(STATUS_UNINITIALIZED, STATUS_IN_RANGE, STATUS_OUT_OF_RANGE)
  annotation class WorkingRangeStatus

  /**
   * Use a [SimpleArrayMap] to store status of components with given working range. The key of the
   * container is combined with the component's global key and the working range name.
   *
   * The global key guarantees the uniqueness of the component in a ComponentTree, and it's
   * consistent across different [LayoutState]s. The working range name is used to find the specific
   * working range since a component can have several working ranges. The value is an integer
   * presenting the status.
   */
  private val _status: MutableMap<String, Int> = HashMap()

  fun isInRange(name: String, component: Component, globalKey: String): Boolean =
      getStatus(name, component, globalKey) == STATUS_IN_RANGE

  /** Components in the collection share same status, we can only check the first component. */
  @WorkingRangeStatus
  private fun getStatus(name: String, component: Component, componentGlobalKey: String): Int {
    val key = generateKey(name, componentGlobalKey)
    return _status[key] ?: STATUS_UNINITIALIZED
  }

  fun setEnteredRangeStatus(name: String, component: Component?, globalKey: String) {
    setStatus(name, component, globalKey, STATUS_IN_RANGE)
  }

  fun setExitedRangeStatus(name: String, component: Component?, globalKey: String) {
    setStatus(name, component, globalKey, STATUS_OUT_OF_RANGE)
  }

  fun clear() {
    _status.clear()
  }

  @get:VisibleForTesting
  val status: Map<String, Int>
    get() = _status

  @VisibleForTesting
  fun setStatus(
      name: String,
      component: Component?,
      componentGlobalKey: String,
      @WorkingRangeStatus status: Int
  ) {
    _status[generateKey(name, componentGlobalKey)] = status
  }

  companion object {
    const val STATUS_UNINITIALIZED = 0
    const val STATUS_IN_RANGE = 1
    const val STATUS_OUT_OF_RANGE = 2

    private fun generateKey(name: String, globalKey: String): String = "$name-$globalKey"
  }
}
