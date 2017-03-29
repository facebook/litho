/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.utils;

import android.view.View.MeasureSpec;
import android.util.Log;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;

import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.getMode;
import static com.facebook.litho.SizeSpec.getSize;

public final class MeasureUtils {

  public static int getViewMeasureSpec(int sizeSpec) {
    switch (getMode(sizeSpec)) {
      case SizeSpec.EXACTLY:
        return MeasureSpec.makeMeasureSpec(getSize(sizeSpec), MeasureSpec.EXACTLY);
      case SizeSpec.AT_MOST:
        return MeasureSpec.makeMeasureSpec(getSize(sizeSpec), MeasureSpec.AT_MOST);
      case SizeSpec.UNSPECIFIED:
        return MeasureSpec.makeMeasureSpec(getSize(sizeSpec), MeasureSpec.UNSPECIFIED);
      default:
        throw new IllegalStateException("Unexpected size spec mode");
    }
  }

  /**
   * Set the {@param outputSize} to respect both Specs and the desired width and height.
   * The desired size is usually the necessary pixels to render the inner content.
   */
  public static void measureWithDesiredPx(
      int widthSpec,
      int heightSpec,
      int desiredWidthPx,
      int desiredHeightPx,
      Size outputSize) {
    outputSize.width = getResultSizePxWithSpecAndDesiredPx(widthSpec, desiredWidthPx);
    outputSize.height = getResultSizePxWithSpecAndDesiredPx(heightSpec, desiredHeightPx);
  }

  private static int getResultSizePxWithSpecAndDesiredPx(int spec, int desiredSize) {
    final int mode = SizeSpec.getMode(spec);
    switch (mode) {
      case SizeSpec.UNSPECIFIED:
        return desiredSize;
      case SizeSpec.AT_MOST:
        return Math.min(SizeSpec.getSize(spec), desiredSize);
      case SizeSpec.EXACTLY:
        return SizeSpec.getSize(spec);
      default:
        throw new IllegalStateException("Unexpected size spec mode");
    }
  }

  /**
