/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.litho.fresco;

import static com.facebook.litho.annotations.ResType.DRAWABLE;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
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
import com.facebook.litho.utils.MeasureUtils;

@MountSpec
public class FrescoImageSpec {

  @PropDefault
  protected static final float imageAspectRatio = FrescoImageDefaults.DEFAULT_IMAGE_ASPECT_RATION;

  @PropDefault
  protected static final ScalingUtils.ScaleType actualImageScaleType =
      FrescoImageDefaults.DEFAULT_ACTUAL_IMAGE_SCALE_TYPE;

  @PropDefault protected static final int fadeDuration = FrescoImageDefaults.DEFAULT_FADE_DURATION;

  @PropDefault
  protected static final ScalingUtils.ScaleType failureImageScaleType =
      FrescoImageDefaults.DEFAULT_SCALE_TYPE;

  @PropDefault
  protected static final PointF placeholderImageFocusPoint =
      FrescoImageDefaults.DEFAULT_PLACEHOLDER_IMAGE_FOCUS_POINT;

  @PropDefault
  protected static final ScalingUtils.ScaleType placeholderImageScaleType =
      FrescoImageDefaults.DEFAULT_SCALE_TYPE;

  @PropDefault
  protected static final ScalingUtils.ScaleType progressBarImageScaleType =
      FrescoImageDefaults.DEFAULT_SCALE_TYPE;

  @PropDefault
  protected static final ScalingUtils.ScaleType retryImageScaleType =
      FrescoImageDefaults.DEFAULT_SCALE_TYPE;

  @OnMeasure
  protected static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(optional = true, resType = ResType.FLOAT) float imageAspectRatio) {
    MeasureUtils.measureWithAspectRatio(widthSpec, heightSpec, imageAspectRatio, size);
  }

  @OnCreateMountContent
  protected static DraweeDrawable<GenericDraweeHierarchy> onCreateMountContent(Context c) {
    GenericDraweeHierarchy draweeHierarchy =
        GenericDraweeHierarchyBuilder.newInstance(c.getResources()).build();
    return new DraweeDrawable<>(c, draweeHierarchy);
  }

  @OnMount
  protected static void onMount(
      ComponentContext c,
      DraweeDrawable<GenericDraweeHierarchy> draweeDrawable,
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
      @Prop(optional = true) ColorFilter colorFilter) {

    GenericDraweeHierarchy draweeHierarchy = draweeDrawable.getDraweeHierarchy();

    FrescoImageHierarchyTools.setupHierarchy(
        actualImageScaleType,
        actualImageFocusPoint,
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
        colorFilter,
        draweeHierarchy);

    draweeDrawable.mount();
  }

  @OnBind
  protected static void onBind(
      ComponentContext c,
      DraweeDrawable<GenericDraweeHierarchy> mountedDrawable,
      @Prop DraweeController controller) {
    mountedDrawable.setController(controller);

    if (controller != null) {
      controller.onViewportVisibilityHint(true);
    }
  }

  @OnUnbind
  protected static void onUnbind(
      ComponentContext c,
      DraweeDrawable<GenericDraweeHierarchy> mountedDrawable,
      @Prop DraweeController controller) {
    mountedDrawable.setController(null);

    if (controller != null) {
      controller.onViewportVisibilityHint(false);
    }
  }

  @OnUnmount
  protected static void onUnmount(
      ComponentContext c, DraweeDrawable<GenericDraweeHierarchy> mountedDrawable) {
    mountedDrawable.unmount();
  }
}
