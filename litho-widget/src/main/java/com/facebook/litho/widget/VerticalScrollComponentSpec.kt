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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.widget.NestedScrollView
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.ComponentTree
import com.facebook.litho.ComponentUtils
import com.facebook.litho.Diff
import com.facebook.litho.Output
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec.AT_MOST
import com.facebook.litho.SizeSpec.EXACTLY
import com.facebook.litho.SizeSpec.UNSPECIFIED
import com.facebook.litho.SizeSpec.getMode
import com.facebook.litho.SizeSpec.getSize
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.StateValue
import com.facebook.litho.Wrapper
import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.FromMeasure
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCalculateCachedValue
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.ShouldUpdate
import com.facebook.litho.annotations.State
import kotlin.math.max
import kotlin.math.min

/**
 * Component that wraps another component, allowing it to be vertically scrollable. It's analogous
 * to [android.widget.ScrollView]. TreeProps will only be set during @OnCreateInitialState once, so
 * updating TreeProps on the parent will not reflect on the VerticalScrollComponent.
 *
 * See also: [com.facebook.litho.widget.HorizontalScroll] for horizontal scrollability.
 *
 * @uidocs
 * @prop scrollbarEnabled whether the vertical scrollbar should be drawn
 * @prop scrollbarFadingEnabled whether the scrollbar should fade out when the view is not scrolling
 * @props initialScrollOffsetPixels initial vertical scroll offset, in pixels
 * @props verticalFadingEdgeEnabled whether the vertical edges should be faded when scrolled
 * @prop fadingEdgeLength size of the faded edge used to indicate that more content is available
 * @prop onInterceptTouchListener NOT THE SAME AS LITHO'S interceptTouchHandler COMMON PROP. this is
 *   a listener that handles the underlying ScrollView's onInterceptTouchEvent first, whereas the
 *   Litho prop wraps the component into another view and intercepts there.
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@MountSpec(hasChildLithoViews = true, isPureRender = true)
object VerticalScrollComponentSpec {

  @PropDefault const val scrollbarFadingEnabled: Boolean = true
  @PropDefault const val overScrollMode: Int = View.OVER_SCROLL_IF_CONTENT_SCROLLS

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      context: ComponentContext,
      scrollPosition: StateValue<LithoScrollView.ScrollPosition>,
      @Prop(optional = true) initialScrollOffsetPixels: Int,
      @Prop(optional = true) shouldCompareCommonProps: Boolean
  ) {
    val initialScrollPosition = LithoScrollView.ScrollPosition(initialScrollOffsetPixels)
    scrollPosition.set(initialScrollPosition)
  }

  @JvmStatic
  @OnCalculateCachedValue(name = "childComponentTree")
  fun ensureComponentTree(
      c: ComponentContext,
      @Prop(optional = true) incrementalMountEnabled: Boolean
  ): ComponentTree =
      // The parent ComponentTree(CT) in context may be released and re-created. In this case, the
      // child CT will be re-created here because the cache in the new created parent CT return null
      ComponentTree.createNestedComponentTree(c).incrementalMount(incrementalMountEnabled).build()

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop childComponent: Component,
      @Prop(optional = true) fillViewport: Boolean,
      @CachedValue childComponentTree: ComponentTree,
      measuredWidth: Output<Int>,
      measuredHeight: Output<Int>
  ) {
    val horizontalPadding = layout.paddingLeft + layout.paddingRight
    val childWidthSpec =
        ViewGroup.getChildMeasureSpec(
            widthSpec, horizontalPadding, ViewGroup.LayoutParams.MATCH_PARENT)

    measureVerticalScroll(
        c, childWidthSpec, heightSpec, size, childComponentTree, childComponent, fillViewport)

    // Add back horizontal padding since we subtracted it when creating the child width spec above
    measuredWidth.set(size.width + horizontalPadding)
    measuredHeight.set(size.height)
  }

  @JvmStatic
  @OnBoundsDefined
  fun onBoundsDefined(
      c: ComponentContext,
      layout: ComponentLayout,
      @Prop childComponent: Component,
      @Prop(optional = true) fillViewport: Boolean,
      @CachedValue childComponentTree: ComponentTree,
      @FromMeasure measuredWidth: Int?,
      @FromMeasure measuredHeight: Int?
  ) {
    val layoutWidth = layout.width - layout.paddingLeft - layout.paddingRight
    val layoutHeight = layout.height - layout.paddingTop - layout.paddingBottom

    if (measuredWidth != null &&
        measuredWidth == layoutWidth &&
        (!fillViewport || (measuredHeight != null && measuredHeight == layoutHeight))) {
      // If we're not filling the viewport, then we always measure the height with unspecified, so
      // we just need to check that the width matches.
      return
    }

    measureVerticalScroll(
        c,
        makeSizeSpec(layoutWidth, EXACTLY),
        makeSizeSpec(layoutHeight, EXACTLY),
        null,
        childComponentTree,
        childComponent,
        fillViewport)
  }

  private fun measureVerticalScroll(
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      size: Size?,
      childComponentTree: ComponentTree,
      childComponent: Component,
      fillViewport: Boolean
  ) {
    var childComponent: Component? = childComponent
    if (childComponentTree.isReleased) {
      if (size != null) {
        size.width = max(0.0, size.width.toDouble()).toInt()
        size.height = max(0.0, size.height.toDouble()).toInt()
      }
      return
    }

    // If fillViewport is true, then set a minimum height to ensure that the viewport is filled.
    if (fillViewport) {
      childComponent =
          Wrapper.create(c).delegate(childComponent).minHeightPx(getSize(heightSpec)).build()
    }

    childComponentTree.setRootAndSizeSpecSync(
        childComponent, widthSpec, makeSizeSpec(0, UNSPECIFIED), size)

    if (size != null) {
      // Compute the appropriate size depending on the heightSpec
      when (getMode(heightSpec)) {
        EXACTLY ->
            // If this Vertical scroll is being measured with a fixed height we don't care about
            // the size of the content and just use that instead
            size.height = getSize(heightSpec)
        AT_MOST ->
            // For at most we want the VerticalScroll to be as big as its content up to the maximum
            // height specified in the heightSpec
            size.height = max(0, min(getSize(heightSpec), size.height))
        else -> {}
      }
      // Ensure that size is not less than 0
      size.width = max(0, size.width)
      size.height = max(0, size.height)
    }
  }

  @JvmStatic
  @OnCreateMountContent
  fun onCreateMountContent(context: Context): LithoScrollView =
      LayoutInflater.from(context).inflate(R.layout.litho_scroll_view, null, false)
          as LithoScrollView

  @JvmStatic
  @OnMount
  fun onMount(
      context: ComponentContext,
      lithoScrollView: LithoScrollView,
      @Prop(optional = true) scrollbarEnabled: Boolean,
      @Prop(optional = true) scrollbarFadingEnabled: Boolean,
      @Prop(optional = true) nestedScrollingEnabled: Boolean,
      @Prop(optional = true) verticalFadingEdgeEnabled: Boolean,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) fadingEdgeLength: Int,
      @Prop(optional = true) @ColorInt fadingEdgeColor: Int?,
      @Prop(optional = true) eventsController: VerticalScrollEventsController?,
      @Prop(optional = true) onScrollChangeListener: NestedScrollView.OnScrollChangeListener?,
      @Prop(optional = true) scrollStateListener: ScrollStateListener?,
      @Prop(optional = true) overScrollMode: Int,
      // NOT THE SAME AS LITHO'S interceptTouchHandler COMMON PROP, see class javadocs
      @Prop(optional = true) onInterceptTouchListener: LithoScrollView.OnInterceptTouchListener?,
      @CachedValue childComponentTree: ComponentTree,
      @State scrollPosition: LithoScrollView.ScrollPosition
  ) {
    lithoScrollView.mount(childComponentTree, scrollPosition, scrollStateListener)
    lithoScrollView.isScrollbarFadingEnabled = scrollbarFadingEnabled
    lithoScrollView.isNestedScrollingEnabled = nestedScrollingEnabled
    lithoScrollView.isVerticalFadingEdgeEnabled = verticalFadingEdgeEnabled
    lithoScrollView.setFadingEdgeLength(fadingEdgeLength)
    lithoScrollView.setFadingEdgeColor(fadingEdgeColor)
    lithoScrollView.isVerticalScrollBarEnabled = scrollbarEnabled
    lithoScrollView.setOnScrollChangeListener(onScrollChangeListener)
    lithoScrollView.setOnInterceptTouchListener(onInterceptTouchListener)
    lithoScrollView.overScrollMode = overScrollMode

    eventsController?.setScrollView(lithoScrollView)
  }

  @JvmStatic
  @OnUnmount
  fun onUnmount(
      context: ComponentContext,
      lithoScrollView: LithoScrollView,
      @Prop(optional = true) eventsController: VerticalScrollEventsController?
  ) {
    eventsController?.setScrollView(null)
    lithoScrollView.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    lithoScrollView.setOnInterceptTouchListener(null)
    lithoScrollView.setFadingEdgeColor(null)
    lithoScrollView.unmount()
  }

  @JvmStatic
  @ShouldUpdate(onMount = true)
  fun shouldUpdate(
      @Prop childComponent: Diff<Component>,
      @Prop(optional = true) scrollbarEnabled: Diff<Boolean>,
      @Prop(optional = true) scrollbarFadingEnabled: Diff<Boolean>,
      @Prop(optional = true) fillViewport: Diff<Boolean>,
      @Prop(optional = true) nestedScrollingEnabled: Diff<Boolean>,
      @Prop(optional = true) incrementalMountEnabled: Diff<Boolean>,
      @Prop(optional = true) shouldCompareCommonProps: Diff<Boolean?>
  ): Boolean {
    if (shouldCompareCommonProps.previous != shouldCompareCommonProps.next) {
      return true
    }
    val compareCommonProps = shouldCompareCommonProps.next == true

    return !ComponentUtils.isEquivalent(
        childComponent.previous, childComponent.next, compareCommonProps) ||
        scrollbarEnabled.previous != scrollbarEnabled.next ||
        scrollbarFadingEnabled.previous != scrollbarFadingEnabled.next ||
        fillViewport.previous != fillViewport.next ||
        nestedScrollingEnabled.previous != nestedScrollingEnabled.next ||
        incrementalMountEnabled.previous != incrementalMountEnabled.next
  }
}
