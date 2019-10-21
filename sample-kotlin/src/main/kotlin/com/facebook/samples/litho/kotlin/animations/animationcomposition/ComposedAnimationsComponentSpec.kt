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

package com.facebook.samples.litho.kotlin.animations.animationcomposition

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo

@LayoutSpec
object ComposedAnimationsComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      RecyclerCollectionComponent.create(c)
          .disablePTR(true)
          .section(
              DataDiffSection.create<Data>(SectionContext(c))
                  .data(generateData(20))
                  .renderEventHandler(ComposedAnimationsComponent.onRender(c))
                  .onCheckIsSameItemEventHandler(ComposedAnimationsComponent.isSameItem(c))
                  .build())
          .build()

  @OnEvent(RenderEvent::class)
  fun onRender(c: ComponentContext, @FromEvent index: Int): RenderInfo {
    val numDemos = 5
    // Keep alternating between demos
    val component: Component = when (index % numDemos) {
      0 -> StoryFooterComponent.create(c).key("footer").build()
      1 -> UpDownBlocksComponent.create(c).build()
      2 -> LeftRightBlocksComponent.create(c).build()
      3 -> OneByOneLeftRightBlocksComponent.create(c).build()
      4 -> LeftRightBlocksSequenceComponent.create(c).build()
      else -> throw RuntimeException("Bad index: $index")
    }
    return ComponentRenderInfo.create().component(component).build()
  }

  @OnEvent(OnCheckIsSameItemEvent::class)
  fun isSameItem(
      c: ComponentContext,
      @FromEvent previousItem: Data,
      @FromEvent nextItem: Data
  ): Boolean = previousItem.number == nextItem.number

  private fun generateData(number: Int): List<Data> {
    val dummyData = mutableListOf<Data>()

    for (i in 0 until number) {
      dummyData.add(Data(i))
    }

    return dummyData
  }
}
