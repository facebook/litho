/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.samples.litho.java.animations.pageindicators;

import android.graphics.Color;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.DimensionValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.Prop;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
class PageIndicatorsSpec {
  private static final int COLOR_SELECTED = Color.BLACK;
  private static final int COLOR_NORMAL = Color.GRAY;

  static final int MAX_DOT_COUNT = 5;
  private static final int MAX_LARGE_DOT_COUNT = 3;

  private static final int RADIUS_LARGE = 4;
  private static final int RADIUS_MEDIUM = 3;
  private static final int RADIUS_SMALL = 2;

  static final int DIRECTION_NONE = 0;
  static final int DIRECTION_LEFT = -1;
  static final int DIRECTION_RIGHT = 1;

  private static final Transition.TransitionAnimator ANIMATOR = Transition.timing(1000);

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop int size, @Prop int selectedIndex, @Prop int firstVisibleIndex) {
    Row.Builder row = Row.create(c).alignItems(YogaAlign.CENTER);
    final int dotCount = Math.min(size, MAX_DOT_COUNT);
    for (int position = 0; position < dotCount; ++position) {
      final int index = firstVisibleIndex + position;
      row.child(
          Circle.create(c)
              .radiusDip(2 * getIndicatorSize(size, firstVisibleIndex, position, selectedIndex))
              .color(index == selectedIndex ? COLOR_SELECTED : COLOR_NORMAL)
              .transitionKey("dot" + index)
              .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
              .marginDip(YogaEdge.ALL, 2 * 1));
    }
    return row.build();
  }

  private static int getIndicatorSize(
      int numPages, int firstPageInWindow, int position, int selectedPage) {

    // if you're at the left edge [oxooo]ooo AND selected is within first three dots
    if (firstPageInWindow == 0 && selectedPage <= MAX_LARGE_DOT_COUNT - 1) {
      if (position == MAX_DOT_COUNT - 1) {
        return RADIUS_SMALL;
      } else if (position == MAX_DOT_COUNT - 2) {
        return RADIUS_MEDIUM;
      } else {
        return RADIUS_LARGE;
      }
    }

    // if you're at the right edge ooo[oooxo] AND selected is within last three dots
    if (firstPageInWindow == numPages - MAX_DOT_COUNT
        && selectedPage >= numPages - MAX_LARGE_DOT_COUNT) {
      if (position == 0) {
        return RADIUS_SMALL;
      } else if (position == 1) {
        return RADIUS_MEDIUM;
      } else {
        return RADIUS_LARGE;
      }
    }

    // if in the middle ooo[ooxoo]ooo
    if (position == 0 || position == MAX_DOT_COUNT - 1) {
      return RADIUS_MEDIUM;
    } else {
      return RADIUS_LARGE;
    }
  }

  @OnCreateTransition
  static Transition onCreateTransition(
      ComponentContext c, @Prop int size, @Prop int movingDirection) {
    if (movingDirection == DIRECTION_NONE) {
      return null;
    }

    final String[] keys = new String[size];
    for (int index = 0; index < size; ++index) {
      keys[index] = "dot" + index;
    }

    final boolean movingRight = movingDirection == DIRECTION_RIGHT;
    return Transition.create(Transition.TransitionKeyType.GLOBAL, keys)
        // moving existing
        .animate(
            AnimatedProperties.X,
            AnimatedProperties.Y,
            AnimatedProperties.HEIGHT,
            AnimatedProperties.WIDTH)
        .animator(ANIMATOR)
        // shifting in/out
        .animate(AnimatedProperties.X)
        .animator(ANIMATOR)
        .appearFrom(DimensionValue.widthPercentageOffset(movingRight ? 100 : -100))
        .disappearTo(DimensionValue.widthPercentageOffset(movingRight ? -100 : 100))
        // zooming in/out
        .animate(AnimatedProperties.SCALE)
        .animator(ANIMATOR)
        .appearFrom(0)
        .disappearTo(0);
  }
}
