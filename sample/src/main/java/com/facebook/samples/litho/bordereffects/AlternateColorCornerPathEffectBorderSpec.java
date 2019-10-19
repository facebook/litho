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

package com.facebook.samples.litho.bordereffects;

import com.facebook.litho.Border;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class AlternateColorCornerPathEffectBorderSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Row.create(c)
        .child(
            Text.create(c)
                .textSizeSp(20)
                .text("This component has a path effect with rounded corners + multiple colors"))
        .border(
            Border.create(c)
                .widthDip(YogaEdge.ALL, 20)
                .color(YogaEdge.LEFT, NiceColor.RED)
                .color(YogaEdge.TOP, NiceColor.ORANGE)
                .color(YogaEdge.RIGHT, NiceColor.GREEN)
                .color(YogaEdge.BOTTOM, NiceColor.BLUE)
                .radiusDip(20f)
                .build())
        .build();
  }
}
