// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentContext;
import com.facebook.components.Container;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

import static android.graphics.Color.GRAY;
import static android.graphics.Typeface.ITALIC;
import static com.facebook.litho.annotations.ResType.STRING;

@LayoutSpec
public class FooterComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop(resType = STRING) String text) {
    return Container.create(c)
        .paddingDip(YogaEdge.ALL, 8)
        .child(
            Text.create(c)
                .text(text)
                .textSizeDip(14)
                .textColor(GRAY)
                .textStyle(ITALIC))
        .build();
  }
}
