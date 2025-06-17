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

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentsSystrace
import com.facebook.litho.KComponent
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.config.PrimitiveRecyclerBinderStrategy
import com.facebook.litho.onCleanup
import com.facebook.litho.sections.widget.NoUpdateItemAnimator
import com.facebook.litho.sections.widget.RecyclerConfiguration
import com.facebook.litho.useCallback
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import com.facebook.litho.widget.ChangeSetCompleteCallback
import com.facebook.litho.widget.CollectionPrimitiveViewScroller
import com.facebook.litho.widget.LithoRecyclerView.OnAfterLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.OnBeforeLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.TouchInterceptor
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.SectionsRecyclerView.SectionsRecyclerViewLogger
import com.facebook.rendercore.PoolScope

/** A component that renders a list of items using a [RecyclerBinder]. */
class CollectionRecyclerComponent(
    private val children: List<CollectionChild>,
    private val componentRenderer: (index: Int, model: CollectionChild) -> RenderInfo,
    private val contentComparator:
        (previousItem: CollectionChild, nextItem: CollectionChild) -> Boolean,
    private val idComparator: (previousItem: CollectionChild, nextItem: CollectionChild) -> Boolean,
    private val recyclerConfiguration: RecyclerConfiguration,
    private val clipChildren: Boolean = true,
    private val clipToPadding: Boolean = true,
    private val bottomPadding: Int = 0,
    private val endPadding: Int = 0,
    private val fadingEdgeLength: Int = 0,
    private val horizontalFadingEdgeEnabled: Boolean = false,
    private val isBottomFadingEnabled: Boolean = true,
    private val isLeftFadingEnabled: Boolean = true,
    private val isRightFadingEnabled: Boolean = true,
    private val isTopFadingEnabled: Boolean = true,
    private val itemAnimator: RecyclerView.ItemAnimator? = null,
    private val itemDecoration: RecyclerView.ItemDecoration? = null,
    private val itemTouchListener: RecyclerView.OnItemTouchListener? = null,
    private val lazyCollectionController: LazyCollectionController? = null,
    private val nestedScrollingEnabled: Boolean = true,
    private val onAfterLayoutListener: OnAfterLayoutListener? = null,
    private val onBeforeLayoutListener: OnBeforeLayoutListener? = null,
    private val onDataBound: OnDataBound? = null,
    private val onDataRendered: OnDataRendered? = null,
    private val onPullToRefresh: (() -> Unit)? = null,
    private val onScrollListeners: List<RecyclerView.OnScrollListener?>? = null,
    private val onViewportChanged: OnViewportChanged? = null,
    private val overScrollMode: Int = View.OVER_SCROLL_ALWAYS,
    private val pullToRefreshEnabled: Boolean = false,
    private val recyclerViewId: Int = View.NO_ID,
    private val refreshProgressBarBackgroundColor: Int? = null,
    private val refreshProgressBarColor: Int = Color.BLACK,
    private val scrollBarStyle: Int = View.SCROLLBARS_INSIDE_OVERLAY,
    private val sectionsViewLogger: SectionsRecyclerViewLogger? = null,
    private val shouldExcludeFromIncrementalMount: Boolean = false,
    private val startPadding: Int = 0,
    private val startupLogger: LithoStartupLogger? = null,
    private val style: Style? = null,
    private val topPadding: Int = 0,
    private val touchInterceptor: TouchInterceptor? = null,
    private val verticalFadingEdgeEnabled: Boolean = false,
) : KComponent() {

  @OptIn(ExperimentalLithoApi::class)
  override fun ComponentScope.render(): Component {
    val latestCommittedData = useState { LatestCommittedData() }.value
    val poolScope = useState { PoolScope.ManuallyManaged() }.value
    val viewportChangedCallback =
        useCallback {
            firstVisibleIndex: Int,
            lastVisibleIndex: Int,
            itemCount: Int,
            firstFullyVisibleIndex: Int,
            lastFullyVisibleIndex: Int ->
          onViewportChanged?.invoke(
              firstVisibleIndex,
              lastVisibleIndex,
              itemCount,
              firstFullyVisibleIndex,
              lastFullyVisibleIndex)
        }
    val binder =
        useState {
              RecyclerBinder.Builder()
                  .layoutInfo(recyclerConfiguration.getLayoutInfo(context))
                  .startupLogger(startupLogger)
                  .recyclerBinderConfig(
                      recyclerConfiguration.recyclerBinderConfiguration.recyclerBinderConfig)
                  .poolScope(poolScope)
                  .build(context)
                  .apply {
                    setViewportChangedListener {
                        firstVisibleIndex,
                        lastVisibleIndex,
                        firstFullyVisibleIndex,
                        lastFullyVisibleIndex,
                        _ ->
                      viewportChangedCallback(
                          firstVisibleIndex,
                          lastVisibleIndex,
                          getItemCount(),
                          firstFullyVisibleIndex,
                          lastFullyVisibleIndex)
                    }
                  }
            }
            .value

    val recyclerEventsController = useState { RecyclerEventsController() }.value
    val collectionPrimitiveViewScroller =
        useState { CollectionPrimitiveViewScroller(context.androidContext) }.value

    // This calculates the diff between the previous and new data to determine what changes need to
    // be made to the RecyclerView. It's performed as a best-effort calculation on the background
    // thread without synchronization locks since this data might be discarded if new updates come
    // in before it's applied.
    val updateCallback =
        calculateDiff(
            previousData = latestCommittedData.data,
            nextData = children,
            renderer = componentRenderer,
            sameItemComparator = idComparator,
            sameContentComparator = contentComparator)

    useEffect(Any()) {
      lazyCollectionController?.recyclerEventsController = recyclerEventsController
      lazyCollectionController?.scrollerDelegate = RecyclerScroller(collectionPrimitiveViewScroller)

      val resolvedUpdateCallback =
          if (updateCallback.prevData !== latestCommittedData.data) {
            // If the data has changed since the last diff calculation, we need to re-calculate the
            // result to make sure we're always applying the latest changeset.
            calculateDiff(
                previousData = latestCommittedData.data,
                nextData = children,
                renderer = componentRenderer,
                sameItemComparator = idComparator,
                sameContentComparator = contentComparator)
          } else {
            updateCallback
          }

      latestCommittedData.data = resolvedUpdateCallback.nextData
      resolvedUpdateCallback.submitTo(
          recyclerBinder = binder, onDataRendered = onDataRendered, onDataBound = onDataBound)

      onCleanup {
        lazyCollectionController?.recyclerEventsController = null
        lazyCollectionController?.scrollerDelegate = null
      }
    }
    useEffect(Unit) { onCleanup { poolScope.releaseScope() } }

    val internalPullToRefreshEnabled =
        (recyclerConfiguration.orientation != OrientationHelper.HORIZONTAL && pullToRefreshEnabled)
    val componentsConfiguration = context.lithoConfiguration.componentsConfig
    val primitiveRecyclerBinderStrategy =
        recyclerConfiguration.recyclerBinderConfiguration.primitiveRecyclerBinderStrategy
            ?: componentsConfiguration.primitiveRecyclerBinderStrategy

    /*
     * This is a temporary solution while we experiment with offering the same behavior regarding
     * the default item animators as in
     * [com.facebook.litho.sections.widget.RecyclerCollectionComponent].
     *
     * This is needed because we will have a crash if we re-use the same animator instance across
     * different RV instances. In this approach we identify if the client opted by using the
     * "default" animator, and if so, it will pass on a new instance of the same type, to avoid a
     * crash that happens due to re-using the same instances in different RVs.
     */
    val itemAnimatorToUse =
        when (itemAnimator) {
          CollectionRecyclerSpec.itemAnimator -> {
            if (context.lithoConfiguration.componentsConfig
                .useDefaultItemAnimatorInLazyCollections &&
                context.lithoConfiguration.componentsConfig.primitiveRecyclerBinderStrategy ==
                    PrimitiveRecyclerBinderStrategy.SPLIT_BINDERS) {
              NoUpdateItemAnimator()
            } else {
              null
            }
          }
          else -> itemAnimator
        }

    return Recycler(
        binderStrategy = primitiveRecyclerBinderStrategy,
        binder = binder,
        bottomPadding = bottomPadding,
        excludeFromIncrementalMount = shouldExcludeFromIncrementalMount,
        fadingEdgeLength = fadingEdgeLength,
        isBottomFadingEnabled = isBottomFadingEnabled,
        isClipChildrenEnabled = clipChildren,
        isClipToPaddingEnabled = clipToPadding,
        isHorizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled,
        isLeftFadingEnabled = isLeftFadingEnabled,
        isNestedScrollingEnabled = nestedScrollingEnabled,
        isPullToRefreshEnabled = internalPullToRefreshEnabled,
        isRightFadingEnabled = isRightFadingEnabled,
        isTopFadingEnabled = isTopFadingEnabled,
        isVerticalFadingEdgeEnabled = verticalFadingEdgeEnabled,
        itemAnimator = itemAnimatorToUse,
        itemDecorations = itemDecoration?.let { listOf(it) },
        leftPadding = startPadding,
        onAfterLayoutListener = onAfterLayoutListener,
        onBeforeLayoutListener = onBeforeLayoutListener,
        onItemTouchListener = itemTouchListener,
        onRefresh =
            if (internalPullToRefreshEnabled && onPullToRefresh != null) {
              onPullToRefresh
            } else {
              null
            },
        onScrollListeners = onScrollListeners?.filterNotNull(),
        overScrollMode = overScrollMode,
        recyclerEventsController = recyclerEventsController,
        recyclerViewId = recyclerViewId,
        refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
        refreshProgressBarColor = refreshProgressBarColor,
        rightPadding = endPadding,
        scrollBarStyle = scrollBarStyle,
        sectionsViewLogger = sectionsViewLogger,
        snapHelper = recyclerConfiguration.snapHelper,
        topPadding = topPadding,
        touchInterceptor = touchInterceptor,
        style = style)
  }

  companion object {

    /**
     * Calculates the diff between two lists of items and applies the changeset to the
     * [CollectionUpdateCallback].
     */
    private fun <T> calculateDiff(
        previousData: List<T>?,
        nextData: List<T>?,
        renderer: (index: Int, model: T) -> RenderInfo,
        sameItemComparator: ((previousItem: T, nextItem: T) -> Boolean)? = null,
        sameContentComparator: ((previousItem: T, nextItem: T) -> Boolean)? = null,
    ): CollectionUpdateCallback<T> {
      try {
        if (ComponentsSystrace.isTracing) {
          ComponentsSystrace.beginSection("diffing")
        }

        val listUpdateCallback =
            CollectionUpdateCallback(prevData = previousData, nextData = nextData)
        val diffCallback =
            CollectionDiffCallback(
                previousData, nextData, sameItemComparator, sameContentComparator)
        val result = DiffUtil.calculateDiff(diffCallback)
        result.dispatchUpdatesTo(listUpdateCallback)

        return listUpdateCallback
      } finally {
        if (ComponentsSystrace.isTracing) {
          ComponentsSystrace.endSection()
        }
      }
    }

    /* Submit the changeset to the recycler binder. */
    private fun CollectionUpdateCallback<*>.submitTo(
        recyclerBinder: RecyclerBinder,
        onDataBound: OnDataBound? = null,
        onDataRendered: OnDataRendered? = null,
    ) {
      try {
        if (ComponentsSystrace.isTracing) {
          ComponentsSystrace.beginSection("submit changeset")
        }
        for (operation in operations) {
          // For INSERT and UPDATE operations, we must have render infos.
          val renderInfos: List<RenderInfo> = operation.renderInfoList

          when (operation.type) {
            CollectionOperation.Type.INSERT -> {
              if (operation.count > 1) {
                recyclerBinder.insertRangeAt(operation.index, renderInfos)
              } else {
                recyclerBinder.insertItemAt(operation.index, renderInfos[0])
              }
            }
            CollectionOperation.Type.DELETE -> {
              if (operation.count > 1) {
                recyclerBinder.removeRangeAt(operation.index, operation.count)
              } else {
                recyclerBinder.removeItemAt(operation.index)
              }
            }
            CollectionOperation.Type.MOVE -> {
              recyclerBinder.moveItem(operation.index, operation.toIndex)
            }
            CollectionOperation.Type.UPDATE -> {
              if (operation.count > 1) {
                recyclerBinder.updateRangeAt(operation.index, renderInfos)
              } else {
                recyclerBinder.updateItemAt(operation.index, renderInfos[0])
              }
            }
          }
        }

        val isDataChanged = operations.isNotEmpty()
        val changeSetCompleteCallback =
            object : ChangeSetCompleteCallback {
              override fun onDataBound() {
                if (!isDataChanged) {
                  return
                }
                onDataBound?.invoke()
              }

              override fun onDataRendered(isMounted: Boolean, uptimeMillis: Long) {
                // Leverage a combined callback from LazyCollections to return the correct index
                onDataRendered?.invoke(isDataChanged, isMounted, uptimeMillis, -1, -1)
              }
            }

        recyclerBinder.notifyChangeSetComplete(isDataChanged, changeSetCompleteCallback)
      } finally {
        if (ComponentsSystrace.isTracing) {
          ComponentsSystrace.endSection()
        }
      }
    }
  }

  private class LatestCommittedData(@Volatile var data: List<CollectionChild>? = null)
}
