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

package com.facebook.litho.sections.widget

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.facebook.litho.widget.SnapUtil

/**
 * Provide layout configuration options to a [Collection]
 * @param orientation @see [RecyclerView.Orientation]
 * @param snapMode How contents snaps to position after a scroll @see [SnapUtil.SnapMode]
 * @param reverse Reverse item traversal and layout order @see
 * [LinearLayoutManager#setReverseLayout]
 */
abstract class CollectionLayout(
    @RecyclerView.Orientation orientation: Int,
    reverse: Boolean,
    hasDynamicItemHeight: Boolean = false,
    val canMeasureRecycler: Boolean = false,
    mainAxisWrapContent: Boolean = false,
) {
  internal abstract fun createRecyclerConfigurationBuilder(): RecyclerConfiguration.Builder

  val recyclerConfiguration: RecyclerConfiguration =
      createRecyclerConfigurationBuilder()
          .orientation(orientation)
          .reverseLayout(reverse)
          .recyclerBinderConfiguration(
              RecyclerBinderConfiguration.create()
                  .useBackgroundChangeSets(true)
                  .apply {
                    if (hasDynamicItemHeight) {
                      hasDynamicItemHeight(hasDynamicItemHeight)
                    }
                  }
                  .wrapContent(mainAxisWrapContent)
                  .build())
          .build()
}

/** Specify how a [Collection] will wrap its contents across the main axis. */
enum class CrossAxisWrapMode(val canMeasureRecycler: Boolean, val hasDynamicItemHeight: Boolean) {
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

  /**
   * Provide layout configuration options for a linear [Collection].
   * @param orientation @see CollectionLayout
   * @param snapMode @see CollectionLayout
   * @param reverse @see CollectionLayout
   * @param crossAxisWrapMode Specify how the [Collection] will wrap its contents across the main
   * axis.
   * @param mainAxisWrapContent If set, the size of the [Collection] along the main axis will match
   * the size of its children
   */
  fun Linear(
      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
      @SnapUtil.SnapMode snapMode: Int = SnapUtil.SNAP_NONE,
      reverse: Boolean = false,
      crossAxisWrapMode: CrossAxisWrapMode = CrossAxisWrapMode.NoWrap,
      mainAxisWrapContent: Boolean = false,
  ): CollectionLayout =
      object :
          CollectionLayout(
              orientation,
              reverse,
              crossAxisWrapMode.hasDynamicItemHeight,
              crossAxisWrapMode.canMeasureRecycler,
              mainAxisWrapContent,
          ) {
        override fun createRecyclerConfigurationBuilder(): RecyclerConfiguration.Builder =
            ListRecyclerConfiguration.Builder().snapMode(snapMode)
      }

  /**
   * Provide layout configuration options for a grid [Collection].
   * @param orientation @see CollectionLayout
   * @param snapMode @see CollectionLayout
   * @param reverse @see CollectionLayout
   * @param columns Number of columns in the grid
   */
  fun Grid(
      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
      @SnapUtil.SnapMode snapMode: Int = SnapUtil.SNAP_NONE,
      reverse: Boolean = false,
      columns: Int = 2,
  ): CollectionLayout =
      object : CollectionLayout(orientation, reverse) {
        override fun createRecyclerConfigurationBuilder(): RecyclerConfiguration.Builder =
            GridRecyclerConfiguration.Builder().snapMode(snapMode).numColumns(columns)
      }

  /**
   * Provide layout configuration options for a staggered grid [Collection].
   * @param orientation @see CollectionLayout
   * @param snapMode @see CollectionLayout
   * @param reverse @see CollectionLayout
   * @param spans Number of spans in the grid
   * @param gapStrategy @see [StaggeredGridLayoutManager#setGapStrategy]
   */
  fun StaggeredGrid(
      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
      reverse: Boolean = false,
      spans: Int = 2,
      gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE
  ): CollectionLayout =
      object : CollectionLayout(orientation, reverse) {
        override fun createRecyclerConfigurationBuilder(): RecyclerConfiguration.Builder =
            StaggeredGridRecyclerConfiguration.Builder().numSpans(spans).gapStrategy(gapStrategy)
      }
}
