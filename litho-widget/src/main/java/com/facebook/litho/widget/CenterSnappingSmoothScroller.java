/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.widget;

import android.content.Context;
import android.util.DisplayMetrics;
import androidx.recyclerview.widget.LinearSmoothScroller;

/** LinearSmoothScroller subclass that snaps the target position to center. */
public class CenterSnappingSmoothScroller extends LinearSmoothScroller {
  private static final float MILLISECONDS_PER_INCH = 100f;
  private final int mOffset;

  public CenterSnappingSmoothScroller(Context context, int offset) {
    super(context);
    mOffset = offset;
  }

  @Override
  public int calculateDtToFit(
      int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
    final int result =
        (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
    return result + mOffset;
  }

  @Override
  protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
    return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
  }
}
