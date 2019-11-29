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
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.SolidColor

/**
 * Renders a horizontal list who gets its height by measuring the first item in the list.
 * This option is enabled by enabling the `canMeasureRecycler` prop on the
 * RecyclerCollectionComponent.
 */
@LayoutSpec
object MeasureFirstItemForHeightHscrollComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop colors: List<Int>): Component {

    return RecyclerCollectionComponent.create(c)
        .recyclerConfiguration(
            ListRecyclerConfiguration.create()
                .orientation(OrientationHelper.HORIZONTAL)
                .build())
        .section(
            DataDiffSection.create<Int>(SectionContext(c))
                .data(colors)
                .renderEventHandler(MeasureFirstItemForHeightHscrollComponent.onRender(c))
                .build())
        .canMeasureRecycler(true)
        .build()
  }

  @OnEvent(RenderEvent::class)
  fun onRender(c: ComponentContext, @FromEvent model: Int, @FromEvent index: Int): RenderInfo {
    if (index == 0) {
      return ComponentRenderInfo.create()
          .component(SolidColor.create(c).color(model).heightDip(100f).widthDip(100f))
          .build()
    }

    if (index == 1) {
      return ComponentRenderInfo.create()
          .component(SolidColor.create(c).color(model).heightDip(200f).widthDip(100f))
          .build()
    }

    return ComponentRenderInfo.create()
        .component(SolidColor.create(c).color(model).heightDip(50f).widthDip(100f))
        .build()
  }
}
