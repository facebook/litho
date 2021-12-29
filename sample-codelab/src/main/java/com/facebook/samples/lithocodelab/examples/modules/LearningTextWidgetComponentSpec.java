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

import android.graphics.Color;
import android.graphics.Typeface;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.Text;

/**
 * A simple Component for learning some common {@literal @}Props on Text Components. Also a good way
 * to learn the basics of {@literal @}Props and the builders they generate.
 */
@LayoutSpec
public class LearningTextWidgetComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Text.create(c)
        .text("Hello, World!")
        .textColor(Color.RED)
        .textSizePx(70)
        .typeface(Typeface.DEFAULT_BOLD)
        .build();
  }
}
