/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.fresco;

import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.litho.Size;
import com.facebook.litho.utils.MeasureUtils;

public class FrescoUtils {

  protected static final float aspectRatio = 1f;
  protected static final ScalingUtils.ScaleType actualImageScaleType =
      FrescoUtils.DEFAULT_ACTUAL_IMAGE_SCALE_TYPE;
  protected static final int fadeDuration = FrescoUtils.DEFAULT_FADE_DURATION;
  protected static final ScalingUtils.ScaleType failureImageScaleType =
      FrescoUtils.DEFAULT_SCALE_TYPE;
  protected static final PointF placeholderImageFocusPoint = new PointF(0.5f, 0.5f);
  protected static final ScalingUtils.ScaleType placeholderImageScaleType =
      FrescoUtils.DEFAULT_SCALE_TYPE;
  protected static final ScalingUtils.ScaleType progressBarImageScaleType =
      FrescoUtils.DEFAULT_SCALE_TYPE;
  protected static final ScalingUtils.ScaleType retryImageScaleType =
      FrescoUtils.DEFAULT_SCALE_TYPE;

  private static final ScalingUtils.ScaleType DEFAULT_ACTUAL_IMAGE_SCALE_TYPE =
      GenericDraweeHierarchyBuilder.DEFAULT_ACTUAL_IMAGE_SCALE_TYPE;
  private static final int DEFAULT_FADE_DURATION =
      GenericDraweeHierarchyBuilder.DEFAULT_FADE_DURATION;
  private static final ScalingUtils.ScaleType DEFAULT_SCALE_TYPE =
      GenericDraweeHierarchyBuilder.DEFAULT_SCALE_TYPE;

  protected static void onMeasure(
      int widthSpec,
      int heightSpec,
      Size size,
      float aspectRatio) {
    MeasureUtils.measureWithAspectRatio(widthSpec, heightSpec, aspectRatio, size);
  }

  protected static void onMount(
      GenericDraweeHierarchy draweeHierarchy,
      ScalingUtils.ScaleType actualImageScaleType,
      int fadeDuration,
      Drawable failureImage,
      ScalingUtils.ScaleType failureImageScaleType,
      Drawable placeholderImage,
      PointF placeholderImageFocusPoint,
      ScalingUtils.ScaleType placeholderImageScaleType,
      Drawable progressBarImage,
      ScalingUtils.ScaleType progressBarImageScaleType,
      Drawable retryImage,
      ScalingUtils.ScaleType retryImageScaleType,
      RoundingParams roundingParams,
      ColorFilter colorFilter) {

    if (placeholderImage == null) {
      draweeHierarchy.setPlaceholderImage(null);
    } else {
      draweeHierarchy.setPlaceholderImage(placeholderImage, placeholderImageScaleType);
    }

    if (placeholderImageScaleType == ScalingUtils.ScaleType.FOCUS_CROP) {
      draweeHierarchy.setPlaceholderImageFocusPoint(placeholderImageFocusPoint);
    }

    draweeHierarchy.setActualImageScaleType(actualImageScaleType);
    draweeHierarchy.setFadeDuration(fadeDuration);

    if (failureImage == null) {
      draweeHierarchy.setFailureImage(null);
    } else {
      draweeHierarchy.setFailureImage(failureImage, failureImageScaleType);
    }

    if (progressBarImage == null) {
      draweeHierarchy.setProgressBarImage(null);
    } else {
      draweeHierarchy.setProgressBarImage(progressBarImage, progressBarImageScaleType);
    }

    if (retryImage == null) {
      draweeHierarchy.setRetryImage(null);
    } else {
      draweeHierarchy.setRetryImage(retryImage, retryImageScaleType);
    }

    draweeHierarchy.setRoundingParams(roundingParams);
    draweeHierarchy.setActualImageColorFilter(colorFilter);
  }
}
