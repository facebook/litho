/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.fresco;

import static com.facebook.litho.annotations.ResType.DRAWABLE;

import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.litho.annotations.Prop;

public class FrescoImageHierarchyTools {

  public static void setupHierarchy(
      @Prop(optional = true) ScalingUtils.ScaleType actualImageScaleType,
      @Prop(optional = true) PointF actualImageFocusPoint,
      @Prop(optional = true) int fadeDuration,
      @Prop(optional = true, resType = DRAWABLE) Drawable failureImage,
      @Prop(optional = true) ScalingUtils.ScaleType failureImageScaleType,
      @Prop(optional = true, resType = DRAWABLE) Drawable placeholderImage,
      @Prop(optional = true) PointF placeholderImageFocusPoint,
      @Prop(optional = true) ScalingUtils.ScaleType placeholderImageScaleType,
      @Prop(optional = true, resType = DRAWABLE) Drawable progressBarImage,
      @Prop(optional = true) ScalingUtils.ScaleType progressBarImageScaleType,
      @Prop(optional = true, resType = DRAWABLE) Drawable retryImage,
      @Prop(optional = true) ScalingUtils.ScaleType retryImageScaleType,
      @Prop(optional = true) RoundingParams roundingParams,
      @Prop(optional = true) ColorFilter colorFilter,
      GenericDraweeHierarchy draweeHierarchy) {

    if (placeholderImage == null) {
      draweeHierarchy.setPlaceholderImage(null);
    } else {
      draweeHierarchy.setPlaceholderImage(placeholderImage, placeholderImageScaleType);
    }

    if (placeholderImageScaleType == ScalingUtils.ScaleType.FOCUS_CROP) {
      draweeHierarchy.setPlaceholderImageFocusPoint(placeholderImageFocusPoint);
    }

    draweeHierarchy.setActualImageScaleType(actualImageScaleType);
    if (actualImageFocusPoint != null
        && actualImageScaleType == ScalingUtils.ScaleType.FOCUS_CROP) {
      draweeHierarchy.setActualImageFocusPoint(actualImageFocusPoint);
    }
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
