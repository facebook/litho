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

package com.facebook.samples.litho.java.changesetdebug;

import com.facebook.litho.Row;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.OnCheckIsSameContentEvent;
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.Text;
import java.util.List;

@GroupSectionSpec
public class FavouriteGroupSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c, @Prop List<DataModel> dataModels) {
    return Children.create()
        .child(
            DataDiffSection.<DataModel>create(new SectionContext(c))
                .data(dataModels)
                .renderEventHandler(FavouriteGroupSection.onRender(c))
                .onCheckIsSameContentEventHandler(FavouriteGroupSection.isSameContent(c))
                .onCheckIsSameItemEventHandler(FavouriteGroupSection.isSameItem(c))
                .build())
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(SectionContext c, @FromEvent DataModel model) {
    return ComponentRenderInfo.create()
        .component(
            Row.create(c)
                .child(Text.create(c).text(model.getData()).textSizeDip(30))
                .child(RowItem.create(c))
                .build())
        .build();
  }

  @OnEvent(OnCheckIsSameItemEvent.class)
  static boolean isSameItem(
      SectionContext context, @FromEvent DataModel previousItem, @FromEvent DataModel nextItem) {
    return previousItem.getId() == nextItem.getId();
  }

  @OnEvent(OnCheckIsSameContentEvent.class)
  static boolean isSameContent(
      SectionContext context, @FromEvent DataModel previousItem, @FromEvent DataModel nextItem) {
    return previousItem.getData().equals(nextItem.getData());
  }
}
