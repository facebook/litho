// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout;

import com.facebook.flexlayout.layoutoutput.LayoutOutput;
import com.facebook.flexlayout.layoutoutput.MeasureOutput;
import com.facebook.flexlayout.styles.FlexItemCallback;

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
        new FlexLayoutNativeMeasureCallback<MeasureResult>() {

          @Override
          MeasureOutput<MeasureResult> measure(
              int idx,
              float minWidth,
              float maxWidth,
              float minHeight,
              float maxHeight,
              float ownerWidth,
              float ownerHeight) {
            MeasureOutput<MeasureResult> measureOutput =
                callbackArray[idx].measure(
                    minWidth, maxWidth, minHeight, maxHeight, ownerWidth, ownerHeight);

            // This measure callback implementation fills in the layout output array of
            // measureResults directly within the measure calculation method. Unlike other
            // platforms, in Java the costs of JNI crossing is high, and this lets us avoid book
            // keeping a lot of local_ref
            // objects.
            layoutOutput.setMeasureResultForChildAt(idx, measureOutput);
            return measureOutput;
          }

          @Override
          float baseline(int idx, float width, float height) {
            return callbackArray[idx].baseline(width, height);
          }
        });
    return layoutOutput;
  }
}
