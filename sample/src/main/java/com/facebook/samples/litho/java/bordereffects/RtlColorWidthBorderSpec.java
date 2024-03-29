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

package com.facebook.samples.litho.java.bordereffects;

import com.facebook.litho.Border;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class RtlColorWidthBorderSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Row.create(c)
        .layoutDirection(YogaDirection.RTL)
        .child(Text.create(c).textSizeSp(20).text("This component is RTL"))
        .border(
            Border.create(c)
                .color(YogaEdge.START, NiceColor.RED)
                .color(YogaEdge.TOP, NiceColor.YELLOW)
                .color(YogaEdge.END, NiceColor.GREEN)
                .color(YogaEdge.BOTTOM, NiceColor.BLUE)
                .widthDip(YogaEdge.START, 2)
                .widthDip(YogaEdge.TOP, 4)
                .widthDip(YogaEdge.END, 8)
                .widthDip(YogaEdge.BOTTOM, 16)
                .build())
        .build();
  }
}
