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
import android.graphics.Color
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.SnapHelper
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Size
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.config.PrimitiveRecyclerBinderStrategy
import com.facebook.litho.eventHandler
import com.facebook.litho.useState
import com.facebook.litho.widget.LithoRecyclerView.OnAfterLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.OnBeforeLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.TouchInterceptor
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

class Recycler
@JvmOverloads
constructor(
    val binder: Binder<RecyclerView>,
    private val binderStrategy: PrimitiveRecyclerBinderStrategy? = null,
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
    private val isLeftFadingEnabled: Boolean = true,
    private val isRightFadingEnabled: Boolean = true,
    private val isTopFadingEnabled: Boolean = true,
    private val isBottomFadingEnabled: Boolean = true,
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
    private val onBeforeLayoutListener: OnBeforeLayoutListener? = null,
    private val onAfterLayoutListener: OnAfterLayoutListener? = null,
    private val style: Style? = null
) : PrimitiveComponent() {

  companion object {
    // This is the default value for refresh spinner background from RecyclerSpec.
    internal const val DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR = 0xFFFAFAFA.toInt()
  }

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val measureVersion = useState { 0 }

    val measureChild: State<View.() -> Unit> = useState {
      {
        val position = (layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
        measure(binder.getChildWidthSpec(position), binder.getChildWidthSpec(position))
      }
    }

    val mountBehavior =
        when (binderStrategy
            ?: context.lithoConfiguration.componentsConfig.primitiveRecyclerBinderStrategy) {
          PrimitiveRecyclerBinderStrategy.SPLIT_BINDERS ->
              SplitBindersMountBehavior(
                  measureVersion,
                  onRefresh,
                  onScrollListeners,
                  recyclerEventsController,
                  onBeforeLayoutListener,
                  onAfterLayoutListener,
                  measureChild.value,
              )
          PrimitiveRecyclerBinderStrategy.DEFAULT ->
              DefaultMountBehavior(
                  measureVersion,
                  onRefresh,
                  onScrollListeners,
                  recyclerEventsController,
                  onBeforeLayoutListener,
                  onAfterLayoutListener,
                  measureChild.value,
              )
        }

    return LithoPrimitive(
        layoutBehavior =
            RecyclerLayoutBehavior(
                binder = binder,
                startPadding = leftPadding,
                endPadding = rightPadding,
                topPadding = topPadding,
                bottomPadding = bottomPadding) {
                  measureVersion.update { v -> v + 1 }
                },
        mountBehavior = mountBehavior,
        style = style)
  }

  private fun PrimitiveComponentScope.DefaultMountBehavior(
      measureVersion: State<Int>,
      onRefresh: (() -> Unit)?,
      onScrollListeners: List<RecyclerView.OnScrollListener>?,
      recyclerEventsController: RecyclerEventsController?,
      onBeforeLayoutListener: OnBeforeLayoutListener?,
      onAfterLayoutListener: OnAfterLayoutListener?,
      measureChild: View.() -> Unit
  ): MountBehavior<SectionsRecyclerView> =
      MountBehavior(ViewAllocator { context -> createSectionsRecyclerView(context) }) {
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
              itemAnimator?.javaClass) { sectionsRecyclerView ->
                val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

                bindLegacyMountBinder(
                    sectionsRecyclerView = sectionsRecyclerView,
                    contentDescription = contentDescription,
                    hasFixedSize = hasFixedSize,
                    isClipToPaddingEnabled = isClipToPaddingEnabled,
                    paddingAdditionDisabled = paddingAdditionDisabled,
                    leftPadding = leftPadding,
                    topPadding = topPadding,
                    rightPadding = rightPadding,
                    bottomPadding = bottomPadding,
                    isClipChildrenEnabled = isClipChildrenEnabled,
                    isNestedScrollingEnabled = isNestedScrollingEnabled,
                    scrollBarStyle = scrollBarStyle,
                    isHorizontalFadingEdgeEnabled = isHorizontalFadingEdgeEnabled,
                    isVerticalFadingEdgeEnabled = isVerticalFadingEdgeEnabled,
                    isLeftFadingEnabled = isLeftFadingEnabled,
                    isRightFadingEnabled = isRightFadingEnabled,
                    isTopFadingEnabled = isTopFadingEnabled,
                    isBottomFadingEnabled = isBottomFadingEnabled,
                    fadingEdgeLength = fadingEdgeLength,
                    recyclerViewId = recyclerViewId,
                    overScrollMode = overScrollMode,
                    edgeFactory = edgeFactory,
                    refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
                    refreshProgressBarColor = refreshProgressBarColor,
                    itemAnimator = itemAnimator)

                binder.mount(recyclerView)

                onUnbind {
                  unbindLegacyMountBinder(
                      sectionsRecyclerView = sectionsRecyclerView,
                      refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
                      edgeFactory = edgeFactory,
                      snapHelper = snapHelper)

                  binder.unmount(recyclerView)
                }
              }
        }

        withDescription("recycler-before-layout") {
          bind(onBeforeLayoutListener) { sectionsRecyclerView ->
            val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
            onBeforeLayoutListener?.let { recyclerView.setOnBeforeLayoutListener(it) }
            onUnbind { recyclerView.setOnBeforeLayoutListener(null) }
          }
        }

        withDescription("recycler-after-layout") {
          bind(onAfterLayoutListener) { sectionsRecyclerView ->
            val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
            onAfterLayoutListener?.let { recyclerView.setOnAfterLayoutListener(it) }
            onUnbind { recyclerView.setOnAfterLayoutListener(null) }
          }
        }

        withDescription("recycler-equivalent-bind") {
          bind(Any()) { sectionsRecyclerView ->
            bindLegacyAttachBinder(
                sectionsRecyclerView = sectionsRecyclerView,
                sectionsViewLogger = sectionsViewLogger,
                isPullToRefreshEnabled = isPullToRefreshEnabled,
                onRefresh = onRefresh,
                onScrollListeners = onScrollListeners,
                touchInterceptor = touchInterceptor,
                onItemTouchListener = onItemTouchListener,
                snapHelper = snapHelper,
                recyclerEventsController = recyclerEventsController)

            onUnbind {
              unbindLegacyAttachBinder(
                  sectionsRecyclerView = sectionsRecyclerView,
                  recyclerEventsController = recyclerEventsController,
                  onScrollListeners = onScrollListeners,
                  onItemTouchListener = onItemTouchListener)
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
      measureVersion: State<Int>,
      onRefresh: (() -> Unit)?,
      onScrollListeners: List<RecyclerView.OnScrollListener>?,
      recyclerEventsController: RecyclerEventsController?,
      onBeforeLayoutListener: OnBeforeLayoutListener?,
      onAfterLayoutListener: OnAfterLayoutListener?,
      measureChild: View.() -> Unit
  ): MountBehavior<SectionsRecyclerView> =
      MountBehavior(ViewAllocator { context -> createSectionsRecyclerView(context) }) {
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
              measureVersion.value,
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
              itemAnimator?.javaClass) { sectionsRecyclerView ->
                bindLegacyMountBinder(
                    sectionsRecyclerView = sectionsRecyclerView,
                    contentDescription = contentDescription,
                    hasFixedSize = hasFixedSize,
                    isClipToPaddingEnabled = isClipToPaddingEnabled,
                    paddingAdditionDisabled = paddingAdditionDisabled,
                    leftPadding = leftPadding,
                    topPadding = topPadding,
                    rightPadding = rightPadding,
                    bottomPadding = bottomPadding,
                    isClipChildrenEnabled = isClipChildrenEnabled,
                    isNestedScrollingEnabled = isNestedScrollingEnabled,
                    scrollBarStyle = scrollBarStyle,
                    isHorizontalFadingEdgeEnabled = isHorizontalFadingEdgeEnabled,
                    isVerticalFadingEdgeEnabled = isVerticalFadingEdgeEnabled,
                    isLeftFadingEnabled = isLeftFadingEnabled,
                    isRightFadingEnabled = isRightFadingEnabled,
                    isTopFadingEnabled = isTopFadingEnabled,
                    isBottomFadingEnabled = isBottomFadingEnabled,
                    fadingEdgeLength = fadingEdgeLength,
                    recyclerViewId = recyclerViewId,
                    overScrollMode = overScrollMode,
                    edgeFactory = edgeFactory,
                    refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
                    refreshProgressBarColor = refreshProgressBarColor,
                    itemAnimator = itemAnimator)

                onUnbind {
                  unbindLegacyMountBinder(
                      sectionsRecyclerView = sectionsRecyclerView,
                      refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
                      edgeFactory = edgeFactory,
                      snapHelper = snapHelper)
                }
              }
        }

        withDescription("recycler-before-layout") {
          bind(onBeforeLayoutListener) { sectionsRecyclerView ->
            val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
            onBeforeLayoutListener?.let { recyclerView.setOnBeforeLayoutListener(it) }
            onUnbind { recyclerView.setOnBeforeLayoutListener(null) }
          }
        }

        withDescription("recycler-after-layout") {
          bind(onAfterLayoutListener) { sectionsRecyclerView ->
            val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
            onAfterLayoutListener?.let { recyclerView.setOnAfterLayoutListener(it) }
            onUnbind { recyclerView.setOnAfterLayoutListener(null) }
          }
        }

        withDescription("recycler-binder") {
          bind(binder) { sectionsRecyclerView ->
            val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()
            binder.mount(recyclerView)
            onUnbind { binder.unmount(recyclerView) }
          }
        }

        withDescription("recycler-equivalent-bind") {
          bind(Any()) { sectionsRecyclerView ->
            bindLegacyAttachBinder(
                sectionsRecyclerView = sectionsRecyclerView,
                sectionsViewLogger = sectionsViewLogger,
                isPullToRefreshEnabled = isPullToRefreshEnabled,
                onRefresh = onRefresh,
                onScrollListeners = onScrollListeners,
                touchInterceptor = touchInterceptor,
                onItemTouchListener = onItemTouchListener,
                snapHelper = snapHelper,
                recyclerEventsController = recyclerEventsController)

            onUnbind {
              unbindLegacyAttachBinder(
                  sectionsRecyclerView = sectionsRecyclerView,
                  recyclerEventsController = recyclerEventsController,
                  onScrollListeners = onScrollListeners,
                  onItemTouchListener = onItemTouchListener)
            }
          }
        }
      }

  private fun createSectionsRecyclerView(context: Context): SectionsRecyclerView =
      SectionsRecyclerView(context, LithoRecyclerView(context)).apply {
        id = R.id.recycler_view_container_id
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
    private val startPadding: Int,
    private val endPadding: Int,
    private val topPadding: Int,
    private val bottomPadding: Int,
    private val onRemeasure: () -> Unit
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val size = Size()

    binder.measure(
        size,
        maybeGetSpecWithPadding(sizeConstraints.toWidthSpec(), startPadding + endPadding),
        maybeGetSpecWithPadding(sizeConstraints.toHeightSpec(), topPadding + bottomPadding),
        if (binder.isCrossAxisWrapContent || binder.isMainAxisWrapContent)
            eventHandler { onRemeasure() }
        else null)
    return PrimitiveLayoutResult(
        max(sizeConstraints.minWidth, size.width), max(sizeConstraints.minHeight, size.height))
  }
}

