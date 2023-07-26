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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.widget.collection.LazyList
import com.facebook.litho.widget.collection.LinearSpacing
import com.facebook.rendercore.dp

// start_linear_spacing_example
class LinearSpacingExample : KComponent() {
  override fun ComponentScope.render(): Component =
      LazyList(
          itemDecoration = LinearSpacing(start = 10.dp, between = 5.dp),
      ) { /* Add children */}
}
// end_linear_spacing_example

// start_fixed_height_hscroll_example
class HScrollFixedHeight : KComponent() {
  override fun ComponentScope.render(): Component =
      LazyList(
          orientation = RecyclerView.HORIZONTAL,
          style = Style.height(100.dp),
      ) { /* Add children */}
}
// end_fixed_height_hscroll_example

@RequiresApi(Build.VERSION_CODES.N)
// start_sticky_header_example
class StickyHeader(val names: List<String>) : KComponent() {
  override fun ComponentScope.render(): Component {
    val namesGroupedByFirstLetter = names.groupBy { it.first() }
    return LazyList {
      namesGroupedByFirstLetter.forEach { (firstLetter, names) ->
        child(id = firstLetter, isSticky = true, component = Text("$firstLetter"))
        children(items = names, id = { it }) { Text(it) }
      }
    }
  }
}
// end_sticky_header_example
