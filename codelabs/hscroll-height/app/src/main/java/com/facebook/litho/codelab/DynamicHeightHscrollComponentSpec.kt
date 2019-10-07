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

package com.facebook.litho.codelab

import androidx.recyclerview.widget.OrientationHelper
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo

/**
 * This component renders a horizontal list with items of various heights, which can adapt height
 * to always accommodate the height of the tallest item. If the height of the h-scroll is already
 * taller than the highest item it will not shrink to fit.
 * Measuring the height this way is extremely inefficient.
 */
@Suppress("MagicNumber")
@LayoutSpec
object DynamicHeightHscrollComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop colors: List<Int>): Component {

    return RecyclerCollectionComponent.create(c)
        .recyclerConfiguration(
            ListRecyclerConfiguration.create()
                .recyclerBinderConfiguration(
                    RecyclerBinderConfiguration.create()
                        .hasDynamicItemHeight(true) // This enables dynamic height measurement.
                        .build())
                .orientation(OrientationHelper.HORIZONTAL).build())
        .section(
            DataDiffSection.create<Int>(SectionContext(c))
                .data(colors)
                .renderEventHandler(DynamicHeightHscrollComponent.onRender(c))
                .build())
        .canMeasureRecycler(true)
        .build()
  }

  @OnEvent(RenderEvent::class)
  fun onRender(c: ComponentContext, @FromEvent model: Int, @FromEvent index: Int): RenderInfo {
    if (index == 0) {
      return ComponentRenderInfo.create()
          .component(
              ExpandibleComponent.create(c)
                  .color(model)
                  .collapseHeight(50f)
                  .expandHeight(250f)
                  .initialHeight(100f)
                  .widthDip(100f))
          .build()
    }

    if (index == 1) {
      return ComponentRenderInfo.create()
          .component(
              ExpandibleComponent.create(c)
                  .color(model)
                  .collapseHeight(50f)
                  .expandHeight(200f)
                  .initialHeight(200f)
                  .widthDip(100f))
          .build()
    }

    return ComponentRenderInfo.create()
        .component(
            ExpandibleComponent.create(c)
                .color(model)
                .collapseHeight(50f)
                .expandHeight(250f)
                .initialHeight(50f)
                .widthDip(100f))
        .build()
  }
}
