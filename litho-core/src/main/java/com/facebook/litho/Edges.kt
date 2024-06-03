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

import com.facebook.rendercore.Equivalence
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaEdge
import kotlin.math.abs
import kotlin.math.min

class Edges : Equivalence<Edges?> {
  // This long maps the indexes of the YogaEdges within the values array.
  // Each group of 4 bits represent an index and the position of these 4 bits is related to the
  // YogaEdge.intValue(). For example, YogaEdge.TOP is position 1. Therefore bits representing TOP
  // are 4..7.
  // We initialize the long to be all "1"s representing all indexes to be undefined.
  private var edgesToValuesIndex = 0L.inv()
  private var values: FloatArray? = null
  private var hasAliasesSet = false

  private fun requireValues(): FloatArray {
    return checkNotNull(values)
  }

  operator fun set(yogaEdge: YogaEdge, value: Float): Boolean {
    val edgeIntValue = yogaEdge.intValue()
    if (!floatsEqual(getRaw(edgeIntValue), value)) {
      val edgeIndex = getIndex(edgeIntValue)

      // If we need to "unset" a previously set edge.
      if (YogaConstants.isUndefined(value)) {
        // UNSET index:
        // Set the 4 bits representing the edge as undefined ('1111').
        edgesToValuesIndex = edgesToValuesIndex or (UNDEFINED_INDEX.toLong() shl edgeIntValue * 4)
        requireValues()[edgeIndex.toInt()] = YogaConstants.UNDEFINED

        // If we need to insert a new edge value.
      } else if (edgeIndex == UNDEFINED_INDEX) {
        val newIndex = firstAvailableIndex
        check(newIndex < EDGES_LENGTH) {
          "The newIndex for the array cannot be bigger than the amount of Yoga Edges."
        }
        // SETS index:
        // Clear the bits at the index position.
        edgesToValuesIndex = edgesToValuesIndex and (0xF.toLong() shl edgeIntValue * 4).inv()
        // Then set the actual 4 bits value of the index. Leaving this as two steps for clarity.
        edgesToValuesIndex = edgesToValuesIndex or (newIndex.toLong() shl edgeIntValue * 4)
        requireValues()[newIndex.toInt()] = value

        // Otherwise we need to overwrite an existing value.
      } else {
        requireValues()[edgeIndex.toInt()] = value
      }

      // 1. It moves the right most 3 "4bits set" represeting ALL, VERTICAL and HORIZONTAL
      //    to the first 3 "4bits set" position of our long array (0xFFF).
      // 2. It converts the array from long to int.
      // 3. It inverts the bits of the current array. UNDEFINED_INDEX is 0xF or "1111".
      //    When an UNDEFINED_INDEX is inverted, we expect all zeros "0000".
      // 4. Now the inverted array is masked with 0xFFF to get only the values we
      //    are interested into.
      // 5. If the result is equal to 0, we know that ALL, VERTICAL and HORIZONTAL were
      //    all containing UNDEFINED_INDEXes. If that's not the case, we have an alias
      //    set and will set the mHasAliasesSet flag to true.
      hasAliasesSet =
          (edgesToValuesIndex shr ALIASES_RIGHT_SHIFT).toInt().inv() and ALIASES_MASK != 0
      return true
    }
    return false
  }

  operator fun get(edge: YogaEdge): Float {
    val defaultValue =
        if (edge == YogaEdge.START || edge == YogaEdge.END) {
          YogaConstants.UNDEFINED
        } else {
          DEFAULT_VALUE
        }

    // Nothing is set.
    if (edgesToValuesIndex == 0L.inv()) {
      return defaultValue
    }

    val edgeIndex = getIndex(edge.intValue())
    if (edgeIndex != UNDEFINED_INDEX) {
      return requireValues()[edgeIndex.toInt()]
    }
    if (hasAliasesSet) {
      val secondTypeEdgeValue =
          if (edge == YogaEdge.TOP || edge == YogaEdge.BOTTOM) {
            VERTICAL_INTVALUE
          } else {
            HORIZONTAL_INTVALUE
          }
      val secondTypeEdgeIndex = getIndex(secondTypeEdgeValue)
      if (secondTypeEdgeIndex != UNDEFINED_INDEX) {
        return requireValues()[secondTypeEdgeIndex.toInt()]
      } else if (getIndex(ALL_INTVALUE) != UNDEFINED_INDEX) {
        return requireValues()[getIndex(ALL_INTVALUE).toInt()]
      }
    }
    return defaultValue
  }

  fun getRaw(edge: YogaEdge): Float {
    val edgeIndex = getIndex(edge.intValue())
    return if (edgeIndex == UNDEFINED_INDEX) {
      YogaConstants.UNDEFINED
    } else {
      requireValues()[edgeIndex.toInt()]
    }
  }

  /** @param edgeEnumValue This method can directly accept the YogaEdge.XXX.intValue(). */
  // This duplicates the other getRaw instead of calling each other to save on method calls.
  fun getRaw(edgeEnumValue: Int): Float {
    val edgeIndex = getIndex(edgeEnumValue)
    return if (edgeIndex == UNDEFINED_INDEX) {
      YogaConstants.UNDEFINED
    } else {
      requireValues()[edgeIndex.toInt()]
    }
  }

  private fun getIndex(edgeEnumValue: Int): Byte {
    return ((edgesToValuesIndex shr (edgeEnumValue * 4)) and INDEX_MASK.toLong()).toByte()
  }

  private val firstAvailableIndex: Byte
    get() {
      if (values == null) {
        values = floatArrayOf(YogaConstants.UNDEFINED, YogaConstants.UNDEFINED)
        return 0
      }

      val oldValues: FloatArray = requireValues()
      for (i in oldValues.indices) {
        if (YogaConstants.isUndefined(oldValues[i])) {
          return i.toByte()
        }
      }

      // We traversed the array without finding an empty spot. We need to increase the array.
      values =
          FloatArray(min((oldValues.size * 2), EDGES_LENGTH)) { index ->
            oldValues.getOrElse(index) { YogaConstants.UNDEFINED }
          }
      return oldValues.size.toByte()
    }

  override fun isEquivalentTo(other: Edges?): Boolean {
    if (this === other) {
      return true
    }
    return if (other == null) {
      false
    } else {
      edgesToValuesIndex == other.edgesToValuesIndex &&
          hasAliasesSet == other.hasAliasesSet &&
          values.contentEquals(other.values)
    }
  }

  companion object {
    val EDGES_LENGTH: Int = YogaEdge.entries.size
    // The index of a value is represented by 4 bits. 0xF ('1111') represents an undefined index.
    private const val UNDEFINED_INDEX: Byte = 0xF
    private const val INDEX_MASK: Byte = 0xF
    private const val DEFAULT_VALUE = 0f
    // The aliases (VERTICAL, HORIZONTAL and ALL) are three continuous 4 bits spaces
    // at position 7*4, 8*4 and 9*4.
    private const val ALIASES_MASK = 0xFFF
    private const val ALIASES_RIGHT_SHIFT = 6 * 4
    private val ALL_INTVALUE = YogaEdge.ALL.intValue()
    private val HORIZONTAL_INTVALUE = YogaEdge.HORIZONTAL.intValue()
    private val VERTICAL_INTVALUE = YogaEdge.VERTICAL.intValue()

    private fun floatsEqual(f1: Float, f2: Float): Boolean {
      return if (f1.isNaN() || f2.isNaN()) {
        f1.isNaN() && f2.isNaN()
      } else {
        abs(f2 - f1) < .00001f
      }
    }
  }
}
