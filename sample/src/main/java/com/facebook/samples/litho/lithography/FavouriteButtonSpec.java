// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import android.view.View;

import com.facebook.components.ClickEvent;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.StateValue;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnEvent;
import com.facebook.components.annotations.FromEvent;
import com.facebook.components.annotations.OnUpdateState;
import com.facebook.components.annotations.State;
import com.facebook.yoga.YogaEdge;

import static android.R.drawable.star_on;
import static android.R.drawable.star_off;

@LayoutSpec
public class FavouriteButtonSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @State boolean favourited) {
    return Container.create(c)
        .backgroundRes(favourited ? star_on : star_off)
        .marginDip(YogaEdge.RIGHT, 2)
        .widthDip(32)
        .heightDip(32)
        .clickHandler(FavouriteButton.onClick(c))
        .build();
  }

  @OnUpdateState
  static void toggleFavourited(StateValue<Boolean> favourited) {
    favourited.set(!favourited.get());
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @FromEvent View view) {
    FavouriteButton.toggleFavourited(c);
  }
}
