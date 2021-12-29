/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.samples.litho.java.stateupdates;

import android.graphics.Color;
import android.graphics.Typeface;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.TouchableFeedback;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import java.util.List;

@GroupSectionSpec
class SectionStateUpdateFromComponentSectionSpec {

  private static final List<Integer> DATA = new ArrayList<>();

  static {
    for (int i = 0; i < 100; i++) {
      DATA.add(i);
    }
  }

  @OnCreateInitialState
  static void onCreateInitialState(SectionContext c, StateValue<Integer> selectedItem) {
    selectedItem.set(null);
  }

  @OnUpdateState
  static void updateSelectedItem(StateValue<Integer> selectedItem, @Param int newSelectedItem) {
    selectedItem.set(newSelectedItem);
  }

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c, @State Integer selectedItem) {
    return Children.create()
        .child(
            SingleComponentSection.create(c)
                .component(
                    Text.create(c)
                        .text("Last selected item: " + selectedItem)
                        .textStyle(Typeface.BOLD)
                        .backgroundColor(Color.WHITE)
                        .paddingDip(YogaEdge.ALL, 8))
                .sticky(true))
        .child(
            DataDiffSection.<Integer>create(new SectionContext(c))
                .data(DATA)
                .renderEventHandler(SectionStateUpdateFromComponentSection.onRenderEvent(c))
                .build())
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRenderEvent(SectionContext c, @FromEvent Integer model) {
    return ComponentRenderInfo.create()
        .component(
            TouchableFeedback.create(c)
                .content(
                    SectionStateUpdateFromComponentItem.create(c)
                        .itemNumber(model)
                        .listItemSelectedEventHandler(
                            SectionStateUpdateFromComponentSection.onListItemSelectedEvent(c))
                        .build()))
        .build();
  }

  @OnEvent(SectionStateUpdateFromComponentItemSpec.ListItemSelectedEvent.class)
  static void onListItemSelectedEvent(SectionContext c, @FromEvent int itemNumber) {
    SectionStateUpdateFromComponentSection.updateSelectedItem(c, itemNumber);
  }
}
