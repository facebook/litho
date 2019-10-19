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

package com.facebook.samples.litho.errors;

import static android.graphics.Typeface.BOLD;
import static com.facebook.litho.annotations.ResType.STRING;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class TitleComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop(resType = STRING) String title) {
    return Text.create(c)
        .text(title)
        .textStyle(BOLD)
        .textSizeDip(24)
        .positionDip(YogaEdge.BOTTOM, 16)
        .build();
  }
}
