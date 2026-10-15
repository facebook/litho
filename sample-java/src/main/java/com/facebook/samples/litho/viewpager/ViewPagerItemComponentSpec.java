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

package com.facebook.samples.litho.java.viewpager;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import androidx.annotation.Nullable;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;

@LayoutSpec
class ViewPagerItemComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop String title, @Prop @Nullable String subtitle, @Prop int bgColor) {
    return Column.create(c)
        .backgroundColor(bgColor)
        .child(
            Column.create(c)
                .flexGrow(1)
                .marginDip(YogaEdge.ALL, 16)
                .justifyContent(YogaJustify.CENTER)
                .alignContent(YogaAlign.CENTER)
                .child(
                    Text.create(c)
                        .text(title)
                        .textSizeSp(22)
                        .textStyle(Typeface.BOLD)
                        .backgroundColor(Color.argb(150, 255, 255, 255))
                        .paddingDip(YogaEdge.ALL, 8)
                        .textAlignment(Layout.Alignment.ALIGN_CENTER))
                .child(
                    subtitle != null
                        ? Text.create(c)
                            .paddingDip(YogaEdge.ALL, 8)
                            .paddingDip(YogaEdge.TOP, 0)
                            .text(subtitle)
                            .textSizeSp(16)
                            .backgroundColor(Color.argb(150, 255, 255, 255))
                            .textAlignment(Layout.Alignment.ALIGN_CENTER)
                        : null))
        .build();
  }
}
