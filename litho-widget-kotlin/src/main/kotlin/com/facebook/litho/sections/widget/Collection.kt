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

import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ContainerDsl
import com.facebook.litho.Dimen
import com.facebook.litho.Handle
import com.facebook.litho.KComponent
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.Style
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.kotlinStyle
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.Section
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.OnCheckIsSameContentEvent
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.LithoRecylerView
import com.facebook.litho.widget.RecyclerBinder.HANDLE_CUSTOM_ATTR_KEY
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.SmoothScrollAlignmentType

typealias OnViewportChanged =
    (
        c: ComponentContext,
        firstVisibleIndex: Int,
        lastVisibleIndex: Int,
        totalCount: Int,
        firstFullyVisibleIndex: Int,
        lastFullyVisibleIndex: Int) -> Unit

/**
 * A scrollable collection of components. A single [Component] can be added using
 * [CollectionContainerScope.child].
 * ```
 * Collection {
 *   child { Text(text = "Foo") }
 * }
 * ```
 *
 * When adding a list, specify an id for each child for automatic diffing.
 * ```
 * Collection {
 *   list.forEach {
 *     child(id = it.id) { Text(text = it.name) }
 *   }
 * ```
 */
class Collection(
    private val recyclerConfiguration: RecyclerConfiguration? = null,
    private val itemAnimator: RecyclerView.ItemAnimator? = null,
    private val itemDecoration: RecyclerView.ItemDecoration? = null,
    private val wrapContentCrossAxis: Boolean = false,
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
    private val touchInterceptor: LithoRecylerView.TouchInterceptor? = null,
    private val itemTouchListener: RecyclerView.OnItemTouchListener? = null,
    private val sectionTreeTag: String? = null,
    private val startupLogger: LithoStartupLogger? = null,
    private val style: Style? = null,
    private val onViewportChanged: OnViewportChanged? = null,
    private val onDataBound: ((c: ComponentContext) -> Unit)? = null,
    handle: Handle? = null,
    private val onPullToRefresh: (() -> Unit)? = null,
    private val pagination: ((lastVisibleIndex: Int, totalCount: Int) -> Unit)? = null,
    private val onScrollListener: RecyclerView.OnScrollListener? = null,
    private val init: CollectionContainerScope.() -> Unit
) : KComponent() {

  // There's a conflict with Component.handle, so use a different name
  private val recyclerHandle: Handle? = handle

  override fun ComponentScope.render(): Component? {
    val sectionContext = SectionContext(context)
    val containerScope = CollectionContainerScope()
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
            .childrenBuilder(containerScope.getChildren(sectionContext))
            .apply { onDataBound?.let { onDataBound(it) } }
            .onViewportChanged(combinedOnViewportChanged)
            .onPullToRefresh(onPullToRefresh)
            .build()

    return CollectionRecycler.create(context)
        .section(section)
        .apply { recyclerConfiguration?.let { recyclerConfiguration(recyclerConfiguration) } }
        .itemAnimator(itemAnimator)
        .itemDecoration(itemDecoration)
        .canMeasureRecycler(wrapContentCrossAxis)
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
        .kotlinStyle(style)
        .build()
  }

  companion object {
    fun scrollTo(c: ComponentContext, handle: Handle, position: Int): Unit =
        CollectionRecycler.onScroll(c, handle, position)

    fun scrollToHandle(
        c: ComponentContext,
        handle: Handle,
        target: Handle,
        @Px offset: Int = 0,
    ): Unit = CollectionRecycler.onScrollToHandle(c, handle, target, offset)

    fun smoothScrollTo(
        c: ComponentContext,
        handle: Handle,
        index: Int,
        @Px offset: Int = 0,
        smoothScrollAlignmentType: SmoothScrollAlignmentType? = SmoothScrollAlignmentType.DEFAULT,
    ): Unit = CollectionRecycler.onSmoothScroll(c, handle, index, offset, smoothScrollAlignmentType)

    fun smoothScrollToHandle(
        c: ComponentContext,
        handle: Handle,
        target: Handle,
        @Px offset: Int = 0,
        smoothScrollAlignmentType: SmoothScrollAlignmentType? = SmoothScrollAlignmentType.DEFAULT,
    ): Unit =
        CollectionRecycler.onSmoothScrollToHandle(
            c, handle, target, offset, smoothScrollAlignmentType)

    fun clearRefreshing(c: ComponentContext, handle: Handle): Unit =
        CollectionRecycler.onClearRefreshing(c, handle)

    /**
     * Create a manager for tail pagination, i.e. fetch more data when a [Collection] is scrolled
     * near to the end. Should be applied to [Collection]'s pagination prop.
     * @param offsetBeforeTailFetch trigger a fetch at some offset before the end of the list
     * @param fetchNextPage lambda to perform the data fetch
     */
    fun tailPagination(
        offsetBeforeTailFetch: Int = 0,
        fetchNextPage: () -> Unit
    ): (Int, Int) -> Unit {
      return { lastVisibleIndex: Int, totalCount: Int ->
        if (lastVisibleIndex >= totalCount - 1 - offsetBeforeTailFetch) {
          fetchNextPage()
        }
      }
    }
  }
}

