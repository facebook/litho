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

package com.facebook.litho.widget;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LongClickEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;

@LayoutSpec
public class LayoutWithInnerClickableChildTesterSpec {
  @PropDefault protected static final boolean shouldSetClickHandler = false;
  @PropDefault protected static final boolean shouldSetLongClickHandler = false;

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop(optional = true) boolean shouldSetClickHandler,
      @Prop(optional = true) boolean shouldSetLongClickHandler) {

    final SimpleMountSpecTester.Builder mountSpecBuilder = SimpleMountSpecTester.create(c);

    if (shouldSetClickHandler) {
      mountSpecBuilder.clickHandler(LayoutWithInnerClickableChildTester.onClickEvent(c));
    }

    if (shouldSetLongClickHandler) {
      mountSpecBuilder.longClickHandler(LayoutWithInnerClickableChildTester.onLongClickEvent(c));
    }

    return Column.create(c).child(mountSpecBuilder.build()).build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(ComponentContext c) {}

  @OnEvent(LongClickEvent.class)
  static boolean onLongClickEvent(ComponentContext c) {
    return false;
  }
}
