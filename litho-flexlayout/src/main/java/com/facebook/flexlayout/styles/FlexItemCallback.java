// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

import com.facebook.flexlayout.layoutoutput.MeasureOutput;

public class FlexItemCallback<MeasureResult> {

  private final FlexLayoutMeasureFunction<MeasureResult> mMeasureFunction;
  private FlexLayoutBaselineFunction mBaselineFunction;

  public FlexItemCallback(FlexLayoutMeasureFunction<MeasureResult> measureFunction) {
    mMeasureFunction = measureFunction;
  }

  public MeasureOutput<MeasureResult> measure(
      final float minWidth,
      final float maxWidth,
      final float minHeight,
      final float maxHeight,
      final float ownerWidth,
      final float ownerHeight) {
    if (mMeasureFunction == null) {
      throw new RuntimeException("Measure function isn't defined!");
    }

    return mMeasureFunction.measure(
        minWidth, maxWidth, minHeight, maxHeight, ownerWidth, ownerHeight);
  }

  public void setBaselineFunction(final FlexLayoutBaselineFunction baselineFunction) {
    mBaselineFunction = baselineFunction;
  }

  public float baseline(final float width, final float height) {
    if (mBaselineFunction == null) {
      throw new RuntimeException("Baseline function isn't defined!");
    }
    return mBaselineFunction.baseline(width, height);
  }
}
