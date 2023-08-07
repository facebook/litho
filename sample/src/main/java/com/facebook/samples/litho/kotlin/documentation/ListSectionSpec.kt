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

package com.facebook.samples.litho.kotlin.documentation

import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.OnCheckIsSameContentEvent
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.Text

// start_data_type
class Model(val id: String, val field1: String, val field2: String)
// end_data_type

// start_example
@GroupSectionSpec
object ListSectionSpec {

  @OnCreateChildren
  fun onCreateChildren(c: SectionContext, @Prop models: List<Model>): Children =
      Children.create()
          .child(
              DataDiffSection.create<Model>(c)
                  .data(models)
                  .renderEventHandler(ListSection.onRenderItem(c))
                  .onCheckIsSameItemEventHandler(ListSection.onCheckIsSameItem(c))
                  .onCheckIsSameContentEventHandler(ListSection.onCheckIsSameContent(c)))
          .build()

  @OnEvent(RenderEvent::class)
  fun onRenderItem(c: SectionContext, @FromEvent model: Model): RenderInfo =
      ComponentRenderInfo.create()
          // highlight-start
          .component(Text.create(c).text("${model.field1} ${model.field2}").build())
          // highlight-end
          .build()

  @OnEvent(OnCheckIsSameItemEvent::class)
  fun onCheckIsSameItem(
      c: SectionContext,
      @FromEvent previousItem: Model,
      @FromEvent nextItem: Model
  ): Boolean =
      // highlight-start
      previousItem.id == nextItem.id
  // highlight-end

  @OnEvent(OnCheckIsSameContentEvent::class)
  fun onCheckIsSameContent(
      sectionContext: SectionContext,
      @FromEvent previousItem: Model,
      @FromEvent nextItem: Model
  ): Boolean = previousItem.field1 == nextItem.field1 && previousItem.field2 == nextItem.field2
}
// end_example
