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
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.Style
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.OnCheckIsSameContentEvent
import com.facebook.litho.sections.common.OnCheckIsSameItemEvent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.LithoRecylerView
import com.facebook.litho.widget.RecyclerBinder.HANDLE_CUSTOM_ATTR_KEY
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.SmoothScrollAlignmentType
import com.facebook.litho.widget.StickyHeaderControllerFactory

/**
 * Constructs a new scrollable collection of components. A single [Component] can be added using
 * [CollectionContainerScope.child].
 * ```
 * Collection {
 *   staticChild { Text(text = "Foo") }
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
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ComponentScope.Collection(
    recyclerConfiguration: RecyclerConfiguration =
        RecyclerCollectionComponentSpec.recyclerConfiguration,
    itemAnimator: RecyclerView.ItemAnimator? = RecyclerCollectionComponentSpec.itemAnimator,
    itemDecoration: RecyclerView.ItemDecoration? = null,
    canMeasureRecycler: Boolean = false,
    loadingComponent: Component? = null,
    emptyComponent: Component? = null,
    errorComponent: Component? = null,
    recyclerViewClipToPadding: Boolean = RecyclerCollectionComponentSpec.clipToPadding,
    recyclerViewClipChildren: Boolean = RecyclerCollectionComponentSpec.clipChildren,
    recyclerViewStartPadding: Dimen? = null,
    recyclerViewEndPadding: Dimen? = null,
    recyclerViewTopPadding: Dimen? = null,
    recyclerViewBottomPadding: Dimen? = null,
    nestedScrollingEnabled: Boolean = RecyclerCollectionComponentSpec.nestedScrollingEnabled,
    scrollBarStyle: Int = RecyclerCollectionComponentSpec.scrollBarStyle,
    recyclerViewId: Int = RecyclerCollectionComponentSpec.recyclerViewId,
    overScrollMode: Int = RecyclerCollectionComponentSpec.overScrollMode,
    refreshProgressBarColor: Int = RecyclerCollectionComponentSpec.refreshProgressBarColor,
    touchInterceptor: LithoRecylerView.TouchInterceptor? = null,
    itemTouchListener: RecyclerView.OnItemTouchListener? = null,
    sectionTreeTag: String? = null,
    startupLogger: LithoStartupLogger? = null,
    stickyHeaderControllerFactory: StickyHeaderControllerFactory? = null,
    style: Style? = null,
    noinline onViewportChanged:
        ((
            c: ComponentContext,
            firstVisibleIndex: Int,
            lastVisibleIndex: Int,
            totalCount: Int,
            firstFullyVisibleIndex: Int,
            lastFullyVisibleIndex: Int) -> Unit)? =
        null,
    noinline onDataBound: ((c: ComponentContext) -> Unit)? = null,
    handle: Handle? = null,
    noinline onPullToRefresh: (() -> Unit)? = null,
    init: CollectionContainerScope.() -> Unit,
): RecyclerCollectionComponent {
  val sectionContext = SectionContext(context)
  val containerScope = CollectionContainerScope()
  containerScope.init()
  val children = Children.Builder().child(containerScope.getSection(sectionContext))
  val section =
      CollectionGroupSection.create(sectionContext)
          .childrenBuilder(children)
          .apply { onDataBound?.let { onDataBound(it) } }
          .apply { onViewportChanged?.let { onViewportChanged(it) } }
          .onPullToRefresh(onPullToRefresh)
          .build()

  return RecyclerCollectionComponent(
      section = section,
      recyclerConfiguration = recyclerConfiguration,
      itemAnimator = itemAnimator,
      itemDecoration = itemDecoration,
      canMeasureRecycler = canMeasureRecycler,
      loadingComponent = loadingComponent,
      emptyComponent = emptyComponent,
      errorComponent = errorComponent,
      recyclerViewClipToPadding = recyclerViewClipToPadding,
      recyclerViewClipChildren = recyclerViewClipChildren,
      recyclerViewStartPadding = recyclerViewStartPadding,
      recyclerViewEndPadding = recyclerViewEndPadding,
      recyclerViewTopPadding = recyclerViewTopPadding,
      recyclerViewBottomPadding = recyclerViewBottomPadding,
      disablePTR = onPullToRefresh == null,
      nestedScrollingEnabled = nestedScrollingEnabled,
      scrollBarStyle = scrollBarStyle,
      recyclerViewId = recyclerViewId,
      overScrollMode = overScrollMode,
      refreshProgressBarColor = refreshProgressBarColor,
      touchInterceptor = touchInterceptor,
      itemTouchListener = itemTouchListener,
      sectionTreeTag = sectionTreeTag,
      startupLogger = startupLogger,
      stickyHeaderControllerFactory = stickyHeaderControllerFactory,
      handle = handle,
      style = style,
  )
}

object CollectionUtils {
  fun scrollTo(c: ComponentContext, handle: Handle, position: Int): Unit =
      RecyclerCollectionComponent.onScroll(c, handle, position, false /* ignored */)

  fun scrollToHandle(
      c: ComponentContext,
      handle: Handle,
      target: Handle,
      @Px offset: Int = 0,
  ): Unit = RecyclerCollectionComponent.onScrollToHandle(c, handle, target, offset)

  fun smoothScrollTo(
      c: ComponentContext,
      handle: Handle,
      index: Int,
      @Px offset: Int = 0,
      smoothScrollAlignmentType: SmoothScrollAlignmentType? = SmoothScrollAlignmentType.DEFAULT,
  ): Unit =
      RecyclerCollectionComponent.onSmoothScroll(
          c, handle, index, offset, smoothScrollAlignmentType)

  fun smoothScrollToHandle(
      c: ComponentContext,
      handle: Handle,
      target: Handle,
      @Px offset: Int = 0,
      smoothScrollAlignmentType: SmoothScrollAlignmentType? = SmoothScrollAlignmentType.DEFAULT,
  ): Unit =
      RecyclerCollectionComponent.onSmoothScrollToHandle(
          c, handle, target, offset, smoothScrollAlignmentType)

  fun clearRefreshing(c: ComponentContext, handle: Handle) {
    RecyclerCollectionComponent.onClearRefreshing(c, handle)
  }
}

