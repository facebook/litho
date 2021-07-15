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

package com.facebook.samples.litho.java.animations.animationcomposition;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;

@LayoutSpec
class ComposedAnimationsComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return RecyclerCollectionComponent.create(c)
        .disablePTR(true)
        .section(
            DataDiffSection.<Data>create(new SectionContext(c))
                .data(generateData(20))
                .renderEventHandler(ComposedAnimationsComponent.onRender(c))
                .onCheckIsSameItemEventHandler(ComposedAnimationsComponent.isSameItem(c))
                .build())
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(ComponentContext c, @FromEvent int index, @FromEvent Data model) {
    final int numDemos = 5;
    Component component;
    // Keep alternating between demos
    switch (index % numDemos) {
      case 0:
        component = StoryFooterComponent.create(c).key("footer").build();
        break;
      case 1:
        component = UpDownBlocksComponent.create(c).build();
        break;
      case 2:
        component = LeftRightBlocksComponent.create(c).build();
        break;
      case 3:
        component = OneByOneLeftRightBlocksComponent.create(c).build();
        break;
      case 4:
        component = LeftRightBlocksSequenceComponent.create(c).build();
        break;
      default:
        throw new RuntimeException("Bad index: " + index);
    }
    return ComponentRenderInfo.create().component(component).build();
  }

  @OnEvent(OnCheckIsSameItemEvent.class)
  static boolean isSameItem(
      ComponentContext c, @FromEvent Data previousItem, @FromEvent Data nextItem) {
    return previousItem.number == nextItem.number;
  }

  private static List<Data> generateData(int number) {
    List<Data> dummyData = new ArrayList<>(number);
    for (int i = 0; i < number; i++) {
      dummyData.add(new Data(i));
    }

    return dummyData;
  }

  static class Data {
    final int number;

    public Data(int number) {
      this.number = number;
    }
  }
}
