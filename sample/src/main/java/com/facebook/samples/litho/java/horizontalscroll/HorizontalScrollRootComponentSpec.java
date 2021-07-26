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

package com.facebook.samples.litho.java.horizontalscroll;

import android.util.Pair;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.HorizontalScroll;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

@LayoutSpec
public class HorizontalScrollRootComponentSpec {

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c,
      StateValue<ImmutableList<Pair<String, Integer>>> items,
      StateValue<Integer> prependCounter,
      StateValue<Integer> appendCounter) {
    final List<Pair<String, Integer>> initialItems = new ArrayList<>();
    initialItems.add(new Pair<>("Coral", 0xFFFF7F50));
    initialItems.add(new Pair<>("Ivory", 0xFFFFFFF0));
    initialItems.add(new Pair<>("PeachPuff", 0xFFFFDAB9));
    initialItems.add(new Pair<>("LightPink", 0xFFFFB6C1));
    initialItems.add(new Pair<>("LavenderBlush", 0xFFFFF0F5));
    initialItems.add(new Pair<>("Gold", 0xFFFFD700));
    initialItems.add(new Pair<>("BlanchedAlmond", 0xFFFFEBCD));
    initialItems.add(new Pair<>("FloralWhite", 0xFFFFFAF0));
    initialItems.add(new Pair<>("Moccasin", 0xFFFFE4B5));
    initialItems.add(new Pair<>("LightYellow", 0xFFFFFFE0));
    items.set(new ImmutableList.Builder<Pair<String, Integer>>().addAll(initialItems).build());
    prependCounter.set(0);
    appendCounter.set(0);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @State ImmutableList<Pair<String, Integer>> items) {
    return Column.create(c)
        .child(
            Row.create(c)
                .paddingDip(YogaEdge.VERTICAL, 10)
                .child(
                    Text.create(c)
                        .paddingDip(YogaEdge.RIGHT, 10)
                        .alignSelf(YogaAlign.CENTER)
                        .clickHandler(HorizontalScrollRootComponent.onClick(c, true))
                        .text("PREPEND")
                        .textSizeSp(20))
                .child(
                    Text.create(c)
                        .paddingDip(YogaEdge.LEFT, 10)
                        .alignSelf(YogaAlign.CENTER)
                        .clickHandler(HorizontalScrollRootComponent.onClick(c, false))
                        .text("APPEND")
                        .textSizeSp(20)))
        .child(HorizontalScroll.create(c).contentProps(createHorizontalScrollChildren(c, items)))
        .build();
  }

  private static Component createHorizontalScrollChildren(
      ComponentContext c, List<Pair<String, Integer>> items) {
    final Row.Builder rowBuilder = Row.create(c);
    for (Pair<String, Integer> colorItem : items) {
      rowBuilder.child(
          Row.create(c)
              .paddingDip(YogaEdge.ALL, 10)
              .backgroundColor(colorItem.second)
              .child(
                  Text.create(c)
                      .text(colorItem.first)
                      .textSizeSp(20)
                      .alignSelf(YogaAlign.CENTER)
                      .heightDip(100)));
    }
    return rowBuilder.build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @State(canUpdateLazily = true) int prependCounter,
      @State(canUpdateLazily = true) int appendCounter,
      @State ImmutableList<Pair<String, Integer>> items,
      @Param boolean isPrepend) {
    final ArrayList<Pair<String, Integer>> updatedItems = new ArrayList<>(items);
    if (isPrepend) {
      updatedItems.add(0, new Pair<>("Prepend#" + prependCounter, 0xFF7CFC00));
      HorizontalScrollRootComponent.lazyUpdatePrependCounter(c, ++prependCounter);
    } else {
      updatedItems.add(new Pair<>("Append#" + appendCounter, 0xFF6495ED));
      HorizontalScrollRootComponent.lazyUpdateAppendCounter(c, ++appendCounter);
    }
    HorizontalScrollRootComponent.updateItems(
        c, new ImmutableList.Builder<Pair<String, Integer>>().addAll(updatedItems).build());
  }

  @OnUpdateState
  static void updateItems(
      StateValue<ImmutableList<Pair<String, Integer>>> items,
      @Param ImmutableList<Pair<String, Integer>> updatedItems) {
    items.set(updatedItems);
  }
}
