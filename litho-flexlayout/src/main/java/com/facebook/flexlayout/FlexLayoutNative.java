// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.flexlayout;

import com.facebook.flexlayout.layoutoutput.LayoutOutput;
import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.soloader.SoLoader;

@DoNotStrip
public class FlexLayoutNative {
  static {
    SoLoader.loadLibrary("flexlayout");
  }

  public static native void jni_calculateLayout(
      float[] flexBoxStyleArray,
      float[][] childrenFlexItemStyleArray,
      float minWidth,
      float maxWidth,
      float minHeight,
      float maxHeight,
      float ownerWidth,
      float ownerHeight,
      LayoutOutput layoutOutput,
      FlexLayoutNativeMeasureCallback measureCallback);
}
