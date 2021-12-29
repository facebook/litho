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
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.common.SingleComponentSection
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.facebook.samples.litho.kotlin.lithography.components.DecadeSeparator
import com.facebook.samples.litho.kotlin.lithography.components.FeedItemCard
import com.facebook.samples.litho.kotlin.lithography.data.Artist
import com.facebook.samples.litho.kotlin.lithography.data.Decade

@GroupSectionSpec
object DecadeSectionSpec {

  @OnCreateChildren
  fun onCreateChildren(c: SectionContext, @Prop decade: Decade): Children =
      Children.create()
          .child(
              SingleComponentSection.create(c)
                  .component(DecadeSeparator(decade = decade))
                  .sticky(true))
          .child(
              DataDiffSection.create<Artist>(c)
                  .data(decade.artists)
                  .renderEventHandler(DecadeSection.render(c))
                  .onCheckIsSameItemEventHandler(DecadeSection.isSameItem(c)))
          .build()

  @OnEvent(RenderEvent::class)
  fun render(c: SectionContext, @FromEvent model: Artist): RenderInfo =
      ComponentRenderInfo.create().component(FeedItemCard(artist = model)).build()

  @OnEvent(OnCheckIsSameItemEvent::class)
  fun isSameItem(
      c: SectionContext,
      @FromEvent previousItem: Artist,
      @FromEvent nextItem: Artist
  ): Boolean = previousItem.name == nextItem.name
}
