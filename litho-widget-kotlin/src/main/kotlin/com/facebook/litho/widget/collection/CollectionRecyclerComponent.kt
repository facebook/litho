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

import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.view.View
import androidx.annotation.UiThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.ComponentsSystrace
import com.facebook.litho.LithoExtraContextForLayoutScope
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.LithoRenderTreeView
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.annotations.Hook
import com.facebook.litho.config.PrimitiveRecyclerBinderStrategy
import com.facebook.litho.effects.useEffect as useLayoutEffect
import com.facebook.litho.onCleanup
import com.facebook.litho.sections.widget.GridRecyclerConfiguration
import com.facebook.litho.sections.widget.NoUpdateItemAnimator
import com.facebook.litho.sections.widget.RecyclerConfiguration
import com.facebook.litho.sections.widget.StaggeredGridRecyclerConfiguration
import com.facebook.litho.useCallback
import com.facebook.litho.useEffect
import com.facebook.litho.useState
import com.facebook.litho.useStateWithDeps
import com.facebook.litho.widget.CollectionItem
import com.facebook.litho.widget.CollectionItemRootHostHolder
import com.facebook.litho.widget.CollectionLayoutData
import com.facebook.litho.widget.CollectionLayoutScope
import com.facebook.litho.widget.CollectionOrientation
import com.facebook.litho.widget.CollectionPreparationManager
import com.facebook.litho.widget.CollectionPrimitiveViewAdapter
import com.facebook.litho.widget.CollectionPrimitiveViewScroller
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.ItemDecorationWithMeasureFunction
import com.facebook.litho.widget.LayoutInfo
import com.facebook.litho.widget.LithoCollectionItem
import com.facebook.litho.widget.LithoRecyclerView.OnAfterLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.OnBeforeLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.TouchInterceptor
import com.facebook.litho.widget.Recycler.Companion.createSectionsRecyclerView
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.SectionsRecyclerView
import com.facebook.litho.widget.SectionsRecyclerView.SectionsRecyclerViewLogger
import com.facebook.litho.widget.SnapUtil
import com.facebook.litho.widget.SnapUtil.SnapMode
import com.facebook.litho.widget.ViewportInfo.ViewportChanged
import com.facebook.litho.widget.areSizeConstraintsCompatible
import com.facebook.litho.widget.bindLegacyAttachBinder
import com.facebook.litho.widget.bindLegacyMountBinder
import com.facebook.litho.widget.calculateLayout
import com.facebook.litho.widget.getChildSizeConstraints
import com.facebook.litho.widget.requireLithoRecyclerView
import com.facebook.litho.widget.unbindLegacyAttachBinder
import com.facebook.litho.widget.unbindLegacyMountBinder
import com.facebook.rendercore.PoolScope
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.thread.utils.ThreadUtils
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import kotlin.math.max

