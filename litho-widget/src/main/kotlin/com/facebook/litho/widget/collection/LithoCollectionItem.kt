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
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoRenderTreeView
import com.facebook.litho.LithoRenderer
import com.facebook.litho.LithoTree
import com.facebook.litho.LithoVisibilityEventsController
import com.facebook.litho.TreePropContainer
import com.facebook.litho.componentsConfig
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.RunnableHandlerProvider
import com.facebook.litho.widget.RenderInfo
import com.facebook.rendercore.IntSize
import com.facebook.rendercore.SizeConstraints
import com.meta.rangemanager.PrepareScope

/** A [CollectionItem] that renders a [LithoRenderTreeView]. */
class LithoCollectionItem(
    componentContext: ComponentContext,
    id: Int = LithoTree.generateComponentTreeId(),
    viewType: Int = DEFAULT_COMPONENT_VIEW_TYPE,
    renderInfo: RenderInfo,
    runnableHandlerProvider: RunnableHandlerProvider = ComponentTree.DefaultLayoutHandlerProvider(),
    private val onChildMeasured: ((LithoCollectionItem, IntSize) -> Unit) = { _, _ -> },
    // Lets bound rows post their background render ahead of other work. Only for a handler the
    // collection owns; on a shared layout thread it would jump ahead of other trees.
    private val prioritizeBoundRows: Boolean = false,
) : CollectionItem<LithoRenderTreeView>(id, viewType, renderInfo) {

  // Capture values from componentContext for deferred LithoRenderer creation.
  // LithoRenderer is the heaviest per-item allocation: it eagerly creates TreeState (with 2
  // StateHandlers, each with up to 9 HashMaps), LithoTree, RenderUnitIdGenerator, and more.
  // Lazy init defers this entire cascade until the item is actually measured, prepared, or bound,
  // saving thousands of object allocations for items outside the viewport + preparation range.
  private val _androidContext: Context = componentContext.androidContext
  private val _componentsConfig: ComponentsConfiguration = componentContext.componentsConfig
  private val _treePropContainer: TreePropContainer? = componentContext.treePropContainerCopy
  private val _runnableHandlerProvider: RunnableHandlerProvider = runnableHandlerProvider
  private val _parentTreePropContainer: TreePropContainer? =
      componentContext.parentTreePropContainer
  private val _visibilityController: LithoVisibilityEventsController? =
      componentContext.lithoVisibilityEventsController

  private val _lazyRenderer = lazy {
    LithoRenderer(
        context = _androidContext,
        id = id,
        componentsConfig = _componentsConfig,
        treePropContainer = _treePropContainer,
        runnableHandlerProvider = _runnableHandlerProvider,
        parentTreePropContainer = _parentTreePropContainer,
        visibilityController = _visibilityController,
    )
  }

  private val renderer: LithoRenderer by _lazyRenderer

  /** Returns the renderer only if it has already been initialized, without triggering lazy init. */
  private val rendererOrNull: LithoRenderer?
    get() = if (_lazyRenderer.isInitialized()) renderer else null

  /**
   * Track the currently bound view to handle RecyclerView change animations correctly. During
   * change animations, the same LithoCollectionItem may be bound to a NEW view while the OLD view
   * is still animating. When the old view's animation completes, onViewRecycled() is called with
   * the old view, but we should only clean up if it's the view we're currently managing.
   */
  private var currentBoundView: LithoRenderTreeView? = null

  override val size: IntSize
    get() =
        rendererOrNull?.currentLayoutState?.let { IntSize(it.width, it.height) } ?: IntSize.Invalid

  override val sizeConstraints: SizeConstraints?
    get() = rendererOrNull?.sizeConstraints

  override fun measure(sizeConstraints: SizeConstraints, result: IntArray?) {
    val layoutState = renderer.renderSync(renderInfo.component, sizeConstraints)
    result?.let {
      result[0] = layoutState.width
      result[1] = layoutState.height
    }
  }

  override fun PrepareScope.prepare() {
    this@prepare as ItemSizeConstraintsProviderScope
    renderer.renderSync(
        root = renderInfo.component,
        constraints = getChildSizeConstraints(this@LithoCollectionItem),
    )
  }

  override fun prepareAsync(sizeConstraints: SizeConstraints) {
    // Early-exit guard: Skip redundant preparation if we already have a layout state
    // with matching constraints and component. This prevents the excessive prepareAsync
    // calls observed during rapid scrolling from reaching the expensive render pipeline, saving
    // significant CPU cycles.
    val existing = rendererOrNull
    if (existing != null) {
      val currentLayoutState = existing.currentLayoutState
      if (
          currentLayoutState != null &&
              currentLayoutState.resolveResult.component === renderInfo.component &&
              existing.sizeConstraints == sizeConstraints
      ) {
        return
      }
    }

    renderer.render(renderInfo.component, sizeConstraints)
  }

  override fun ItemSizeConstraintsProviderScope.onBindView(view: LithoRenderTreeView) {
    val previousView = currentBoundView

    // If we're binding to a different view (RecyclerView change animation scenario),
    // we must clean up the old view first since onViewRecycled will skip it
    if (previousView != null && previousView !== view) {
      rendererOrNull?.unbind()
      previousView.clean()
    }

    // Track the view we're binding to
    currentBoundView = view

    renderer.bindTo(view)
    // Getting size constraints from the scope is the safest way to ensure rendering the correct
    // size constraints, like in the following scenarios:
    // 1) We missed chances to prepare content ahead of time
    // 2) The collection size has changed in the dynamic size mode
    val constraints = getChildSizeConstraints(this@LithoCollectionItem)
    // A different previous view means a change animation is cross-fading to this view, which must
    // not start from stale content. Dynamic sizing needs the real size from onChildMeasured below.
    val keptCommittedLayout =
        ComponentsConfiguration.useCommittedLayoutOnCollectionBind &&
            !isDynamicSize &&
            (previousView == null || previousView === view) &&
            renderer.renderAsyncKeepingCommittedLayout(
                renderInfo.component,
                constraints,
                postAtFront = prioritizeBoundRows,
            )
    if (!keptCommittedLayout) {
      renderer.renderSyncWithCompatibilityChecks(renderInfo.component, constraints)
    }
    onChildMeasured(this@LithoCollectionItem, size)
  }

  override fun onViewRecycled(view: LithoRenderTreeView) {
    if (view !== currentBoundView) {
      // During RecyclerView change animations, the same LithoCollectionItem
      // can be bound to a NEW view while the OLD view is still animating. When the old
      // view's animation completes, onViewRecycled() is called with the old view, but we're
      // now managing a different view. We should ONLY clean up views we're currently managing.
      return
    }

    // This is the view we're currently managing - clean it up
    rendererOrNull?.unbind()
    view.clean()
    currentBoundView = null
  }

  override fun PrepareScope.unprepare() {
    rendererOrNull?.cleanup()
  }

  companion object {
    const val DEFAULT_COMPONENT_VIEW_TYPE: Int = 0
  }
}
