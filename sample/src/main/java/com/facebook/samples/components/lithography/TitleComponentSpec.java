// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.lithography;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.widget.Text;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;

import static android.graphics.Typeface.BOLD;
import static com.facebook.components.annotations.ResType.STRING;

@LayoutSpec
public class TitleComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop(resType = STRING) String title) {
    return Text.create(c)
        .text(title)
        .textStyle(BOLD)
        .textSizeDip(24)
        .withLayout()
        .backgroundColor(0xDDFFFFFF)
        .positionType(YogaPositionType.ABSOLUTE)
        .positionDip(YogaEdge.BOTTOM, 4)
        .positionDip(YogaEdge.LEFT, 4)
        .paddingDip(YogaEdge.HORIZONTAL, 6)
        .build();
  }
}
