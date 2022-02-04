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
import com.facebook.litho.CommonProps
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Dimen
import com.facebook.litho.Handle
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.kotlinStyle
import com.facebook.litho.sections.ChangesInfo
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.Section
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.OnCheckIsSameContentEvent
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.LithoRecyclerView
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.RecyclerEventsController

typealias OnViewportChanged =
    (
        c: ComponentContext,
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        totalCount: Int,
        firstFullyVisibleIndex: Int,
        lastFullyVisibleIndex: Int) -> Unit

typealias OnDataRendered =
    (
        c: ComponentContext,
        isDataChanged: Boolean,
        isMounted: Boolean,
        monoTimestampMs: Long,
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        changesInfo: ChangesInfo,
        globalOffset: Int) -> Unit

fun ResourcesScope.LazyCollection(
    layout: CollectionLayout = Collection.defaultLayout,
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
    onViewportChanged: OnViewportChanged? = null,
    onDataBound: ((c: ComponentContext) -> Unit)? = null,
    handle: Handle? = null,
    onPullToRefresh: (() -> Unit)? = null,
    pagination: ((lastVisibleIndex: Int, totalCount: Int) -> Unit)? = null,
    onScrollListener: RecyclerView.OnScrollListener? = null,
    onScrollListeners: List<RecyclerView.OnScrollListener?>? = null,
    // Avoid using recyclerEventsController. This is only to assist with transitioning from
    // Sections to Collections and will be removed in future.
    recyclerEventsController: RecyclerEventsController? = null,
    onDataRendered: OnDataRendered? = null,
    init: CollectionContainerScope.() -> Unit
): Component {
  val sectionContext = SectionContext(context)
  val containerScope = CollectionContainerScope(context)
  containerScope.init()

  val combinedOnViewportChanged: OnViewportChanged =
      {
      c,
      firstVisibleIndex,
      lastVisibleIndex,
      totalCount,
      firstFullyVisibleIndex,
      lastFullyVisibleIndex ->
    pagination?.invoke(lastVisibleIndex, totalCount)
    onViewportChanged?.invoke(
        c,
        firstVisibleIndex,
        lastVisibleIndex,
        totalCount,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex)
  }
  val section =
      CollectionGroupSection.create(sectionContext)
          .childrenBuilder(
              Children.create()
                  .child(createDataDiffSection(sectionContext, containerScope.collectionChildren)))
          .apply { onDataBound?.let { onDataBound(it) } }
          .onViewportChanged(combinedOnViewportChanged)
          .onPullToRefresh(onPullToRefresh)
          .onDataRendered(onDataRendered)
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
      .handle(handle)
      .onScrollListener(onScrollListener)
      .onScrollListeners(onScrollListeners)
      .recyclerEventsController(recyclerEventsController)
      .kotlinStyle(style)
      .build()
}

private fun createDataDiffSection(
    sectionContext: SectionContext,
    children: List<CollectionChild>
): Section {
  return DataDiffSection.create<CollectionChild>(sectionContext)
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
                  item.component?.handle?.let {
                    customAttribute(RecyclerBinder.HANDLE_CUSTOM_ATTR_KEY, it)
                  }
                }
                .component(component)
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
  return first?.isEquivalentTo(second) == true
}

private fun commonPropsEquivalent(first: CommonProps?, second: CommonProps?): Boolean {
  if (first == null && second == null) return true
  return first?.isEquivalentTo(second) == true
}

private fun isChildEquivalent(event: OnCheckIsSameContentEvent<CollectionChild>): Boolean =
    isChildEquivalent(event.previousItem, event.nextItem)

fun isChildEquivalent(previous: CollectionChild, next: CollectionChild): Boolean {
  if (previous.deps != null || next.deps != null) {
    return previous.deps?.contentDeepEquals(next.deps) == true
  }

  return componentsEquivalent(previous.component, next.component) &&
      commonPropsEquivalent(previous.component?.commonProps, next.component?.commonProps)
}
