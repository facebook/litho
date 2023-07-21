// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.rendercore.text;

import android.content.Context;
import android.graphics.Typeface;

public interface TypefaceCreator {
  Typeface createTypefaceFromFontFamily(Context context, String fontFamily, int style);
}
