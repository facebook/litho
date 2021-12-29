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

package com.facebook.samples.litho.java.onboarding;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.Text;
import com.facebook.samples.litho.R;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class LayoutWithImageComponentSpec {

  // start
  @OnCreateLayout
  static Component OnCreateLayout(ComponentContext c, @Prop String name) {
    return Row.create(c)
        .paddingDip(YogaEdge.ALL, 8)
        .alignItems(YogaAlign.CENTER)
        .child(Image.create(c).drawableRes(R.drawable.ic_launcher))
        .child(
            Column.create(c)
                .paddingDip(YogaEdge.START, 8)
                .child(Text.create(c).text("Hello " + name + "!"))
                .child(Text.create(c).text("Layouts in Litho use the Flexbox API")))
        .build();
  }
  // end
}
