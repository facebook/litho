/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho;

import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaEdge;
import java.util.Arrays;

public class Edges {

  public static final int EDGES_LENGTH = YogaEdge.values().length;

  // The index of a value is represented by 4 bits. 0xF ('1111') represents an undefined index.
  private static final byte UNDEFINED_INDEX = 0xF;
  private static final byte INDEX_MASK = 0xF;
  private static final float DEFAULT_VALUE = 0;

  // The aliases (VERTICAL, HORIZONTAL and ALL) are three continuous 4 bits spaces
  // at position 7*4, 8*4 and 9*4.
  private static final int ALIASES_MASK = 0xFFF;
  private static final int ALIASES_RIGHT_SHIFT = 6 * 4;

  private static final int ALL_INTVALUE = YogaEdge.ALL.intValue();
  private static final int HORIZONTAL_INTVALUE = YogaEdge.HORIZONTAL.intValue();
  private static final int VERTICAL_INTVALUE = YogaEdge.VERTICAL.intValue();

  // This long maps the indexes of the YogaEdges within the mValue array.
  // Each group of 4 bits represent an index and the position of these 4 bits is related to the
  // YogaEdge.intValue(). For example, YogaEdge.TOP is position 1. Therefore bits representing TOP
  // are 4..7.
  // We initialize the long to be all "1"s representing all indexes to be undefined.
  private long mEdgesToValuesIndex = ~0L;
  private float[] mValues;
  private boolean mHasAliasesSet;

  public boolean set(YogaEdge yogaEdge, float value) {
    final int edgeIntValue = yogaEdge.intValue();
    if (!floatsEqual(getRaw(edgeIntValue), value)) {
      final byte edgeIndex = getIndex(edgeIntValue);

      // If we need to "unset" a previously set edge.
      if (YogaConstants.isUndefined(value)) {
        // UNSET index:
        // Set the 4 bits representing the edge as undefined ('1111').
        mEdgesToValuesIndex |= ((long) UNDEFINED_INDEX << (edgeIntValue * 4));
        mValues[edgeIndex] = YogaConstants.UNDEFINED;

        // If we need to insert a new edge value.
      } else if (edgeIndex == UNDEFINED_INDEX) {
        final byte newIndex = getFirstAvailableIndex();
        if (newIndex >= EDGES_LENGTH) {
          throw new IllegalStateException(
              "The newIndex for the array cannot be bigger than the amount of Yoga Edges.");
        }
        // SETS index:
        // Clear the bits at the index position.
        mEdgesToValuesIndex &= ~((long) (0xF) << (edgeIntValue * 4));
        // Then set the actual 4 bits value of the index. Leaving this as two steps for clarity.
        mEdgesToValuesIndex |= ((long) newIndex << (edgeIntValue * 4));
        mValues[newIndex] = value;

        // Otherwise we need to overwrite an existing value.
      } else {
        mValues[edgeIndex] = value;
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
      mHasAliasesSet = (~((int) (mEdgesToValuesIndex >> ALIASES_RIGHT_SHIFT)) & ALIASES_MASK) != 0;

      return true;
    }

    return false;
  }

  public float get(YogaEdge edge) {
    float defaultValue =
        (edge == YogaEdge.START || edge == YogaEdge.END ? YogaConstants.UNDEFINED : DEFAULT_VALUE);

    // Nothing is set.
    if (mEdgesToValuesIndex == ~0) {
      return defaultValue;
    }

    final byte edgeIndex = getIndex(edge.intValue());
    if (edgeIndex != UNDEFINED_INDEX) {
      return mValues[edgeIndex];
    }

    if (mHasAliasesSet) {
      final int secondTypeEdgeValue =
          edge == YogaEdge.TOP || edge == YogaEdge.BOTTOM ? VERTICAL_INTVALUE : HORIZONTAL_INTVALUE;
      final byte secondTypeEdgeIndex = getIndex(secondTypeEdgeValue);

      if (secondTypeEdgeIndex != UNDEFINED_INDEX) {
        return mValues[secondTypeEdgeIndex];

      } else if (getIndex(ALL_INTVALUE) != UNDEFINED_INDEX) {
        return mValues[getIndex(ALL_INTVALUE)];
      }
    }

    return defaultValue;
  }

  public float getRaw(YogaEdge edge) {
    final byte edgeIndex = getIndex(edge.intValue());
    if (edgeIndex == UNDEFINED_INDEX) {
      return YogaConstants.UNDEFINED;
    }

    return mValues[edgeIndex];
  }

  /** @param edgeEnumValue This method can directly accept the YogaEdge.XXX.intValue(). */
  // This duplicates the other getRaw instead of calling each other to save on method calls.
  public float getRaw(int edgeEnumValue) {
    final byte edgeIndex = getIndex(edgeEnumValue);
    if (edgeIndex == UNDEFINED_INDEX) {
      return YogaConstants.UNDEFINED;
    }

    return mValues[edgeIndex];
  }

  private byte getIndex(int edgeEnumValue) {
    return (byte) ((mEdgesToValuesIndex >> (edgeEnumValue * 4)) & INDEX_MASK);
  }

  private byte getFirstAvailableIndex() {
    if (mValues == null) {
      mValues = new float[] {YogaConstants.UNDEFINED, YogaConstants.UNDEFINED};
      return 0;
    }

    for (int i = 0; i < mValues.length; i++) {
      if (YogaConstants.isUndefined(mValues[i])) {
        return (byte) i;
      }
    }

    // We traversed the array without finding an empty spot. We need to increase the array.
    float[] oldValues = mValues;
    mValues = new float[Math.min(oldValues.length * 2, EDGES_LENGTH)];
    System.arraycopy(oldValues, 0, mValues, 0, oldValues.length);
    Arrays.fill(mValues, oldValues.length, mValues.length, YogaConstants.UNDEFINED);

    return (byte) oldValues.length;
  }

  private static boolean floatsEqual(float f1, float f2) {
    if (Float.isNaN(f1) || Float.isNaN(f2)) {
      return Float.isNaN(f1) && Float.isNaN(f2);
    }
    return Math.abs(f2 - f1) < .00001f;
  }
}
