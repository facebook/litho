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

package com.facebook.samples.lithocodelab.examples.modules;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

/**
 * Introduction to basic {@literal @}Props usage. Make your own Component with {@literal @}Props and
 * see what is generated. and how to interact with it.
 */
@LayoutSpec
public class LearningPropsComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop String text1, @Prop String text2) {
    return Column.create(c)
        .child(Text.create(c).text(text1).textSizeDip(50))
        .child(
            Text.create(c).text(text2).textColorRes(android.R.color.holo_green_dark).textSizeSp(30))
        .build();
  }
}
