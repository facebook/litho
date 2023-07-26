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
import com.facebook.litho.kotlin.widget.Progress
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.widget.collection.LazyList
import com.facebook.litho.widget.collection.OnNearCallback
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaAlign

class PaginationCollectionKComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val paginatedData = useCached {
      PagedDataWithDelay(data = (0..100).map { Item(it, "$it") }, pageSize = 40)
    }
    val list = useState { paginatedData.next() }
    return PagedExample(
        PaginatedList(
            list = list.value,
            fetchNextPage = {
              paginatedData.fetchDelayed { newData -> list.update { it + newData } }
            },
            hasNextPage = paginatedData.hasNext))
  }
}

class PaginatedList<T>(val list: List<T>, val fetchNextPage: () -> Unit, val hasNextPage: Boolean)

class Item(val id: Int, val text: String)

// start_example
class PagedExample(private val pagedList: PaginatedList<Item>) : KComponent() {
  override fun ComponentScope.render(): Component =
      LazyList(
          onNearEnd = OnNearCallback { if (pagedList.hasNextPage) pagedList.fetchNextPage() },
      ) {
        // Add the retrieved items
        children(items = pagedList.list, id = { it.id }) { Text(it.text) }

        // Optionally add a progress spinner
        if (pagedList.hasNextPage) {
          child(ProgressSpinner())
        }
      }
}
// end_example

// A progress spinner centered in a Column
class ProgressSpinner : KComponent() {
  override fun ComponentScope.render(): Component =
      Column(alignItems = YogaAlign.CENTER) {
        child(component = Progress(style = Style.height(50.dp).height(50.dp)))
      }
}

// A paged datasource with a simulated network delay
class PagedDataWithDelay<T>(data: List<T>, pageSize: Int) {
  private val pagedData = data.chunked(pageSize).iterator()
  private var isFetching = false
  val hasNext
    get() = pagedData.hasNext()

  fun next(): List<T> = pagedData.next()

  fun fetchDelayed(callback: (newData: List<T>) -> Unit) {
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
