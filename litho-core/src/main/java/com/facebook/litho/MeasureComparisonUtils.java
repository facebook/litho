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
      int newSizeSpecMode, int newSizeSpecSize, float oldMeasuredSize) {
    return (newSizeSpecMode == EXACTLY) && (Math.abs(newSizeSpecSize - oldMeasuredSize) < DELTA);
  }

  private static boolean oldSizeIsUnspecifiedAndStillFits(
      int oldSizeSpecMode, int newSizeSpecMode, int newSizeSpecSize, float oldMeasuredSize) {
    return newSizeSpecMode == AT_MOST
        && oldSizeSpecMode == UNSPECIFIED
        && newSizeSpecSize >= oldMeasuredSize;
  }

  private static boolean newMeasureSizeIsStricterAndStillValid(
      int oldSizeSpecMode,
      int newSizeSpecMode,
      int oldSizeSpecSize,
      int newSizeSpecSize,
      float oldMeasuredSize) {
    return oldSizeSpecMode == AT_MOST
        && newSizeSpecMode == AT_MOST
        && oldSizeSpecSize > newSizeSpecSize
        && oldMeasuredSize <= newSizeSpecSize;
  }

  public static boolean areMeasureSpecsEquivalent(int specA, int specB) {
    return specA == specB
        || (SizeSpec.getMode(specA) == UNSPECIFIED && SizeSpec.getMode(specB) == UNSPECIFIED);
  }

  public static boolean isMeasureSpecCompatible(
      int oldSizeSpec, int sizeSpec, int oldMeasuredSize) {
    final int newSpecMode = SizeSpec.getMode(sizeSpec);
    final int newSpecSize = SizeSpec.getSize(sizeSpec);
    final int oldSpecMode = SizeSpec.getMode(oldSizeSpec);
    final int oldSpecSize = SizeSpec.getSize(oldSizeSpec);

    return oldSizeSpec == sizeSpec
        || (oldSpecMode == UNSPECIFIED && newSpecMode == UNSPECIFIED)
        || newSizeIsExactAndMatchesOldMeasuredSize(newSpecMode, newSpecSize, oldMeasuredSize)
        || oldSizeIsUnspecifiedAndStillFits(oldSpecMode, newSpecMode, newSpecSize, oldMeasuredSize)
        || newMeasureSizeIsStricterAndStillValid(
            oldSpecMode, newSpecMode, oldSpecSize, newSpecSize, oldMeasuredSize);
  }
}
