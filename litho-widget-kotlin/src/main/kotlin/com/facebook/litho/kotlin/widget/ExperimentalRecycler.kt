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

package com.facebook.litho.kotlin.widget

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.SnapHelper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.litho.Dimen
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.Size
import com.facebook.litho.Style
import com.facebook.litho.dp
import com.facebook.litho.eventHandler
import com.facebook.litho.sections.widget.NoUpdateItemAnimator
import com.facebook.litho.useState
import com.facebook.litho.widget.Binder as LithoBinder
import com.facebook.litho.widget.LithoRecyclerView
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.SectionsRecyclerView
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.Mountable
import com.facebook.rendercore.RenderState

class ExperimentalRecycler(
    private val binder: LithoBinder<RecyclerView>,
    private val hasFixedSize: Boolean = true,
    private val isClipToPaddingEnabled: Boolean = true,
    private @ColorInt val refreshProgressBarBackgroundColor: Int? = null,
    private @ColorInt val refreshProgressBarColor: Int = Color.BLACK,
    private val isClipChildrenEnabled: Boolean = true,
    private val isNestedScrollingEnabled: Boolean = true,
    private val scrollBarStyle: Int = View.SCROLLBARS_INSIDE_OVERLAY,
    private val itemDecoration: ItemDecoration? = null,
    private val isHorizontalFadingEdgeEnabled: Boolean = false,
    private val isVerticalFadingEdgeEnabled: Boolean = false,
    private val fadingEdgeLength: Dimen = 0.dp,
    private @IdRes val recyclerViewId: Int = View.NO_ID,
    private val overScrollMode: Int = View.OVER_SCROLL_ALWAYS,
    private val contentDescription: CharSequence? = null,
    private val itemAnimator: ItemAnimator = NoUpdateItemAnimator(),
    private val recyclerEventsController: RecyclerEventsController? = null,
    private val onScrollListeners: List<RecyclerView.OnScrollListener?> = emptyList(),
    private val snapHelper: SnapHelper? = null,
    private val isPullToRefreshEnabled: Boolean = true,
    private val touchInterceptor: LithoRecyclerView.TouchInterceptor? = null,
    private val onItemTouchListener: RecyclerView.OnItemTouchListener? = null,
    private val onRefresh: (() -> Unit)? = null,
    private val sectionsViewLogger: SectionsRecyclerView.SectionsRecyclerViewLogger? = null,
    private val useTwoBindersRecycler: Boolean = false,
    private val style: Style? = null
) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    val measureVersion = useState { 0 }
    return MountableRenderResult(
        ExperimentalRecyclerMountable(
            binder = binder,
            hasFixedSize = hasFixedSize,
            isClipToPaddingEnabled = isClipToPaddingEnabled,
            refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
            refreshProgressBarColor = refreshProgressBarColor,
            isClipChildrenEnabled = isClipChildrenEnabled,
            isNestedScrollingEnabled = isNestedScrollingEnabled,
            scrollBarStyle = scrollBarStyle,
            itemDecoration = itemDecoration,
            isHorizontalFadingEdgeEnabled = isHorizontalFadingEdgeEnabled,
            isVerticalFadingEdgeEnabled = isVerticalFadingEdgeEnabled,
            fadingEdgeLength = fadingEdgeLength.toPixels(),
            recyclerViewId = recyclerViewId,
            overScrollMode = overScrollMode,
            contentDescription = contentDescription,
            itemAnimator = itemAnimator,
            recyclerEventsController = recyclerEventsController,
            onScrollListeners = onScrollListeners,
            snapHelper = snapHelper,
            isPullToRefreshEnabled = isPullToRefreshEnabled,
            touchInterceptor = touchInterceptor,
            onItemTouchListener = onItemTouchListener,
            onRefresh = onRefresh,
            sectionsViewLogger = sectionsViewLogger,
            useTwoBindersRecycler = useTwoBindersRecycler,
            onRemeasure = { measureVersion.update { m -> m + 1 } }),
        style)
  }
}

