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
import com.facebook.litho.ComponentScope
import com.facebook.litho.Handle
import com.facebook.litho.KComponent
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.Style
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.kotlinStyle
import com.facebook.litho.sections.ChangesInfo
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.Section
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.OnCheckIsSameContentEvent
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.useState
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.LithoRecyclerView
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.rendercore.Dimen

typealias OnViewportChanged =
    (
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        totalCount: Int,
        firstFullyVisibleIndex: Int,
        lastFullyVisibleIndex: Int) -> Unit

typealias OnDataRendered =
    (
        isDataChanged: Boolean,
        isMounted: Boolean,
        monoTimestampMs: Long,
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        changesInfo: ChangesInfo,
        globalOffset: Int) -> Unit

class LazyCollection(
    private val layout: CollectionLayout,
    private val itemAnimator: RecyclerView.ItemAnimator? = null,
    private val itemDecoration: RecyclerView.ItemDecoration? = null,
    private val clipToPadding: Boolean? = null,
    private val clipChildren: Boolean? = null,
    private val startPadding: Dimen? = null,
    private val endPadding: Dimen? = null,
    private val topPadding: Dimen? = null,
    private val bottomPadding: Dimen? = null,
    private val nestedScrollingEnabled: Boolean? = null,
    private val scrollBarStyle: Int? = null,
    private val recyclerViewId: Int? = null,
    private val overScrollMode: Int? = null,
    private val refreshProgressBarColor: Int? = null,
    private val touchInterceptor: LithoRecyclerView.TouchInterceptor? = null,
    private val itemTouchListener: RecyclerView.OnItemTouchListener? = null,
    private val sectionTreeTag: String? = null,
    private val startupLogger: LithoStartupLogger? = null,
    private val style: Style? = null,
    private val onViewportChanged: OnViewportChanged? = null,
    private val onDataBound: (() -> Unit)? = null,
    handle: Handle? = null,
    private val onPullToRefresh: (() -> Unit)? = null,
    private val onNearEnd: OnNearCallback? = null,
    private val onScrollListener: RecyclerView.OnScrollListener? = null,
    private val onScrollListeners: List<RecyclerView.OnScrollListener?>? = null,
    private val lazyCollectionController: LazyCollectionController? = null,
    private val onDataRendered: OnDataRendered? = null,
    private val childEquivalenceIncludesCommonProps: Boolean = true,
    private val overlayRenderCount: Boolean = false,
    private val alwaysDetectDuplicates: Boolean = false,
    private val fadingEdgeLength: Dimen? = null,
    private val lazyCollectionChildren: LazyCollectionChildren
) : KComponent() {

  // There's a conflict with Component.handle, so use a different name
  private val recyclerHandle: Handle? = handle

  override fun ComponentScope.render(): Component {
    val sectionContext = SectionContext(context)
    val childTracker = useState { ChildVisibilityTracker() }.value

    val combinedOnViewportChanged: OnViewportChanged =
        {
            firstVisibleIndex,
            lastVisibleIndex,
            totalCount,
            firstFullyVisibleIndex,
            lastFullyVisibleIndex ->
          childTracker.onScrollOrUpdated(
              lazyCollectionChildren.effectiveIndexToId,
              lazyCollectionChildren.idToChild,
              firstVisibleIndex,
              lastVisibleIndex)
          onNearEnd?.let {
            if (lastVisibleIndex >= totalCount - 1 - it.offset) {
              it.callback()
            }
          }
          onViewportChanged?.invoke(
              firstVisibleIndex,
              lastVisibleIndex,
              totalCount,
              firstFullyVisibleIndex,
              lastFullyVisibleIndex)
        }

    val combinedOnDataRendered: OnDataRendered =
        {
            isDataChanged: Boolean,
            isMounted: Boolean,
            monoTimestampMs: Long,
            firstVisibleIndex: Int,
            lastVisibleIndex: Int,
            changesInfo: ChangesInfo,
            globalOffset: Int ->
          childTracker.onScrollOrUpdated(
              lazyCollectionChildren.effectiveIndexToId,
              lazyCollectionChildren.idToChild,
              firstVisibleIndex,
              lastVisibleIndex)
          onDataRendered?.invoke(
              isDataChanged,
              isMounted,
              monoTimestampMs,
              firstVisibleIndex,
              lastVisibleIndex,
              changesInfo,
              globalOffset)
        }

    val section =
        CollectionGroupSection.create(sectionContext)
            .childrenBuilder(
                Children.create()
                    .child(
                        createDataDiffSection(
                            sectionContext,
                            lazyCollectionChildren.collectionChildren,
                            alwaysDetectDuplicates)))
            .apply { onDataBound?.let { onDataBound(it) } }
            .onViewportChanged(combinedOnViewportChanged)
            .onPullToRefresh(onPullToRefresh)
            .onDataRendered(combinedOnDataRendered)
            .build()

    return CollectionRecycler.create(context)
        .section(section)
        .recyclerConfiguration(layout.recyclerConfiguration)
        .itemAnimator(itemAnimator)
        .itemDecoration(itemDecoration)
        .canMeasureRecycler(layout.canMeasureRecycler)
        .clipToPadding(clipToPadding)
        .clipChildren(clipChildren)
        .startPaddingPx(startPadding?.toPixels(resourceResolver) ?: 0)
        .endPaddingPx(endPadding?.toPixels(resourceResolver) ?: 0)
        .topPaddingPx(topPadding?.toPixels(resourceResolver) ?: 0)
        .bottomPaddingPx(bottomPadding?.toPixels(resourceResolver) ?: 0)
        .pullToRefreshEnabled(onPullToRefresh != null)
        .nestedScrollingEnabled(nestedScrollingEnabled)
        .scrollBarStyle(scrollBarStyle)
        .recyclerViewId(recyclerViewId)
        .overScrollMode(overScrollMode)
        .refreshProgressBarColor(refreshProgressBarColor)
        .touchInterceptor(touchInterceptor)
        .itemTouchListener(itemTouchListener)
        .sectionTreeTag(sectionTreeTag)
        .startupLogger(startupLogger)
        .handle(recyclerHandle)
        .onScrollListener(onScrollListener)
        .onScrollListeners(onScrollListeners)
        .lazyCollectionController(lazyCollectionController)
        .apply {
          val fadingEdgeLengthPx = fadingEdgeLength?.toPixels()
          if (fadingEdgeLengthPx != null && fadingEdgeLengthPx > 0) {
            fadingEdgeLengthPx(fadingEdgeLengthPx)
            if (layout.isVertical) {
              verticalFadingEdgeEnabled(true)
            } else {
              horizontalFadingEdgeEnabled(true)
            }
          }
        }
        .kotlinStyle(style)
        .build()
  }

  private fun createDataDiffSection(
      sectionContext: SectionContext,
      children: List<CollectionChild>,
      alwaysDetectDuplicates: Boolean,
  ): Section {
    return DataDiffSection.create<CollectionChild>(sectionContext)
        .alwaysDetectDuplicates(alwaysDetectDuplicates)
        .data(children)
        .renderEventHandler(
            eventHandlerWithReturn { renderEvent ->
              val item = renderEvent.model
              val component =
                  item.component
                      ?: item.componentFunction?.invoke() ?: return@eventHandlerWithReturn null
              ComponentRenderInfo.create()
                  .apply {
                    if (item.isSticky) {
                      isSticky(item.isSticky)
                    }
                    if (item.isFullSpan) {
                      isFullSpan(item.isFullSpan)
                    }
                    item.spanSize?.let { spanSize(it) }
                    customAttribute(RecyclerBinder.ID_CUSTOM_ATTR_KEY, item.id)
                  }
                  .component(
                      if (ComponentsConfiguration.isDebugModeEnabled && overlayRenderCount)
                          component.overlayRenderCount
                      else component)
                  .build()
            })
        .onCheckIsSameItemEventHandler(eventHandlerWithReturn(::isSameID))
        .onCheckIsSameContentEventHandler(eventHandlerWithReturn(::isChildEquivalent))
        .build()
  }

  private fun isSameID(event: OnCheckIsSameItemEvent<CollectionChild>): Boolean {
    return event.previousItem.id == event.nextItem.id
  }

  private fun componentsEquivalent(first: Component?, second: Component?): Boolean {
    if (first == null && second == null) return true
    return first?.isEquivalentTo(second, childEquivalenceIncludesCommonProps) == true
  }

  private fun isChildEquivalent(event: OnCheckIsSameContentEvent<CollectionChild>): Boolean =
      isChildEquivalent(event.previousItem, event.nextItem)

  fun isChildEquivalent(previous: CollectionChild, next: CollectionChild): Boolean {
    if (previous.deps != null || next.deps != null) {
      return previous.deps?.contentDeepEquals(next.deps) == true
    }

    return componentsEquivalent(previous.component, next.component)
  }
}

/**
 * Track which children are visible after an update or scroll, and trigger any necessary callbacks.
 */
private class ChildVisibilityTracker {

  private var previouslyVisibleIds: Set<Any> = setOf()

  fun onScrollOrUpdated(
      effectiveIndexToId: Map<Int, MutableSet<Any>>,
      idToChild: Map<Any, CollectionChild>,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int
  ) {
    val visibleIds =
        mutableSetOf<Any>()
            .apply {
              (firstVisibleIndex..lastVisibleIndex).forEach { visibleIndex ->
                effectiveIndexToId[visibleIndex]?.let { ids -> addAll(ids) }
              }
            }
            .toSet()

    val enteredIds = visibleIds - previouslyVisibleIds
    enteredIds.forEach { id -> idToChild[id]?.onNearViewport?.callback?.invoke() }

    previouslyVisibleIds = visibleIds
  }
}
