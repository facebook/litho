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

package com.facebook.litho.widget

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.litho.BaseMountingView
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.HasLithoViewChildren
import com.facebook.litho.LithoView
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventDispatcher.beginTrace
import com.facebook.rendercore.debug.DebugEventDispatcher.dispatch
import com.facebook.rendercore.debug.DebugEventDispatcher.endTrace
import com.facebook.rendercore.debug.DebugEventDispatcher.generateTraceIdentifier

/**
 * Wrapper that encapsulates all the features [Recycler] provides such as sticky header and
 * pull-to-refresh
 */
open class SectionsRecyclerView(context: Context, recyclerView: RecyclerView) :
    SwipeRefreshLayout(context), HasLithoViewChildren {

  val stickyHeader: LithoView
  val recyclerView: RecyclerView

  val defaultEdgeEffectFactory: RecyclerView.EdgeEffectFactory
  private var sectionsRecyclerViewLogger: SectionsRecyclerViewLogger? = null
  private var isFirstLayout = true

  /**
   * Indicates whether [RecyclerView] has been detached. In such case we need to make sure to
   * relayout its children eventually.
   */
  private var hasBeenDetachedFromWindow = false

  /**
   * When we set an ItemAnimator during mount, we want to store the one that was already set on the
   * RecyclerView so that we can reset it during unmount.
   */
  private var detachedItemAnimator: RecyclerView.ItemAnimator? = null

  init {
    val handler: ((Any) -> Unit)? =
        ComponentsConfiguration.defaultInstance.sectionsRecyclerViewOnCreateHandler
    handler?.invoke(this)

    this.recyclerView = recyclerView
    // Get the default edge effect factory (at least the default one in the initialized recycler)
    defaultEdgeEffectFactory = recyclerView.edgeEffectFactory

    // We need to draw first visible item on top of other children to support sticky headers
    recyclerView.setChildDrawingOrderCallback { childCount, i -> childCount - 1 - i }
    // ViewCache doesn't work well with RecyclerBinder which assumes that whenever item comes back
    // to viewport it should be rebound which does not happen with ViewCache. Consider this case:
    // LithoView goes out of screen and it is added to ViewCache, then its ComponentTree is assigned
    // to another LV which means our LithoView's ComponentTree reference is nullified. It comes back
    // to screen and it is not rebound therefore we will see 0 height LithoView which actually
    // happened in multiple product surfaces. Disabling it fixes the issue.
    recyclerView.setItemViewCacheSize(0)

    addView(this.recyclerView)
    stickyHeader = LithoView(ComponentContext(getContext()), null)
    stickyHeader.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    addView(stickyHeader)
  }

  fun setSectionsRecyclerViewLogger(lithoViewLogger: SectionsRecyclerViewLogger?) {
    sectionsRecyclerViewLogger = lithoViewLogger
  }

  fun setStickyComponent(componentTree: ComponentTree) {
    val existingLithoView = componentTree.lithoView
    stickyHeader.componentTree = componentTree

    // Set this after because setComponentTree clears temporary detached component tree
    if (existingLithoView != null && stickyHeader !== existingLithoView) {
      existingLithoView.setTemporaryDetachedComponentTree(componentTree)
    }
    measureStickyHeader(width)
  }

  fun setStickyHeaderVerticalOffset(verticalOffset: Int) {
    stickyHeader.translationY = verticalOffset.toFloat()
  }

  fun showStickyHeader() {
    stickyHeader.visibility = View.VISIBLE

    layoutStickyHeader()
  }

  fun hideStickyHeader() {
    stickyHeader.unmountAllItems()
    stickyHeader.visibility = View.GONE
  }

  fun maybeRestoreDetachedComponentTree(stickyHeaderPosition: Int) {
    if (stickyHeaderPosition <= -1 ||
        !ComponentsConfiguration.initStickyHeaderInLayoutWhenComponentTreeIsNull ||
        stickyHeader.componentTree == null) {
      return
    }

    val viewHolder = recyclerView.findViewHolderForAdapterPosition(stickyHeaderPosition)
    val recyclerBinderViewHolder = if (viewHolder is RecyclerBinderViewHolder) viewHolder else null
    val lithoView = recyclerBinderViewHolder?.lithoView
    if (lithoView != null &&
        lithoView.componentTree == null &&
        lithoView.hasTemporaryDetachedComponentTree()) {
      lithoView.requestLayout()
    }
  }

  val isStickyHeaderHidden: Boolean
    get() = stickyHeader.visibility == View.GONE

  fun setItemAnimator(itemAnimator: RecyclerView.ItemAnimator?) {
    detachedItemAnimator = recyclerView.itemAnimator
    recyclerView.itemAnimator = itemAnimator
  }

  fun resetItemAnimator() {
    recyclerView.itemAnimator = detachedItemAnimator
    detachedItemAnimator = null
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    measureStickyHeader(MeasureSpec.getSize(widthMeasureSpec))
  }

  private fun measureStickyHeader(parentWidth: Int) {
    measureChild(
        stickyHeader,
        MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY),
        MeasureSpec.UNSPECIFIED)
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    dispatch(
        type = DebugEvent.ViewOnLayout + ":start",
        renderStateId = { "-1" },
        attributesAccumulator = { attrs ->
          attrs[DebugEventAttribute.Id] = hashCode()
          attrs[DebugEventAttribute.Name] = "SectionsRecyclerView"
        })

    val traceId = generateTraceIdentifier(DebugEvent.ViewOnLayout)
    if (traceId != null) {
      val attributes: MutableMap<String, Any> = LinkedHashMap()
      attributes[DebugEventAttribute.Id] = hashCode()
      attributes[DebugEventAttribute.Name] = "SectionsRecyclerView"
      attributes[DebugEventAttribute.Bounds] = Rect(left, top, right, bottom)
      beginTrace(traceId, DebugEvent.ViewOnLayout, "-1", attributes)
    }

    try {
      sectionsRecyclerViewLogger?.onLayoutStarted(isFirstLayout)
      super.onLayout(changed, left, top, right, bottom)
      layoutStickyHeader()
    } finally {
      sectionsRecyclerViewLogger?.onLayoutEnded(isFirstLayout)
      isFirstLayout = false
      if (traceId != null) {
        endTrace(traceId)
      }
    }
  }

  private fun layoutStickyHeader() {
    if (stickyHeader.visibility == View.GONE) {
      return
    }

    val stickyHeaderLeft = paddingLeft
    val stickyHeaderTop = paddingTop
    stickyHeader.layout(
        stickyHeaderLeft,
        stickyHeaderTop,
        stickyHeaderLeft + stickyHeader.measuredWidth,
        stickyHeaderTop + stickyHeader.measuredHeight)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    hasBeenDetachedFromWindow = true
  }

  fun hasBeenDetachedFromWindow(): Boolean {
    return hasBeenDetachedFromWindow
  }

  fun setHasBeenDetachedFromWindow(hasBeenDetachedFromWindow: Boolean) {
    this.hasBeenDetachedFromWindow = hasBeenDetachedFromWindow
  }

  override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    super.requestDisallowInterceptTouchEvent(disallowIntercept)

    // SwipeRefreshLayout can ignore this request if nested scrolling is disabled on the child,
    // but it fails to delegate the request up to the parents.
    // This fixes a bug that can cause parents to improperly intercept scroll events from
    // nested recyclers.
    if (parent != null && !isNestedScrollingEnabled) {
      parent.requestDisallowInterceptTouchEvent(disallowIntercept)
    }
  }

  override fun setOnTouchListener(listener: OnTouchListener) {
    // When setting touch handler for RecyclerSpec we want RecyclerView to handle it.
    recyclerView.setOnTouchListener(listener)
  }

  override fun obtainLithoViewChildren(lithoViews: MutableList<BaseMountingView>) {
    lithoViews.add(stickyHeader)
    val size = recyclerView.childCount
    for (i in 0 until size) {
      val child = recyclerView.getChildAt(i)
      if (child is LithoView) {
        lithoViews.add(child)
      }
    }
  }

  /** Pass to a SectionsRecyclerView to do custom logging. */
  interface SectionsRecyclerViewLogger {
    fun onLayoutStarted(isFirstLayoutAfterRecycle: Boolean)

    fun onLayoutEnded(isFirstLayoutAfterRecycle: Boolean)
  }

  companion object {
    @JvmStatic
    fun getParentRecycler(recyclerView: RecyclerView): SectionsRecyclerView? {
      if (recyclerView.parent is SectionsRecyclerView) {
        return recyclerView.parent as SectionsRecyclerView
      }
      return null
    }
  }
}