/** A component that renders a list of items using a [RecyclerBinder]. */
@OptIn(ExperimentalLithoApi::class)
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
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val layoutConfig: CollectionLayoutConfig = useConfig(recyclerConfiguration)
    val measureVersion = useState { 0 }
    val latestCommittedData = useState { LatestCommittedData() }.value
    val poolScope = useState { PoolScope.ManuallyManaged() }.value
    val layoutInfo =
        useStateWithDeps(
                layoutConfig.orientation,
                layoutConfig.reverseLayout,
                layoutConfig.stackFromEnd,
                layoutConfig.spanCount,
                layoutConfig.gapStrategy) {
                  recyclerConfiguration.getLayoutInfo(context)
                }
            .value
    val adapter = useState { CollectionPrimitiveViewAdapter() }.value
    val collectionPreparationManager =
        useStateWithDeps(layoutInfo) { CollectionPreparationManager(layoutInfo) }.value

    // This calculates the diff between the previous and new data to determine what changes need to
    // be made to the RecyclerView. It's performed as a best-effort calculation on the background
    // thread without synchronization locks since this data might be discarded if new updates come
    // in before it's applied.
    val changeset =
        generateChangeset(
            context,
            adapter,
            latestCommittedData,
            children,
            contentComparator,
            idComparator,
            componentRenderer)

    val recyclerEventsController = useState { RecyclerEventsController() }.value
    val collectionPrimitiveViewScroller =
        useState { CollectionPrimitiveViewScroller(context.androidContext) }.value
    useEffect(lazyCollectionController) {
      lazyCollectionController?.recyclerEventsController = recyclerEventsController
      lazyCollectionController?.scrollerDelegate = RecyclerScroller(collectionPrimitiveViewScroller)
      onCleanup {
        lazyCollectionController?.recyclerEventsController = null
        lazyCollectionController?.scrollerDelegate = null
      }
    }

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
    useEffect(adapter, collectionPreparationManager) {
      val viewportChangedListener =
          ViewportChanged {
              firstVisibleIndex,
              lastVisibleIndex,
              firstFullyVisibleIndex,
              lastFullyVisibleIndex,
              state ->
            viewportChangedCallback(
                firstVisibleIndex,
                lastVisibleIndex,
                adapter.getItemCount(),
                firstFullyVisibleIndex,
                lastFullyVisibleIndex)
          }
      collectionPreparationManager.addViewportChangedListener(viewportChangedListener)
      onCleanup {
        collectionPreparationManager.removeViewportChangedListener(viewportChangedListener)
      }
    }

    useEffect(Unit) { onCleanup { poolScope.releaseScope() } }

    val componentRendererCallback = useCallback { index: Int, model: CollectionChild ->
      componentRenderer(index, model)
    }
    val contentComparatorCallback =
        useCallback { previousItem: CollectionChild, nextItem: CollectionChild ->
          contentComparator(previousItem, nextItem)
        }
    val idComparatorCallback =
        useCallback { previousItem: CollectionChild, nextItem: CollectionChild ->
          idComparator(previousItem, nextItem)
        }
    val onDataBoundCallback: () -> Unit = useCallback { -> onDataBound?.invoke() }
    val onDataRenderedCallback: (Boolean, Boolean, Long, Int, Int) -> Unit =
        useCallback {
            isDataChanged: Boolean,
            isMounted: Boolean,
            monoTimestampMs: Long,
            firstVisibleIndex: Int,
            lastVisibleIndex: Int ->
          onDataRendered?.invoke(
              isDataChanged, isMounted, monoTimestampMs, firstVisibleIndex, lastVisibleIndex)
        }
    val remeasureCallback: () -> Unit = useCallback { -> measureVersion.update { it + 1 } }
    val pullToRefreshCallback: () -> Unit = useCallback { -> onPullToRefresh?.invoke() }
    val internalPullToRefreshEnabled = (layoutConfig.orientation.isVertical && pullToRefreshEnabled)
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
    val padding =
        useStateWithDeps(startPadding, topPadding, endPadding, bottomPadding) {
              Padding(
                  startPadding = startPadding,
                  endPadding = endPadding,
                  topPadding = topPadding,
                  bottomPadding = bottomPadding)
            }
            .value
    return LithoPrimitive(
        layoutBehavior =
            CollectionPrimitiveViewLayoutBehavior(
                adapter = adapter,
                layoutConfig = layoutConfig,
                layoutInfo = layoutInfo,
                changeset = changeset,
                children = children,
                componentRenderer = componentRendererCallback,
                contentComparator = contentComparatorCallback,
                idComparator = idComparatorCallback,
                latestCommittedData = latestCommittedData,
                preparationManager = collectionPreparationManager,
                onDataRendered = onDataRenderedCallback,
                onDataBound = onDataBoundCallback,
                padding = padding,
                onRemeasure = remeasureCallback,
            ),
        mountBehavior =
            CollectionPrimitiveViewMountBehavior(
                measureVersion = measureVersion.value,
                layoutConfig = layoutConfig,
                layoutInfo = layoutInfo,
                adapter = adapter,
                preparationManager = collectionPreparationManager,
                scroller = collectionPrimitiveViewScroller,
                clipChildren = clipChildren,
                clipToPadding = clipToPadding,
                padding = padding,
                onRefresh =
                    if (internalPullToRefreshEnabled) {
                      pullToRefreshCallback
                    } else {
                      null
                    },
                snapHelper = layoutConfig.snapHelper,
                excludeFromIncrementalMount =
                    this@CollectionRecyclerComponent.shouldExcludeFromIncrementalMount,
                fadingEdgeLength = fadingEdgeLength,
                horizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled,
                isBottomFadingEnabled = isBottomFadingEnabled,
                isLeftFadingEnabled = isLeftFadingEnabled,
                isRightFadingEnabled = isRightFadingEnabled,
                isTopFadingEnabled = isTopFadingEnabled,
                itemAnimator = itemAnimatorToUse,
                itemDecorations = itemDecoration?.let { listOf(it) },
                itemTouchListener = itemTouchListener,
                nestedScrollingEnabled = nestedScrollingEnabled,
                onAfterLayoutListener = onAfterLayoutListener,
                onBeforeLayoutListener = onBeforeLayoutListener,
                onScrollListeners = onScrollListeners?.filterNotNull(),
                overScrollMode = overScrollMode,
                pullToRefreshEnabled = pullToRefreshEnabled,
                recyclerEventsController = recyclerEventsController,
                recyclerViewId = recyclerViewId,
                refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
                refreshProgressBarColor = refreshProgressBarColor,
                scrollBarStyle = scrollBarStyle,
                sectionsViewLogger = sectionsViewLogger,
                touchInterceptor = touchInterceptor,
                verticalFadingEdgeEnabled = verticalFadingEdgeEnabled),
        style = style)
  }

  companion object {

    /**
     * Reads configuration values from a RecyclerConfiguration and creates a new
     * CollectionLayoutConfig if there's any change. This function extracts various layout and
     * behavior settings from the provided configuration and wraps them in a state-managed
     * CollectionLayoutConfig object for use in the component.
     */
    @OptIn(ExperimentalLithoApi::class)
    @Hook
    private fun ComponentScope.useConfig(config: RecyclerConfiguration): CollectionLayoutConfig {
      val mainAxisWrapContent = config.recyclerBinderConfiguration.recyclerBinderConfig.wrapContent
      val crossAxisWrapMode =
          config.recyclerBinderConfiguration.recyclerBinderConfig.crossAxisWrapMode
      val snapHelper = config.snapHelper
      val snapMode = config.snapMode
      val rangeRatio = config.recyclerBinderConfiguration.recyclerBinderConfig.rangeRatio
      val orientation = CollectionOrientation.fromInt(config.orientation)
      val reverseLayout = config.reverseLayout
      val stackFromEnd = config.stackFromEnd
      val spanCount =
          when (config) {
            is GridRecyclerConfiguration -> config.numColumns
            is StaggeredGridRecyclerConfiguration -> config.numSpans
            else -> GridLayoutManager.DEFAULT_SPAN_COUNT
          }
      val gapStrategy =
          if (config is StaggeredGridRecyclerConfiguration) {
            config.gapStrategy
          } else {
            StaggeredGridLayoutManager.GAP_HANDLING_NONE
          }
      val enableItemPrefetch =
          config.recyclerBinderConfiguration.recyclerBinderConfig.recyclerViewItemPrefetch
      val itemViewCacheSize =
          config.recyclerBinderConfiguration.recyclerBinderConfig.itemViewCacheSize

      return useStateWithDeps(
              mainAxisWrapContent,
              crossAxisWrapMode,
              snapHelper,
              snapMode,
              rangeRatio,
              orientation,
              reverseLayout,
              stackFromEnd,
              spanCount,
              gapStrategy,
              enableItemPrefetch,
              itemViewCacheSize) {
                CollectionLayoutConfig(
                    mainAxisWrapContent = mainAxisWrapContent,
                    crossAxisWrapMode = crossAxisWrapMode,
                    snapHelper = snapHelper,
                    snapMode = snapMode,
                    rangeRatio = rangeRatio,
                    orientation = orientation,
                    reverseLayout = reverseLayout,
                    stackFromEnd = stackFromEnd,
                    spanCount = spanCount,
                    gapStrategy = gapStrategy,
                    enableItemPrefetch = enableItemPrefetch,
                    itemViewCacheSize = itemViewCacheSize,
                )
              }
          .value
    }
  }
}

