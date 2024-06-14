// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.layoutoutput;

import com.facebook.proguard.annotations.DoNotStrip;

@DoNotStrip
public class LayoutOutput<MeasureResult> {
  // Must be kept in sync with LayoutOutputKeys in JNI code
  private enum Keys {
    WIDTH,
    HEIGHT,
    BASELINE
  }

  // Must be kept in sync with LayoutOutputChildKeys in JNI code
  private enum ChildKeys {
    LEFT,
    TOP,
    WIDTH,
    HEIGHT
  }

  /*
   The array has the following format:

   [0]: width
   [1]: height
   [2]: baseline

   Then, for each child:
   [i]: left
   [i + 1]: top
   [i + 2]: width
   [i + 3]: height
  */
  @SuppressWarnings("MismatchedReadAndWriteOfArray") // Written to from native code
  @DoNotStrip
  private float[] arr;

  private final Object[] measureResults;

  public LayoutOutput(int childrenSize) {
    measureResults = new Object[childrenSize];
    // See format description above
    final int storageSize = Keys.values().length + childrenSize * ChildKeys.values().length;
    arr = new float[storageSize];
  }

  public float getWidth() {
    return arr[Keys.WIDTH.ordinal()];
  }

  public float getHeight() {
    return arr[Keys.HEIGHT.ordinal()];
  }

  public float getBaseline() {
    return arr[Keys.BASELINE.ordinal()];
  }

  public int getChildrenCount() {
    return measureResults.length;
  }

  public float getLeftForChildAt(int idx) {
    return arr[Keys.values().length + idx * ChildKeys.values().length + ChildKeys.LEFT.ordinal()];
  }

  public float getTopForChildAt(int idx) {
    return arr[Keys.values().length + idx * ChildKeys.values().length + ChildKeys.TOP.ordinal()];
  }

  public float getWidthForChildAt(int idx) {
    return arr[Keys.values().length + idx * ChildKeys.values().length + ChildKeys.WIDTH.ordinal()];
  }

  public float getHeightForChildAt(int idx) {
    return arr[Keys.values().length + idx * ChildKeys.values().length + ChildKeys.HEIGHT.ordinal()];
  }

  public void setMeasureResultForChildAt(int idx, MeasureOutput<MeasureResult> measureOutput) {
    measureResults[idx] = measureOutput.getMeasureResult();
  }

  public MeasureResult getMeasureResultForChildAt(int idx) {
    //noinspection unchecked
    return (MeasureResult) measureResults[idx];
  }
}
