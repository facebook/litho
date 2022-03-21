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

import android.os.Handler
import android.os.Looper
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.dp
import com.facebook.litho.kotlin.widget.Progress
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.widget.collection.LazyList
import com.facebook.litho.widget.collection.OnNearCallback
import com.facebook.yoga.YogaAlign

// start_example
class PaginationCollectionKComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val paginatedData = useCached { PaginatedDataSource(pageSize = 40) }
    val list = useState { paginatedData.next() }

    return LazyList(
        onNearEnd =
            OnNearCallback(offset = 10) {
              paginatedData.fetchDelayed { newData -> list.update { it + newData } }
            },
    ) {
      list.value.forEach { child(id = it, component = Text("$it")) }

      if (paginatedData.hasNext) {
        child(
            Column(alignItems = YogaAlign.CENTER) {
              child(component = Progress(style = Style.height(50.dp).height(50.dp)))
            })
      }
    }
  }
}
// end_example

// A paginated datasource with a simulated network delay
class PaginatedDataSource(pageSize: Int) {
  private val data = (0..150).chunked(pageSize).iterator()
  private var isFetching = false
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
            simulatedNetworkDelayMs)
  }

  companion object {
    const val simulatedNetworkDelayMs = 1000L
  }
}
