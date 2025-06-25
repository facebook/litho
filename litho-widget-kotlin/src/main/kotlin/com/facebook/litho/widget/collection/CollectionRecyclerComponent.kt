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
import android.os.SystemClock
import android.view.View
import androidx.annotation.UiThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.SnapHelper
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentsSystrace
import com.facebook.litho.KComponent
import com.facebook.litho.LithoRenderTreeView
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.PrimitiveComponentScope
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
import com.facebook.litho.widget.CollectionItem
import com.facebook.litho.widget.CollectionLayoutManager
import com.facebook.litho.widget.CollectionPrimitiveViewAdapter
import com.facebook.litho.widget.CollectionPrimitiveViewScroller
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.ItemDecorationWithMeasureFunction
import com.facebook.litho.widget.LithoCollectionItem
import com.facebook.litho.widget.LithoRecyclerView.OnAfterLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.OnBeforeLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.TouchInterceptor
import com.facebook.litho.widget.PrimitiveRecyclerViewHolder
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.Recycler.Companion.createSectionsRecyclerView
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.SectionsRecyclerView
import com.facebook.litho.widget.SectionsRecyclerView.SectionsRecyclerViewLogger
import com.facebook.litho.widget.bindLegacyAttachBinder
import com.facebook.litho.widget.bindLegacyMountBinder
import com.facebook.litho.widget.requireLithoRecyclerView
import com.facebook.litho.widget.unbindLegacyAttachBinder
import com.facebook.litho.widget.unbindLegacyMountBinder
import com.facebook.rendercore.PoolScope
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import kotlin.math.max

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
    val adapter = useState { CollectionPrimitiveViewAdapter(viewHolderCreator) }.value
    val recyclerEventsController = useState { RecyclerEventsController() }.value
    val collectionPrimitiveViewScroller =
        useState { CollectionPrimitiveViewScroller(context.androidContext) }.value

    // This calculates the diff between the previous and new data to determine what changes need to
    // be made to the RecyclerView. It's performed as a best-effort calculation on the background
    // thread without synchronization locks since this data might be discarded if new updates come
    // in before it's applied.
    val updateOperation =
        calculateDiff(
            previousData = latestCommittedData.data,
            nextData = children,
            sameItemComparator = idComparator,
            sameContentComparator = contentComparator)
    // We're going to measure the list with a best effort calculation of the changeset.
    val collectionItems = buildCollectionItems(context, adapter, componentRenderer, updateOperation)

    useEffect(Any()) {
      lazyCollectionController?.recyclerEventsController = recyclerEventsController
      lazyCollectionController?.scrollerDelegate = RecyclerScroller(collectionPrimitiveViewScroller)

      var resolvedItems: List<CollectionItem<*>> = collectionItems
      var resolvedUpdateOperation: CollectionUpdateOperation<CollectionChild> = updateOperation

      if (updateOperation.prevData !== latestCommittedData.data) {
        // If the data has changed since the last diff calculation, we need to re-calculate the
        // result to make sure we're always applying the latest changeset.
        val operation =
            calculateDiff(
                previousData = latestCommittedData.data,
                nextData = children,
                sameItemComparator = idComparator,
                sameContentComparator = contentComparator)
        val items = buildCollectionItems(context, adapter, componentRenderer, updateOperation)
        // todo remeasure the list and trigger re-layout if the size doesn't match
        resolvedItems = items
        resolvedUpdateOperation = operation
      }

      latestCommittedData.data = resolvedUpdateOperation.nextData
      applyUpdateOperations(
          updateOperation = resolvedUpdateOperation,
          adapter = adapter,
          items = resolvedItems,
          onDataRendered = onDataRendered,
          onDataBound = onDataBound)

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
     * Calculates the difference between two lists and returns a CollectionUpdateOperation
     * containing the operations needed to transform the previous list into the next list.
     *
     * @param T The type of items in the lists
     * @param previousData The original list of items (can be null)
     * @param nextData The new list of items to compare against (can be null)
     * @param sameItemComparator Optional comparator to determine if two items represent the same
     *   entity
     * @param sameContentComparator Optional comparator to determine if two items have the same
     *   content
     * @return CollectionUpdateOperation containing the diff operations and data references
     */
    private fun <T> calculateDiff(
        previousData: List<T>?,
        nextData: List<T>?,
        sameItemComparator: ((previousItem: T, nextItem: T) -> Boolean)? = null,
        sameContentComparator: ((previousItem: T, nextItem: T) -> Boolean)? = null,
    ): CollectionUpdateOperation<T> {
      ComponentsSystrace.trace("diffing") {
        val updateOperation =
            CollectionUpdateOperation(prevData = previousData, nextData = nextData)
        val diffCallback =
            CollectionDiffCallback(
                previousData, nextData, sameItemComparator, sameContentComparator)
        val result = DiffUtil.calculateDiff(diffCallback)
        result.dispatchUpdatesTo(updateOperation)

        return updateOperation
      }
    }

    /**
     * Builds a list of CollectionItem objects by applying update operations to an existing
     * adapter's items. This method processes insert, delete, move, and update operations to
     * transform the current collection into the target state defined by the update callback.
     *
     * @param context The ComponentContext used for creating new collection items
     * @param adapter The CollectionPrimitiveViewAdapter containing the current items
     * @param renderer Function that converts a model at a given index into RenderInfo
     * @param updateOperation Contains the operations and target data for the collection update
     * @return List of CollectionItem objects representing the updated collection state
     */
    private fun buildCollectionItems(
        context: ComponentContext,
        adapter: CollectionPrimitiveViewAdapter,
        renderer: (index: Int, model: CollectionChild) -> RenderInfo,
        updateOperation: CollectionUpdateOperation<CollectionChild>,
    ): List<CollectionItem<*>> {

      if (updateOperation.operations.isEmpty()) {
        // Returns a read only list of items if there are no operations to apply
        return adapter.readOnlyItems()
      }

      // We're creating a speculative changeset for measurement without side effects,
      // which may be discarded if the dataset changes before committing the result,
      // so we need to duplicate the items and apply modifications to the copy.
      val updatedItems = adapter.copyItems()
      val itemsNeedToRefreshRenderInfo = mutableSetOf<Int>()
      for (operation in updateOperation.operations) {
        when (operation.type) {
          CollectionOperation.Type.INSERT -> {
            for (index in 0 until operation.count) {
              val item =
                  LithoCollectionItem(
                      componentContext = context, renderInfo = ComponentRenderInfo.createEmpty())
              updatedItems.add(operation.index + index, item)
              itemsNeedToRefreshRenderInfo.add(item.id)
            }
          }
          CollectionOperation.Type.DELETE -> {
            repeat(operation.count) { updatedItems.removeAt(operation.index) }
          }
          CollectionOperation.Type.MOVE -> {
            updatedItems.add(operation.toIndex, updatedItems.removeAt(operation.index))
          }
          CollectionOperation.Type.UPDATE -> {
            for (index in 0 until operation.count) {
              val oldItem = updatedItems[operation.index + index]
              itemsNeedToRefreshRenderInfo.add(oldItem.id)
            }
          }
        }
      }

      if (updateOperation.nextData != null && updateOperation.nextData.size != updatedItems.size) {
        // We may encounter a scenario where the data size doesn't match the result after applying
        // the changeset operations. In such cases, we need to clear all existing items and
        // repopulate the list with the new data.
        updateOperation.operations.clear()
        updatedItems.clear()
        updateOperation.operations.add(
            CollectionOperation(
                type = CollectionOperation.Type.DELETE, index = 0, count = updatedItems.size))

        // Refill the list with new data
        for (index in updateOperation.nextData.indices) {
          val model = updateOperation.nextData[index]
          val renderInfo = renderer(index, model)
          val item = LithoCollectionItem(componentContext = context, renderInfo = renderInfo)
          updatedItems.add(item)
        }
        updateOperation.operations.add(
            CollectionOperation(
                type = CollectionOperation.Type.INSERT,
                index = 0,
                count = updateOperation.nextData.size))
      } else {
        for (index in 0 until updatedItems.size) {
          val item = updatedItems[index]
          // Generate render info for all changed items
          if (itemsNeedToRefreshRenderInfo.contains(item.id)) {
            item.renderInfo =
                (updateOperation.nextData?.get(index)?.let { model -> renderer(index, model) }
                    ?: ComponentRenderInfo.Companion.createEmpty())
          }
        }
      }
      return updatedItems
    }

    /**
     * Applies update operations to the RecyclerView adapter and notifies it of data changes. This
     * method processes a collection of operations (insert, delete, move, update) and dispatches the
     * appropriate notifications to the adapter to trigger UI updates.
     *
     * @param updateOperation The collection update operation containing the list of changes to
     *   apply
     * @param adapter The adapter that manages the RecyclerView items
     * @param items The updated list of collection items to set on the adapter
     * @param onDataBound Optional callback invoked when data binding is complete
     * @param onDataRendered Optional callback invoked when data rendering is complete
     */
    @UiThread
    private fun applyUpdateOperations(
        updateOperation: CollectionUpdateOperation<*>,
        adapter: CollectionPrimitiveViewAdapter,
        items: List<CollectionItem<*>>,
        onDataBound: OnDataBound? = null,
        onDataRendered: OnDataRendered? = null,
    ) {
      ComponentsSystrace.trace("applyUpdateOperations") {
        val isDataChanged = updateOperation.operations.isNotEmpty()
        if (isDataChanged) {
          adapter.setItems(items)
          for (operation in updateOperation.operations) {
            when (operation.type) {
              CollectionOperation.Type.INSERT -> {
                if (operation.count > 1) {
                  adapter.notifyItemRangeInserted(operation.index, operation.count)
                } else {
                  adapter.notifyItemInserted(operation.index)
                }
              }
              CollectionOperation.Type.DELETE -> {
                if (operation.count > 1) {
                  adapter.notifyItemRangeRemoved(operation.index, operation.count)
                } else {
                  adapter.notifyItemRemoved(operation.index)
                }
              }
              CollectionOperation.Type.MOVE -> {
                adapter.notifyItemMoved(operation.index, operation.toIndex)
              }
              CollectionOperation.Type.UPDATE -> {
                if (operation.count > 1) {
                  adapter.notifyItemRangeChanged(operation.index, operation.count)
                } else {
                  adapter.notifyItemChanged(operation.index)
                }
              }
            }
          }
        }
        val changeSetCompleteCallback =
            object : ChangeSetCompleteCallback {
              override fun onDataBound() {
                if (!isDataChanged) {
                  return
                }
                onDataBound?.invoke()
              }

              override fun onDataRendered(isMounted: Boolean, uptimeMillis: Long) {
                onDataRendered?.invoke(
                    isDataChanged,
                    isMounted,
                    uptimeMillis,
                    -1, // todo: read firstVisiblePosition from layout manager
                    -1, // todo: read lastVisiblePosition from layout manager
                )
              }
            }
        // todo use a controller to dispatch data change events
        changeSetCompleteCallback.onDataBound()
        changeSetCompleteCallback.onDataRendered(true, SystemClock.uptimeMillis())
      }
    }

    /**
     * Factory function that creates PrimitiveRecyclerViewHolder instances for the RecyclerView.
     * This lambda takes a parent View and viewType parameter and returns a configured ViewHolder
     * that uses LithoRenderTreeView as its content view for rendering Litho components.
     *
     * @param parent The parent View
     * @param viewType The view type identifier
     * @return PrimitiveRecyclerViewHolder configured with LithoRenderTreeView
     */
    private val viewHolderCreator: (View, Int) -> PrimitiveRecyclerViewHolder =
        { parent, viewType ->
          PrimitiveRecyclerViewHolder(parent.context) { context -> LithoRenderTreeView(context) }
        }
  }

  private class LatestCommittedData(@Volatile var data: List<CollectionChild>? = null)
}

