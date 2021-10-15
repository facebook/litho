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

import android.os.Handler
import android.os.Looper
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.dp
import com.facebook.litho.sections.widget.Collection
import com.facebook.litho.sections.widget.Collection.Companion.tailPagination
import com.facebook.litho.useRef
import com.facebook.litho.useState
import com.facebook.litho.widget.Progress
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaAlign

// start_example
class PaginationCollectionKComponent : KComponent() {

  override fun ComponentScope.render(): Component? {
    val paginatedData = useRef { PaginatedDataSource() }
    val list = useState { paginatedData.value.next() }

    return Collection(
        pagination =
            tailPagination(offsetBeforeTailFetch = 10) {
              paginatedData.value.fetchDelayed { newData -> list.update { it + newData } }
            },
    ) {
      list.value.forEach { child(id = it) { Text("$it") } }

      if (paginatedData.value.hasNext) {
        child {
          Column(alignItems = YogaAlign.CENTER) {
            child(Progress(style = Style.height(50.dp).height(50.dp)))
          }
        }
      }
    }
  }
}
// end_example

// A paginated datasource with a simulated network delay
class PaginatedDataSource {
  val data = (0..150).chunked(40).iterator()
  var isFetching = false
  val hasNext
    get() = data.hasNext()

  fun next(): List<Int> = data.next()

  fun fetchDelayed(callback: (newData: List<Int>) -> Unit) {
    if (isFetching || !hasNext) return
    isFetching = true
    Handler(Looper.getMainLooper())
        .postDelayed(
            {
              callback.invoke(next())
              isFetching = false
            },
            1000)
  }
}
