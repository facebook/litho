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

package com.facebook.samples.litho.kotlin.collection

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.flexbox.flex
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.sections.widget.Collection.Companion.tailPagination
import com.facebook.litho.useState
import com.facebook.litho.widget.Progress
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign

class PaginationCollectionKComponent : KComponent() {

  private val paginatedData = (0..300).chunked(50).iterator()

  override fun ComponentScope.render(): Component? {
    val list = useState { paginatedData.next() }

    return Collection(
        style = Style.flex(grow = 1f),
        pagination =
            tailPagination(offsetBeforeTailFetch = 10) {
              if (paginatedData.hasNext()) {
                list.update(list.value + paginatedData.next())
              }
            },
    ) {
      list.value.forEach { child(id = it) { Text("$it") } }

      if (paginatedData.hasNext()) {
        staticChild {
          Column(alignItems = YogaAlign.CENTER) {
            child(Progress.create(context).heightDip(50f).widthDip(50f).build())
          }
        }
      }
    }
  }
}