/**
 * Layout behavior implementation for primitive collection views that handles the measurement and
 * layout of collection items within specified size constraints and padding.
 */
@OptIn(ExperimentalLithoApi::class)
private class CollectionPrimitiveViewLayoutBehavior(
    private val items: List<CollectionItem<*>>,
    private val collectionLayoutManager: CollectionLayoutManager,
    private val startPadding: Int,
    private val endPadding: Int,
    private val topPadding: Int,
    private val bottomPadding: Int,
) : LayoutBehavior {

  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val horizontalPadding = startPadding + endPadding
    val verticalPadding = topPadding + bottomPadding
    val constraints =
        sizeConstraintsWithoutPadding(sizeConstraints, horizontalPadding, verticalPadding)
    val size = collectionLayoutManager.measure(items = items, parentSizeConstraints = constraints)
    return PrimitiveLayoutResult(size.width, size.height)
  }

  companion object {
    /** Exclude paddings from the size constraints */
    private fun sizeConstraintsWithoutPadding(
        constraints: SizeConstraints,
        horizontalPadding: Int,
        verticalPadding: Int,
    ): SizeConstraints {

      var minWidth: Int = constraints.minWidth
      var maxWidth: Int = constraints.maxWidth
      var minHeight: Int = constraints.minHeight
      var maxHeight: Int = constraints.maxHeight
      if (constraints.hasBoundedWidth) {
        maxWidth = max(constraints.maxWidth - horizontalPadding, 0)
      } else if (constraints.hasExactWidth) {
        minWidth = max(constraints.minWidth - horizontalPadding, 0)
        maxWidth = max(constraints.maxWidth - horizontalPadding, 0)
      }
      if (constraints.hasBoundedHeight) {
        maxHeight = max(constraints.maxHeight - verticalPadding, 0)
      } else if (constraints.hasExactHeight) {
        minHeight = max(constraints.minHeight - verticalPadding, 0)
        maxHeight = max(constraints.maxHeight - verticalPadding, 0)
      }
      return SizeConstraints(
          minWidth = minWidth, maxWidth = maxWidth, minHeight = minHeight, maxHeight = maxHeight)
    }
  }
}

