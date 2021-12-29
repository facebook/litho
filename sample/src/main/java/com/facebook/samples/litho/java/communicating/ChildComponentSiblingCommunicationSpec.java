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

package com.facebook.samples.litho.java.communicating;

import android.graphics.Color;
import com.facebook.litho.Border;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.SolidColor;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec(events = {NotifyParentEvent.class, SelectedRadioButtonEvent.class})
class ChildComponentSiblingCommunicationSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop boolean isSelected, @Prop int id) {
    return Row.create(c)
        .clickHandler(ChildComponentSiblingCommunication.onSelectRadioButton(c))
        .marginDip(YogaEdge.ALL, 30)
        .child(
            SolidColor.create(c)
                .widthDip(20)
                .heightDip(20)
                .marginDip(YogaEdge.TOP, 10)
                .marginDip(YogaEdge.RIGHT, 30)
                .color(isSelected ? Color.BLUE : Color.WHITE)
                .border(
                    Border.create(c)
                        .color(YogaEdge.ALL, Color.BLUE)
                        .widthDip(YogaEdge.ALL, 1)
                        .build()))
        .child(Column.create(c).child(Text.create(c).text("ChildComponent " + id).textSizeDip(20)))
        .build();
  }

  // start_dispatch_to_parent
  @OnEvent(ClickEvent.class)
  static void onSelectRadioButton(ComponentContext c, @Prop int id) {
    ChildComponentSiblingCommunication.dispatchSelectedRadioButtonEvent(
        ChildComponentSiblingCommunication.getSelectedRadioButtonEventHandler(c), id);
  }
  // end_dispatch_to_parent
}
