// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;

@LayoutSpec
public class DecadeSeparatorSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final Decade decade) {
    return Container.create(c)
        .flexDirection(YogaFlexDirection.ROW)
        .alignItems(YogaAlign.CENTER)
        .paddingDip(YogaEdge.HORIZONTAL, 16)
        .paddingDip(YogaEdge.VERTICAL, 16)
        .child(
            Container.create(c)
                .heightPx(1)
                .backgroundColor(0xFFAAAAAA)
                .flex(1))
        .child(
            Text.create(c)
            .text(String.valueOf(decade.year))
            .textSizeDip(14)
            .textColor(0xFFAAAAAA)
            .withLayout()
            .marginDip(YogaEdge.HORIZONTAL, 10)
            .flex(0))
        .child(
            Container.create(c)
                .heightPx(1)
                .backgroundColor(0xFFAAAAAA)
                .flex(1))
        .build();
  }
}
