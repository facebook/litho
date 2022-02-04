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

package com.facebook.samples.litho.kotlin.collection

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.Dimen
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.px
import com.facebook.litho.view.background
import com.facebook.litho.widget.collection.LazyStaggeredGrid
import com.facebook.samples.litho.kotlin.drawable.RoundedRect

class StaggeredGridCollectionExample : KComponent() {

  private val colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA)
  private val baseHeightDp = 25.dp

  override fun ComponentScope.render(): Component =
      LazyStaggeredGrid(spans = 3) { (0..30).forEach { child(id = it, component = item(it)) } }

  private fun colorForIndex(index: Int): Int = colors[index % 4]

  private fun ResourcesScope.heightForIndex(index: Int): Dimen =
      (((index % 4) + 1) * baseHeightDp.toPixels()).px

  private fun ResourcesScope.item(index: Int): Component =
      Column(style = Style.padding(all = 5.dp)) {
        child(
            Column(
                style =
                    Style.background(RoundedRect(colorForIndex(index), 10.dp))
                        .height(heightForIndex(index))))
      }
}
