/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
 * This component renders a horizontal list. The height of the list is fixed, and it's set
 * by passing a height prop to the RecyclerCollectionComponent.
 */
@Suppress("MagicNumber")
@LayoutSpec
object FixedHeightHscrollComponentSpec {

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
                .renderEventHandler(FixedHeightHscrollComponent.onRender(c))
                .build())
        .heightDip(150f)
        .build()
  }

  @OnEvent(RenderEvent::class)
  fun onRender(c: ComponentContext, @FromEvent model: Int): RenderInfo {
    return ComponentRenderInfo.create()
        .component(SolidColor.create(c).color(model).heightDip(100f).widthDip(100f))
        .build()
  }
}
