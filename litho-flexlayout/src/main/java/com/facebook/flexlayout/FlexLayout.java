// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout;

import com.facebook.flexlayout.layoutoutput.LayoutOutput;
import com.facebook.flexlayout.styles.FlexItemCallback;
import com.facebook.proguard.annotations.DoNotStrip;

@DoNotStrip
public class FlexLayout {
  public static <MeasureResult> LayoutOutput<MeasureResult> calculateLayout(
      float[] flexBoxStyle,
      float[][] childrenStyles,
      float minWidth,
      float maxWidth,
      float minHeight,
      float maxHeight,
      float ownerWidth,
      float ownerHeight,
      FlexItemCallback<MeasureResult>[] callbackArray) {
    final LayoutOutput<MeasureResult> layoutOutput = new LayoutOutput<>(childrenStyles.length);
    FlexLayoutNative.jni_calculateLayout(
        flexBoxStyle,
        childrenStyles,
        minWidth,
        maxWidth,
        minHeight,
        maxHeight,
        ownerWidth,
        ownerHeight,
        layoutOutput,
        callbackArray);
    return layoutOutput;
  }
}
