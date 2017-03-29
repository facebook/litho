/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.ProgressBar;

import com.facebook.R;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.reference.Reference;
import com.facebook.litho.reference.ResourceDrawableReference;
import com.facebook.litho.utils.MeasureUtils;

/**
 * Renders an infinitely spinning progress bar.
 */
@MountSpec(isPureRender = true)
class ProgressSpec {

  static final int DEFAULT_SIZE = 50;

  @PropDefault static final int color = Color.TRANSPARENT;

  @OnLoadStyle
  static void onLoadStyle(
      ComponentContext c,
      Output<Reference<Drawable>> indeterminateDrawable) {

    indeterminateDrawable.set(getStyledIndeterminateDrawable(c, 0));
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop(optional = true, resType = ResType.DRAWABLE) Reference<Drawable> indeterminateDrawable,
      Output<Reference<Drawable>> resolvedIndeterminateDrawable) {
    if (indeterminateDrawable != null) {
      resolvedIndeterminateDrawable.set(indeterminateDrawable);
    } else {
      resolvedIndeterminateDrawable.set(getStyledIndeterminateDrawable(
          c,
          android.R.attr.progressBarStyle));
    }
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size) {
    if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED &&
        SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
      size.width = DEFAULT_SIZE;
      size.height = DEFAULT_SIZE;
    } else {
      MeasureUtils.measureWithEqualDimens(widthSpec, heightSpec, size);
    }
  }

  @OnMount
