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
import com.facebook.litho.widget.RenderInfo;
import java.util.List;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@GroupSectionSpec
class CollectionDataDiffSectionSpec<TModel> {

  @OnCreateChildren
  static <TModel> Children onCreateChildren(SectionContext c, @Prop List<TModel> data) {
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
  static <TModel> RenderInfo onRender(
      SectionContext c, @Prop Function1<TModel, RenderInfo> render, @FromEvent TModel model) {
    return render.invoke(model);
  }

  @OnEvent(OnCheckIsSameItemEvent.class)
  static <TModel> boolean onCheckIsSameItem(
      SectionContext c,
      @Prop Function2<TModel, TModel, Boolean> checkIsSameItem,
      @FromEvent TModel previousItem,
      @FromEvent TModel nextItem) {
    return checkIsSameItem.invoke(previousItem, nextItem);
  }

  @OnEvent(OnCheckIsSameContentEvent.class)
  static <TModel> boolean onCheckIsSameContent(
      SectionContext c,
      @Prop Function2<TModel, TModel, Boolean> checkIsSameContent,
      @FromEvent TModel previousItem,
      @FromEvent TModel nextItem) {
    return checkIsSameContent.invoke(previousItem, nextItem);
  }
}
