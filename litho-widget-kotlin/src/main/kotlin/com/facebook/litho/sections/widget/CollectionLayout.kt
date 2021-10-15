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

package com.facebook.litho.sections.widget

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.facebook.litho.widget.SnapUtil

/** Provide configuration options to a [Collection] */
abstract class CollectionLayout(
    @RecyclerView.Orientation private val orientation: Int,
    @SnapUtil.SnapMode private val snapMode: Int,
    private val reverseLayout: Boolean,
    private val hasDynamicItemHeight: Boolean?,
) {
  abstract val recyclerConfigurationBuilder: RecyclerConfiguration.Builder

  val recyclerConfiguration: RecyclerConfiguration
    get() {
      return recyclerConfigurationBuilder
          .orientation(orientation)
          .snapMode(snapMode)
          .reverseLayout(reverseLayout)
          .apply {
            if (hasDynamicItemHeight != null) {
              recyclerBinderConfiguration(
                  RecyclerBinderConfiguration.create()
                      .hasDynamicItemHeight(hasDynamicItemHeight)
                      .build())
            }
          }
          .build()
    }
}

/** Provide [CollectionLayout]s that can be applied to [Collection]'s `layout` parameter. */
interface CollectionLayouts {

  fun Linear(
      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
      @SnapUtil.SnapMode snapMode: Int = SnapUtil.SNAP_NONE,
      reverseLayout: Boolean = false,
      hasDynamicItemHeight: Boolean? = null,
  ): CollectionLayout =
      object : CollectionLayout(orientation, snapMode, reverseLayout, hasDynamicItemHeight) {
        override val recyclerConfigurationBuilder = ListRecyclerConfiguration.Builder()
      }

  fun Grid(
      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
      @SnapUtil.SnapMode snapMode: Int = SnapUtil.SNAP_NONE,
      reverseLayout: Boolean = false,
      columns: Int = 2,
  ): CollectionLayout =
      object : CollectionLayout(orientation, snapMode, reverseLayout, false) {
        override val recyclerConfigurationBuilder =
            GridRecyclerConfiguration.Builder().numColumns(columns)
      }

  fun StaggeredGrid(
      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
      @SnapUtil.SnapMode snapMode: Int = SnapUtil.SNAP_NONE,
      reverseLayout: Boolean = false,
      spans: Int = 2,
      gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE
  ): CollectionLayout =
      object : CollectionLayout(orientation, snapMode, reverseLayout, false) {
        override val recyclerConfigurationBuilder =
            StaggeredGridRecyclerConfiguration.Builder().numSpans(spans).gapStrategy(gapStrategy)
      }
}
