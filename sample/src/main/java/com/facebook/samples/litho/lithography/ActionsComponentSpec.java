/**
 * Copyright 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE-examples file in the root directory of this source tree.
 */

package com.facebook.samples.litho.lithography;

import com.facebook.yoga.YogaAlign;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Container;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnLayoutTransition;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaPositionType;

import static android.R.drawable.ic_media_ff;
import static android.R.drawable.ic_media_rew;

@LayoutSpec
public class ActionsComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @State boolean snapToLeft) {
    return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
        .backgroundColor(0xDDFFFFFF)
        .positionType(YogaPositionType.ABSOLUTE)
        .positionDip(snapToLeft ? YogaEdge.LEFT : YogaEdge.RIGHT, 4)
        .transitionKey("left_right_slide")
        .positionDip(YogaEdge.TOP, 4)
        .paddingDip(YogaEdge.ALL, 2)
        .flexDirection(YogaFlexDirection.ROW)
        .child(FavouriteButton.create(c))
        .child(
            Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                .backgroundRes(snapToLeft ? ic_media_ff : ic_media_rew)
                .widthDip(32)
                .heightDip(32)
                .clickHandler(ActionsComponent.onClick(c)))
        .build();
  }

  @OnUpdateState
  static void updateState(StateValue<Boolean> snapToLeft) {
    snapToLeft.set(!snapToLeft.get());
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    ActionsComponent.updateState(c);
  }

  @OnLayoutTransition
  static Transition onLayoutTransition(ComponentContext c) {
    return Transition.createSet(
        Transition.create("left_right_slide")
            .translationX()
            .build());
  }
}
