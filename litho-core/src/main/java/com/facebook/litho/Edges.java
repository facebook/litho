/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

  // The index of a value is represented by 4 bits. 0xF ('1111') represents an undefined index.
  private static final byte UNDEFINED_INDEX = 0xF;
  private static final byte INDEX_MASK = 0xF;
  private static final float DEFAULT_VALUE = 0;

  // This long maps the indexes of the YogaEdges within the mValue array.
  // Each group of 4 bits represent an index and the position of these 4 bits is related to the
  // YogaEdge.intValue(). For example, YogaEdge.TOP is position 1. Therefore bits representing TOP
  // are 4..7.
  // We initialize the long to be all "1"s representing all indexes to be undefined.
  private long mEdgesToValuesIndex = ~0;
  private float[] mValues;
  private boolean mHasAliasesSet;

  public boolean set(YogaEdge yogaEdge, float value) {
    if (!floatsEqual(getRaw(yogaEdge), value)) {
      final byte edgeIndex = getIndex(yogaEdge);

      // If we need to "unset" a previously set edge.
      if (YogaConstants.isUndefined(value)) {
        unsetIndex(yogaEdge);
        mValues[edgeIndex] = YogaConstants.UNDEFINED;

        // If we need to insert a new edge value.
      } else if (edgeIndex == UNDEFINED_INDEX) {
        final byte newIndex = getFirstAvailableIndex();
        setIndex(yogaEdge, newIndex);
        mValues[newIndex] = value;

        // Otherwise we need to overwrite an existing value.
      } else {
        mValues[edgeIndex] = value;
      }

      mHasAliasesSet =
          getIndex(YogaEdge.ALL) != UNDEFINED_INDEX
              || getIndex(YogaEdge.VERTICAL) != UNDEFINED_INDEX
              || getIndex(YogaEdge.HORIZONTAL) != UNDEFINED_INDEX;

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

    final byte edgeIndex = getIndex(edge);
    if (edgeIndex != UNDEFINED_INDEX) {
      return mValues[edgeIndex];
    }

    if (mHasAliasesSet) {
      final YogaEdge secondType =
          edge == YogaEdge.TOP || edge == YogaEdge.BOTTOM ? YogaEdge.VERTICAL : YogaEdge.HORIZONTAL;
      final byte secondTypeEdgeIndex = getIndex(secondType);

      if (secondTypeEdgeIndex != UNDEFINED_INDEX) {
        return mValues[secondTypeEdgeIndex];

      } else if (getIndex(YogaEdge.ALL) != UNDEFINED_INDEX) {
        return mValues[getIndex(YogaEdge.ALL)];
      }
    }

    return defaultValue;
  }

  public float getRaw(YogaEdge edge) {
    final byte edgeIndex = getIndex(edge);
    if (edgeIndex == UNDEFINED_INDEX) {
      return YogaConstants.UNDEFINED;
    }

    return mValues[edgeIndex];
  }

  private byte getIndex(YogaEdge edge) {
    return (byte) ((mEdgesToValuesIndex >> (edge.intValue() * 4)) & INDEX_MASK);
  }

  // Set the 4 bits representing the edge as undefined ('1111').
  private void unsetIndex(YogaEdge edge) {
    mEdgesToValuesIndex |= ((long) UNDEFINED_INDEX << (edge.intValue() * 4));
  }

  private void setIndex(YogaEdge edge, byte index) {
    if (index >= YogaEdge.values().length || index > INDEX_MASK) {
      throw new IllegalStateException(
          "The index of the array cannot be bigger than the amount of " + "Yoga Edges.");
    }

    // Clear the bits at the index position.
    mEdgesToValuesIndex &= ~((long) (0xF) << (edge.intValue() * 4));
    // Then set the actual 4 bits value of the index. Leaving this as two steps for clarity.
    mEdgesToValuesIndex |= ((long) index << (edge.intValue() * 4));
  }

  private byte getFirstAvailableIndex() {
    if (mValues == null) {
      mValues = new float[2];
      Arrays.fill(mValues, YogaConstants.UNDEFINED);
      return 0;
    }

    for (int i = 0; i < mValues.length; i++) {
      if (YogaConstants.isUndefined(mValues[i])) {
        return (byte) i;
      }
    }

    // We traversed the array without finding an empty spot. We need to increase the array.
    float[] oldValues = mValues;
    mValues = new float[Math.min(oldValues.length * 2, YogaEdge.values().length)];
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
