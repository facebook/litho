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

package com.facebook.samples.litho.lithography;

import static android.graphics.Color.GRAY;
import static android.graphics.Typeface.ITALIC;
import static com.facebook.litho.annotations.ResType.STRING;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.InvisibleEvent;
import com.facebook.litho.VisibleEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class FooterComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop(resType = STRING) String text) {
    return Column.create(c)
            .paddingDip(YogaEdge.ALL, 8)
            .child(Text.create(c)
                    .visibleHandler(FooterComponent.onVisible(c))
                    .invisibleHandler(FooterComponent.onInvisible(c))
                    .text(text).textSizeDip(14).textColor(GRAY).textStyle(ITALIC))
            .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(ComponentContext c, @Prop(resType = STRING) String text) {
    android.util.Log.d("TEST_VIS", "Footer VISIBLE: " + text);
  }

  @OnEvent(InvisibleEvent.class)
  static void onInvisible(ComponentContext c, @Prop(resType = STRING) String text) {
    android.util.Log.d("TEST_VIS", "Footer INVISIBLE: " + text);
  }
}
