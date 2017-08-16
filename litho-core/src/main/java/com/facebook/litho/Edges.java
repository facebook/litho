/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaEdge;
import java.util.Arrays;

public class Edges {

  private static final int[] sFlagsMap = {
    1, /*LEFT*/
    2, /*TOP*/
    4, /*RIGHT*/
    8, /*BOTTOM*/
    16, /*START*/
    32, /*END*/
    64, /*HORIZONTAL*/
    128, /*VERTICAL*/
    256, /*ALL*/
  };

  private final float[] mEdges = new float[] {
      YogaConstants.UNDEFINED,
      YogaConstants.UNDEFINED,
      YogaConstants.UNDEFINED,
      YogaConstants.UNDEFINED,
      YogaConstants.UNDEFINED,
      YogaConstants.UNDEFINED,
      YogaConstants.UNDEFINED,
      YogaConstants.UNDEFINED,
      YogaConstants.UNDEFINED,
  };

  private int mValueFlags = 0;
  private final float mDefaultValue = 0;
  private boolean mHasAliasesSet;

  public boolean set(YogaEdge edge, float value) {
    if (!floatsEqual(mEdges[edge.intValue()], value)) {
      mEdges[edge.intValue()] = value;

      if (YogaConstants.isUndefined(value)) {
        mValueFlags &= ~sFlagsMap[edge.intValue()];
      } else {
        mValueFlags |= sFlagsMap[edge.intValue()];
      }

      mHasAliasesSet =
          (mValueFlags & sFlagsMap[YogaEdge.ALL.intValue()]) != 0 ||
          (mValueFlags & sFlagsMap[YogaEdge.VERTICAL.intValue()]) != 0 ||
          (mValueFlags & sFlagsMap[YogaEdge.HORIZONTAL.intValue()]) != 0;

      return true;
    }

    return false;
  }

  public float get(YogaEdge edge) {
    float defaultValue = (edge == YogaEdge.START || edge == YogaEdge.END
        ? YogaConstants.UNDEFINED
        : mDefaultValue);

    if (mValueFlags == 0) {
      return defaultValue;
    }

    if ((mValueFlags & sFlagsMap[edge.intValue()]) != 0) {
      return mEdges[edge.intValue()];
    }

    if (mHasAliasesSet) {
      YogaEdge secondType = edge == YogaEdge.TOP || edge == YogaEdge.BOTTOM
          ? YogaEdge.VERTICAL
          : YogaEdge.HORIZONTAL;
      if ((mValueFlags & sFlagsMap[secondType.intValue()]) != 0) {
        return mEdges[secondType.intValue()];
      } else if ((mValueFlags & sFlagsMap[YogaEdge.ALL.intValue()]) != 0) {
        return mEdges[YogaEdge.ALL.intValue()];
      }
    }

    return defaultValue;
  }

  public float getRaw(YogaEdge edge) {
    return mEdges[edge.intValue()];
  }

  public void reset() {
    Arrays.fill(mEdges, YogaConstants.UNDEFINED);
    mHasAliasesSet = false;
    mValueFlags = 0;
  }

  private static boolean floatsEqual(float f1, float f2) {
    if (Float.isNaN(f1) || Float.isNaN(f2)) {
      return Float.isNaN(f1) && Float.isNaN(f2);
    }
    return Math.abs(f2 - f1) < .00001f;
  }
}