@ContainerDsl
class CollectionContainerScope {

  private data class CollectionData(
      val id: Any?,
      val listTag: Any?,
      val component: Component?,
      val renderInfo: RenderInfo,
  )
  private val collectionChildrenModels = mutableListOf<CollectionData>()
  private var nextStaticId = 0

  /**
   * Add a child where changes to position or content are not expected and will not be animated when
   * the Collection is updated.
   */
  fun staticChild(
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
      component: () -> Component?
  ) {
    childInternal(component(), generateStaticId(), null, isSticky, isFullSpan, spanSize)
  }

  fun child(
      id: Any,
      listTag: Any? = null,
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
      component: () -> Component?
  ) {
    childInternal(component(), id, listTag, isSticky, isFullSpan, spanSize)
  }

  private fun childInternal(
      component: Component?,
      id: Any,
      listTag: Any? = null,
      isSticky: Boolean = false,
      isFullSpan: Boolean = false,
      spanSize: Int? = null,
  ) {
    collectionChildrenModels.add(
        CollectionData(
            id,
            listTag,
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
                .build()))
  }

  fun getSection(sectionContext: SectionContext): DataDiffSection<*> {
    return DataDiffSection.create<CollectionData>(sectionContext)
        .data(collectionChildrenModels)
        .renderEventHandler(eventHandlerWithReturn { it.model.renderInfo })
        .onCheckIsSameItemEventHandler(eventHandlerWithReturn(::isSameID))
        .onCheckIsSameContentEventHandler(eventHandlerWithReturn(::isComponentEquivalent))
        .build()
  }

  private fun isSameID(event: OnCheckIsSameItemEvent<CollectionData>): Boolean {
    return event.previousItem.id == event.nextItem.id &&
        event.previousItem.listTag == event.nextItem.listTag
  }

  private fun isComponentEquivalent(event: OnCheckIsSameContentEvent<CollectionData>): Boolean {
    return event.previousItem.component?.isEquivalentTo(event.nextItem.component) ?: false
  }

  private inline fun generateStaticId(): Any {
    return "staticId:${nextStaticId++}"
  }
}
