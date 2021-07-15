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

package com.facebook.samples.litho.java.incrementalmount;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;
import java.util.ArrayList;
import java.util.List;

@LayoutSpec
public class SimpleListComponentSpec {

  private static final List<Integer> DATA = new ArrayList<>();

  static {
    for (int i = 0; i < 100; i++) {
      DATA.add(i);
    }
  }

  @OnCreateLayout
  static Component OnCreateLayout(ComponentContext c) {
    return RecyclerCollectionComponent.create(c)
        .disablePTR(true)
        .section(
            DataDiffSection.<Integer>create(new SectionContext(c))
                .data(DATA)
                .renderEventHandler(SimpleListComponent.onRenderEvent(c))
                .build())
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRenderEvent(ComponentContext c, @FromEvent Integer model) {
    return ComponentRenderInfo.create()
        .component(Text.create(c).paddingDip(YogaEdge.ALL, 8).text("Item " + model))
        .build();
  }
}
