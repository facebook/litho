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

import android.graphics.Color;
import com.facebook.litho.Border;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Wrapper;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
class CardBodyComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop Component content) {
    return Wrapper.create(c)
        .paddingDip(YogaEdge.ALL, 4)
        .border(Border.create(c).color(YogaEdge.ALL, Color.BLACK).widthDip(YogaEdge.ALL, 2).build())
        .delegate(content)
        .build();
  }
}
