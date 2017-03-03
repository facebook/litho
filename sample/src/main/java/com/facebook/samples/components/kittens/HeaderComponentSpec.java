// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.kittens;

import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.widget.Text;

import static android.graphics.Color.BLACK;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.components.annotations.ResType.STRING;
import static com.facebook.components.widget.VerticalGravity.CENTER;

@LayoutSpec
public class HeaderComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop(resType = STRING) String text) {
    return Text.create(c)
        .text(text)
        .verticalGravity(CENTER)
        .minLines(2)
        .textColor(BLACK)
        .textSizeDip(16)
        .isSingleLine(true)
        .withLayout()
        .paddingDip(ALL, 16)
        .build();
  }
}