/**
 * Internal data class that holds the latest committed data for the collection. Uses @Volatile to
 * ensure thread-safe access to the data field across different threads.
 *
 * @param data The list of CollectionChild items that have been committed to the collection.
 *   Defaults to null when no data has been committed yet.
 */
internal class LatestCommittedData(@Volatile var data: List<CollectionChild>? = null)

/**
 * Calculates the difference between two lists and returns a CollectionUpdateOperation containing
 * the operations needed to transform the previous list into the next list.
 *
 * @param T The type of items in the lists
 * @param previousData The original list of items (can be null)
 * @param nextData The new list of items to compare against (can be null)
 * @param sameItemComparator Optional comparator to determine if two items represent the same entity
 * @param sameContentComparator Optional comparator to determine if two items have the same content
 * @return CollectionUpdateOperation containing the diff operations and data references
 */
private fun <T> calculateDiff(
    previousData: List<T>?,
    nextData: List<T>?,
    sameItemComparator: ((previousItem: T, nextItem: T) -> Boolean)? = null,
    sameContentComparator: ((previousItem: T, nextItem: T) -> Boolean)? = null,
): CollectionUpdateOperation<T> {
  ComponentsSystrace.trace("diffing") {
    val updateOperation = CollectionUpdateOperation(prevData = previousData, nextData = nextData)
    val diffCallback =
        CollectionDiffCallback(previousData, nextData, sameItemComparator, sameContentComparator)
    val result = DiffUtil.calculateDiff(diffCallback)
    result.dispatchUpdatesTo(updateOperation)
    return updateOperation
  }
}

