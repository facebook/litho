// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore.utils;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;

import android.view.View;

/**
 * An utility class to verify that an old measured size is still compatible to be used with a new
 * measureSpec.
 */
public class MeasureSpecUtils {

  private static final float DELTA = 0.5f;

  public static int unspecified() {
    return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
  }

  public static int atMost(int px) {
    return View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.AT_MOST);
  }

  public static int exactly(int px) {
    return View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY);
  }

  public static boolean areMeasureSpecsEquivalent(int specA, int specB) {
    return specA == specB
        || (View.MeasureSpec.getMode(specA) == UNSPECIFIED
            && View.MeasureSpec.getMode(specB) == UNSPECIFIED);
  }

  public static boolean isMeasureSpecCompatible(
      int oldSizeSpec, int sizeSpec, int oldMeasuredSize) {
    final int newSpecMode = View.MeasureSpec.getMode(sizeSpec);
    final int newSpecSize = View.MeasureSpec.getSize(sizeSpec);
    final int oldSpecMode = View.MeasureSpec.getMode(oldSizeSpec);
    final int oldSpecSize = View.MeasureSpec.getSize(oldSizeSpec);

    return oldSizeSpec == sizeSpec
        || (oldSpecMode == UNSPECIFIED && newSpecMode == UNSPECIFIED)
        || newSizeIsExactAndMatchesOldMeasuredSize(newSpecMode, newSpecSize, oldMeasuredSize)
        || oldSizeIsUnspecifiedAndStillFits(oldSpecMode, newSpecMode, newSpecSize, oldMeasuredSize)
        || newMeasureSizeIsStricterAndStillValid(
            oldSpecMode, newSpecMode, oldSpecSize, newSpecSize, oldMeasuredSize);
  }

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
}
