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

package com.facebook.samples.litho.kotlin.bordereffects

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.Text
import java.lang.reflect.InvocationTargetException

@LayoutSpec
object BorderEffectsComponentSpec {

  private val componentsToBuild =
      listOf(
          AlternateColorBorder::class.java,
          AlternateWidthBorder::class.java,
          AlternateColorWidthBorder::class.java,
          RtlColorWidthBorder::class.java,
          DashPathEffectBorder::class.java,
          VerticalDashPathEffectBorder::class.java,
          AlternateColorPathEffectBorder::class.java,
          AlternateColorCornerPathEffectBorder::class.java,
          CompositePathEffectBorder::class.java,
          VaryingRadiiBorder::class.java)

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component =
      RecyclerCollectionComponent.create(c)
          .disablePTR(true)
          .section(
              DataDiffSection.create<Class<out Component>>(SectionContext(c))
                  .data(componentsToBuild)
                  .renderEventHandler(BorderEffectsComponent.onRender(c))
                  .build())
          .build()

  @OnEvent(RenderEvent::class)
  fun onRender(c: ComponentContext, @FromEvent model: Class<out Component>): RenderInfo {
    val component =
        try {
          val createMethod = model.getMethod("create", ComponentContext::class.java)
          val componentBuilder = createMethod.invoke(null, c) as Component.Builder<*>
          componentBuilder.build()
        } catch (ex: Exception) {
          val textComponent = Text.create(c).textSizeDip(32f).text(ex.localizedMessage).build()

          when (ex) {
            is NoSuchMethodException,
            is IllegalAccessException,
            is IllegalArgumentException,
            is InvocationTargetException -> textComponent
            else -> textComponent
          }
        }

    return ComponentRenderInfo.create().component(component).build()
  }
}
