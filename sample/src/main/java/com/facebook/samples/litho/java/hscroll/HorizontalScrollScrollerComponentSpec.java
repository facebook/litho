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

package com.facebook.samples.litho.java.hscroll;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController;
import com.facebook.litho.widget.ItemSelectedEvent;
import com.facebook.litho.widget.Spinner;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import java.util.List;

@LayoutSpec
public class HorizontalScrollScrollerComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop Integer[] colors) {
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 5)
        .child(
            Row.create(c)
                .alignContent(YogaAlign.STRETCH)
                .marginDip(YogaEdge.TOP, 10)
                .child(
                    Text.create(c)
                        .alignSelf(YogaAlign.CENTER)
                        .flexGrow(2f)
                        .text("Scroll to: ")
                        .textSizeSp(20))
                .child(
                    Text.create(c)
                        .alignSelf(YogaAlign.CENTER)
                        .flexGrow(0.5f)
                        .text("PREVIOUS")
                        .clickHandler(HorizontalScrollScrollerComponent.onClick(c, false))
                        .textSizeSp(20))
                .child(
                    Text.create(c)
                        .alignSelf(YogaAlign.CENTER)
                        .flexGrow(0.5f)
                        .text("NEXT")
                        .clickHandler(HorizontalScrollScrollerComponent.onClick(c, true))
                        .textSizeSp(20)))
        .child(
            Row.create(c)
                .alignContent(YogaAlign.STRETCH)
                .marginDip(YogaEdge.TOP, 10)
                .child(
                    Text.create(c)
                        .alignSelf(YogaAlign.CENTER)
                        .flexGrow(2f)
                        .text("Smooth scroll to: ")
                        .textSizeSp(20))
                .child(
                    Spinner.create(c)
                        .flexGrow(1.f)
                        .options(getPositionsFromDataset(colors))
                        .selectedOption("0")
                        .itemSelectedEventHandler(
                            HorizontalScrollScrollerComponent.onScrollToPositionSelected(c))))
        .build();
  }

  private static List<String> getPositionsFromDataset(Integer[] colors) {
    final List<String> positions = new ArrayList<>();
    for (int i = 0; i < colors.length; i++) {
      positions.add(i, Integer.toString(i));
    }
    return positions;
  }

  @OnEvent(ItemSelectedEvent.class)
  static void onScrollToPositionSelected(
      ComponentContext c,
      @Prop RecyclerCollectionEventsController eventsController,
      @FromEvent String newSelection) {
    eventsController.requestScrollToPositionWithSnap(Integer.parseInt(newSelection));
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @Prop RecyclerCollectionEventsController eventsController,
      @Param boolean forward) {
    if (forward) {
      eventsController.requestScrollToNextPosition(true);
    } else {
      eventsController.requestScrollToPreviousPosition(true);
    }
  }
}
