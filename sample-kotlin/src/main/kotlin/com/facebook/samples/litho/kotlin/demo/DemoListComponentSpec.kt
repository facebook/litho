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

package com.facebook.samples.litho.kotlin.demo

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.OnCheckIsSameContentEvent
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import java.util.Arrays

@LayoutSpec
object DemoListComponentSpec {

  private val MAIN_SCREEN = "main_screen"

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop dataModels: List<DemoListDataModel>): Component =
      RecyclerCollectionComponent.create(c)
          .section(
              DataDiffSection.create<DemoListDataModel>(SectionContext(c))
                  .data(dataModels)
                  .renderEventHandler(DemoListComponent.onRender(c))
                  .onCheckIsSameItemEventHandler(DemoListComponent.isSameItem(c))
                  .onCheckIsSameContentEventHandler(DemoListComponent.isSameContent(c))
                  .build())
          .disablePTR(true)
          .testKey(MAIN_SCREEN)
          .build()

  @OnEvent(RenderEvent::class)
  fun onRender(
      c: ComponentContext,
      @Prop parentIndices: IntArray?,
      @FromEvent model: DemoListDataModel,
      @FromEvent index: Int
  ): RenderInfo =
      ComponentRenderInfo.create()
          .component(
              DemoListItemComponent.create(c)
                  .model(model)
                  .currentIndices(getUpdatedIndices(parentIndices, index))
                  .build())
          .build()

  private fun getUpdatedIndices(parentIndices: IntArray?, currentIndex: Int): IntArray =
      if (parentIndices == null) {
        intArrayOf(currentIndex)
      } else {
        val updatedIndices = Arrays.copyOf(parentIndices, parentIndices.size + 1)
        updatedIndices[parentIndices.size] = currentIndex
        updatedIndices
      }

  /**
   * Called during DataDiffSection's diffing to determine if two objects represent the same item.
   * See [androidx.recyclerview.widget.DiffUtil.Callback.areItemsTheSame] for more info.
   *
   * @return true if the two objects in the event represent the same item.
   */
  @OnEvent(OnCheckIsSameItemEvent::class)
  fun isSameItem(
      c: ComponentContext,
      @FromEvent previousItem: DemoListDataModel,
      @FromEvent nextItem: DemoListDataModel
  ): Boolean = previousItem === nextItem

  /**
   * Called during DataDiffSection's diffing to determine if two objects contain the same data. This
   * is used to detect of contents of an item have changed. See [ ]
   * [androidx.recyclerview.widget.DiffUtil.Callback.areContentsTheSame] for more info.
   *
   * @return true if the two objects contain the same data.
   */
  @OnEvent(OnCheckIsSameContentEvent::class)
  fun isSameContent(
      c: ComponentContext,
      @FromEvent previousItem: DemoListDataModel?,
      @FromEvent nextItem: DemoListDataModel?
  ): Boolean =
      // We're only displaying the name so checking if that's equal here is enough for our use case.
      if (previousItem == null) {
        nextItem == null
      } else {
        nextItem?.name == previousItem.name
      }
}
