/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.widget.ImageView.ScaleType;

import com.facebook.R;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.DrawableMatrix;
import com.facebook.litho.MatrixDrawable;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.reference.Reference;
import com.facebook.litho.reference.ResourceDrawableReference;
import com.facebook.litho.utils.MeasureUtils;

import static com.facebook.litho.SizeSpec.UNSPECIFIED;

/**
 * A component that is able to display drawable resources. It takes a drawable
 * resource ID as prop.
 */
@MountSpec(isPureRender = true, poolSize = 30)
class ImageSpec {

  private static final ScaleType[] SCALE_TYPE = ScaleType.values();

  @OnLoadStyle
  static void onLoadStyle(
      ComponentContext c,
      Output<Reference<Drawable>> src,
      Output<ScaleType> scaleType) {

    final TypedArray a = c.obtainStyledAttributes(R.styleable.Image, 0);

    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == R.styleable.Image_android_src) {
        src.set(ResourceDrawableReference.create(c)
            .resId(a.getResourceId(attr, 0))
            .build());
      } else if (attr == R.styleable.Image_android_scaleType) {
        scaleType.set(SCALE_TYPE[a.getInteger(attr, -1)]);
      }
    }

    a.recycle();
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(resType = ResType.DRAWABLE) Reference<Drawable> src) {
    if (src == null) {
      size.width = 0;
      size.height = 0;
      return;
    }

    final Drawable drawable = Reference.acquire(c, src);
    try {
      if (drawable == null ||
          drawable.getIntrinsicWidth() <= 0 ||
          drawable.getIntrinsicHeight() <= 0) {
        size.width = 0;
        size.height = 0;
        return;
      }

      final int intrinsicHeight = drawable.getIntrinsicHeight();
      final int intrinsicWidth = drawable.getIntrinsicWidth();

      if (SizeSpec.getMode(widthSpec) == UNSPECIFIED &&
          SizeSpec.getMode(heightSpec) == UNSPECIFIED) {
        size.width = intrinsicWidth;
        size.height = intrinsicHeight;
        return;
      }

      final float aspectRatio = intrinsicWidth / (float) intrinsicHeight;
      MeasureUtils.measureWithAspectRatio(
          widthSpec,
          heightSpec,
          intrinsicWidth,
          intrinsicHeight,
          aspectRatio,
          size);
    } finally {
      Reference.release(c, drawable, src);
    }
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      @Prop(resType = ResType.DRAWABLE) Reference<Drawable> src,
      @Prop(optional = true) ScaleType scaleType,
      Output<DrawableMatrix> drawableMatrix,
      Output<Integer> drawableWidth,
      Output<Integer> drawableHeight) {

    final Drawable d = Reference.acquire(c, src);
    try {
      final int horizontalPadding = layout.getPaddingLeft() + layout.getPaddingRight();
      final int verticalPadding = layout.getPaddingTop() + layout.getPaddingBottom();

      if (ScaleType.FIT_XY == scaleType
          || d.getIntrinsicWidth() <= 0
          || d.getIntrinsicHeight() <= 0) {
        drawableMatrix.set(null);
        drawableWidth.set(layout.getWidth() - horizontalPadding);
        drawableHeight.set(layout.getHeight() - verticalPadding);
      } else {
        final DrawableMatrix matrix = DrawableMatrix.create(
            d,
            scaleType,
            layout.getWidth() - horizontalPadding,
            layout.getHeight() - verticalPadding);

        drawableMatrix.set(matrix);
        drawableWidth.set(d.getIntrinsicWidth());
        drawableHeight.set(d.getIntrinsicHeight());
      }
    } finally {
      Reference.release(c, d, src);
    }
  }

  @OnCreateMountContent
  static MatrixDrawable onCreateMountContent(ComponentContext c) {
    return new MatrixDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      MatrixDrawable matrixDrawable,
      @Prop(resType = ResType.DRAWABLE) Reference<Drawable> src,
      @FromBoundsDefined DrawableMatrix drawableMatrix) {
    matrixDrawable.mount(Reference.acquire(c, src), drawableMatrix);
  }

  @OnBind
  static void onBind(
      ComponentContext c,
      MatrixDrawable mountedDrawable,
      @FromBoundsDefined Integer drawableWidth,
      @FromBoundsDefined Integer drawableHeight) {
    mountedDrawable.bind(drawableWidth, drawableHeight);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      MatrixDrawable convertDrawable,
      @Prop(resType = ResType.DRAWABLE) Reference<Drawable> src) {
    Reference.release(c, convertDrawable.getMountedDrawable(), src);
    convertDrawable.unmount();
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(
