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
import com.facebook.litho.widget.RecyclerBinder.HANDLE_CUSTOM_ATTR_KEY
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.SmoothScrollAlignmentType

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

/**
 * A scrollable collection of components. A single [Component] can be added using
 * [CollectionContainerScope.child].
 * ```
 * Collection {
 *   child(Text(text = "Foo"))
 * }
 * ```
 *
 * When adding a list, specify an id for each child for automatic diffing.
 * ```
 * Collection {
 *   list.forEach {
 *     child(id = it.id, component = Text(text = it.name))
 *   }
 * ```
 */
class Collection(
    private val layout: CollectionLayout = defaultLayout,
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
    private val onDataBound: ((c: ComponentContext) -> Unit)? = null,
    handle: Handle? = null,
    private val onPullToRefresh: (() -> Unit)? = null,
    private val pagination: ((lastVisibleIndex: Int, totalCount: Int) -> Unit)? = null,
    private val onScrollListener: RecyclerView.OnScrollListener? = null,
    private val onScrollListeners: List<RecyclerView.OnScrollListener?>? = null,
    // Avoid using recyclerEventsController. This is only to assist with transitioning from
    // Sections to Collections and will be removed in future.
    private val recyclerEventsController: RecyclerEventsController? = null,
    private val onDataRendered: OnDataRendered? = null,
    private val init: CollectionContainerScope.() -> Unit
) : KComponent() {

  // There's a conflict with Component.handle, so use a different name
  private val recyclerHandle: Handle? = handle

  override fun ComponentScope.render(): Component? {
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
                Children.create().child(containerScope.createDataDiffSection(sectionContext)))
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
        .handle(recyclerHandle)
        .onScrollListener(onScrollListener)
        .onScrollListeners(onScrollListeners)
        .recyclerEventsController(recyclerEventsController)
        .kotlinStyle(style)
        .build()
  }

  companion object : CollectionLayouts {

    val defaultLayout = Linear()

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
class CollectionContainerScope(override val context: ComponentContext) : ResourcesScope {

  internal data class CollectionData(
      val id: Any? = null,
      val component: Component? = null,
      val componentFunction: (() -> Component?)? = null,
      val isSticky: Boolean = false,
      val isFullSpan: Boolean = false,
      val spanSize: Int? = null,
      val deps: Array<Any?>? = null,
  )
  internal val collectionChildrenModels = mutableListOf<CollectionData>()
  private var nextStaticId = 0
  private var typeToFreq: MutableMap<Int, Int>? = null

  /** Prepare the final id that will be assigned to the child. */
  private fun getResolvedId(id: Any?, component: Component? = null): Any {
    // Generate an id that is unique to the [CollectionContainerScope] in which it was defined
    // If an id has been explicitly defined on the child, use that
    // If the child has a component generate an id including the type and frequency
    // Otherwise the child has a null component or a lambda generator, so generate an id.
    return id ?: generateIdForComponent(component) ?: generateStaticId()
  }

  /** Generate an id for a non-null Component. */
  private fun generateIdForComponent(component: Component?): Any? {
    val typeId = component?.typeId ?: return null
    if (typeToFreq == null) {
      typeToFreq = mutableMapOf()
    }

    return typeToFreq?.let {
      it[typeId] = (it[typeId] ?: 0) + 1
      "${typeId}:${it[typeId]}"
    }
  }

  fun child(
      component: Component?,
      id: Any? = null,
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
  ) {
    val resolvedId = getResolvedId(id, component)
    component ?: return
    collectionChildrenModels.add(
        CollectionData(resolvedId, component, null, isSticky, isFullSpan, spanSize, null))
  }

  fun child(
      id: Any? = null,
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
      deps: Array<Any?>,
      componentFunction: () -> Component?,
  ) {
    collectionChildrenModels.add(
        CollectionData(
            getResolvedId(id), null, componentFunction, isSticky, isFullSpan, spanSize, deps))
  }

  internal fun createDataDiffSection(sectionContext: SectionContext): Section {
    return DataDiffSection.create<CollectionData>(sectionContext)
        .data(collectionChildrenModels.toList())
        .renderEventHandler(
            eventHandlerWithReturn {
              val item = it.model
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
                    item.component?.handle?.let { customAttribute(HANDLE_CUSTOM_ATTR_KEY, it) }
                  }
                  .component(component)
                  .build()
            })
        .onCheckIsSameItemEventHandler(eventHandlerWithReturn(::isSameID))
        .onCheckIsSameContentEventHandler(eventHandlerWithReturn(::isComponentEquivalent))
        .build()
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
