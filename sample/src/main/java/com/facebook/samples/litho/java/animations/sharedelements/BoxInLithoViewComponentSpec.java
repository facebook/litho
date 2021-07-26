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

package com.facebook.samples.litho.java.animations.sharedelements;

import android.graphics.Color;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;

@LayoutSpec
public class BoxInLithoViewComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .heightPx(200)
        .widthPx(200)
        .backgroundColor(Color.RED)
        .transitionName("lithoView")
        .clickHandler(BoxInLithoViewComponent.onClickEvent(c, Color.RED))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(
      ComponentContext c,
      @Prop SharedElementsFragmentActivity.FirstFragment firstFragment,
      @FromEvent View view,
      @Param int color) {
    firstFragment.nextFragment(view, color);
  }
}
