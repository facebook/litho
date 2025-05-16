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
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.Diff
import com.facebook.litho.EventHandler
import com.facebook.litho.Output
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec.getSize
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.FromMeasure
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.ShouldAlwaysRemeasure
import com.facebook.litho.annotations.ShouldExcludeFromIncrementalMount
import com.facebook.litho.annotations.ShouldUpdate
import com.facebook.litho.annotations.State
import com.facebook.rendercore.utils.MeasureSpecUtils.exactly
import kotlin.Function1

/**
 * Components that renders a [RecyclerView].
 *
 * @uidocs
 * @prop binder Binder for RecyclerView.
 * @prop refreshHandler Event handler for refresh event.
 * @prop hasFixedSize If set, makes RecyclerView not affected by adapter changes.
 * @prop clipToPadding Clip RecyclerView to its padding.
 * @prop clipChildren Clip RecyclerView children to their bounds.
 * @prop nestedScrollingEnabled Enables nested scrolling on the RecyclerView.
 * @prop itemDecoration Item decoration for the RecyclerView.
 * @prop refreshProgressBarBackgroundColor Color for progress background.
 * @prop refreshProgressBarColor Color for progress animation.
 * @prop recyclerViewId View ID for the RecyclerView.
 * @prop recyclerEventsController Controller to pass events from outside the component.
 * @prop onScrollListener Listener for RecyclerView's scroll events.
 */
@Deprecated(
    "This component is deprecated and will be removed in the future. Please use com.facebook.litho.widget.Recycler instead.")
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@MountSpec(hasChildLithoViews = true, isPureRender = true, events = [PTRRefreshEvent::class])
internal object LegacyRecyclerSpec {

  @PropDefault val scrollBarStyle: Int = View.SCROLLBARS_INSIDE_OVERLAY
  @PropDefault val hasFixedSize: Boolean = true
  @PropDefault val nestedScrollingEnabled: Boolean = true
  @PropDefault val itemAnimator: RecyclerView.ItemAnimator = NoUpdateItemAnimator()
  @PropDefault val recyclerViewId: Int = View.NO_ID
  @PropDefault val overScrollMode: Int = View.OVER_SCROLL_ALWAYS

  @PropDefault val refreshProgressBarColor: Int = Color.BLACK
  @PropDefault val clipToPadding: Boolean = true
  @PropDefault val clipChildren: Boolean = true
  @PropDefault val leftPadding: Int = 0
  @PropDefault val rightPadding: Int = 0
  @PropDefault val topPadding: Int = 0
  @PropDefault val bottomPadding: Int = 0
  @PropDefault val pullToRefresh: Boolean = true

  // This is the default value for refresh spinner background from
  // SwipeRefreshLayout.CIRCLE_BG_LIGHT which is unfortunately private.
  const val DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR: Int = -0x50506

  @ShouldExcludeFromIncrementalMount
  fun shouldExcludeFromIncrementalMount(
      @Prop(optional = true) shouldExcludeFromIncrementalMount: Boolean
  ): Boolean {
    return shouldExcludeFromIncrementalMount
  }

  @OnMeasure
  fun onMeasure(
      c: ComponentContext?,
      layout: ComponentLayout?,
      widthSpec: Int,
      heightSpec: Int,
      measureOutput: Size,
      @Prop(optional = true) leftPadding: Int,
      @Prop(optional = true) rightPadding: Int,
      @Prop(optional = true) topPadding: Int,
      @Prop(optional = true) bottomPadding: Int,
      @Prop binder: Binder<RecyclerView>,
      measuredWidth: Output<Int?>,
      measuredHeight: Output<Int?>
  ) {
    val widthSpecToUse = maybeGetSpecWithPadding(widthSpec, leftPadding + rightPadding)
    val heightSpecToUse = maybeGetSpecWithPadding(heightSpec, topPadding + bottomPadding)

    binder.measure(
        measureOutput,
        widthSpecToUse,
        heightSpecToUse,
        if ((binder.isCrossAxisWrapContent || binder.isMainAxisWrapContent))
            LegacyRecycler.onRemeasure(c)
        else null)

    measuredWidth.set(measureOutput.width)
    measuredHeight.set(measureOutput.height)
  }

