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

package com.facebook.samples.litho.kotlin.bordereffects

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo

class BorderEffectsComponentKotlin : KComponent() {

  private val components =
      listOf(
          AllBorder(),
          AlternateColorBorder(),
          AlternateWidthBorder(),
          AlternateColorWidthBorder(),
          RtlColorWidthBorder(),
          DashPathEffectBorder(),
          VerticalDashPathEffectBorder(),
          AlternateColorPathEffectBorder(),
          AlternateColorCornerPathEffectBorder(),
          CompositePathEffectBorder(),
          VaryingRadiiBorder())

  override fun ComponentScope.render(): Component {
    return RecyclerCollectionComponent(
        section =
            DataDiffSection.create<Component>(SectionContext(context))
                .data(components)
                .renderEventHandler(
                    eventHandlerWithReturn { event: RenderEvent<out Component> -> onRender(event) })
                .build(),
        disablePTR = true)
  }

  private fun onRender(event: RenderEvent<out Component>): RenderInfo {
    return ComponentRenderInfo.create().component(event.model).build()
  }
}
