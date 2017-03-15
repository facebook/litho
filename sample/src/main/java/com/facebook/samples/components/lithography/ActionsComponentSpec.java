// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.lithography;

import android.R;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaPositionType;

@LayoutSpec
public class ActionsComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    return Container.create(c)
        .backgroundColor(0xDDFFFFFF)
        .positionType(YogaPositionType.ABSOLUTE)
        .positionDip(YogaEdge.RIGHT, 4)
        .positionDip(YogaEdge.TOP, 4)
        .paddingDip(YogaEdge.ALL, 2)
        .flexDirection(YogaFlexDirection.ROW)
        .child(Container.create(c)
            .backgroundRes(R.drawable.star_off)
            .marginDip(YogaEdge.RIGHT, 2)
            .widthDip(32)
            .heightDip(32))
        .child(Container.create(c)
            .backgroundRes(R.drawable.ic_menu_search)
            .widthDip(32)
            .heightDip(32))
        .build();
  }
}
