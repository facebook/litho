/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import android.view.View;

import com.facebook.components.ClickEvent;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.StateValue;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
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