@ContainerDsl
class CollectionContainerScope {

  private data class CollectionData(
      val id: Any? = null,
      val component: Component? = null,
      val renderInfo: RenderInfo? = null,
      val deps: Array<Any?>? = null,
      val section: Section? = null,
  )
  private val collectionChildrenModels = mutableListOf<CollectionData>()
  private var nextStaticId = 0

  fun child(
      id: Any? = null,
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
      deps: Array<Any?>? = null,
      componentFunction: () -> Component?
  ) {
    val component = componentFunction()
    collectionChildrenModels.add(
        CollectionData(
            id ?: generateStaticId(),
            component,
            ComponentRenderInfo.create()
                .apply {
                  if (isSticky) {
                    isSticky(isSticky)
                  }
                  if (isFullSpan) {
                    isFullSpan(isFullSpan)
                  }
                  spanSize?.let { spanSize(it) }
                  component?.handle?.let { customAttribute(HANDLE_CUSTOM_ATTR_KEY, it) }
                }
                .component(component)
                .build(),
            deps))
  }

  /** This is a temporary api, that will soon be removed. Please do not use it */
  fun section_DO_NOT_USE(section: Section) {
    collectionChildrenModels.add(CollectionData(section = section))
  }

  private fun createDataDiffSection(
      sectionContext: SectionContext,
      forDataDiffSection: List<CollectionData>
  ): Section {
    return DataDiffSection.create<CollectionData>(sectionContext)
        .data(forDataDiffSection.toList())
        .renderEventHandler(eventHandlerWithReturn { it.model.renderInfo })
        .onCheckIsSameItemEventHandler(eventHandlerWithReturn(::isSameID))
        .onCheckIsSameContentEventHandler(eventHandlerWithReturn(::isComponentEquivalent))
        .build()
  }

  internal fun getChildren(sectionContext: SectionContext): Children.Builder {
    val children = Children.create()
    val forDataDiffSection = mutableListOf<CollectionData>()
    collectionChildrenModels.forEach { item ->
      if (item.section != null) {
        children.child(createDataDiffSection(sectionContext, forDataDiffSection))
        forDataDiffSection.clear()
        children.child(item.section)
      } else {
        forDataDiffSection.add(item)
      }
    }
    if (forDataDiffSection.isNotEmpty()) {
      children.child(createDataDiffSection(sectionContext, forDataDiffSection))
    }
    return children
  }

  private fun isSameID(event: OnCheckIsSameItemEvent<CollectionData>): Boolean {
    return event.previousItem.id == event.nextItem.id
  }

  private fun isComponentEquivalent(event: OnCheckIsSameContentEvent<CollectionData>): Boolean {
    val previousItemDeps = event.previousItem.deps
    val nextItemDeps = event.nextItem.deps

    if (previousItemDeps == null || nextItemDeps == null) {
      if (event.previousItem.component?.isEquivalentTo(event.nextItem.component) == false) {
        return false
      }
      return event.previousItem.component?.commonProps?.isEquivalentTo(
          event.nextItem.component?.commonProps)
          ?: false
    }

    return event.previousItem.deps?.contentDeepEquals(event.nextItem.deps) ?: false
  }

  private inline fun generateStaticId(): Any {
    return "staticId:${nextStaticId++}"
  }
}
