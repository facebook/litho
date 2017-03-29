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
   * Set the {@param outputSize} to respect both Specs and try to keep both width and height equal.
   * This will only not guarantee equal width and height if thes Specs use modes and sizes which
   * prevent it.
   */
  public static void measureWithEqualDimens(int widthSpec, int heightSpec, Size outputSize) {
    final int widthMode = SizeSpec.getMode(widthSpec);
    final int widthSize = SizeSpec.getSize(widthSpec);
    final int heightMode = SizeSpec.getMode(heightSpec);
    final int heightSize = SizeSpec.getSize(heightSpec);

    if (widthMode == UNSPECIFIED && heightMode == UNSPECIFIED) {
      outputSize.width = 0;
      outputSize.height = 0;

      if (ComponentsConfiguration.IS_INTERNAL_BUILD) {
        Log.d(
            "com.facebook.litho.utils.MeasureUtils",
            "Default to size {0, 0} because both width and height are UNSPECIFIED");
      }
      return;
    }

    if (widthMode == EXACTLY) {
      outputSize.width = widthSize;

      switch (heightMode) {
        case EXACTLY:
          outputSize.height = heightSize;
          return;
        case AT_MOST:
          outputSize.height = Math.min(widthSize, heightSize);
          return;
        case UNSPECIFIED:
          outputSize.height = widthSize;
          return;
      }
    } else if (widthMode == AT_MOST) {
      switch (heightMode) {
        case EXACTLY:
          outputSize.height = heightSize;
          outputSize.width = Math.min(widthSize, heightSize);
          return;
        case AT_MOST:
          // if both are AT_MOST, choose the smaller one to keep width and height equal
          final int chosenSize = Math.min(widthSize, heightSize);
          outputSize.width = chosenSize;
          outputSize.height = chosenSize;
          return;
        case UNSPECIFIED:
          outputSize.width = widthSize;
          outputSize.height = widthSize;
          return;
      }
    }

    // heightMode is either EXACTLY or AT_MOST, and widthMode is UNSPECIFIED
    outputSize.height = heightSize;
    outputSize.width = heightSize;
  }

  /**
   * Measure according to an aspect ratio an width and height constraints. This version
   * of measureWithAspectRatio will respect the intrinsic size of the component being measured.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   * @param intrinsicWidth A pixel value for the intrinsic width of the measured component
   * @param intrinsicHeight A pixel value for the intrinsic height of the measured component
   * @param aspectRatio The aspect ration size against
   * @param outputSize The output size of this measurement
   */
  public static void measureWithAspectRatio(
      int widthSpec,
      int heightSpec,
      int intrinsicWidth,
      int intrinsicHeight,
      float aspectRatio,
      Size outputSize) {

    if (SizeSpec.getMode(widthSpec) == AT_MOST &&
        SizeSpec.getSize(widthSpec) > intrinsicWidth) {
      widthSpec = SizeSpec.makeSizeSpec(intrinsicWidth, AT_MOST);
    }

    if (SizeSpec.getMode(heightSpec) == AT_MOST &&
        SizeSpec.getSize(heightSpec) > intrinsicHeight) {
      heightSpec = SizeSpec.makeSizeSpec(intrinsicHeight, AT_MOST);
    }

    measureWithAspectRatio(widthSpec, heightSpec, aspectRatio, outputSize);
  }

  /**
   * Measure according to an aspect ratio an width and height constraints.
   *
   * @param widthSpec A SizeSpec for the width
   * @param heightSpec A SizeSpec for the height
   * @param aspectRatio The aspect ration size against
   * @param outputSize The output size of this measurement
   */
  public static void measureWithAspectRatio(
      int widthSpec,
      int heightSpec,
      float aspectRatio,
      Size outputSize) {

    final int widthMode = SizeSpec.getMode(widthSpec);
    final int widthSize = SizeSpec.getSize(widthSpec);
    final int heightMode = SizeSpec.getMode(heightSpec);
    final int heightSize = SizeSpec.getSize(heightSpec);
    final int widthBasedHeight = (int) Math.ceil(widthSize / aspectRatio);
    final int heightBasedWidth = (int) Math.ceil(heightSize * aspectRatio);

    if (widthMode == UNSPECIFIED && heightMode == UNSPECIFIED) {
      outputSize.width = 0;
      outputSize.height = 0;

      if (ComponentsConfiguration.IS_INTERNAL_BUILD) {
        Log.d(
            "com.facebook.litho.utils.MeasureUtils",
            "Default to size {0, 0} because both width and height are UNSPECIFIED");
      }
      return;
    }

    // Both modes are AT_MOST, find the largest possible size which respects both constraints.
    if (widthMode == AT_MOST && heightMode == AT_MOST) {
      if (widthBasedHeight > heightSize) {
        outputSize.width = heightBasedWidth;
        outputSize.height = heightSize;
      } else {
        outputSize.width = widthSize;
        outputSize.height = widthBasedHeight;
      }
    }
    // Width is set to exact measurement and the height is either unspecified or is allowed to be
    // large enough to accommodate the given aspect ratio.
    else if (widthMode == EXACTLY) {
      outputSize.width = widthSize;

