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

import androidx.annotation.GuardedBy
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Class that handles generation of unique IDs for RenderUnits for a given ComponentTree. The ID
 * generation uses the component key to create an ID, and creates unique IDs for any components
 * under the same ComponentTree.
 */
class RenderUnitIdGenerator(
    /** Returns the ComponentTree ID that this ID-generator is linked with. */
    val componentTreeId: Int
) {

  private val nextId = AtomicInteger(1)

  @GuardedBy("this") private val keyToId = HashMap<String, Int>()

  /**
   * Calculates a returns a unique ID for a given component key and output type. The IDs will be
   * unique for components in the ComponentTree this ID generator is linked with. If an ID was
   * already generated for a given component, the same ID will be returned. Otherwise, a new unique
   * ID will be generated
   *
   * @param componentKey The component key
   * @param type The output type @see OutputUnitType
   */
  fun calculateLayoutOutputId(componentKey: String, @OutputUnitType type: Int): Long =
      addTypeAndComponentTreeToId(getId(componentKey), type, componentTreeId)

  @Synchronized
  private fun getId(key: String): Int = keyToId.getOrPut(key) { nextId.getAndIncrement() }

  companion object {
    private fun addTypeAndComponentTreeToId(
        id: Int,
        @OutputUnitType type: Int,
        componentTreeId: Int
    ): Long = id.toLong() or (type.toLong() shl 32) or (componentTreeId.toLong() shl 35)
  }
}
