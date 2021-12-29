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

package com.facebook.samples.litho.kotlin.animations.animationcomposition

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.key
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo

class ComposedAnimationsComponentKotlin : KComponent() {

  override fun ComponentScope.render(): Component? {
    return RecyclerCollectionComponent.create(context)
        .disablePTR(true)
        .section(
            DataDiffSection.create<Data>(SectionContext(context))
                .data(generateData(20))
                .renderEventHandler(eventHandlerWithReturn { e -> onRender(e.index) })
                .onCheckIsSameItemEventHandler(
                    eventHandlerWithReturn { e -> isSameItem(e.previousItem, e.nextItem) })
                .build())
        .build()
  }

  private fun onRender(index: Int): RenderInfo {
    val numDemos = 5
    // Keep alternating between demos
    val component: Component =
        when (index % numDemos) {
          0 -> key("footer") { StoryFooterComponent() }
          1 -> UpDownBlocksComponent()
          2 -> LeftRightBlocksComponent()
          3 -> OneByOneLeftRightBlocksComponent()
          4 -> LeftRightBlocksSequenceComponent()
          else -> throw RuntimeException("Bad index: $index")
        }
    return ComponentRenderInfo.create().component(component).build()
  }

  private fun isSameItem(previousItem: Data, nextItem: Data): Boolean =
      previousItem.number == nextItem.number

  private fun generateData(number: Int): List<Data> {
    val dummyData = mutableListOf<Data>()

    for (i in 0 until number) {
      dummyData.add(Data(i))
    }

    return dummyData
  }
}
