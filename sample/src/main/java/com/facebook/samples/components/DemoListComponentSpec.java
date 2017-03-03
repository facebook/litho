// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.widget.Recycler;

@LayoutSpec
public class DemoListComponentSpec {

  private static final String MAIN_SCREEN = "main_screen";

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    return Recycler.create(c)
        .binder(new DemoListBinder(c))
        .withLayout()
        .testKey(MAIN_SCREEN)
        .build();
  }
}
