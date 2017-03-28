// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.lithobarebones;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

import static com.facebook.yoga.YogaEdge.ALL;

@LayoutSpec
public class FeedItemSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop int color,
      @Prop String message) {
    return Container.create(c)
        .paddingDip(ALL, 16)
        .backgroundColor(color)
        .child(
            Text.create(c)
                .text(message)
                .textSizeSp(40))
        .build();
  }
}
