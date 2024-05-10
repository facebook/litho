// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout;

import com.facebook.flexlayout.layoutoutput.MeasureOutput;
import com.facebook.proguard.annotations.DoNotStrip;

@DoNotStrip
abstract class FlexLayoutNativeMeasureCallback<MeasureResult> {
  abstract MeasureOutput<MeasureResult> measure(
      final int idx,
      final float minWidth,
      final float maxWidth,
      final float minHeight,
      final float maxHeight,
      final float ownerWidth,
      final float ownerHeight);

  abstract float baseline(final int idx, final float width, final float height);

  @DoNotStrip
  public final MeasureOutput<MeasureResult> measureNative(
      final int idx,
      final float minWidth,
      final float maxWidth,
      final float minHeight,
      final float maxHeight,
      final float ownerWidth,
      final float ownerHeight) {
    return measure(idx, minWidth, maxWidth, minHeight, maxHeight, ownerWidth, ownerHeight);
  }

  @DoNotStrip
  public final float baselineNative(final int idx, final float width, final float height) {
    return baseline(idx, width, height);
  }
}
