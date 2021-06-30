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

package com.facebook.litho.sections.widget;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
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
import java.util.List;

@GroupSectionSpec
class CollectionDataDiffSectionSpec {

  public interface ItemRenderer<T> {

    Component render(ComponentContext c, T model);

    boolean checkIsSameItem(T previous, T next);

    boolean checkIsSameContent(T previous, T next);
  }

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c, @Prop List data) {
    return Children.create()
        .child(
            DataDiffSection.create(c)
                .data(data)
                .renderEventHandler(CollectionDataDiffSection.onRender(c))
                .onCheckIsSameItemEventHandler(CollectionDataDiffSection.onCheckIsSameItem(c))
                .onCheckIsSameContentEventHandler(
                    CollectionDataDiffSection.onCheckIsSameContent(c)))
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(
      SectionContext c, @Prop ItemRenderer itemRenderer, @FromEvent Object model) {
    return ComponentRenderInfo.create().component(itemRenderer.render(c, model)).build();
  }

  @OnEvent(OnCheckIsSameItemEvent.class)
  static boolean onCheckIsSameItem(
      SectionContext c,
      @Prop ItemRenderer itemRenderer,
      @FromEvent Object previousItem,
      @FromEvent Object nextItem) {
    return itemRenderer.checkIsSameItem(previousItem, nextItem);
  }

  @OnEvent(OnCheckIsSameContentEvent.class)
  static boolean onCheckIsSameContent(
      SectionContext c,
      @Prop ItemRenderer itemRenderer,
      @FromEvent Object previousItem,
      @FromEvent Object nextItem) {
    return itemRenderer.checkIsSameContent(previousItem, nextItem);
  }
}
