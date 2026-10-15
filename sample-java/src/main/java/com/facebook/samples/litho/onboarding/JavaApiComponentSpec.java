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

import android.graphics.Typeface;
import android.widget.ImageView;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.Style;
import com.facebook.litho.StyleCompat;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.flexbox.FlexboxStyleKt;
import com.facebook.litho.widget.PrimitiveImage;
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
                    PrimitiveImage.createDp(
                        c.getResourceResolver().resolveDrawableRes(avatarRes),
                        ImageView.ScaleType.FIT_CENTER,
                        true,
                        36f,
                        36f,
                        StyleCompat.marginDip(YogaEdge.START, 4f)
                            .marginDip(YogaEdge.END, 8f)
                            .build()))
                .child(Text.create(c).text(username).textStyle(Typeface.BOLD)))
        .child(
            PrimitiveImage.create(
                c.getResourceResolver().resolveDrawableRes(imageRes),
                ImageView.ScaleType.CENTER_CROP,
                true,
                null,
                null,
                FlexboxStyleKt.aspectRatio(Style.Companion, 1f)))
        .build();
  }
}
// end_example