/**
 * Binds the [sectionsRecyclerView] to the data according to the behavior that was present in the
 * initial version of the RecyclerSpec regarding mount binders (previously `RecyclerSpec.onMount`).
 */
private fun ResourcesScope.bindLegacyMountBinder(
    sectionsRecyclerView: SectionsRecyclerView,
    contentDescription: CharSequence?,
    hasFixedSize: Boolean,
    isClipToPaddingEnabled: Boolean,
    paddingAdditionDisabled: Boolean,
    leftPadding: Int,
    topPadding: Int,
    rightPadding: Int,
    bottomPadding: Int,
    isClipChildrenEnabled: Boolean,
    isNestedScrollingEnabled: Boolean,
    scrollBarStyle: Int,
    isHorizontalFadingEdgeEnabled: Boolean,
    isVerticalFadingEdgeEnabled: Boolean,
    isLeftFadingEnabled: Boolean,
    isRightFadingEnabled: Boolean,
    isTopFadingEnabled: Boolean,
    isBottomFadingEnabled: Boolean,
    fadingEdgeLength: Int,
    @IdRes recyclerViewId: Int,
    overScrollMode: Int,
    edgeFactory: RecyclerView.EdgeEffectFactory?,
    @ColorInt refreshProgressBarBackgroundColor: Int?,
    @ColorInt refreshProgressBarColor: Int,
    itemAnimator: ItemAnimator?
): LithoRecyclerView {
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
  recyclerView.setLeftFadingEnabled(isLeftFadingEnabled)
  recyclerView.setRightFadingEnabled(isRightFadingEnabled)
  recyclerView.setTopFadingEnabled(isTopFadingEnabled)
  recyclerView.setBottomFadingEnabled(isBottomFadingEnabled)
  recyclerView.setFadingEdgeLength(fadingEdgeLength.dp.toPixels())
  recyclerView.id = recyclerViewId
  recyclerView.overScrollMode = overScrollMode
  edgeFactory?.let { recyclerView.edgeEffectFactory = it }

  if (refreshProgressBarBackgroundColor != null) {
    sectionsRecyclerView.setProgressBackgroundColorSchemeColor(refreshProgressBarBackgroundColor)
  }

  sectionsRecyclerView.setColorSchemeColors(refreshProgressBarColor)

  sectionsRecyclerView.setItemAnimator(itemAnimator)

  return recyclerView
}

