// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.reference.ColorDrawableReference;

import static android.widget.ImageView.ScaleType.FIT_XY;
import static com.facebook.components.annotations.ResType.COLOR;

/**
 * A component that renders a solid color.
 */
@LayoutSpec
class SolidColorSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop(resType = COLOR) int color) {
    return Image.create(c)
        .scaleType(FIT_XY)
        .src(ColorDrawableReference.create(c)
            .color(color))
        .buildWithLayout();
  }
}
