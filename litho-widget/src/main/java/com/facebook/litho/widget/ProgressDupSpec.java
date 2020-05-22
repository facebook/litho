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

package com.facebook.litho.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.ProgressBar;
import androidx.core.content.ContextCompat;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Output;
import com.facebook.litho.R;
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
import com.facebook.litho.utils.MeasureUtils;
import javax.annotation.Nullable;

/**
 * Renders an infinitely spinning progress bar.
 *
 * @uidocs https://fburl.com/Progress:3805
 * @prop indeterminateDrawable Drawable to be shown to show progress.
 * @prop color Tint color for the drawable.
 */
@MountSpec(isPureRender = true)
class ProgressDupSpec {

  static final int DEFAULT_SIZE = 50;

  @PropDefault static final int color = Color.TRANSPARENT;

  @OnLoadStyle
  static void onLoadStyle(ComponentContext c, Output<Drawable> indeterminateDrawable) {
    indeterminateDrawable.set(getStyledIndeterminateDrawable(c, 0));
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop(optional = true, resType = ResType.DRAWABLE) Drawable indeterminateDrawable,
      Output<Drawable> resolvedIndeterminateDrawable) {
    if (indeterminateDrawable != null) {
      resolvedIndeterminateDrawable.set(indeterminateDrawable);
    } else {
      resolvedIndeterminateDrawable.set(
          getStyledIndeterminateDrawable(c, android.R.attr.progressBarStyle));
    }
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED
        && SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
      size.width = DEFAULT_SIZE;
      size.height = DEFAULT_SIZE;
    } else {
      MeasureUtils.measureWithEqualDimens(widthSpec, heightSpec, size);
    }
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      ProgressBar progressBar,
      @Prop(optional = true, resType = ResType.COLOR) int color,
      @FromPrepare Drawable resolvedIndeterminateDrawable) {

    if (resolvedIndeterminateDrawable != null) {
      progressBar.setIndeterminateDrawable(resolvedIndeterminateDrawable);
    }

    if (color != Color.TRANSPARENT && progressBar.getIndeterminateDrawable() != null) {
      progressBar
          .getIndeterminateDrawable()
          .mutate()
          .setColorFilter(color, PorterDuff.Mode.MULTIPLY);
    }
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      ProgressBar progressBar,
      @Prop(optional = true, resType = ResType.COLOR) int color,
      @FromPrepare Drawable resolvedIndeterminateDrawable) {

    // restore the color first, since it acts on the indeterminateDrawable
    if (color != Color.TRANSPARENT && progressBar.getIndeterminateDrawable() != null) {
      progressBar.getIndeterminateDrawable().mutate().clearColorFilter();
    }

    progressBar.setIndeterminateDrawable(null);
  }

  @OnCreateMountContent
  static ProgressBar onCreateMountContent(Context c) {
    return new ProgressView(c);
  }

  static @Nullable Drawable getStyledIndeterminateDrawable(ComponentContext c, int defStyle) {
    Drawable indeterminateDrawable = null;

    final TypedArray styledAttributes = c.obtainStyledAttributes(R.styleable.Progress, defStyle);

    for (int i = 0, size = styledAttributes.getIndexCount(); i < size; i++) {
      final int attr = styledAttributes.getIndex(i);

      if (attr == R.styleable.Progress_android_indeterminateDrawable) {
        indeterminateDrawable =
            ContextCompat.getDrawable(
                c.getAndroidContext(), styledAttributes.getResourceId(attr, 0));
      }
    }

    styledAttributes.recycle();

    return indeterminateDrawable;
  }

  private static class ProgressView extends ProgressBar {

    private ProgressView(Context context) {
      super(context);
    }

    /**
     * ProgressBar is not setting the right bounds on the drawable passed to {@link
     * ProgressBar#setIndeterminateDrawable(Drawable)}. Overriding the method and setting the bounds
     * before passing the drawable in solves the issue.
     */
    @Override
    public void setIndeterminateDrawable(Drawable d) {
      if (d != null) {
        d.setBounds(0, 0, getWidth(), getHeight());
      }

      super.setIndeterminateDrawable(d);
    }
  }
}
