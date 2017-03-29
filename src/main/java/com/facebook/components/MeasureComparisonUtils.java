/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

/**
 * An utility class to verify that an old measured size is still compatible to be used with a new
 * measureSpec.
 */
public class MeasureComparisonUtils {
  private static final float DELTA = 0.5f;

  private static boolean newSizeIsExactAndMatchesOldMeasuredSize(
      int newSizeSpecMode,
      int newSizeSpecSize,
      float oldMeasuredSize) {
    return (newSizeSpecMode == EXACTLY) &&
        (Math.abs(newSizeSpecSize - oldMeasuredSize) < DELTA);
  }

  private static boolean oldSizeIsUnspecifiedAndStillFits(
      int oldSizeSpecMode,
      int newSizeSpecMode,
      int newSizeSpecSize,
      float oldMeasuredSize) {
    return newSizeSpecMode == AT_MOST &&
        oldSizeSpecMode == UNSPECIFIED &&
        newSizeSpecSize >= oldMeasuredSize;
  }

  private static boolean newMeasureSizeIsStricterAndStillValid(
      int oldSizeSpecMode,
      int newSizeSpecMode,
      int oldSizeSpecSize,
      int newSizeSpecSize,
      float oldMeasuredSize) {
    return oldSizeSpecMode == AT_MOST &&
        newSizeSpecMode == AT_MOST &&
        oldSizeSpecSize > newSizeSpecSize &&
        oldMeasuredSize <= newSizeSpecSize;
  }

  public static boolean isMeasureSpecCompatible(
      int oldSizeSpec,
      int sizeSpec,
      int oldMeasuredSize) {
    final int newSpecMode = SizeSpec.getMode(sizeSpec);
    final int newSpecSize = SizeSpec.getSize(sizeSpec);
    final int oldSpecMode = SizeSpec.getMode(oldSizeSpec);
    final int oldSpecSize = SizeSpec.getSize(oldSizeSpec);

    return oldSizeSpec == sizeSpec ||
        newSizeIsExactAndMatchesOldMeasuredSize(
            newSpecMode,
            newSpecSize,
            oldMeasuredSize) ||
        oldSizeIsUnspecifiedAndStillFits(
            oldSpecMode,
            newSpecMode,
            newSpecSize,
            oldMeasuredSize) ||
        newMeasureSizeIsStricterAndStillValid(
            oldSpecMode,
            newSpecMode,
            oldSpecSize,
            newSpecSize,
            oldMeasuredSize);
  }
}
