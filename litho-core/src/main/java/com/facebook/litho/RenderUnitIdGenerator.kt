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
import androidx.collection.MutableObjectIntMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Class that handles generation of unique IDs for RenderUnits for a given ComponentTree. The ID
 * generation uses the component key to create an ID, and creates unique IDs for any components
 * under the same ComponentTree.
 */
class RenderUnitIdGenerator(
    /** Returns the ComponentTree ID that this ID-generator is linked with. */
    val componentTreeId: Int,
) {

  private val nextId = AtomicInteger(1)

  @GuardedBy("this") private var keyToId: MutableObjectIntMap<GlobalKey>? = null

  /**
   * Calculates a returns a unique ID for a given component key and output type. The IDs will be
   * unique for components in the ComponentTree this ID generator is linked with. If an ID was
   * already generated for a given component, the same ID will be returned. Otherwise, a new unique
   * ID will be generated
   *
   * @param componentKey The component key
   * @param type The output type @see OutputUnitType
   */
  fun calculateLayoutOutputId(componentKey: GlobalKey, @OutputUnitType type: Int): Long =
      encodeId(getId(componentKey), type, componentTreeId)

  @Synchronized
  private fun getId(key: GlobalKey): Int {
    val map = keyToId ?: MutableObjectIntMap<GlobalKey>().also { keyToId = it }
    return map.getOrPut(key) { nextId.getAndIncrement() }
  }

  /**
   * Layout-output id codec.
   *
   * IDs pack three values into a single `Long`:
   * - bits 0-31: per-tree key id (from [getId])
   * - bits 32-34: [OutputUnitType] (3 bits; values are 0..4)
   * - bits 35+: [componentTreeId]
   *
   * [encodeId] and [decodeType] are inverses — keep them in sync if the layout changes.
   */
  companion object {
    private const val TYPE_SHIFT = 32
    private const val COMPONENT_TREE_SHIFT = 35
    private const val TYPE_MASK = 0x7L

    /** Packs [keyId], [type], and [componentTreeId] into a single layout-output id. */
    @JvmStatic
    fun encodeId(
        keyId: Int,
        @OutputUnitType type: Int,
        componentTreeId: Int,
    ): Long =
        keyId.toLong() or
            (type.toLong() shl TYPE_SHIFT) or
            (componentTreeId.toLong() shl COMPONENT_TREE_SHIFT)

    /**
     * Extracts the [OutputUnitType] from an [id] produced by [encodeId]. The mask is required
     * because the bits above [TYPE_SHIFT] also carry the componentTreeId.
     */
    @JvmStatic
    @OutputUnitType
    fun decodeType(id: Long): Int = ((id shr TYPE_SHIFT) and TYPE_MASK).toInt()
  }
}
