// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout.styles;

import com.facebook.flexlayout.layoutoutput.MeasureOutput;

public interface FlexLayoutMeasureFunction<MeasureResult> {
  MeasureOutput<MeasureResult> measure(
      float minWidth,
      float maxWidth,
      float minHeight,
      float maxHeight,
      float ownerWidth,
      float ownerHeight);
}