  @OnBoundsDefined
  fun onBoundsDefined(
      context: ComponentContext?,
      layout: ComponentLayout,
      @Prop(optional = true) leftPadding: Int,
      @Prop(optional = true) rightPadding: Int,
      @Prop(optional = true) topPadding: Int,
      @Prop(optional = true) bottomPadding: Int,
      @Prop binder: Binder<RecyclerView>,
      @FromMeasure measuredWidth: Int?,
      @FromMeasure measuredHeight: Int?
  ) {
    val layoutWidth = layout.width
    val layoutHeight = layout.height

    var width = layoutWidth
    if (measuredWidth == null || measuredWidth != layoutWidth) {
      val spec = maybeGetSpecWithPadding(exactly(layoutWidth), leftPadding + rightPadding)
      width = getSize(spec)
    }

    var height = layoutHeight
    if (measuredHeight == null || measuredHeight != layoutHeight) {
      val spec = maybeGetSpecWithPadding(exactly(layoutHeight), topPadding + bottomPadding)
      height = getSize(spec)
    }

    binder.setSize(width, height)
  }

  @OnCreateMountContent
  fun onCreateMountContent(c: Context?): SectionsRecyclerView {
    val sectionsRecyclerView = SectionsRecyclerView(requireNotNull(c), LithoRecyclerView(c))
    sectionsRecyclerView.id = R.id.recycler_view_container_id
    return sectionsRecyclerView
  }

