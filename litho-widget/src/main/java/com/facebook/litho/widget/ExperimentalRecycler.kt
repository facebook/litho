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
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.config.PrimitiveRecyclerBinderStrategy
import com.facebook.litho.eventHandler
import com.facebook.litho.useState
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
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
    private val binderStrategy: PrimitiveRecyclerBinderStrategy =
        PrimitiveRecyclerBinderStrategy.RECYCLER_SPEC_EQUIVALENT,
    val binder: Binder<RecyclerView>,
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
    private val itemAnimator: RecyclerView.ItemAnimator? = NoUpdateItemAnimator(),
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
    // This is the default value for refresh spinner background from RecyclerSpec.
    private const val DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR = 0xFFFAFAFA.toInt()
  }

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val measureVersion = useState { 0 }

    val mountBehavior =
        when (binderStrategy) {
          PrimitiveRecyclerBinderStrategy.RECYCLER_SPEC_EQUIVALENT ->
              RecyclerSpecEquivalentMountBehavior(
                  measureVersion, onRefresh, onScrollListeners, recyclerEventsController)
          PrimitiveRecyclerBinderStrategy.SPLIT_BINDERS ->
              SplitBindersMountBehavior(onRefresh, onScrollListeners, recyclerEventsController)
        }

    return LithoPrimitive(
        layoutBehavior = RecyclerLayoutBehavior(binder) { measureVersion.update { v -> v + 1 } },
        mountBehavior = mountBehavior,
        style = style)
  }

  private fun PrimitiveComponentScope.RecyclerSpecEquivalentMountBehavior(
      measureVersion: State<Int>,
      onRefresh: (() -> Unit)?,
      onScrollListeners: List<RecyclerView.OnScrollListener>?,
      recyclerEventsController: RecyclerEventsController?
  ): MountBehavior<SectionsRecyclerView> =
      MountBehavior(
          ViewAllocator { context -> SectionsRecyclerView(context, LithoRecyclerView(context)) }) {
            doesMountRenderTreeHosts = true
            shouldExcludeFromIncrementalMount = excludeFromIncrementalMount

            // RecyclerSpec's @OnMount and @OnUnmount
            withDescription("recycler-equivalent-mount") {
              bind(
                  measureVersion.value,
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
                  itemAnimator?.javaClass,
                  itemDecorations) { sectionsRecyclerView ->
                    val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

                    recyclerView.contentDescription = contentDescription
                    recyclerView.setHasFixedSize(hasFixedSize)
                    recyclerView.clipToPadding = isClipToPaddingEnabled
                    sectionsRecyclerView.clipToPadding = isClipToPaddingEnabled
                    if (!paddingAdditionDisabled) {
                      ViewCompat.setPaddingRelative(
                          recyclerView, leftPadding, topPadding, rightPadding, bottomPadding)
                    }
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

                    if (refreshProgressBarBackgroundColor != null) {
                      sectionsRecyclerView.setProgressBackgroundColorSchemeColor(
                          refreshProgressBarBackgroundColor)
                    }

                    sectionsRecyclerView.setColorSchemeColors(refreshProgressBarColor)

                    itemDecorations?.forEach { recyclerView.addItemDecoration(it) }

                    sectionsRecyclerView.setItemAnimator(itemAnimator)

                    binder.mount(recyclerView)

                    onUnbind {
                      recyclerView.id = View.NO_ID

                      if (refreshProgressBarBackgroundColor != null) {
                        sectionsRecyclerView.setProgressBackgroundColorSchemeColor(
                            DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR)
                      }

                      itemDecorations?.forEach { recyclerView.removeItemDecoration(it) }

                      if (edgeFactory != null) {
                        recyclerView.edgeEffectFactory =
                            sectionsRecyclerView.defaultEdgeEffectFactory
                      }

                      binder.unmount(recyclerView)

                      snapHelper?.attachToRecyclerView(null)

                      sectionsRecyclerView.resetItemAnimator()
                    }
                  }
            }

            // RecyclerSpec's @OnBind and @OnUnbind
            withDescription("recycler-equivalent-bind") {
              bind(Any()) { sectionsRecyclerView ->
                sectionsRecyclerView.setSectionsRecyclerViewLogger(sectionsViewLogger)

                // contentDescription should be set on the recyclerView itself, and not the
                // sectionsRecycler.
                sectionsRecyclerView.contentDescription = null

                sectionsRecyclerView.isEnabled = isPullToRefreshEnabled && onRefresh != null

                if (onRefresh != null) {
                  sectionsRecyclerView.setOnRefreshListener { onRefresh.invoke() }
                }

                val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

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
            }
          }

  /**
   * This is one [MountBehavior<SectionsRecyclerView>] that uses three different binders to mount
   * the content.
   *
   * The different mount binders are as follows:
   * - Content: This is responsible for mounting/unmounting all recycler view like properties
   *   related to the content and listeners.
   * - ItemDecorations: This will bind/unbind the [itemDecorations]
   * - Binder<RecyclerView>: This will bind/unbind the [binder].
   */
  private fun PrimitiveComponentScope.SplitBindersMountBehavior(
      onRefresh: (() -> Unit)?,
      onScrollListeners: List<RecyclerView.OnScrollListener>?,
      recyclerEventsController: RecyclerEventsController?
  ): MountBehavior<SectionsRecyclerView> =
      MountBehavior(
          ViewAllocator { context -> SectionsRecyclerView(context, LithoRecyclerView(context)) }) {
            doesMountRenderTreeHosts = true
            shouldExcludeFromIncrementalMount = excludeFromIncrementalMount

            withDescription("recycler-content") {
              bind(
                  contentDescription,
                  hasFixedSize,
                  isClipToPaddingEnabled,
                  paddingAdditionDisabled,
                  leftPadding,
                  topPadding,
                  rightPadding,
                  bottomPadding,
                  isClipChildrenEnabled,
                  isNestedScrollingEnabled,
                  scrollBarStyle,
                  isHorizontalFadingEdgeEnabled,
                  isVerticalFadingEdgeEnabled,
                  fadingEdgeLength,
                  recyclerViewId,
                  overScrollMode,
                  edgeFactory,
                  itemAnimator?.javaClass) { sectionsRecyclerView ->
                    val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

                    recyclerView.contentDescription = contentDescription
                    recyclerView.setHasFixedSize(hasFixedSize)
                    recyclerView.clipToPadding = isClipToPaddingEnabled
                    sectionsRecyclerView.clipToPadding = isClipToPaddingEnabled
                    if (!paddingAdditionDisabled) {
                      ViewCompat.setPaddingRelative(
                          recyclerView, leftPadding, topPadding, rightPadding, bottomPadding)
                    }

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

                    if (refreshProgressBarBackgroundColor != null) {
                      sectionsRecyclerView.setProgressBackgroundColorSchemeColor(
                          refreshProgressBarBackgroundColor)
                    }

                    sectionsRecyclerView.setColorSchemeColors(refreshProgressBarColor)

                    sectionsRecyclerView.setItemAnimator(itemAnimator)

                    sectionsRecyclerView.setSectionsRecyclerViewLogger(sectionsViewLogger)

                    // contentDescription should be set on the recyclerView itself, and not the
                    // sectionsRecycler.
                    sectionsRecyclerView.contentDescription = null

                    sectionsRecyclerView.isEnabled = isPullToRefreshEnabled && onRefresh != null

                    if (onRefresh != null) {
                      sectionsRecyclerView.setOnRefreshListener { onRefresh.invoke() }
                    }

                    // @OnBind in RecyclerSpec

                    // contentDescription should be set on the recyclerView itself, and not the
                    // sectionsRecycler.
                    sectionsRecyclerView.contentDescription = null

                    sectionsRecyclerView.isEnabled = isPullToRefreshEnabled && onRefresh != null

                    if (onRefresh != null) {
                      sectionsRecyclerView.setOnRefreshListener { onRefresh.invoke() }
                    }

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

                    if (recyclerEventsController != null) {
                      recyclerEventsController.setSectionsRecyclerView(sectionsRecyclerView)
                      recyclerEventsController.snapHelper = snapHelper
                    }

                    if (sectionsRecyclerView.hasBeenDetachedFromWindow()) {
                      recyclerView.requestLayout()
                      sectionsRecyclerView.setHasBeenDetachedFromWindow(false)
                    }

                    onUnbind {
                      recyclerView.id = View.NO_ID

                      if (refreshProgressBarBackgroundColor != null) {
                        sectionsRecyclerView.setProgressBackgroundColorSchemeColor(
                            DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR)
                      }

                      if (edgeFactory != null) {
                        recyclerView.edgeEffectFactory =
                            sectionsRecyclerView.defaultEdgeEffectFactory
                      }

                      snapHelper?.attachToRecyclerView(null)

                      sectionsRecyclerView.resetItemAnimator()

                      // @OnUnbind in RecyclerSpec
                      sectionsRecyclerView.setSectionsRecyclerViewLogger(null)

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
            }

            withDescription("recycler-decorations") {
              bind(itemDecorations) { sectionsRecyclerView ->
                val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

                itemDecorations?.forEach(recyclerView::addItemDecoration)

                onUnbind { itemDecorations?.forEach(recyclerView::removeItemDecoration) }
              }
            }

            withDescription("recycler-binder") {
              bind(binder) { sectionsRecyclerView ->
                val recyclerView: RecyclerView = sectionsRecyclerView.requireLithoRecyclerView()

                binder.mount(recyclerView)
                binder.bind(recyclerView)

                onUnbind {
                  binder.unbind(recyclerView)
                  binder.unmount(recyclerView)
                }
              }
            }
          }

  class NoUpdateItemAnimator : DefaultItemAnimator() {
    init {
      supportsChangeAnimations = false
    }
  }
}

private fun SectionsRecyclerView.requireLithoRecyclerView(): LithoRecyclerView =
    recyclerView as? LithoRecyclerView
        ?: throw java.lang.IllegalStateException(
            "RecyclerView not found, it should not be removed from SwipeRefreshLayout")

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
