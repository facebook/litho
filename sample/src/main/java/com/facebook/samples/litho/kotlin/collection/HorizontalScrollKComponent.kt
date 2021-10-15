// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.samples.litho.kotlin.collection

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

import android.graphics.Color
import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sp
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.widget.Text

// start_example
class HorizontalScrollKComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    return Collection(
        recyclerConfiguration =
            ListRecyclerConfiguration.create().orientation(OrientationHelper.HORIZONTAL).build(),
        itemDecoration = ItemSpacingDecorator(10.dp.toPixels()),
        topPadding = 10.dp,
        style = Style.height(100.dp),
    ) {
      (0..10).forEach {
        child(id = it) {
          Text(
              text = "$it",
              textSize = 24.sp,
              style = Style.padding(30.dp).backgroundColor(Color.LTGRAY))
        }
      }
    }
  }
}
// end_example

/*
 * Apply spacing to each item in the Collection. Note we cannot achieve this by applying margin to
 * each component because they are root components so margin is ignored.
 */
private class ItemSpacingDecorator(
    @Px private val horizontal: Int = 0,
    @Px private val vertical: Int = 0,
) : RecyclerView.ItemDecoration() {
  override fun getItemOffsets(
      outRect: Rect,
      view: View,
      parent: RecyclerView,
      state: RecyclerView.State
  ) =
      with(outRect) {
        if (parent.getChildAdapterPosition(view) == 0) {
          left = horizontal
          top = vertical
        }
        right = horizontal
        bottom = vertical
      }
}
