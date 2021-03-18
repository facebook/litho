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

package com.facebook.litho.testing.sections;

import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import java.util.List;

@GroupSectionSpec
class TestDataDiffSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(
      SectionContext c,
      @Prop List<String> data,
      @Prop(optional = true) Boolean alwaysDetectDuplicates,
      @Prop(optional = true) boolean skipCheckIsSameHandler) {
    if (skipCheckIsSameHandler) {
      return Children.create()
          .child(
              DataDiffSection.<String>create(c)
                  .data(data)
                  .renderEventHandler(TestDataDiffSection.onRender(c))
                  .alwaysDetectDuplicates(alwaysDetectDuplicates))
          .build();
    }
    return Children.create()
        .child(
            DataDiffSection.<String>create(c)
                .data(data)
                .renderEventHandler(TestDataDiffSection.onRender(c))
                .onCheckIsSameItemEventHandler(TestDataDiffSection.onCheckIsSameItem(c))
                .alwaysDetectDuplicates(alwaysDetectDuplicates))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(SectionContext c, @FromEvent String model) {
    return ComponentRenderInfo.create().component(Text.create(c).text(model).build()).build();
  }

  @OnEvent(OnCheckIsSameItemEvent.class)
  public static Boolean onCheckIsSameItem(
      SectionContext c, @FromEvent String previousItem, @FromEvent String nextItem) {
    return previousItem.equals(nextItem);
  }
}
