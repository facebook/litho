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
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.SingleComponentSection
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.LithoRecylerView
import com.facebook.litho.widget.RecyclerBinder.HANDLE_CUSTOM_ATTR_KEY
import com.facebook.litho.widget.SmoothScrollAlignmentType
import com.facebook.litho.widget.StickyHeaderControllerFactory

/**
 * Constructs a new scrollable collection of components. A single [Component] can be added using
 * [CollectionContainerScope.item].
 * ```
 * Collection() {
 *   item(Text(text = "Foo"))
 * }
 * ```
 *
 * A list can be added with a renderer function to convert each item into a [Component] using
 * [CollectionContainerScope.items].
 * ```
 * Collection() {
 * items(
 *   data = arrayListOf("Foo", "Bar"),
 *   renderer = { item, _ -> Text(text = item) })
 * }
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
  val containerScope = CollectionContainerScope(context)
  containerScope.init()
  val section =
      CollectionGroupSection.create(containerScope.sectionContext)
          .childrenBuilder(containerScope.childrenBuilder)
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
class CollectionContainerScope(componentContext: ComponentContext) {

  val childrenBuilder: Children.Builder = Children.Builder()
  val sectionContext: SectionContext = SectionContext(componentContext)

  /** Adds a Component as a child to the collection being initialized. */
  fun item(component: Component, sticky: Boolean = false) {
    childrenBuilder.child(
        SingleComponentSection.create(sectionContext)
            .apply {
              component.handle?.let {
                customAttributes(mapOf(Pair(HANDLE_CUSTOM_ATTR_KEY, component.handle)))
              }
            }
            .sticky(sticky)
            .component(component)
            .build())
  }

  /** Adds a List of Components as children to the collection being initialized. */
  fun items(components: List<Component>) {
    components.forEach { item(it) }
  }

  /**
   * Adds a list of models and a renderer function to convert each model into a component.
   * @param data A list of models
   * @param render A function that converts a model into a component
   * @isSameItem Used during diffing. Determine if two models represent the same item in the
   * collection
   * @isSameContent Used during diffing. Determine if two models that represent the same item also
   * have the same content (and therefore does not need to be updated).
   */
  fun <T> items(
      data: List<T>,
      isSameItem: (previous: T, next: T) -> Boolean,
      isSameContent: (previous: T, next: T) -> Boolean = { previous, next -> previous == next },
      render: (item: T) -> Component,
  ) {
    childrenBuilder.child(
        CollectionDataDiffSection.create<T>(sectionContext)
            .data(data)
            .render { ComponentRenderInfo.create().component(render(it)).build() }
            .checkIsSameItem(isSameItem)
            .checkIsSameContent(isSameContent)
            .build())
  }

  /** Create an isSameItem parameter for items(..) for model comparison using a unique id field. */
  fun <T> itemId(getField: (T) -> Any?): (previous: T?, next: T?) -> Boolean {
    return { previous, next ->
      if (previous === null || next === null) false else getField(previous) == getField(next)
    }
  }
}