/**
 * Creates a MountBehavior for CollectionPrimitiveView that handles the mounting and configuration
 * of a SectionsRecyclerView with all necessary properties, listeners, and decorations.
 */
private fun PrimitiveComponentScope.CollectionPrimitiveViewMountBehavior(
    adapter: CollectionPrimitiveViewAdapter,
    bottomPadding: Int,
    clipChildren: Boolean,
    clipToPadding: Boolean,
    endPadding: Int,
    excludeFromIncrementalMount: Boolean,
    fadingEdgeLength: Int,
    horizontalFadingEdgeEnabled: Boolean,
    isBottomFadingEnabled: Boolean,
    isLeftFadingEnabled: Boolean,
    isRightFadingEnabled: Boolean,
    isTopFadingEnabled: Boolean,
    itemAnimator: ItemAnimator?,
    itemDecorations: List<RecyclerView.ItemDecoration>?,
    itemTouchListener: RecyclerView.OnItemTouchListener?,
    measureChild: View.() -> Unit,
    nestedScrollingEnabled: Boolean,
    onAfterLayoutListener: OnAfterLayoutListener?,
    onBeforeLayoutListener: OnBeforeLayoutListener?,
    onRefresh: (() -> Unit)?,
    onScrollListeners: List<RecyclerView.OnScrollListener>?,
    overScrollMode: Int,
    pullToRefreshEnabled: Boolean,
    recyclerEventsController: RecyclerEventsController?,
    recyclerViewId: Int,
    refreshProgressBarBackgroundColor: Int,
    refreshProgressBarColor: Int,
    scrollBarStyle: Int,
    sectionsViewLogger: SectionsRecyclerViewLogger?,
    snapHelper: SnapHelper?,
    startPadding: Int,
    topPadding: Int,
    touchInterceptor: TouchInterceptor?,
    verticalFadingEdgeEnabled: Boolean
): MountBehavior<SectionsRecyclerView> {

  return MountBehavior(ViewAllocator { context -> createSectionsRecyclerView(context) }) {
    doesMountRenderTreeHosts = true
    shouldExcludeFromIncrementalMount = excludeFromIncrementalMount

    withDescription("recycler-decorations") {
      bind(itemDecorations, measureChild) { sectionsRecyclerView ->
        val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

        itemDecorations?.forEach { decoration ->
          if (decoration is ItemDecorationWithMeasureFunction) {
            decoration.measure = measureChild
          }
          recyclerView.addItemDecoration(decoration)
        }

        onUnbind {
          itemDecorations?.forEach { decoration ->
            recyclerView.removeItemDecoration(decoration)
            if (decoration is ItemDecorationWithMeasureFunction) {
              decoration.measure = null
            }
          }
        }
      }
    }

    withDescription("recycler-equivalent-mount") {
      bind(
          clipToPadding,
          startPadding,
          topPadding,
          endPadding,
          bottomPadding,
          clipChildren,
          scrollBarStyle,
          horizontalFadingEdgeEnabled,
          verticalFadingEdgeEnabled,
          fadingEdgeLength,
          refreshProgressBarBackgroundColor,
          refreshProgressBarColor,
          itemAnimator?.javaClass) { sectionsRecyclerView ->
            bindLegacyMountBinder(
                sectionsRecyclerView = sectionsRecyclerView,
                contentDescription = "", // not supported yet, using default value instead
                hasFixedSize = true, // not supported yet, using default value instead
                isClipToPaddingEnabled = clipToPadding,
                paddingAdditionDisabled = false, // not supported yet, using default value instead
                leftPadding = startPadding,
                topPadding = topPadding,
                rightPadding = endPadding,
                bottomPadding = bottomPadding,
                isClipChildrenEnabled = clipChildren,
                isNestedScrollingEnabled = nestedScrollingEnabled,
                scrollBarStyle = scrollBarStyle,
                isHorizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled,
                isVerticalFadingEdgeEnabled = verticalFadingEdgeEnabled,
                isLeftFadingEnabled = isLeftFadingEnabled,
                isRightFadingEnabled = isRightFadingEnabled,
                isTopFadingEnabled = isTopFadingEnabled,
                isBottomFadingEnabled = isBottomFadingEnabled,
                fadingEdgeLength = fadingEdgeLength,
                recyclerViewId = recyclerViewId,
                overScrollMode = overScrollMode,
                edgeFactory = null, // // not supported yet, using default value instead
                refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
                refreshProgressBarColor = refreshProgressBarColor,
                itemAnimator = itemAnimator)

            onUnbind {
              unbindLegacyMountBinder(
                  sectionsRecyclerView = sectionsRecyclerView,
                  refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
                  edgeFactory = null,
                  snapHelper = snapHelper)
            }
          }
    }

    withDescription("recycler-adapter") {
      bind(adapter) { sectionsRecyclerView ->
        val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
        recyclerView.adapter = adapter
        onUnbind { recyclerView.adapter = null }
      }
    }

    withDescription("recycler-before-layout") {
      bind(onBeforeLayoutListener) { sectionsRecyclerView ->
        val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
        recyclerView.setOnBeforeLayoutListener(onBeforeLayoutListener)
        onUnbind { recyclerView.setOnBeforeLayoutListener(null) }
      }
    }

    withDescription("recycler-after-layout") {
      bind(onAfterLayoutListener) { sectionsRecyclerView ->
        val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
        recyclerView.setOnAfterLayoutListener(onAfterLayoutListener)
        onUnbind { recyclerView.setOnAfterLayoutListener(null) }
      }
    }

    withDescription("recycler-equivalent-bind") {
      bind(Any()) { sectionsRecyclerView ->
        bindLegacyAttachBinder(
            sectionsRecyclerView = sectionsRecyclerView,
            sectionsViewLogger = sectionsViewLogger,
            isPullToRefreshEnabled = pullToRefreshEnabled,
            onRefresh = onRefresh,
            onScrollListeners = onScrollListeners,
            touchInterceptor = touchInterceptor,
            onItemTouchListener = itemTouchListener,
            snapHelper = snapHelper,
            recyclerEventsController = recyclerEventsController)

        onUnbind {
          unbindLegacyAttachBinder(
              sectionsRecyclerView = sectionsRecyclerView,
              recyclerEventsController = recyclerEventsController,
              onScrollListeners = onScrollListeners,
              onItemTouchListener = itemTouchListener)
        }
      }
    }
  }
}
