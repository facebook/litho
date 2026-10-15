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

package com.facebook.samples.litho.integration.listpagination

import android.os.Handler
import android.os.Looper
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Progress
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.widget.collection.LazyGrid
import com.facebook.litho.widget.collection.OnNearCallback
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/**
 * Demonstrates a paginated grid layout using LazyGrid with `onNearEnd` pagination.
 *
 * Inspired by IG's CrossAppSavesGridComponent which uses LazyGrid with column count and
 * `isFullSpan` for loading indicators.
 */
// start_example
class PaginatedGridComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val dataSource = useCached { FakeGridDataSource() }
    val items = useState { dataSource.initialLoad() }
    val isLoadingMore = useState { false }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "Paginated Grid",
              textSize = 18.sp,
              style = Style.padding(bottom = 12.dp),
          ),
      )
      child(
          LazyGrid(
              style = Style.flex(grow = 1f),
              columns = NUM_COLUMNS,
              onNearEnd =
                  OnNearCallback(offset = PAGINATION_THRESHOLD) {
                    if (dataSource.hasMore && !isLoadingMore.value) {
                      isLoadingMore.update(true)
                      dataSource.loadMore { newItems ->
                        items.update { it + newItems }
                        isLoadingMore.update(false)
                      }
                    }
                  },
          ) {
            children(items = items.value, id = { it.id }) { item -> GridTile(item = item) }
            if (isLoadingMore.value) {
              child(
                  id = "loading",
                  isFullSpan = true,
                  component =
                      Column(
                          alignItems = YogaAlign.CENTER,
                          style = Style.padding(vertical = 16.dp),
                      ) {
                        child(Progress(style = Style.height(32.dp).width(32.dp)))
                      },
              )
            }
          },
      )
    }
  }

  companion object {
    private const val NUM_COLUMNS = 3
    private const val PAGINATION_THRESHOLD = 6
  }
}

private class GridTile(private val item: ListItem) : KComponent() {
  override fun ComponentScope.render(): Component {
    return Column(
        alignItems = YogaAlign.CENTER,
        style = Style.padding(all = 8.dp).height(80.dp),
    ) {
      child(Text(text = item.title, textSize = 14.sp))
    }
  }
}

// end_example

class FakeGridDataSource {
  private var nextId = 0
  private var pagesLoaded = 0
  private val totalPages = 4
  private val pageSize = 12

  val hasMore: Boolean
    get() = pagesLoaded < totalPages

  fun initialLoad(): List<ListItem> {
    val items = (0 until pageSize).map { ListItem(id = nextId + it, title = "Tile ${nextId + it}") }
    nextId += pageSize
    pagesLoaded++
    return items
  }

  fun loadMore(callback: (List<ListItem>) -> Unit) {
    if (!hasMore) return
    Handler(Looper.getMainLooper())
        .postDelayed(
            {
              val items =
                  (0 until pageSize).map {
                    ListItem(id = nextId + it, title = "Tile ${nextId + it}")
                  }
              nextId += pageSize
              pagesLoaded++
              callback(items)
            },
            800L,
        )
  }
}
