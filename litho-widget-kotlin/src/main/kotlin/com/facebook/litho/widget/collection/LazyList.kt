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

package com.facebook.litho.widget.collection

import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.Handle
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.widget.LithoRecyclerView
import com.facebook.litho.widget.SnapUtil
import com.facebook.rendercore.Dimen

/** A scrollable collection of components arranged linearly */
@Suppress("FunctionName")
inline fun ResourcesScope.LazyList(
    @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
    @SnapUtil.SnapMode snapMode: Int = SnapUtil.SNAP_NONE,
    reverse: Boolean = false,
    crossAxisWrapMode: CrossAxisWrapMode = CrossAxisWrapMode.NoWrap,
    mainAxisWrapContent: Boolean = false,
    itemAnimator: RecyclerView.ItemAnimator? = null,
    itemDecoration: RecyclerView.ItemDecoration? = null,
    clipToPadding: Boolean? = null,
    clipChildren: Boolean? = null,
    startPadding: Dimen? = null,
    endPadding: Dimen? = null,
    topPadding: Dimen? = null,
    bottomPadding: Dimen? = null,
    nestedScrollingEnabled: Boolean? = null,
    scrollBarStyle: Int? = null,
    recyclerViewId: Int? = null,
    overScrollMode: Int? = null,
    refreshProgressBarColor: Int? = null,
    touchInterceptor: LithoRecyclerView.TouchInterceptor? = null,
    itemTouchListener: RecyclerView.OnItemTouchListener? = null,
    sectionTreeTag: String? = null,
    startupLogger: LithoStartupLogger? = null,
    style: Style? = null,
    noinline onViewportChanged: OnViewportChanged? = null,
    noinline onDataBound: (() -> Unit)? = null,
    handle: Handle? = null,
    noinline onPullToRefresh: (() -> Unit)? = null,
    onNearEnd: OnNearCallback? = null,
    onScrollListener: RecyclerView.OnScrollListener? = null,
    onScrollListeners: List<RecyclerView.OnScrollListener?>? = null,
    lazyCollectionController: LazyCollectionController? = null,
    noinline onDataRendered: OnDataRendered? = null,
    rangeRatio: Float? = null,
    useBackgroundChangeSets: Boolean = false,
    isReconciliationEnabled: Boolean =
        if (ComponentsConfiguration.isLazyListUsingComponentContextReconciliationConfig) {
          context.lithoConfiguration.isReconciliationEnabled
        } else {
          false
        },
    preallocationPerMountContentEnabled: Boolean =
        context.lithoConfiguration.preallocationPerMountContentEnabled,
    childEquivalenceIncludesCommonProps: Boolean = true,
    overlayRenderCount: Boolean = false,
    alwaysDetectDuplicates: Boolean = false,
    fadingEdgeLength: Dimen? = null,
    init: LazyListScope.() -> Unit
): Component {
  val lazyListScope = LazyListScope(context).apply { init() }
  return LazyCollection(
      layout =
          CollectionLayouts.Linear(
              orientation = orientation,
              snapMode = snapMode,
              reverse = reverse,
              rangeRatio = rangeRatio,
              useBackgroundChangeSets = useBackgroundChangeSets,
              isReconciliationEnabled = isReconciliationEnabled,
              preallocationPerMountContentEnabled = preallocationPerMountContentEnabled,
              crossAxisWrapMode = crossAxisWrapMode,
              mainAxisWrapContent = mainAxisWrapContent),
      itemAnimator,
      itemDecoration,
      clipToPadding,
      clipChildren,
      startPadding,
      endPadding,
      topPadding,
      bottomPadding,
      nestedScrollingEnabled,
      scrollBarStyle,
      recyclerViewId,
      overScrollMode,
      refreshProgressBarColor,
      touchInterceptor,
      itemTouchListener,
      sectionTreeTag,
      startupLogger,
      style,
      onViewportChanged,
      onDataBound,
      handle,
      onPullToRefresh,
      onNearEnd,
      onScrollListener,
      onScrollListeners,
      lazyCollectionController,
      onDataRendered,
      childEquivalenceIncludesCommonProps,
      overlayRenderCount,
      alwaysDetectDuplicates,
      fadingEdgeLength,
      lazyListScope.children)
}
