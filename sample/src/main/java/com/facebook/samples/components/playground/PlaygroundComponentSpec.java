// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.plaground;

import android.graphics.Color;

import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentContext;
import com.facebook.components.Container;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.widget.Text;

@LayoutSpec
public class PlaygroundComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    return Container.create(c)
        .backgroundColor(Color.WHITE)
        .child(
            Text.create(c)
                .textSizeSp(20)
                .text("Playground sample"))
        .build();
  }
}
