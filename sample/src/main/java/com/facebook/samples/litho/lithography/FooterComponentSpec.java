// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentContext;
import com.facebook.components.Container;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.widget.Text;
import com.facebook.yoga.YogaEdge;

import static android.graphics.Color.GRAY;
import static android.graphics.Typeface.ITALIC;
import static com.facebook.components.annotations.ResType.STRING;

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
