// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.layoutoutput;

import com.facebook.proguard.annotations.DoNotStrip;

@DoNotStrip
public class MeasureOutput<MeasureResult> {
  private enum Keys {
    WIDTH,
    HEIGHT,
    BASELINE,
  }

  @SuppressWarnings("MismatchedReadAndWriteOfArray") // Read from in native code
  @DoNotStrip
  private float[] arr = new float[Keys.values().length];

  private final MeasureResult measureResult;

  public MeasureOutput(float width, float height, float baseline, MeasureResult measureResult) {
    arr[Keys.WIDTH.ordinal()] = width;
    arr[Keys.HEIGHT.ordinal()] = height;
    arr[Keys.BASELINE.ordinal()] = baseline;
    this.measureResult = measureResult;
  }

  MeasureResult getMeasureResult() {
    return measureResult;
  }
}
