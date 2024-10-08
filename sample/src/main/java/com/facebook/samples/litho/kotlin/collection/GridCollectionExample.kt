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
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.flexbox.aspectRatio
import com.facebook.litho.view.background
import com.facebook.litho.widget.collection.LazyGrid
import com.facebook.rendercore.dp
import com.facebook.samples.litho.kotlin.drawable.RoundedRect

class GridCollectionExample : KComponent() {

  private val colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA)

  override fun ComponentScope.render(): Component {
    val paddingToUse = 4.dp

    return Column(style = Style.padding(horizontal = paddingToUse)) {
      child(
          LazyGrid(
              columns = 2,
              topPadding = paddingToUse,
              bottomPadding = paddingToUse,
              clipToPadding = false) {
                children(items = (0..31), id = { it }) {
                  item(it, Style.padding(all = paddingToUse))
                }
              })
    }
  }

  private fun ResourcesScope.item(index: Int, style: Style): Component =
      Column(style = style) {
        child(
            Column(
                style = Style.background(RoundedRect(colorForIndex(index), 10.dp)).aspectRatio(1f)))
      }

  private fun colorForIndex(index: Int): Int = colors[index % 4]
}
