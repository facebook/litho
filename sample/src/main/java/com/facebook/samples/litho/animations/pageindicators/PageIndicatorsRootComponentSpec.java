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

package com.facebook.samples.litho.animations.pageindicators;

import static com.facebook.samples.litho.animations.pageindicators.PageIndicatorsSpec.DIRECTION_LEFT;
import static com.facebook.samples.litho.animations.pageindicators.PageIndicatorsSpec.DIRECTION_NONE;
import static com.facebook.samples.litho.animations.pageindicators.PageIndicatorsSpec.DIRECTION_RIGHT;

import com.facebook.litho.*;
import com.facebook.litho.annotations.*;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;

@LayoutSpec
public class PageIndicatorsRootComponentSpec {
  private static final int PAGES_COUNT = 10;
  private static final int WINDOW_SIZE = PageIndicatorsSpec.MAX_DOT_COUNT;

  @OnCreateLayout
  public static Component onCrateLayout(
      ComponentContext c,
      @State int selectedPage,
      @State int firstVisibleIndex,
      @State int movingDirection) {
    final String title = (selectedPage + 1) + "/" + PAGES_COUNT;
    return Column.create(c)
        .alignItems(YogaAlign.CENTER)
        .justifyContent(YogaJustify.CENTER)
        .child(Text.create(c).textSizeDip(20).text(title))
        .child(
            Row.create(c)
                .alignSelf(YogaAlign.STRETCH)
                .justifyContent(YogaJustify.SPACE_BETWEEN)
                .child(
                    Text.create(c)
                        .paddingDip(YogaEdge.ALL, 12)
                        .textSizeDip(20)
                        .text("Prev")
                        .clickHandler(PageIndicatorsRootComponent.onPrevClick(c)))
                .child(
                    PageIndicators.create(c)
                        .size(PAGES_COUNT)
                        .selectedIndex(selectedPage)
                        .firstVisibleIndex(firstVisibleIndex)
                        .movingDirection(movingDirection))
                .child(
                    Text.create(c)
                        .paddingDip(YogaEdge.ALL, 12)
                        .textSizeDip(20)
                        .text("Next")
                        .clickHandler(PageIndicatorsRootComponent.onNextClick(c))))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onPrevClick(ComponentContext c) {
    PageIndicatorsRootComponent.updateSelectedPageIndexSync(c, false);
  }

  @OnEvent(ClickEvent.class)
  static void onNextClick(ComponentContext c) {
    PageIndicatorsRootComponent.updateSelectedPageIndexSync(c, true);
  }

  @OnUpdateState
  static void updateSelectedPageIndex(
      StateValue<Integer> selectedPage,
      StateValue<Integer> firstVisibleIndex,
      StateValue<Integer> movingDirection,
      @Param boolean next) {
    final int prevPageIndex = selectedPage.get();
    final int newPageIndex =
        next ? Math.min(prevPageIndex + 1, PAGES_COUNT - 1) : Math.max(prevPageIndex - 1, 0);
    if (newPageIndex == prevPageIndex) {
      movingDirection.set(DIRECTION_NONE);
      return;
    }
    movingDirection.set(next ? DIRECTION_RIGHT : DIRECTION_LEFT);
    selectedPage.set(newPageIndex);

    final int firstVisible = firstVisibleIndex.get();
    if (next) {
      final int lastVisible = firstVisible + WINDOW_SIZE - 1;
      if (lastVisible == PAGES_COUNT - 1) {
        // nowhere to move
        return;
      }
      if (newPageIndex < lastVisible) {
        // no need to move
        return;
      }
      firstVisibleIndex.set(firstVisible + 1);
    } else {
      if (firstVisible == 0) {
        // nowhere to move
        return;
      }
      if (newPageIndex > firstVisible) {
        // no need to move
        return;
      }
      firstVisibleIndex.set(firstVisible - 1);
    }
  }
}
