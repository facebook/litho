/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.fresco;

import static com.facebook.litho.annotations.ResType.DRAWABLE;

import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.GenericDraweeView;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;

@MountSpec
class FrescoImageViewSpec {

  @PropDefault protected static final float aspectRatio = FrescoUtils.aspectRatio;

  @PropDefault
  protected static final ScalingUtils.ScaleType actualImageScaleType =
      FrescoUtils.actualImageScaleType;

  @PropDefault protected static final int fadeDuration = FrescoUtils.fadeDuration;

  @PropDefault
  protected static final ScalingUtils.ScaleType failureImageScaleType =
      FrescoUtils.failureImageScaleType;

  @PropDefault
  protected static final PointF placeholderImageFocusPoint = FrescoUtils.placeholderImageFocusPoint;

  @PropDefault
  protected static final ScalingUtils.ScaleType placeholderImageScaleType =
      FrescoUtils.placeholderImageScaleType;

  @PropDefault
  protected static final ScalingUtils.ScaleType progressBarImageScaleType =
      FrescoUtils.progressBarImageScaleType;

  @PropDefault
  protected static final ScalingUtils.ScaleType retryImageScaleType =
      FrescoUtils.retryImageScaleType;

  @OnMeasure
  protected static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(optional = true, resType = ResType.FLOAT) float aspectRatio) {
    FrescoUtils.onMeasure(widthSpec, heightSpec, size, aspectRatio);
  }

  @OnCreateMountContent
  protected static GenericDraweeView onCreateMountContent(ComponentContext c) {
//    GenericDraweeHierarchy draweeHierarchy =
//        GenericDraweeHierarchyBuilder.newInstance(c.getResources()).build();
    GenericDraweeView view = new GenericDraweeView(c.getBaseContext());
    view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//    view.setHierarchy(draweeHierarchy);
    return view;
  }

  @OnMount
  protected static void onMount(
      ComponentContext c,
      GenericDraweeView view,
      @Prop(optional = true) ScalingUtils.ScaleType actualImageScaleType,
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
      @Prop(optional = true) ColorFilter colorFilter) {

    FrescoUtils.onMount(
        view.getHierarchy(),
        actualImageScaleType,
        fadeDuration,
        failureImage,
        failureImageScaleType,
        placeholderImage,
        placeholderImageFocusPoint,
        placeholderImageScaleType,
        progressBarImage,
        progressBarImageScaleType,
        retryImage,
        retryImageScaleType,
        roundingParams,
        colorFilter);
  }

  @OnBind
  protected static void onBind(
      ComponentContext c,
      GenericDraweeView view,
      @Prop DraweeController controller) {
    view.setController(controller);
  }
}
