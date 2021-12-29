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
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.Event;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec(events = {SectionStateUpdateFromComponentItemSpec.ListItemSelectedEvent.class})
public class SectionStateUpdateFromComponentItemSpec {

  @Event
  public static class ListItemSelectedEvent {

    public ListItemSelectedEvent() {}

    public ListItemSelectedEvent(int itemNumber) {
      this.itemNumber = itemNumber;
    }

    public int itemNumber;
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(ComponentContext c, @Prop int itemNumber) {
    SectionStateUpdateFromComponentItem.getListItemSelectedEventHandler(c)
        .dispatchEvent(new ListItemSelectedEvent(itemNumber));
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop int itemNumber) {
    return Text.create(c)
        .paddingDip(YogaEdge.ALL, 8)
        .text("Item " + itemNumber)
        .backgroundColor(Color.WHITE)
        .clickHandler(SectionStateUpdateFromComponentItem.onClickEvent(c))
        .build();
  }
}
