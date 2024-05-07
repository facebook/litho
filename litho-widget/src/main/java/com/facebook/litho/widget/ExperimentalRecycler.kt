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

import android.graphics.Color
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Size
import com.facebook.litho.Style
import com.facebook.litho.eventHandler
import com.facebook.litho.useState
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import kotlin.math.max

/**
 * This is an experimental version of the [RecyclerSpec] implemented with Primitives.
 *
 * Please do not use this code outside of Litho as this is still under experimentation.
 */
class ExperimentalRecycler(
    private val binder: Binder<RecyclerView>,
    private val hasFixedSize: Boolean = true,
    private val isClipToPaddingEnabled: Boolean = true,
    private val leftPadding: Int = 0,
    private val topPadding: Int = 0,
    private val rightPadding: Int = 0,
    private val bottomPadding: Int = 0,
    @ColorInt private val refreshProgressBarBackgroundColor: Int? = null,
    @ColorInt private val refreshProgressBarColor: Int = Color.BLACK,
    private val isClipChildrenEnabled: Boolean = true,
    private val isNestedScrollingEnabled: Boolean = true,
    private val scrollBarStyle: Int = View.SCROLLBARS_INSIDE_OVERLAY,
    private val itemDecorations: List<RecyclerView.ItemDecoration>? = null,
    private val isHorizontalFadingEdgeEnabled: Boolean = false,
    private val isVerticalFadingEdgeEnabled: Boolean = false,
    private val fadingEdgeLength: Int = 0,
    private val edgeFactory: RecyclerView.EdgeEffectFactory? = null,
    @IdRes private val recyclerViewId: Int = View.NO_ID,
    private val overScrollMode: Int = View.OVER_SCROLL_ALWAYS,
    private val contentDescription: CharSequence? = null,
    private val itemAnimator: RecyclerView.ItemAnimator? = DEFAULT_ITEM_ANIMATOR,
    private val recyclerEventsController: RecyclerEventsController? = null,
    private val onScrollListeners: List<RecyclerView.OnScrollListener>? = null,
    private val snapHelper: SnapHelper? = null,
    private val isPullToRefreshEnabled: Boolean = true,
    private val touchInterceptor: LithoRecyclerView.TouchInterceptor? = null,
    private val onItemTouchListener: RecyclerView.OnItemTouchListener? = null,
    private val onRefresh: (() -> Unit)? = null,
    private val sectionsViewLogger: SectionsRecyclerView.SectionsRecyclerViewLogger? = null,
    private val excludeFromIncrementalMount: Boolean = false,
    private val paddingAdditionDisabled: Boolean = false,
    private val style: Style? = null
) : PrimitiveComponent() {

  companion object {
    val DEFAULT_ITEM_ANIMATOR: RecyclerView.ItemAnimator = NoUpdateItemAnimator()

    // This is the default value for refresh spinner background from RecyclerSpec.
    private const val DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR = 0xFFFAFAFA.toInt()
  }

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val measureVersion = useState { 0 }

    return LithoPrimitive(
        layoutBehavior = RecyclerLayoutBehavior(binder) { measureVersion.update { v -> v + 1 } },
        mountBehavior =
            MountBehavior(
                ViewAllocator { context ->
                  SectionsRecyclerView(context, LithoRecyclerView(context))
                }) {
                  doesMountRenderTreeHosts = true
                  shouldExcludeFromIncrementalMount = excludeFromIncrementalMount

                  // RecyclerSpec's @OnMount and @OnUnmount
                  bind(
                      binder,
                      hasFixedSize,
                      isClipToPaddingEnabled,
                      leftPadding,
                      topPadding,
                      rightPadding,
                      bottomPadding,
                      isClipChildrenEnabled,
                      scrollBarStyle,
                      isHorizontalFadingEdgeEnabled,
                      isVerticalFadingEdgeEnabled,
                      fadingEdgeLength,
                      refreshProgressBarBackgroundColor,
                      refreshProgressBarColor,
                      itemAnimator,
                      itemDecorations) { sectionsRecyclerView ->
                        val recyclerView: RecyclerView =
                            sectionsRecyclerView.recyclerView
                                ?: throw java.lang.IllegalStateException(
                                    "RecyclerView not found, it should not be removed from SwipeRefreshLayout")

                        recyclerView.contentDescription = contentDescription
                        recyclerView.setHasFixedSize(hasFixedSize)
                        recyclerView.clipToPadding = isClipToPaddingEnabled
                        sectionsRecyclerView.clipToPadding = isClipToPaddingEnabled
                        ViewCompat.setPaddingRelative(
                            sectionsRecyclerView.recyclerView,
                            leftPadding,
                            topPadding,
                            rightPadding,
                            bottomPadding)
                        recyclerView.clipChildren = isClipChildrenEnabled
                        sectionsRecyclerView.clipChildren = isClipChildrenEnabled
                        recyclerView.isNestedScrollingEnabled = isNestedScrollingEnabled
                        sectionsRecyclerView.isNestedScrollingEnabled = isNestedScrollingEnabled
                        recyclerView.scrollBarStyle = scrollBarStyle
                        recyclerView.isHorizontalFadingEdgeEnabled = isHorizontalFadingEdgeEnabled
                        recyclerView.isVerticalFadingEdgeEnabled = isVerticalFadingEdgeEnabled
                        recyclerView.setFadingEdgeLength(fadingEdgeLength.dp.toPixels())
                        recyclerView.id = recyclerViewId
                        recyclerView.overScrollMode = overScrollMode
                        edgeFactory?.let { recyclerView.edgeEffectFactory = it }

                        if (!paddingAdditionDisabled) {
                          ViewCompat.setPaddingRelative(
                              recyclerView, leftPadding, topPadding, rightPadding, bottomPadding)
                        }

                        if (refreshProgressBarBackgroundColor != null) {
                          sectionsRecyclerView.setProgressBackgroundColorSchemeColor(
                              refreshProgressBarBackgroundColor)
                        }

                        sectionsRecyclerView.setColorSchemeColors(refreshProgressBarColor)

                        itemDecorations?.forEach { recyclerView.addItemDecoration(it) }

                        sectionsRecyclerView.setItemAnimator(
                            if (itemAnimator !== DEFAULT_ITEM_ANIMATOR) itemAnimator
                            else DEFAULT_ITEM_ANIMATOR)

                        binder.mount(recyclerView)

                        onUnbind {
                          recyclerView.id = View.NO_ID

                          if (refreshProgressBarBackgroundColor != null) {
                            sectionsRecyclerView.setProgressBackgroundColorSchemeColor(
                                DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR)
                          }

                          itemDecorations?.forEach { recyclerView.removeItemDecoration(it) }

                          binder.unmount(recyclerView)

                          snapHelper?.attachToRecyclerView(null)

                          sectionsRecyclerView.resetItemAnimator()
                        }
                      }

                  // RecyclerSpec's @OnBind and @OnUnbind
                  bind(Any()) { sectionsRecyclerView ->
                    sectionsRecyclerView.setSectionsRecyclerViewLogger(sectionsViewLogger)

                    // contentDescription should be set on the recyclerView itself, and not the
                    // sectionsRecycler.
                    sectionsRecyclerView.contentDescription = null

                    sectionsRecyclerView.isEnabled = isPullToRefreshEnabled && onRefresh != null

                    if (onRefresh != null) {
                      sectionsRecyclerView.setOnRefreshListener { onRefresh.invoke() }
                    }

                    val recyclerView =
                        sectionsRecyclerView.recyclerView as? LithoRecyclerView
                            ?: throw IllegalStateException(
                                "RecyclerView not found, it should not be removed from SwipeRefreshLayout " +
                                    "before unmounting")

                    if (onScrollListeners != null) {
                      for (i in onScrollListeners.indices) {
                        recyclerView.addOnScrollListener(onScrollListeners[i])
                      }
                    }

                    if (touchInterceptor != null) {
                      recyclerView.setTouchInterceptor(touchInterceptor)
                    }

                    if (onItemTouchListener != null) {
                      recyclerView.addOnItemTouchListener(onItemTouchListener)
                    }

                    // We cannot detach the snap helper in unbind, so it may be possible for it to
                    // get attached twice which causes SnapHelper to raise an exception.
                    if (recyclerView.onFlingListener == null) {
                      snapHelper?.attachToRecyclerView(recyclerView)
                    }

                    binder.bind(recyclerView)

                    if (recyclerEventsController != null) {
                      recyclerEventsController.setSectionsRecyclerView(sectionsRecyclerView)
                      recyclerEventsController.snapHelper = snapHelper
                    }

                    if (sectionsRecyclerView.hasBeenDetachedFromWindow()) {
                      recyclerView.requestLayout()
                      sectionsRecyclerView.setHasBeenDetachedFromWindow(false)
                    }

                    onUnbind {
                      sectionsRecyclerView.setSectionsRecyclerViewLogger(null)

                      binder.unbind(recyclerView)

                      if (recyclerEventsController != null) {
                        recyclerEventsController.setSectionsRecyclerView(null)
                        recyclerEventsController.snapHelper = null
                      }

                      if (onScrollListeners != null) {
                        for (i in onScrollListeners.indices) {
                          recyclerView.removeOnScrollListener(onScrollListeners[i])
                        }
                      }

                      if (onItemTouchListener != null) {
                        recyclerView.removeOnItemTouchListener(onItemTouchListener)
                      }

                      recyclerView.setTouchInterceptor(null)

                      sectionsRecyclerView.setOnRefreshListener(null)
                    }
                  }
                },
        style = style)
  }

  class NoUpdateItemAnimator : DefaultItemAnimator() {
    init {
      supportsChangeAnimations = false
    }
  }
}

private class RecyclerLayoutBehavior(
    private val binder: Binder<RecyclerView>,
    private val onRemeasure: () -> Unit
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val size = Size()
    binder.measure(
        size,
        sizeConstraints.toWidthSpec(),
        sizeConstraints.toHeightSpec(),
        if (binder.canMeasure() || binder.isWrapContent) eventHandler { onRemeasure() } else null)
    return PrimitiveLayoutResult(
        max(sizeConstraints.minWidth, size.width), max(sizeConstraints.minHeight, size.height))
  }
}
