// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.widget.Recycler;
import com.facebook.components.widget.RecyclerBinder;

@LayoutSpec
public class LithographyRootComponentSpec {

  private static final String MAIN_SCREEN = "main_screen";

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final RecyclerBinder recyclerBinder) {

    return Recycler.create(c)
        .binder(recyclerBinder)
        .withLayout()
        .testKey(MAIN_SCREEN)
        .build();
  }
}
