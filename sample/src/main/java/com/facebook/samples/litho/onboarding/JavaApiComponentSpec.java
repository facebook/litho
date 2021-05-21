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

package com.facebook.samples.litho.onboarding;

import android.graphics.Typeface;
import android.widget.ImageView;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;

// start_example
@LayoutSpec
public class JavaApiComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop String username, @Prop int avatarRes, @Prop int imageRes) {
    return Column.create(c)
        .child(
            Row.create(c)
                .paddingDip(YogaEdge.ALL, 8)
                .alignItems(YogaAlign.CENTER)
                .child(
                    Image.create(c)
                        .drawableRes(avatarRes)
                        .widthDip(36)
                        .heightDip(36)
                        .marginDip(YogaEdge.START, 4)
                        .marginDip(YogaEdge.END, 8))
                .child(Text.create(c).text(username).textStyle(Typeface.BOLD)))
        .child(
            Image.create(c)
                .drawableRes(imageRes)
                .scaleType(ImageView.ScaleType.CENTER_CROP)
                .aspectRatio(1))
        .build();
  }
}
// end_example