  @OnMount
  fun onMount(
      c: ComponentContext?,
      sectionsRecycler: SectionsRecyclerView,
      @Prop binder: Binder<RecyclerView>,
      @Prop(optional = true) hasFixedSize: Boolean,
      @Prop(optional = true) clipToPadding: Boolean,
      @Prop(optional = true) leftPadding: Int,
      @Prop(optional = true) rightPadding: Int,
      @Prop(optional = true) topPadding: Int,
      @Prop(optional = true) bottomPadding: Int,
      @Prop(optional = true) disableAddingPadding: Boolean,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarBackgroundColor: Int?,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarColor: Int,
      @Prop(optional = true) clipChildren: Boolean,
      @Prop(optional = true) nestedScrollingEnabled: Boolean,
      @Prop(optional = true) scrollBarStyle: Int,
      @Prop(optional = true, varArg = "itemDecoration")
      itemDecorations: List<@JvmSuppressWildcards RecyclerView.ItemDecoration>?,
      @State childMeasureFunction: Function1<@JvmSuppressWildcards View, Unit>,
      @Prop(optional = true) horizontalFadingEdgeEnabled: Boolean,
      @Prop(optional = true) verticalFadingEdgeEnabled: Boolean,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) fadingEdgeLength: Int,
      @Prop(optional = true) @IdRes recyclerViewId: Int,
      @Prop(optional = true) overScrollMode: Int,
      @Prop(optional = true) edgeEffectFactory: RecyclerView.EdgeEffectFactory?,
      @Prop(optional = true, isCommonProp = true) contentDescription: CharSequence?,
      @Prop(optional = true) itemAnimator: RecyclerView.ItemAnimator?
  ) {
    val recyclerView =
        sectionsRecycler.recyclerView
            ?: throw IllegalStateException(
                "RecyclerView not found, it should not be removed from SwipeRefreshLayout")

    recyclerView.contentDescription = contentDescription
    recyclerView.setHasFixedSize(hasFixedSize)
    recyclerView.clipToPadding = clipToPadding
    sectionsRecycler.clipToPadding = clipToPadding
    if (!disableAddingPadding) {
      ViewCompat.setPaddingRelative(
          recyclerView, leftPadding, topPadding, rightPadding, bottomPadding)
    }
    recyclerView.clipChildren = clipChildren
    sectionsRecycler.clipChildren = clipChildren
    recyclerView.isNestedScrollingEnabled = nestedScrollingEnabled
    sectionsRecycler.isNestedScrollingEnabled = nestedScrollingEnabled
    recyclerView.scrollBarStyle = scrollBarStyle
    recyclerView.isHorizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled
    recyclerView.isVerticalFadingEdgeEnabled = verticalFadingEdgeEnabled
    recyclerView.setFadingEdgeLength(fadingEdgeLength)
    // TODO (t14949498) determine if this is necessary
    recyclerView.id = recyclerViewId
    recyclerView.overScrollMode = overScrollMode
    if (edgeEffectFactory != null) {
      recyclerView.edgeEffectFactory = edgeEffectFactory
    }
    if (refreshProgressBarBackgroundColor != null) {
      sectionsRecycler.setProgressBackgroundColorSchemeColor(refreshProgressBarBackgroundColor)
    }
    sectionsRecycler.setColorSchemeColors(refreshProgressBarColor)

    sectionsRecycler.setItemAnimator(
        if (itemAnimator !== LegacyRecyclerSpec.itemAnimator) itemAnimator
        else NoUpdateItemAnimator())

    if (itemDecorations != null) {
      for (itemDecoration in itemDecorations) {
        if (itemDecoration is ItemDecorationWithMeasureFunction) {
          itemDecoration.measure = childMeasureFunction
        }
        recyclerView.addItemDecoration(itemDecoration)
      }
    }

    binder.mount(recyclerView)
  }

  @OnBind
  fun onBind(
      context: ComponentContext?,
      sectionsRecycler: SectionsRecyclerView,
      @Prop binder: Binder<RecyclerView>,
      @Prop(optional = true) recyclerEventsController: RecyclerEventsController?,
      @Prop(optional = true, varArg = "onScrollListener")
      onScrollListeners: List<RecyclerView.OnScrollListener?>?,
      @Prop(optional = true) snapHelper: SnapHelper?,
      @Prop(optional = true) pullToRefresh: Boolean,
      @Prop(optional = true) touchInterceptor: LithoRecyclerView.TouchInterceptor?,
      @Prop(optional = true) onItemTouchListener: RecyclerView.OnItemTouchListener?,
      @Prop(optional = true) refreshHandler: EventHandler<*>?,
      @Prop(optional = true) sectionsViewLogger: SectionsRecyclerView.SectionsRecyclerViewLogger?
  ) {
    sectionsRecycler.setSectionsRecyclerViewLogger(sectionsViewLogger)

    // contentDescription should be set on the recyclerView itself, and not the sectionsRecycler.
    sectionsRecycler.contentDescription = null

    sectionsRecycler.isEnabled = pullToRefresh && refreshHandler != null
    sectionsRecycler.setOnRefreshListener(
        if (refreshHandler != null)
            SwipeRefreshLayout.OnRefreshListener {
              LegacyRecycler.dispatchPTRRefreshEvent(refreshHandler)
            }
        else null)

    val recyclerView =
        sectionsRecycler.recyclerView as LithoRecyclerView
            ?: throw IllegalStateException(
                "RecyclerView not found, it should not be removed from SwipeRefreshLayout " +
                    "before unmounting")

    if (onScrollListeners != null) {
      for (onScrollListener in onScrollListeners) {
        if (onScrollListener != null) {
          recyclerView.addOnScrollListener(onScrollListener)
        }
      }
    }

    if (touchInterceptor != null) {
      recyclerView.setTouchInterceptor(touchInterceptor)
    }

    if (onItemTouchListener != null) {
      recyclerView.addOnItemTouchListener(onItemTouchListener)
    }

    // We cannot detach the snap helper in unbind, so it may be possible for it to get
    // attached twice which causes SnapHelper to raise an exception.
    if (snapHelper != null && recyclerView.onFlingListener == null) {
      snapHelper.attachToRecyclerView(recyclerView)
    }

    if (recyclerEventsController != null) {
      recyclerEventsController.setSectionsRecyclerView(sectionsRecycler)
      recyclerEventsController.snapHelper = snapHelper
    }

    if (sectionsRecycler.hasBeenDetachedFromWindow()) {
      recyclerView.requestLayout()
      sectionsRecycler.setHasBeenDetachedFromWindow(false)
    }
  }

  @OnUnbind
  fun onUnbind(
      context: ComponentContext?,
      sectionsRecycler: SectionsRecyclerView,
      @Prop binder: Binder<RecyclerView>,
      @Prop(optional = true) recyclerEventsController: RecyclerEventsController?,
      @Prop(optional = true) onItemTouchListener: RecyclerView.OnItemTouchListener?,
      @Prop(optional = true, varArg = "onScrollListener")
      onScrollListeners: List<RecyclerView.OnScrollListener?>?
  ) {
    sectionsRecycler.setSectionsRecyclerViewLogger(null)

    val recyclerView =
        sectionsRecycler.recyclerView as LithoRecyclerView
            ?: throw IllegalStateException(
                "RecyclerView not found, it should not be removed from SwipeRefreshLayout " +
                    "before unmounting")

    if (recyclerEventsController != null) {
      recyclerEventsController.setSectionsRecyclerView(null)
      recyclerEventsController.snapHelper = null
    }

    if (onScrollListeners != null) {
      for (onScrollListener in onScrollListeners) {
        if (onScrollListener != null) {
          recyclerView.removeOnScrollListener(onScrollListener)
        }
      }
    }

    if (onItemTouchListener != null) {
      recyclerView.removeOnItemTouchListener(onItemTouchListener)
    }

    recyclerView.setTouchInterceptor(null)

    sectionsRecycler.setOnRefreshListener(null)
  }

  @OnUnmount
  fun onUnmount(
      context: ComponentContext?,
      sectionsRecycler: SectionsRecyclerView,
      @Prop binder: Binder<RecyclerView>,
      @Prop(optional = true, varArg = "itemDecoration")
      itemDecorations: List<@JvmSuppressWildcards RecyclerView.ItemDecoration>?,
      @Prop(optional = true) edgeEffectFactory: RecyclerView.EdgeEffectFactory?,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarBackgroundColor: Int?,
      @Prop(optional = true) snapHelper: SnapHelper?
  ) {
    val recyclerView =
        sectionsRecycler.recyclerView
            ?: throw IllegalStateException(
                "RecyclerView not found, it should not be removed from SwipeRefreshLayout " +
                    "before unmounting")

    recyclerView.id = recyclerViewId

    if (refreshProgressBarBackgroundColor != null) {
      sectionsRecycler.setProgressBackgroundColorSchemeColor(
          DEFAULT_REFRESH_SPINNER_BACKGROUND_COLOR)
    }

    if (edgeEffectFactory != null) {
      recyclerView.edgeEffectFactory = sectionsRecycler.defaultEdgeEffectFactory
    }

    snapHelper?.attachToRecyclerView(null)

    sectionsRecycler.resetItemAnimator()

    if (itemDecorations != null) {
      for (itemDecoration in itemDecorations) {
        if (itemDecoration is ItemDecorationWithMeasureFunction) {
          itemDecoration.measure = null
        }
        recyclerView.removeItemDecoration(itemDecoration)
      }
    }

    binder.unmount(recyclerView)
  }

  @ShouldUpdate(onMount = true)
  fun shouldUpdate(
      @Prop binder: Diff<Binder<RecyclerView>>,
      @Prop(optional = true) hasFixedSize: Diff<Boolean>,
      @Prop(optional = true) clipToPadding: Diff<Boolean>,
      @Prop(optional = true) leftPadding: Diff<Int>,
      @Prop(optional = true) rightPadding: Diff<Int>,
      @Prop(optional = true) topPadding: Diff<Int>,
      @Prop(optional = true) bottomPadding: Diff<Int>,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarBackgroundColor: Diff<Int?>,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarColor: Diff<Int>,
      @Prop(optional = true) clipChildren: Diff<Boolean>,
      @Prop(optional = true) scrollBarStyle: Diff<Int>,
      @Prop(optional = true, varArg = "itemDecoration")
      itemDecorations: Diff<List<@JvmSuppressWildcards RecyclerView.ItemDecoration>?>,
      @Prop(optional = true) horizontalFadingEdgeEnabled: Diff<Boolean>,
      @Prop(optional = true) verticalFadingEdgeEnabled: Diff<Boolean>,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) fadingEdgeLength: Diff<Int>,
      @Prop(optional = true) itemAnimator: Diff<RecyclerView.ItemAnimator?>,
      @State measureVersion: Diff<Int>
  ): Boolean {
    if (measureVersion.previous != measureVersion.next) {
      return true
    }

    if (binder.previous !== binder.next) {
      return true
    }

    if (hasFixedSize.previous != hasFixedSize.next) {
      return true
    }

    if (clipToPadding.previous != clipToPadding.next) {
      return true
    }

    if (leftPadding.previous != leftPadding.next) {
      return true
    }

    if (rightPadding.previous != rightPadding.next) {
      return true
    }

    if (topPadding.previous != topPadding.next) {
      return true
    }

    if (bottomPadding.previous != bottomPadding.next) {
      return true
    }

    if (clipChildren.previous != clipChildren.next) {
      return true
    }

    if (scrollBarStyle.previous != scrollBarStyle.next) {
      return true
    }

    if (horizontalFadingEdgeEnabled.previous != horizontalFadingEdgeEnabled.next) {
      return true
    }

    if (verticalFadingEdgeEnabled.previous != verticalFadingEdgeEnabled.next) {
      return true
    }

    if (fadingEdgeLength.previous != fadingEdgeLength.next) {
      return true
    }

    val previousRefreshBgColor = refreshProgressBarBackgroundColor.previous
    val nextRefreshBgColor = refreshProgressBarBackgroundColor.next
    if (if (previousRefreshBgColor == null) nextRefreshBgColor != null
    else previousRefreshBgColor != nextRefreshBgColor) {
      return true
    }

    if (refreshProgressBarColor.previous != refreshProgressBarColor.next) {
      return true
    }

    val previousItemAnimator = itemAnimator.previous
    val nextItemAnimator = itemAnimator.next

    if (if (previousItemAnimator == null) nextItemAnimator != null
    else previousItemAnimator.javaClass != nextItemAnimator?.javaClass) {
      return true
    }

    val previous = itemDecorations.previous
    val next = itemDecorations.next
    val itemDecorationIsEqual = if ((previous == null)) (next == null) else previous == next

    return !itemDecorationIsEqual
  }

  @OnEvent(ReMeasureEvent::class)
  fun onRemeasure(c: ComponentContext?, @State measureVersion: Int) {
    LegacyRecycler.onUpdateMeasureAsync(c, measureVersion + 1)
  }

  @OnCreateInitialState
  fun onCreateInitialState(
      measureVersion: StateValue<Int>,
      childMeasureFunction: StateValue<Function1<@JvmSuppressWildcards View, Unit>>,
      @Prop binder: Binder<RecyclerView>
  ) {
    measureVersion.set(0)
    childMeasureFunction.set { view: View ->
      val position = (view.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
      view.measure(binder.getChildWidthSpec(position), binder.getChildWidthSpec(position))
    }
  }

  @OnUpdateState
  fun onUpdateMeasure(@Param measureVer: Int, measureVersion: StateValue<Int>) {
    // We don't really need to update a state here. This state update is only really used to force
    // a re-layout on the tree containing this Recycler.
    measureVersion.set(measureVer)
  }

  @ShouldAlwaysRemeasure
  fun shouldAlwaysRemeasure(@Prop binder: Binder<RecyclerView>): Boolean {
    return binder.isMainAxisWrapContent || binder.isCrossAxisWrapContent
  }

  class NoUpdateItemAnimator : DefaultItemAnimator() {
    init {
      supportsChangeAnimations = false
    }
  }
}
