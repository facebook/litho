/**
 * Copyright (c) 2014-present, Facebook, Inc.
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

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.fresco.common.GenericReferenceDraweeHierarchy;
import com.facebook.litho.reference.Reference;
import com.facebook.litho.utils.MeasureUtils;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;

import static com.facebook.litho.annotations.ResType.DRAWABLE;

@MountSpec
class FrescoComponentSpec {

  private static final ScalingUtils.ScaleType DEFAULT_ACTUAL_IMAGE_SCALE_TYPE =
      GenericDraweeHierarchyBuilder.DEFAULT_ACTUAL_IMAGE_SCALE_TYPE;
  private static final int DEFAULT_FADE_DURATION =
      GenericDraweeHierarchyBuilder.DEFAULT_FADE_DURATION;
  private static final ScalingUtils.ScaleType DEFAULT_SCALE_TYPE =
      GenericDraweeHierarchyBuilder.DEFAULT_SCALE_TYPE;

  @PropDefault protected static final float aspectRatio = 1f;
  @PropDefault protected static final ScalingUtils.ScaleType actualImageScaleType =
      DEFAULT_ACTUAL_IMAGE_SCALE_TYPE;
  @PropDefault protected static final int fadeDuration = DEFAULT_FADE_DURATION;
  @PropDefault protected static final ScalingUtils.ScaleType failureImageScaleType =
      DEFAULT_SCALE_TYPE;
  @PropDefault protected static final PointF placeholderImageFocusPoint = new PointF(0.5f, 0.5f);
  @PropDefault protected static final ScalingUtils.ScaleType placeholderImageScaleType =
      DEFAULT_SCALE_TYPE;
  @PropDefault protected static final ScalingUtils.ScaleType progressBarImageScaleType =
      DEFAULT_SCALE_TYPE;
  @PropDefault protected static final ScalingUtils.ScaleType retryImageScaleType =
      DEFAULT_SCALE_TYPE;

  @OnMeasure
  protected static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
