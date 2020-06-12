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

package com.facebook.samples.litho.duplicatestate;

import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.widget.Text;
import com.facebook.samples.litho.R;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
class DuplicateStateSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 20)
        .child(
            Row.create(c)
                .paddingDip(YogaEdge.ALL, 5)
                .child(
                    Row.create(c)
                        .alignContent(YogaAlign.FLEX_END)
                        .widthDip(500)
                        .clickable(true)
                        .backgroundColor(0x20000000)
                        .child(
                            Text.create(c)
                                .alignSelf(YogaAlign.FLEX_END)
                                .marginDip(YogaEdge.ALL, 2)
                                .duplicateParentState(true)
                                .backgroundRes(R.drawable.press_selector)
                                .text("Press parent to highlight me")
                                .widthDip(150)
                                .minLines(2)
                                .maxLines(2)
                                .textSizeSp(18))))
        .child(
            Row.create(c)
                .paddingDip(YogaEdge.ALL, 5)
                .child(
                    Row.create(c)
                        .alignContent(YogaAlign.FLEX_END)
                        .widthDip(500)
                        .duplicateChildrenStates(true)
                        .backgroundRes(R.drawable.press_selector)
                        .child(
                            Text.create(c)
                                .marginDip(YogaEdge.ALL, 2)
                                .text("Press me to highlight parent")
                                .widthDip(150)
                                .minLines(2)
                                .maxLines(2)
                                .textSizeSp(18)
                                .clickable(true)
                                .backgroundColor(0x20000000))))
        .child(
            Row.create(c)
                .paddingDip(YogaEdge.ALL, 5)
                .child(
                    Row.create(c)
                        .alignContent(YogaAlign.FLEX_END)
                        .widthDip(500)
                        .duplicateChildrenStates(true)
                        .backgroundRes(R.drawable.focus_selector)
                        .child(
                            Text.create(c)
                                .marginDip(YogaEdge.ALL, 2)
                                .text("Focus on me to highlight parent")
                                .focusable(true)
                                .widthDip(150)
                                .minLines(2)
                                .maxLines(2)
                                .textSizeSp(18)
                                .backgroundColor(0x20000000))))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent2(ComponentContext c, @FromEvent View view) {}

  @OnEvent(ClickEvent.class)
  static void onClickEvent(ComponentContext c, @FromEvent View view) {}
}
