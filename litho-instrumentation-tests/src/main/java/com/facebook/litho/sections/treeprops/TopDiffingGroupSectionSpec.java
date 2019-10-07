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

package com.facebook.litho.sections.treeprops;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;

@GroupSectionSpec
public class TopDiffingGroupSectionSpec {

  @OnCreateChildren
  protected static Children onCreateChildren(SectionContext c) {
    return Children.create()
        .child(
            DataDiffSection.<Integer>create(c)
                .data(generateData(3))
                .renderEventHandler(TopDiffingGroupSection.onRender(c)))
        .build();
  }

  @OnCreateTreeProp
  static LogContext onCreateTestTreeProp(SectionContext c, @TreeProp LogContext t) {
    return LogContext.append(t, "top");
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(final SectionContext c, @FromEvent Integer model) {
    return ComponentRenderInfo.create().component(LeafComponent.create(c).viewTag(model)).build();
  }

  private static List<Integer> generateData(int count) {
    final List<Integer> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      list.add(i);
    }
    return list;
  }
}
