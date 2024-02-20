// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

import com.facebook.flexlayout.layoutoutput.MeasureOutput;
import com.facebook.proguard.annotations.DoNotStrip;

@DoNotStrip
public class FlexItemCallback<MeasureResult> {

  private final FlexLayoutMeasureFunction<MeasureResult> mMeasureFunction;
  private FlexLayoutBaselineFunction mBaselineFunction;

  public FlexItemCallback(FlexLayoutMeasureFunction<MeasureResult> measureFunction) {
    mMeasureFunction = measureFunction;
  }

  // Implementation Note: Why this method needs to stay final
  //
  // We cache the jmethodid for this method in Yoga code. This means that even if a subclass
  // were to override measure, we'd still call this implementation from layout code since the
  // overriding method will have a different jmethodid. This is final to prevent that mistake.
  @DoNotStrip
  public final MeasureOutput<MeasureResult> measure(
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

  @DoNotStrip
  public final float baseline(final float width, final float height) {
    if (mBaselineFunction == null) {
      throw new RuntimeException("Baseline function isn't defined!");
    }
    return mBaselineFunction.baseline(width, height);
  }
}