internal class ExperimentalRecyclerMountable(
    private val binder: LithoBinder<RecyclerView>,
    private val hasFixedSize: Boolean,
    private val isClipToPaddingEnabled: Boolean,
    private @ColorInt val refreshProgressBarBackgroundColor: Int?,
    private @ColorInt val refreshProgressBarColor: Int,
    private val isClipChildrenEnabled: Boolean,
    private val isNestedScrollingEnabled: Boolean,
    private val scrollBarStyle: Int,
    private val itemDecoration: ItemDecoration?,
    private val isHorizontalFadingEdgeEnabled: Boolean,
    private val isVerticalFadingEdgeEnabled: Boolean,
    private val fadingEdgeLength: Int,
    private @IdRes val recyclerViewId: Int,
    private val overScrollMode: Int,
    private val contentDescription: CharSequence?,
    private val itemAnimator: ItemAnimator,
    private val recyclerEventsController: RecyclerEventsController?,
    private val onScrollListeners: List<RecyclerView.OnScrollListener?>,
    private val snapHelper: SnapHelper?,
    private val isPullToRefreshEnabled: Boolean,
    private val touchInterceptor: LithoRecyclerView.TouchInterceptor?,
    private val onItemTouchListener: RecyclerView.OnItemTouchListener?,
    private val onRefresh: (() -> Unit)?,
    private val sectionsViewLogger: SectionsRecyclerView.SectionsRecyclerViewLogger?,
    private val useTwoBindersRecycler: Boolean,
    private val onRemeasure: () -> Unit
) : Mountable<SectionsRecyclerView>(RenderType.VIEW), ContentAllocator {

  companion object {
    private val CONSTANT_PROPS_ATTACH_BINDER:
        Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> =
        object : Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> {
          override fun shouldUpdate(
              currentModel: ExperimentalRecyclerMountable,
              newModel: ExperimentalRecyclerMountable,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean = true

          override fun bind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            with(model) {
              sectionsViewLogger?.let { content.setSectionsRecyclerViewLogger(sectionsViewLogger) }
              // contentDescription should be set on the recyclerView itself, and not the
              // sectionsRecycler.
              content.contentDescription = null
              content.setEnabled(isPullToRefreshEnabled && onRefresh != null)
              content.setOnRefreshListener(
                  if (onRefresh != null) SwipeRefreshLayout.OnRefreshListener { onRefresh.invoke() }
                  else null)
              if (onScrollListeners.isNotEmpty()) {
                for (onScrollListener in onScrollListeners) {
                  onScrollListener?.let { content.recyclerView.addOnScrollListener(it) }
                }
              }
              touchInterceptor?.let {
                (content.recyclerView as LithoRecyclerView).setTouchInterceptor(touchInterceptor)
              }
              onItemTouchListener?.let {
                content.recyclerView.addOnItemTouchListener(onItemTouchListener)
              }
              // We cannot detach the snap helper in unbind, so it may be possible for it to get
              // attached twice which causes SnapHelper to raise an exception.
              if (snapHelper != null && content.recyclerView.getOnFlingListener() == null) {
                snapHelper.attachToRecyclerView(content.recyclerView)
              }
              recyclerEventsController?.setSectionsRecyclerView(content)
              if (content.hasBeenDetachedFromWindow()) {
                content.recyclerView.requestLayout()
                content.setHasBeenDetachedFromWindow(false)
              }
            }
          }

          override fun unbind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            content.contentDescription = null
            content.setSectionsRecyclerViewLogger(null)
            content.setOnRefreshListener(null)
            if (model.snapHelper != null) {
              model.snapHelper.attachToRecyclerView(null)
            }
            model.recyclerEventsController?.setSectionsRecyclerView(null)
            if (model.onScrollListeners.isNotEmpty()) {
              for (onScrollListener in model.onScrollListeners) {
                onScrollListener?.let { content.recyclerView.removeOnScrollListener(it) }
              }
            }

            (content.recyclerView as LithoRecyclerView).setTouchInterceptor(null)

            model.onItemTouchListener?.let {
              content.recyclerView.removeOnItemTouchListener(model.onItemTouchListener)
            }
          }
        }

    private val CONSTANT_PROPS_MOUNT_BINDER:
        Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> =
        object : Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> {
          override fun shouldUpdate(
              currentModel: ExperimentalRecyclerMountable,
              newModel: ExperimentalRecyclerMountable,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean =
              currentModel.hasFixedSize != newModel.hasFixedSize &&
                  currentModel.contentDescription != newModel.contentDescription &&
                  currentModel.isClipToPaddingEnabled != newModel.isClipToPaddingEnabled &&
                  currentModel.isClipChildrenEnabled != newModel.isClipChildrenEnabled &&
                  currentModel.scrollBarStyle != newModel.scrollBarStyle &&
                  currentModel.refreshProgressBarBackgroundColor !=
                      newModel.refreshProgressBarBackgroundColor &&
                  currentModel.refreshProgressBarColor != newModel.refreshProgressBarColor &&
                  currentModel.isHorizontalFadingEdgeEnabled !=
                      newModel.isHorizontalFadingEdgeEnabled &&
                  currentModel.isVerticalFadingEdgeEnabled !=
                      newModel.isVerticalFadingEdgeEnabled &&
                  currentModel.fadingEdgeLength != newModel.fadingEdgeLength &&
                  currentModel.itemAnimator != newModel.itemAnimator &&
                  currentModel.overScrollMode != newModel.overScrollMode

          override fun bind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            with(model) {
              contentDescription?.let { content.contentDescription = contentDescription }
              content.recyclerView.setHasFixedSize(hasFixedSize)
              content.recyclerView.clipToPadding = isClipToPaddingEnabled
              content.clipToPadding = isClipToPaddingEnabled
              content.recyclerView.clipChildren = isClipChildrenEnabled
              content.clipChildren = isClipChildrenEnabled
              content.recyclerView.isNestedScrollingEnabled = isNestedScrollingEnabled
              content.isNestedScrollingEnabled = isNestedScrollingEnabled
              content.recyclerView.scrollBarStyle = scrollBarStyle
              content.recyclerView.isHorizontalFadingEdgeEnabled = isHorizontalFadingEdgeEnabled
              content.recyclerView.isVerticalFadingEdgeEnabled = isVerticalFadingEdgeEnabled
              content.recyclerView.setFadingEdgeLength(fadingEdgeLength)
              content.recyclerView.id = recyclerViewId
              content.recyclerView.overScrollMode = overScrollMode
              if (refreshProgressBarBackgroundColor != null) {
                content.setProgressBackgroundColorSchemeColor(refreshProgressBarBackgroundColor)
              }
              content.setColorSchemeColors(refreshProgressBarColor)
              content.setItemAnimator(
                  if (itemAnimator != content.recyclerView.itemAnimator) itemAnimator
                  else NoUpdateItemAnimator())
            }
          }

          override fun unbind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            content.recyclerView.setHasFixedSize(false)
            content.recyclerView.clipToPadding = false
            content.clipToPadding = false
            content.recyclerView.clipChildren = false
            content.clipChildren = false
            content.recyclerView.isNestedScrollingEnabled = false
            content.recyclerView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            content.recyclerView.id = View.NO_ID
            content.recyclerView.isHorizontalFadingEdgeEnabled = false
            content.recyclerView.isVerticalFadingEdgeEnabled = false
            content.recyclerView.setFadingEdgeLength(0)
            content.setProgressBackgroundColorSchemeColor(0)
            content.setColorSchemeColors(0)
            content.recyclerView.overScrollMode = View.OVER_SCROLL_ALWAYS
            content.resetItemAnimator()
          }
        }

    private val ITEM_DECORATOR_BINDER: Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> =
        object : Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> {
          override fun shouldUpdate(
              currentModel: ExperimentalRecyclerMountable,
              newModel: ExperimentalRecyclerMountable,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean = currentModel.itemDecoration != newModel.itemDecoration

          override fun bind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            model.itemDecoration?.let { content.recyclerView.addItemDecoration(it) }
          }

          override fun unbind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            model.itemDecoration?.let { content.recyclerView.removeItemDecoration(it) }
          }
        }

    private val CONTENT_MOUNT_BINDER: Binder<LithoBinder<RecyclerView>, SectionsRecyclerView> =
        object : Binder<LithoBinder<RecyclerView>, SectionsRecyclerView> {
          override fun shouldUpdate(
              currentModel: LithoBinder<RecyclerView>,
              newModel: LithoBinder<RecyclerView>,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean = currentModel != newModel

          override fun bind(
              context: Context,
              content: SectionsRecyclerView,
              binder: LithoBinder<RecyclerView>,
              layoutData: Any?
          ) {
            binder.mount(content.recyclerView)
          }

          override fun unbind(
              context: Context,
              content: SectionsRecyclerView,
              binder: LithoBinder<RecyclerView>,
              layoutData: Any?
          ) {
            binder.unmount(content.recyclerView)
          }
        }

    private val CONTENT_ATTACH_BINDER: Binder<LithoBinder<RecyclerView>, SectionsRecyclerView> =
        object : Binder<LithoBinder<RecyclerView>, SectionsRecyclerView> {
          override fun shouldUpdate(
              currentModel: LithoBinder<RecyclerView>,
              newModel: LithoBinder<RecyclerView>,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean = true

          override fun bind(
              context: Context,
              content: SectionsRecyclerView,
              binder: LithoBinder<RecyclerView>,
              layoutData: Any?
          ) {
            binder.bind(content.recyclerView)
          }

          override fun unbind(
              context: Context,
              content: SectionsRecyclerView,
              binder: LithoBinder<RecyclerView>,
              layoutData: Any?
          ) {
            binder.unbind(content.recyclerView)
          }
        }

    private val MOUNT_BINDER: Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> =
        object : Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> {
          override fun shouldUpdate(
              currentModel: ExperimentalRecyclerMountable,
              newModel: ExperimentalRecyclerMountable,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean =
              currentModel.hasFixedSize != newModel.hasFixedSize &&
                  currentModel.isClipToPaddingEnabled != newModel.isClipToPaddingEnabled &&
                  currentModel.isClipChildrenEnabled != newModel.isClipChildrenEnabled &&
                  currentModel.scrollBarStyle != newModel.scrollBarStyle &&
                  currentModel.refreshProgressBarBackgroundColor !=
                      newModel.refreshProgressBarBackgroundColor &&
                  currentModel.refreshProgressBarColor != newModel.refreshProgressBarColor &&
                  currentModel.isHorizontalFadingEdgeEnabled !=
                      newModel.isHorizontalFadingEdgeEnabled &&
                  currentModel.isVerticalFadingEdgeEnabled !=
                      newModel.isVerticalFadingEdgeEnabled &&
                  currentModel.fadingEdgeLength != newModel.fadingEdgeLength &&
                  currentModel.itemAnimator != newModel.itemAnimator &&
                  currentModel.contentDescription != newModel.contentDescription &&
                  currentModel.isNestedScrollingEnabled != newModel.isNestedScrollingEnabled &&
                  currentModel.contentDescription != newModel.contentDescription &&
                  currentModel.recyclerViewId != newModel.recyclerViewId &&
                  currentModel.overScrollMode != newModel.overScrollMode &&
                  currentModel.itemDecoration != newModel.itemDecoration

          override fun bind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            with(model) {
              contentDescription?.let { content.contentDescription = contentDescription }
              content.recyclerView.setHasFixedSize(hasFixedSize)
              content.recyclerView.clipToPadding = isClipToPaddingEnabled
              content.clipToPadding = isClipToPaddingEnabled
              content.recyclerView.clipChildren = isClipChildrenEnabled
              content.clipChildren = isClipChildrenEnabled
              content.recyclerView.isNestedScrollingEnabled = isNestedScrollingEnabled
              content.isNestedScrollingEnabled = isNestedScrollingEnabled
              content.recyclerView.scrollBarStyle = scrollBarStyle
              content.recyclerView.isHorizontalFadingEdgeEnabled = isHorizontalFadingEdgeEnabled
              content.recyclerView.isVerticalFadingEdgeEnabled = isVerticalFadingEdgeEnabled
              content.recyclerView.setFadingEdgeLength(fadingEdgeLength)
              content.recyclerView.id = recyclerViewId
              content.recyclerView.overScrollMode = overScrollMode
              if (refreshProgressBarBackgroundColor != null) {
                content.setProgressBackgroundColorSchemeColor(refreshProgressBarBackgroundColor)
              }
              content.setColorSchemeColors(refreshProgressBarColor)
              itemDecoration?.let { content.recyclerView.addItemDecoration(it) }
              content.setItemAnimator(
                  if (itemAnimator != content.recyclerView.itemAnimator) itemAnimator
                  else NoUpdateItemAnimator())
              binder.mount(content.recyclerView)
            }
          }

          override fun unbind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            content.recyclerView.id = View.NO_ID
            content.setProgressBackgroundColorSchemeColor(0)
            model.itemDecoration?.let {
              content.recyclerView.removeItemDecoration(model.itemDecoration)
            }
            model.binder.unmount(content.recyclerView)
            if (model.snapHelper != null) {
              model.snapHelper.attachToRecyclerView(null)
            }
            content.resetItemAnimator()
          }
        }

    private val ATTACH_BINDER: Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> =
        object : Binder<ExperimentalRecyclerMountable, SectionsRecyclerView> {
          override fun shouldUpdate(
              currentModel: ExperimentalRecyclerMountable,
              newModel: ExperimentalRecyclerMountable,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean =
              currentModel.sectionsViewLogger != newModel.sectionsViewLogger &&
                  currentModel.contentDescription != newModel.contentDescription &&
                  currentModel.isPullToRefreshEnabled != newModel.isPullToRefreshEnabled &&
                  currentModel.onScrollListeners != newModel.onScrollListeners &&
                  currentModel.touchInterceptor != newModel.touchInterceptor &&
                  currentModel.onItemTouchListener != newModel.onItemTouchListener &&
                  currentModel.snapHelper != newModel.snapHelper &&
                  currentModel.binder != newModel.binder &&
                  currentModel.recyclerEventsController != newModel.recyclerEventsController

          override fun bind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            with(model) {
              sectionsViewLogger?.let { content.setSectionsRecyclerViewLogger(sectionsViewLogger) }
              content.contentDescription = null
              content.setEnabled(isPullToRefreshEnabled && onRefresh != null)
              content.setOnRefreshListener(
                  if (onRefresh != null) SwipeRefreshLayout.OnRefreshListener { onRefresh.invoke() }
                  else null)
              if (onScrollListeners.isNotEmpty()) {
                for (onScrollListener in onScrollListeners) {
                  onScrollListener?.let { content.recyclerView.addOnScrollListener(it) }
                }
              }
              touchInterceptor?.let {
                (content.recyclerView as LithoRecyclerView).setTouchInterceptor(touchInterceptor)
              }
              onItemTouchListener?.let {
                content.recyclerView.addOnItemTouchListener(onItemTouchListener)
              }
              // attached twice which causes SnapHelper to raise an exception.
              if (snapHelper != null && content.recyclerView.getOnFlingListener() == null) {
                snapHelper.attachToRecyclerView(content.recyclerView)
              }
              binder.bind(content.recyclerView)
              recyclerEventsController?.setSectionsRecyclerView(content)
              if (content.hasBeenDetachedFromWindow()) {
                content.recyclerView.requestLayout()
                content.setHasBeenDetachedFromWindow(false)
              }
            }
          }

          override fun unbind(
              context: Context,
              content: SectionsRecyclerView,
              model: ExperimentalRecyclerMountable,
              layoutData: Any?
          ) {
            content.setSectionsRecyclerViewLogger(null)
            model.binder.unbind(content.recyclerView)
            model.recyclerEventsController?.setSectionsRecyclerView(null)
            if (model.onScrollListeners.isNotEmpty()) {
              for (onScrollListener in model.onScrollListeners) {
                onScrollListener?.let { content.recyclerView.removeOnScrollListener(it) }
              }
            }
            model.onItemTouchListener?.let {
              content.recyclerView.removeOnItemTouchListener(model.onItemTouchListener)
            }

            (content.recyclerView as LithoRecyclerView).setTouchInterceptor(null)

            content.setOnRefreshListener(null)
          }
        }
  }

  init {
    if (useTwoBindersRecycler) {
      addMountBinders(
          DelegateBinder.createDelegateBinder(this, MOUNT_BINDER),
      )
      addAttachBinder(
          DelegateBinder.createDelegateBinder(this, ATTACH_BINDER),
      )
    } else {
      addMountBinders(
          DelegateBinder.createDelegateBinder(this, ITEM_DECORATOR_BINDER),
          DelegateBinder.createDelegateBinder(this, CONSTANT_PROPS_MOUNT_BINDER),
          DelegateBinder.createDelegateBinder(binder, CONTENT_MOUNT_BINDER),
      )
      addAttachBinders(
          DelegateBinder.createDelegateBinder(this, CONSTANT_PROPS_ATTACH_BINDER),
          DelegateBinder.createDelegateBinder(binder, CONTENT_ATTACH_BINDER),
      )
    }
  }
  override fun measure(
      context: RenderState.LayoutContext<*>,
      widthSpec: Int,
      heightSpec: Int,
      previousLayoutData: Any?
  ): MeasureResult {
    val size = Size()
    binder.measure(
        size,
        widthSpec,
        heightSpec,
        if (binder.canMeasure() || binder.isWrapContent) eventHandler { onRemeasure() } else null)
    return MeasureResult(size.width, size.height, null)
  }

  override fun createContent(context: Context): SectionsRecyclerView =
      SectionsRecyclerView(context, LithoRecyclerView(context))

  override fun doesMountRenderTreeHosts(): Boolean = true

  override fun getContentAllocator(): ContentAllocator = this
}
