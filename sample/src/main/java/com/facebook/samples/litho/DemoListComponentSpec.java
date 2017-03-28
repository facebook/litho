// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho;

import android.support.v7.widget.OrientationHelper;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

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