/**
 * Builds a list of CollectionItem objects by applying update operations to an existing adapter's
 * items. This method processes insert, delete, move, and update operations to transform the current
 * collection into the target state defined by the update callback.
 *
 * @param context The ComponentContext used for creating new collection items
 * @param updatedItems The list of CollectionItem objects to be applied with the update operations
 * @param renderer Function that converts a model at a given index into RenderInfo
 * @param updateOperation Contains the operations and target data for the collection update
 * @return List of CollectionItem objects representing the updated collection state
 */
private fun buildCollectionItems(
    context: ComponentContext,
    updatedItems: MutableList<CollectionItem<*>>,
    renderer: (index: Int, model: CollectionChild) -> RenderInfo,
    updateOperation: CollectionUpdateOperation<CollectionChild>,
): List<CollectionItem<*>> {
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
 * @param changeset The changeset containing the operations and updated items to apply
 */
@UiThread
private fun CollectionPrimitiveViewAdapter.applyChangeset(changeset: Changeset) {
  ThreadUtils.assertMainThread()
  ComponentsSystrace.trace("applyChangeset") {
    val isDataChanged = changeset.operation.hasChanges
    if (isDataChanged) {
      setItems(changeset.items)
      for (operation in changeset.operation.operations) {
        when (operation.type) {
          CollectionOperation.Type.INSERT -> {
            if (operation.count > 1) {
              notifyItemRangeInserted(operation.index, operation.count)
            } else {
              notifyItemInserted(operation.index)
            }
          }
          CollectionOperation.Type.DELETE -> {
            if (operation.count > 1) {
              notifyItemRangeRemoved(operation.index, operation.count)
            } else {
              notifyItemRemoved(operation.index)
            }
          }
          CollectionOperation.Type.MOVE -> {
            notifyItemMoved(operation.index, operation.toIndex)
          }
          CollectionOperation.Type.UPDATE -> {
            if (operation.count > 1) {
              notifyItemRangeChanged(operation.index, operation.count)
            } else {
              notifyItemChanged(operation.index)
            }
          }
        }
      }
    }
  }
}

/**
 * Dispatches update callbacks to notify listeners about data changes and rendering completion. This
 * function handles the invocation of data bound and data rendered callbacks based on whether the
 * underlying data has changed.
 *
 * @param firstVisiblePosition The index of the first visible item in the collection
 * @param lastVisiblePosition The index of the last visible item in the collection
 * @param isDataChanged Boolean flag indicating whether the data has been modified
 * @param onDataBound Optional callback invoked when data has been bound to the collection
 * @param onDataRendered Optional callback invoked when data rendering is complete
 */
private fun dispatchUpdateCallback(
    firstVisiblePosition: Int,
    lastVisiblePosition: Int,
    isDataChanged: Boolean,
    onDataBound: OnDataBound?,
    onDataRendered: OnDataRendered?,
) {
  if (isDataChanged) {
    onDataBound?.invoke()
  }
  onDataRendered?.invoke(
      isDataChanged, true, SystemClock.uptimeMillis(), firstVisiblePosition, lastVisiblePosition)
}

/**
 * Layout behavior implementation for primitive collection views that handles the measurement and
 * layout of collection items within specified size constraints and padding.
 */
@OptIn(ExperimentalLithoApi::class)
private class CollectionPrimitiveViewLayoutBehavior(
    private val adapter: CollectionPrimitiveViewAdapter,
    private val layoutConfig: CollectionLayoutConfig,
    private val layoutInfo: LayoutInfo,
    private val changeset: Changeset,
    private val children: List<CollectionChild>,
    private val componentRenderer: (index: Int, model: CollectionChild) -> RenderInfo,
    private val contentComparator:
        (previousItem: CollectionChild, nextItem: CollectionChild) -> Boolean,
    private val idComparator: (previousItem: CollectionChild, nextItem: CollectionChild) -> Boolean,
    private val latestCommittedData: LatestCommittedData,
    private val onDataBound: OnDataBound?,
    private val onDataRendered: OnDataRendered?,
    private val preparationManager: CollectionPreparationManager,
    private val padding: Padding,
    private val onRemeasure: () -> Unit,
) : LayoutBehavior {

  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val previousLayoutData = previousLayoutData as CollectionLayoutData?
    val componentContext = (extraContext as LithoExtraContextForLayoutScope).componentContext

    val constraintsWithoutPadding =
        sizeConstraintsWithoutPadding(
            sizeConstraints, padding.horizontalPadding, padding.verticalPadding)
    val latestItems =
        if (previousLayoutData != null && latestCommittedData.data === children) {
          // Items may have been modified after layout effects, so we use the cached version
          // from the previous layout data to maintain consistency.
          previousLayoutData.items
        } else {
          changeset.items
        }
    val scope =
        CollectionLayoutScope(
            layoutInfo,
            constraintsWithoutPadding,
            adapter.layoutData?.collectionSize,
            isVertical = layoutConfig.orientation.isVertical,
            wrapInMainAxis = layoutConfig.mainAxisWrapContent,
            crossAxisWrapMode = layoutConfig.crossAxisWrapMode)

    val latestSize = scope.calculateLayout(latestItems)

    val layoutData =
        CollectionLayoutData(
            layoutInfo = layoutInfo,
            collectionConstraints = constraintsWithoutPadding,
            collectionSize = latestSize,
            items = latestItems,
            isVertical = layoutConfig.orientation.isVertical,
            isDynamicSize = layoutConfig.crossAxisWrapMode == CrossAxisWrapMode.Dynamic)

    if (!preparationManager.hasApproximateRangeSize) {
      // Measure the first child item with the calculated size constraints
      // todo: use different strategies to have a more precise estimation
      latestItems.firstOrNull()?.let { firstChild ->
        val output = IntArray(2)
        firstChild.measure(layoutData.getChildSizeConstraints(firstChild), output)
        preparationManager.estimateItemsInViewPort(latestSize, Size(output[0], output[1]))
      }
    }

    useLayoutEffect(adapter) {
      adapter.viewHolderCreator = ViewHolderCreator
      onCleanup { adapter.viewHolderCreator = null }
    }

    useLayoutEffect(layoutInfo, adapter) {
      layoutInfo.setRenderInfoCollection { position ->
        val item =
            adapter.findItemByPosition(position)
                ?: throw IllegalStateException("Trying to find a child item out of range!")
        item.renderInfo
      }
      onCleanup { layoutInfo.setRenderInfoCollection(null) }
    }

    useLayoutEffect(changeset) {
      val resolvedChangeset =
          if (changeset.operation.prevData !== latestCommittedData.data) {
            // If the data has changed since the last diff calculation, we need to re-calculate
            // the result to make sure we're always applying the latest changeset.
            val newChangeset =
                generateChangeset(
                    componentContext,
                    adapter,
                    latestCommittedData,
                    children,
                    contentComparator,
                    idComparator,
                    componentRenderer)
            // Remeasure the list and trigger re-layout if the size doesn't match
            val remeasuredSize = scope.calculateLayout(newChangeset.items)
            if (latestSize != remeasuredSize) {
              onRemeasure()
            }
            newChangeset
          } else {
            changeset
          }

      latestCommittedData.data = resolvedChangeset.operation.nextData
      adapter.applyChangeset(resolvedChangeset)
      dispatchUpdateCallback(
          firstVisiblePosition = layoutInfo.findFirstVisibleItemPosition(),
          lastVisiblePosition = layoutInfo.findLastVisibleItemPosition(),
          isDataChanged = resolvedChangeset.operation.hasChanges,
          onDataBound = onDataBound,
          onDataRendered = onDataRendered)

      layoutData.items = resolvedChangeset.items
      adapter.layoutData = layoutData
      onCleanup { adapter.layoutData = null }
    }

    return PrimitiveLayoutResult(
        width = latestSize.width, height = latestSize.height, layoutData = layoutData)
  }

  companion object {

    /**
     * Factory function that creates CollectionItemRootHostHolder instances for RecyclerView items.
     */
    private val ViewHolderCreator:
        (View, Int) -> CollectionItemRootHostHolder<out View, out CollectionItem<out View>> =
        { parent, viewType ->
          when (viewType) {
            LithoCollectionItem.DEFAULT_COMPONENT_VIEW_TYPE -> {
              LithoCollectionItemViewHolder(parent.context)
            }
            else -> {
              throw IllegalArgumentException("Unknown view type: $viewType")
            }
          }
        }

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
 * Represents a changeset containing the operations needed to update a collection and the resulting
 * items. This data class encapsulates both the diff operations and the final list of collection
 * items after applying those operations.
 *
 * @param operation The collection update operation containing insert, delete, move, and update
 *   operations
 * @param items The resulting list of collection items after applying the update operations
 */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
private data class Changeset(
    val operation: CollectionUpdateOperation<CollectionChild>,
    val items: List<CollectionItem<*>>
)

/**
 * Represents padding values for all four sides of a UI component. Provides convenient computed
 * properties for total horizontal and vertical padding.
 *
 * @param startPadding Padding at the start edge
 * @param endPadding Padding at the end edge
 * @param topPadding Padding at the top edge
 * @param bottomPadding Padding at the bottom edge
 */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
private data class Padding(
    val startPadding: Int,
    val endPadding: Int,
    val topPadding: Int,
    val bottomPadding: Int
) {
  /** Total horizontal padding combining start and end padding values */
  val horizontalPadding: Int = startPadding + endPadding

  /** Total vertical padding combining top and bottom padding values */
  val verticalPadding: Int = topPadding + bottomPadding
}

/**
 * Generates a changeset by calculating the difference between the latest committed data and new
 * children, then builds the corresponding collection items based on whether changes were detected.
 */
private fun generateChangeset(
    componentContext: ComponentContext,
    adapter: CollectionPrimitiveViewAdapter,
    latestCommittedData: LatestCommittedData,
    children: List<CollectionChild>,
    contentComparator: (previousItem: CollectionChild, nextItem: CollectionChild) -> Boolean,
    idComparator: (previousItem: CollectionChild, nextItem: CollectionChild) -> Boolean,
    componentRenderer: (index: Int, model: CollectionChild) -> RenderInfo,
): Changeset {
  val operation =
      calculateDiff(
          previousData = latestCommittedData.data,
          nextData = children,
          sameItemComparator = idComparator,
          sameContentComparator = contentComparator)
  val items =
      if (operation.hasChanges) {
        // We're creating a speculative changeset for measurement without side effects,
        // which may be discarded if the dataset changes before committing the result,
        // so we need to duplicate the items and apply modifications to the copy.
        buildCollectionItems(
            context = componentContext,
            updatedItems = adapter.getSnapshotOfItemsAsMutableList(),
            renderer = componentRenderer,
            updateOperation = operation)
      } else {
        adapter.getItems()
      }
  return Changeset(operation, items)
}

/**
 * ViewHolder implementation for Litho collection items that wraps a LithoRenderTreeView. This class
 * serves as a bridge between RecyclerView's ViewHolder pattern and Litho's component rendering
 * system, providing the view container for Litho components within a RecyclerView.
 *
 * @param context The Android context used to create the LithoRenderTreeView
 */
internal class LithoCollectionItemViewHolder(context: Context) :
    CollectionItemRootHostHolder<LithoRenderTreeView, LithoCollectionItem>() {

  /**
   * The root view for this ViewHolder, which is a LithoRenderTreeView that can render Litho
   * components within the RecyclerView item.
   */
  override val view: LithoRenderTreeView = LithoRenderTreeView(context)
}

/**
 * Creates a MountBehavior for CollectionPrimitiveView that handles the mounting and configuration
 * of a SectionsRecyclerView with all necessary properties, listeners, and decorations.
 */
@OptIn(ExperimentalLithoApi::class)
private fun PrimitiveComponentScope.CollectionPrimitiveViewMountBehavior(
    measureVersion: Int,
    layoutConfig: CollectionLayoutConfig,
    layoutInfo: LayoutInfo,
    adapter: CollectionPrimitiveViewAdapter,
    preparationManager: CollectionPreparationManager,
    scroller: CollectionPrimitiveViewScroller,
    clipChildren: Boolean,
    clipToPadding: Boolean,
    padding: Padding,
    excludeFromIncrementalMount: Boolean,
    fadingEdgeLength: Int,
    horizontalFadingEdgeEnabled: Boolean,
    isBottomFadingEnabled: Boolean,
    isLeftFadingEnabled: Boolean,
    isRightFadingEnabled: Boolean,
    isTopFadingEnabled: Boolean,
    itemAnimator: RecyclerView.ItemAnimator?,
    itemDecorations: List<RecyclerView.ItemDecoration>?,
    itemTouchListener: RecyclerView.OnItemTouchListener?,
    nestedScrollingEnabled: Boolean,
    onAfterLayoutListener: OnAfterLayoutListener?,
    onBeforeLayoutListener: OnBeforeLayoutListener?,
    onRefresh: (() -> Unit)?,
    onScrollListeners: List<RecyclerView.OnScrollListener>?,
    overScrollMode: Int,
    pullToRefreshEnabled: Boolean,
    recyclerEventsController: RecyclerEventsController?,
    recyclerViewId: Int,
    refreshProgressBarBackgroundColor: Int?,
    refreshProgressBarColor: Int,
    scrollBarStyle: Int,
    sectionsViewLogger: SectionsRecyclerViewLogger?,
    snapHelper: SnapHelper?,
    touchInterceptor: TouchInterceptor?,
    verticalFadingEdgeEnabled: Boolean
): MountBehavior<SectionsRecyclerView> {

  return MountBehavior(ViewAllocator { context -> createSectionsRecyclerView(context) }) {
    doesMountRenderTreeHosts = true
    shouldExcludeFromIncrementalMount = excludeFromIncrementalMount

    withDescription("recycler-decorations") {
      bind(itemDecorations, adapter) { sectionsRecyclerView ->
        val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

        val measureFunction: (View.() -> Unit) = {
          val position = (layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
          adapter.layoutData?.let { scope ->
            val item = scope.items[position]
            val constraints = scope.getChildSizeConstraints(item)
            measure(constraints.toWidthSpec(), constraints.toHeightSpec())
          }
        }

        itemDecorations?.forEach { decoration ->
          if (decoration is ItemDecorationWithMeasureFunction) {
            decoration.measure = measureFunction
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
          measureVersion,
          clipToPadding,
          padding,
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
                leftPadding = padding.startPadding,
                topPadding = padding.topPadding,
                rightPadding = padding.endPadding,
                bottomPadding = padding.bottomPadding,
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

    withDescription("layout-manager") {
      bind(layoutInfo, layoutConfig.enableItemPrefetch, layoutConfig.itemViewCacheSize) {
          sectionsRecyclerView ->
        val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
        val layoutManager = layoutInfo.getLayoutManager()
        layoutManager.isItemPrefetchEnabled = layoutConfig.enableItemPrefetch
        recyclerView.setItemViewCacheSize(layoutConfig.itemViewCacheSize)
        recyclerView.layoutManager = layoutManager
        onUnbind { recyclerView.layoutManager = null }
      }
    }

    withDescription("recycler-adapter") {
      bind(adapter) { sectionsRecyclerView ->
        val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
        recyclerView.adapter = adapter
        onUnbind { recyclerView.adapter = null }
      }
    }

    withDescription("preparation-manager") {
      bind(preparationManager, layoutConfig.rangeRatio, adapter) { sectionsRecyclerView ->
        val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
        val onEnterRangeCallback: (Int) -> Unit = { position ->
          adapter.layoutData?.let { scope ->
            val item = scope.items[position]
            val constraints = scope.getChildSizeConstraints(item)
            if (!item.areSizeConstraintsCompatible(constraints)) {
              item.prepare(constraints)
            }
          }
        }
        val onExitRangeCallback: (Int) -> Unit = { position ->
          adapter.layoutData?.let { scope ->
            val item = scope.items[position]
            item.unprepare()
          }
        }
        preparationManager.bind(
            recyclerView, layoutConfig.rangeRatio, onEnterRangeCallback, onExitRangeCallback)
        onUnbind { preparationManager.unbind(recyclerView) }
      }
    }

    withDescription("recycler-scroller") {
      bind(scroller, layoutInfo) { sectionsRecyclerView ->
        val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
        scroller.bind(layoutInfo, adapter)
        onUnbind {
          scroller.rememberScrollOffset(recyclerView)
          scroller.unbind()
        }
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

/** An internal model that helps us access these configs as dependency. */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
private data class CollectionLayoutConfig(
    val mainAxisWrapContent: Boolean = false,
    val crossAxisWrapMode: CrossAxisWrapMode = CrossAxisWrapMode.NoWrap,
    val snapHelper: SnapHelper? = null,
    @SnapMode val snapMode: Int = SnapUtil.SNAP_NONE,
    val rangeRatio: Float = DEFAULT_RANGE_RATIO,
    /**
     * Set whether item prefetch should be enabled on the underlying RecyclerView.LayoutManager.
     * Defaults to false.
     *
     * <p>ItemPrefetching feature of RecyclerView clashes with RecyclerBinder's compute range
     * optimization and in certain scenarios (like sticky header) it might reset ComponentTree of
     * LithoView while it is still on screen making it render blank or zero height.
     *
     * <p>As ItemPrefetching is built on top of item view cache, please do remember to set a proper
     * cache size if you want to enable this feature. Otherwise, prefetched item will be thrown into
     * the recycler pool immediately.
     *
     * See [RecyclerView.LayoutManager.setItemPrefetchEnabled].
     */
    @JvmField val enableItemPrefetch: Boolean = true,
    /**
     * Set the number of offscreen views to retain before adding them to the potentially shared
     * recycled view pool.
     *
     * <p>The offscreen view cache stays aware of changes in the attached adapter, allowing a
     * LayoutManager to reuse those views unmodified without needing to return to the adapter to
     * rebind them.
     *
     * See [RecyclerView.setItemViewCacheSize].
     */
    @JvmField val itemViewCacheSize: Int = DEFAULT_CACHE_SIZE,

    // Configs that requires regenerating a new layout info
    val orientation: CollectionOrientation = CollectionOrientation.VERTICAL,
    val reverseLayout: Boolean = false,
    val stackFromEnd: Boolean = false,
    val spanCount: Int = GridLayoutManager.DEFAULT_SPAN_COUNT,
    val gapStrategy: Int = StaggeredGridLayoutManager.GAP_HANDLING_NONE
) {

  companion object {
    // Default range ratio for the collection
    private const val DEFAULT_RANGE_RATIO: Float = 2f
    // Default item view cache size for the collection
    private const val DEFAULT_CACHE_SIZE: Int = 2
  }
}
