// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.lithobarebones;

import android.graphics.Color;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.widget.Text;

import static com.facebook.yoga.YogaEdge.ALL;

@LayoutSpec
public class FeedItemSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    return Container.create(c)
        .paddingDip(ALL, 16)
        .backgroundColor(Color.WHITE)
        .child(
            Text.create(c)
                .text("Hello World")
                .textSizeSp(40)
                .build())
        .build();
  }
}