/**
 * Unbinds the [sectionsRecyclerView] to the data according to the behavior that was present in the
 * initial version of the RecyclerSpec regarding mount binders (previously
 * `RecyclerSpec.onUnmount`).
 */
private fun unbindLegacyMountBinder(
    sectionsRecyclerView: SectionsRecyclerView,
    @ColorInt refreshProgressBarBackgroundColor: Int?,
    edgeFactory: RecyclerView.EdgeEffectFactory?,
    snapHelper: SnapHelper?
) {
  val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

  recyclerView.id = View.NO_ID

  if (refreshProgressBarBackgroundColor != null) {
    sectionsRecyclerView.setProgressBackgroundColorSchemeColor(
        Recycler.DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR)
  }

  if (edgeFactory != null) {
    recyclerView.edgeEffectFactory = sectionsRecyclerView.defaultEdgeEffectFactory
  }

  snapHelper?.attachToRecyclerView(null)

  sectionsRecyclerView.resetItemAnimator()
}

/**
 * Binds the [sectionsRecyclerView] to the data according to the behavior that was present in the
 * initial version of the RecyclerSpec regarding the attach binders (previously
 * RecyclerSpec.onBind).
 */
private fun bindLegacyAttachBinder(
    sectionsRecyclerView: SectionsRecyclerView,
    sectionsViewLogger: SectionsRecyclerView.SectionsRecyclerViewLogger?,
    isPullToRefreshEnabled: Boolean,
    onRefresh: (() -> Unit)?,
    onScrollListeners: List<RecyclerView.OnScrollListener>?,
    touchInterceptor: TouchInterceptor?,
    onItemTouchListener: RecyclerView.OnItemTouchListener?,
    snapHelper: SnapHelper?,
    recyclerEventsController: RecyclerEventsController?
): LithoRecyclerView {
  val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

  sectionsRecyclerView.setSectionsRecyclerViewLogger(sectionsViewLogger)

  // contentDescription should be set on the recyclerView itself, and not the
  // sectionsRecycler.
  sectionsRecyclerView.contentDescription = null

  sectionsRecyclerView.isEnabled = isPullToRefreshEnabled && onRefresh != null

  if (onRefresh != null) {
    sectionsRecyclerView.setOnRefreshListener { onRefresh() }
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

  return recyclerView
}

/**
 * Unbinds the [sectionsRecyclerView] to the data according to the behavior that was present in the
 * initial version of the RecyclerSpec regarding the attach binders (previously
 * `RecyclerSpec.onUnbind`).
 */
private fun unbindLegacyAttachBinder(
    sectionsRecyclerView: SectionsRecyclerView,
    recyclerEventsController: RecyclerEventsController?,
    onScrollListeners: List<RecyclerView.OnScrollListener>?,
    onItemTouchListener: RecyclerView.OnItemTouchListener?,
) {
  val recyclerView = sectionsRecyclerView.requireLithoRecyclerView()

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
