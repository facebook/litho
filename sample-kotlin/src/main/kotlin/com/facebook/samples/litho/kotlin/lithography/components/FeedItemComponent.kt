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

import android.graphics.Typeface.BOLD
import android.widget.LinearLayout
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.DslScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.dp
import com.facebook.litho.drawableColor
import com.facebook.litho.flexbox.aspectRatio
import com.facebook.litho.flexbox.padding
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.sp
import com.facebook.litho.widget.SnapUtil
import com.facebook.litho.widget.Text
import com.facebook.samples.litho.kotlin.lithography.data.Artist
import com.facebook.samples.litho.kotlin.lithography.sections.ImagesSection
import com.facebook.yoga.YogaPositionType

class FeedItemComponent(val artist: Artist) : KComponent() {
  override fun DslScope.render() =
      Column(
          children =
              listOf(
                  Column(
                      children =
                          listOf(
                              imageBlock(artist),
                              Text(
                                  text = artist.name,
                                  style =
                                      Style.position(start = 4.dp, bottom = 4.dp)
                                          .positionType(YogaPositionType.ABSOLUTE)
                                          .padding(horizontal = 6.dp)
                                          .background(drawableColor(0xddffffff)),
                                  textSize = 24.sp,
                                  textStyle = BOLD),
                              ActionsComponent(
                                  style =
                                      Style.position(top = 4.dp, end = 4.dp)
                                          .positionType(YogaPositionType.ABSOLUTE)),
                          )),
                  FooterComponent(text = artist.biography),
              ))
}

private val recyclerConfiguration =
    ListRecyclerConfiguration.create()
        .orientation(LinearLayout.HORIZONTAL)
        .snapMode(SnapUtil.SNAP_TO_CENTER)
        .build()

private fun DslScope.imageBlock(artist: Artist): Component =
    when (artist.images.size) {
      1 -> SingleImageComponent(imageUri = artist.images[0], imageAspectRatio = 2f)
      else -> imageRecycler(artist)
    }

private fun DslScope.imageRecycler(artist: Artist): Component =
    RecyclerCollectionComponent.create(context)
        .recyclerConfiguration(recyclerConfiguration)
        .section(ImagesSection.create(SectionContext(context)).images(artist.images).build())
        .aspectRatio(2f)
        .build()
