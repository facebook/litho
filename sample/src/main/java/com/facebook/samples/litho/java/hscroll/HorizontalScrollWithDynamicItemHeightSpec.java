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

package com.facebook.samples.litho.java.hscroll;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.sections.widget.RecyclerConfiguration;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.TextAlignment;
import com.facebook.litho.widget.VerticalGravity;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;
import java.util.Arrays;
import java.util.List;

@LayoutSpec
public class HorizontalScrollWithDynamicItemHeightSpec {

  private static final List<Integer> ITEM_HEIGHTS =
      Arrays.asList(20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120);

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    final RecyclerConfiguration recyclerConfiguration =
        ListRecyclerConfiguration.create()
            .orientation(LinearLayoutManager.HORIZONTAL)
            .recyclerBinderConfiguration(
                RecyclerBinderConfiguration.create().hasDynamicItemHeight(true).build())
            .build();

    return Column.create(c)
        .child(
            Text.create(c)
                .paddingDip(YogaEdge.VERTICAL, 8)
                .textSizeSp(32)
                .textColor(Color.DKGRAY)
                .text("Content Above HScroll")
                .alignment(TextAlignment.CENTER))
        .child(
            RecyclerCollectionComponent.create(c)
                .disablePTR(true)
                .recyclerConfiguration(recyclerConfiguration)
                .section(
                    DataDiffSection.<Integer>create(new SectionContext(c))
                        .data(ITEM_HEIGHTS)
                        .renderEventHandler(
                            HorizontalScrollWithDynamicItemHeight.<Integer>onRender(c))
                        .build())
                .canMeasureRecycler(true))
        .child(
            Text.create(c)
                .paddingDip(YogaEdge.VERTICAL, 8)
                .textSizeSp(32)
                .textColor(Color.DKGRAY)
                .text("Content Below HScroll")
                .alignment(TextAlignment.CENTER))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(ComponentContext c, @FromEvent Integer model, @FromEvent int index) {
    final int height = (int) model;
    return ComponentRenderInfo.create()
        .component(
            Column.create(c)
                .paddingDip(YogaEdge.HORIZONTAL, 8)
                .child(
                    Column.create(c)
                        .justifyContent(YogaJustify.CENTER)
                        .widthDip(120)
                        .heightDip(height)
                        .flexGrow(1)
                        .backgroundColor(hsvToColor(height, .5f, 1f))
                        .child(
                            Text.create(c)
                                .textSizeSp(20)
                                .textColor(Color.DKGRAY)
                                .text("" + height + ".dp")
                                .alignment(TextAlignment.CENTER)
                                .verticalGravity(VerticalGravity.CENTER))))
        .build();
  }

  private static @ColorInt int hsvToColor(int hue, float sat, float value) {
    return Color.HSVToColor(new float[] {hue, sat, value});
  }
}
