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

package com.facebook.samples.lithobarebones;

import android.graphics.Color;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import com.facebook.litho.StateValue;
import java.util.Collections;
import java.util.List;

@GroupSectionSpec
class ListSectionSpec {

  @OnCreateInitialState
  static void onCreateInitialState(
      SectionContext c,
      StateValue<List<Integer>> dataList,
      @Prop MySimpleCallback callback) {
    dataList.set(generateData(32));
    callback.setListener((indexFrom, indexTo) -> ListSection.moveItem(c, indexFrom, indexTo));
  }

  @OnUpdateState
  static void moveItem(
      StateValue<List<Integer>> dataList, @Param int indexFrom, @Param int indexTo) {
    // 1. Copy current state.
    ArrayList<Integer> newData = new ArrayList<>(dataList.get());

    // 2. Move the item.
    if (indexFrom < indexTo) {
      Collections.rotate(newData.subList(indexFrom, indexTo + 1), -1);
    }
    if (indexFrom > indexTo) {
      Collections.rotate(newData.subList(indexTo, indexFrom + 1), 1);
    }

    // 3. Update state.
    dataList.set(newData);
  }

  @OnCreateChildren
  static Children onCreateChildren(final SectionContext c, @State List<Integer> dataList) {
    return Children.create()
        .child(
            DataDiffSection.<Integer>create(c)
                .data(dataList)
                .renderEventHandler(ListSection.onRender(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(final SectionContext c, @FromEvent Integer model) {
    return ComponentRenderInfo.create()
        .component(
            ListItem.create(c)
                .color(model % 2 == 0 ? Color.WHITE : Color.LTGRAY)
                .title(model + ". Hello, world!")
                .subtitle("Litho tutorial")
                .build())
        .build();
  }

  private static List<Integer> generateData(int count) {
    final List<Integer> data = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      data.add(i);
    }
    return data;
  }
}
