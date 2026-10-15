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
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
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
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.collection.LazyList
import com.facebook.litho.widget.collection.OnNearCallback
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/**
 * Demonstrates a paginated list with loading, error, and empty states using sealed UiState
 * dispatch.
 *
 * Key patterns from IG's CommentListComponent:
 * - LazyList with `onNearEnd` for pagination triggers
 * - Sealed UiState dispatch for loading/loaded/empty/error
 * - Loading footer while fetching next page
 */
// start_example
sealed interface ListUiState {
  data object Loading : ListUiState

  @DataClassGenerate
  data class Loaded(
      val items: List<ListItem>,
      val isLoadingMore: Boolean,
      val hasMore: Boolean,
  ) : ListUiState

  data object Empty : ListUiState

  @DataClassGenerate data class Error(val message: String, val isRetryable: Boolean) : ListUiState
}

@DataClassGenerate data class ListItem(val id: Int, val title: String)

class PaginatedListComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val dataSource = useCached { FakePagedDataSource() }
    val uiState = useState<ListUiState> { ListUiState.Loading }

    val loadPage = useCached {
      fun() {
        dataSource.loadPage { result ->
          result.fold(
              onSuccess = { items ->
                if (items.isEmpty()) {
                  uiState.update { current ->
                    if (current is ListUiState.Loaded && current.items.isNotEmpty()) {
                      current.copy(isLoadingMore = false, hasMore = false)
                    } else {
                      ListUiState.Empty
                    }
                  }
                } else {
                  uiState.update { current ->
                    val existingItems =
                        if (current is ListUiState.Loaded) current.items else emptyList()
                    ListUiState.Loaded(
                        items = existingItems + items,
                        isLoadingMore = false,
                        hasMore = dataSource.hasMore,
                    )
                  }
                }
              },
              onFailure = { error ->
                uiState.update(
                    ListUiState.Error(
                        message = error.message ?: "Unknown error",
                        isRetryable = true,
                    ),
                )
              },
          )
        }
      }
    }

    // Initial load
    useEffect(Unit) {
      loadPage()
      null
    }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "Paginated List with UiState",
              textSize = 18.sp,
              style = Style.padding(bottom = 12.dp),
          ),
      )
      child(
          when (val state = uiState.value) {
            is ListUiState.Loading -> {
              Column(alignItems = YogaAlign.CENTER, style = Style.padding(vertical = 32.dp)) {
                child(Progress(style = Style.width(48.dp).height(48.dp)))
              }
            }
            is ListUiState.Loaded -> {
              LazyList(
                  style = Style.flex(grow = 1f),
                  onNearEnd =
                      OnNearCallback(offset = 3) {
                        if (state.hasMore && !state.isLoadingMore) {
                          uiState.update { current ->
                            if (current is ListUiState.Loaded) current.copy(isLoadingMore = true)
                            else current
                          }
                          loadPage()
                        }
                      },
              ) {
                children(items = state.items, id = { it.id }) { item ->
                  Text(
                      text = item.title,
                      textSize = 16.sp,
                      style = Style.padding(vertical = 8.dp),
                  )
                }
                if (state.isLoadingMore) {
                  child(
                      id = "loading_footer",
                      component =
                          Column(
                              alignItems = YogaAlign.CENTER,
                              style = Style.padding(vertical = 16.dp),
                          ) {
                            child(Progress(style = Style.width(32.dp).height(32.dp)))
                          },
                  )
                }
              }
            }
            is ListUiState.Empty -> {
              Column(alignItems = YogaAlign.CENTER, style = Style.padding(vertical = 32.dp)) {
                child(Text(text = "No items found", textSize = 16.sp))
              }
            }
            is ListUiState.Error -> {
              Column(style = Style.padding(vertical = 16.dp)) {
                child(Text(text = "Error: ${state.message}", textSize = 16.sp))
                if (state.isRetryable) {
                  child(
                      Text(
                          text = "Retry",
                          textSize = 16.sp,
                          style =
                              Style.padding(top = 8.dp).onClick {
                                uiState.update(ListUiState.Loading)
                                dataSource.reset()
                                loadPage()
                              },
                      ),
                  )
                }
              }
            }
          },
      )
    }
  }
}

// end_example

class FakePagedDataSource {
  private var nextId = 0
  private var pagesLoaded = 0
  private val totalPages = 5
  private val pageSize = 10
  private var isFetching = false

  val hasMore: Boolean
    get() = pagesLoaded < totalPages

  fun reset() {
    nextId = 0
    pagesLoaded = 0
    isFetching = false
  }

  fun loadPage(callback: (Result<List<ListItem>>) -> Unit) {
    if (isFetching || !hasMore) return
    isFetching = true
    Handler(Looper.getMainLooper())
        .postDelayed(
            {
              val items =
                  (0 until pageSize).map { i ->
                    ListItem(id = nextId + i, title = "Item ${nextId + i}")
                  }
              nextId += pageSize
              pagesLoaded++
              isFetching = false
              callback(Result.success(items))
            },
            1000L,
        )
  }
}
