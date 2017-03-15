// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components;

import android.support.v7.widget.OrientationHelper;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.widget.LinearLayoutInfo;
import com.facebook.components.widget.Recycler;
import com.facebook.components.widget.RecyclerBinder;

@LayoutSpec
public class DemoListComponentSpec {

  private static final String MAIN_SCREEN = "main_screen";

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    final RecyclerBinder recyclerBinder = new RecyclerBinder(
            c,
            4.0f,
            new LinearLayoutInfo(c, OrientationHelper.VERTICAL, false));

    Demos.addAllToBinder(recyclerBinder, c);

    return Recycler.create(c)
        .binder(recyclerBinder)
        .withLayout()
        .testKey(MAIN_SCREEN)
        .build();
  }
}
