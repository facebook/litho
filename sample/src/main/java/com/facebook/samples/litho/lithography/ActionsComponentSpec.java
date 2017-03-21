// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho.lithography;

import com.facebook.components.ClickEvent;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.StateValue;
import com.facebook.components.Transition;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.OnEvent;
import com.facebook.components.annotations.OnLayoutTransition;
import com.facebook.components.annotations.OnUpdateState;
import com.facebook.components.annotations.State;
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
    return Container.create(c)
        .backgroundColor(0xDDFFFFFF)
        .positionType(YogaPositionType.ABSOLUTE)
        .positionDip(snapToLeft ? YogaEdge.LEFT : YogaEdge.RIGHT, 4)
        .transitionKey("left_right_slide")
        .positionDip(YogaEdge.TOP, 4)
        .paddingDip(YogaEdge.ALL, 2)
        .flexDirection(YogaFlexDirection.ROW)
        .child(FavouriteButton.create(c))
        .child(
            Container.create(c)
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
