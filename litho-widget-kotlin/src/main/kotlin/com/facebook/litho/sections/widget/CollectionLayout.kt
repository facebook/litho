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
    private val hasDynamicItemHeight: Boolean = false,
    val canMeasureRecycler: Boolean = false,
) {
  internal abstract fun createRecyclerConfigurationBuilder(): RecyclerConfiguration.Builder

  val recyclerConfiguration: RecyclerConfiguration =
      createRecyclerConfigurationBuilder()
          .orientation(orientation)
          .snapMode(snapMode)
          .reverseLayout(reverseLayout)
          .recyclerBinderConfiguration(
              RecyclerBinderConfiguration.create()
                  .useBackgroundChangeSets(true)
                  .apply {
                    if (hasDynamicItemHeight) {
                      hasDynamicItemHeight(hasDynamicItemHeight)
                    }
                  }
                  .build())
          .build()
}

enum class WrapMode(val canMeasureRecycler: Boolean, val hasDynamicItemHeight: Boolean) {
  /** No wrapping specified. The size should be specified on the [Collection]'s style parameter. */
  NoWrap(false, false),

  /** The cross axis dimension will match the first child in the [Collection] */
  MatchFirstChild(true, false),

  /**
   * The cross axis dimension will match the largest item in the [Collection]. Measuring all the
   * children comes with a high performance cost, especially for infinite scrolls. This should only
   * be used if absolutely necessary.
   */
  Dynamic(true, true),
}

/** Provide [CollectionLayout]s that can be applied to [Collection]'s `layout` parameter. */
interface CollectionLayouts {

  fun Linear(
      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
      @SnapUtil.SnapMode snapMode: Int = SnapUtil.SNAP_NONE,
      reverseLayout: Boolean = false,
      wrapMode: WrapMode = WrapMode.NoWrap,
  ): CollectionLayout =
      object :
          CollectionLayout(
              orientation,
              snapMode,
              reverseLayout,
              wrapMode.hasDynamicItemHeight,
              wrapMode.canMeasureRecycler) {
        override fun createRecyclerConfigurationBuilder(): RecyclerConfiguration.Builder =
            ListRecyclerConfiguration.Builder()
      }

  fun Grid(
      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
      @SnapUtil.SnapMode snapMode: Int = SnapUtil.SNAP_NONE,
      reverseLayout: Boolean = false,
      columns: Int = 2,
  ): CollectionLayout =
      object : CollectionLayout(orientation, snapMode, reverseLayout) {
        override fun createRecyclerConfigurationBuilder(): RecyclerConfiguration.Builder =
            GridRecyclerConfiguration.Builder().numColumns(columns)
      }

  fun StaggeredGrid(
      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
      @SnapUtil.SnapMode snapMode: Int = SnapUtil.SNAP_NONE,
      reverseLayout: Boolean = false,
      spans: Int = 2,
      gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE
  ): CollectionLayout =
      object : CollectionLayout(orientation, snapMode, reverseLayout) {
        override fun createRecyclerConfigurationBuilder(): RecyclerConfiguration.Builder =
            StaggeredGridRecyclerConfiguration.Builder().numSpans(spans).gapStrategy(gapStrategy)
      }
}
