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

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useState
import com.facebook.litho.widget.SmoothScrollAlignmentType
import com.facebook.litho.widget.collection.LazyCollectionController
import com.facebook.litho.widget.collection.LazyList

class ScrollToCollectionKComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val controller = useState { LazyCollectionController() }.value
    return Column(style = Style.padding(16.dp)) {
      child(
          Row {
            child(Button("First") { controller.scrollToIndex(0) })
            child(Button("Position 10") { controller.scrollToIndex(10) })
            child(
                Button("50 to center") {
                  controller.smoothScrollToIndex(
                      50, smoothScrollAlignmentType = SmoothScrollAlignmentType.SNAP_TO_CENTER)
                })
            child(Button("End") { controller.smoothScrollToId("End") })
          })
      child(
          LazyList(
              lazyCollectionController = controller,
              style = Style.flex(grow = 1f),
          ) {
            (0..99).forEach { child(id = it, component = Text("$it ")) }
            child(id = "End", component = Text("End"))
          })
    }
  }
}
