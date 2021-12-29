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

package com.facebook.samples.litho.kotlin.lithography.sections

import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.facebook.samples.litho.kotlin.lithography.components.SingleImageComponent

@GroupSectionSpec
object ImagesSectionSpec {

  @OnCreateChildren
  fun onCreateChildren(c: SectionContext, @Prop images: List<String>): Children =
      Children.create()
          .child(
              DataDiffSection.create<String>(c)
                  .data(images)
                  .renderEventHandler(ImagesSection.onRender(c)))
          .build()

  @OnEvent(RenderEvent::class)
  fun onRender(c: SectionContext, @FromEvent model: String): RenderInfo =
      ComponentRenderInfo.create()
          .component(SingleImageComponent(imageUri = model, imageAspectRatio = 2f))
          .build()
}
