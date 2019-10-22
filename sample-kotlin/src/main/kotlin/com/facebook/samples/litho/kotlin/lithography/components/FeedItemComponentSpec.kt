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

package com.facebook.samples.litho.kotlin.lithography.components

import android.graphics.Typeface
import android.widget.LinearLayout
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.build
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.SnapUtil.SNAP_TO_CENTER
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.kotlin.lithography.data.Artist
import com.facebook.samples.litho.kotlin.lithography.sections.ImagesSection
import com.facebook.yoga.YogaEdge.BOTTOM
import com.facebook.yoga.YogaEdge.HORIZONTAL
import com.facebook.yoga.YogaEdge.LEFT
import com.facebook.yoga.YogaPositionType.ABSOLUTE

@LayoutSpec
object FeedItemComponentSpec {

  private val recyclerConfiguration = ListRecyclerConfiguration.create()
      .orientation(LinearLayout.HORIZONTAL)
      .snapMode(SNAP_TO_CENTER)
      .build()

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop artist: Artist
  ): Component = build(c) {
    Column {
      +Column {
        +imageBlock(artist)
        +Text.create(c)
            .text(artist.name)
            .textStyle(Typeface.BOLD)
            .textSizeDip(24f)
            .backgroundColor(0xDDFFFFFF.toInt())
            .positionType(ABSOLUTE)
            .positionDip(BOTTOM, 4f)
            .positionDip(LEFT, 4f)
            .paddingDip(HORIZONTAL, 6f)
        +ActionsComponent.create(c)
      }
      +FooterComponent.create(c).text(artist.biography)
    }
  }

  private fun ComponentContext.imageBlock(artist: Artist): Component.Builder<*> =
      when (artist.images.size) {
        1 -> singleImage(artist)
        else -> imageRecycler(artist)
      }

  private fun ComponentContext.imageRecycler(artist: Artist): Component.Builder<*> =
      RecyclerCollectionComponent.create(this)
          .recyclerConfiguration(recyclerConfiguration)
          .section(
              ImagesSection.create(SectionContext(this))
                  .images(artist.images)
                  .build())
          .aspectRatio(2f)

  private fun ComponentContext.singleImage(artist: Artist): Component.Builder<*> =
      SingleImageComponent.create(this)
          .imageUri(artist.images[0])
          .imageAspectRatio(2f)
}
