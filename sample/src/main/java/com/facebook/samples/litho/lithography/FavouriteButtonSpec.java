/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import android.view.View;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;

import static android.R.drawable.star_off;
import static android.R.drawable.star_on;

@LayoutSpec
public class FavouriteButtonSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @State boolean favourited) {
    return Row.create(c)
        .backgroundRes(favourited ? star_on : star_off)
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
