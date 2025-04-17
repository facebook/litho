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
import android.view.View
import android.widget.HorizontalScrollView
import androidx.core.view.OneShotPreDrawListener
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.ComponentTree
import com.facebook.litho.Output
import com.facebook.litho.R
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.FromBoundsDefined
import com.facebook.litho.annotations.FromMeasure
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCalculateCachedValue
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnLoadStyle
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.State
import com.facebook.yoga.YogaDirection
import kotlin.math.max
import kotlin.math.min

/**
 * A component that wraps another component and allow it to be horizontally scrollable. It's
 * analogous to a [android.widget.HorizontalScrollView].
 *
 * @uidocs
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@MountSpec(hasChildLithoViews = true)
internal object HorizontalScrollComponentSpec {

  private const val LAST_SCROLL_POSITION_UNSET = -1

  @PropDefault const val scrollbarEnabled: Boolean = true
  @PropDefault const val initialScrollPosition: Int = LAST_SCROLL_POSITION_UNSET
  @PropDefault const val incrementalMountEnabled: Boolean = false
  @PropDefault const val overScrollMode: Int = View.OVER_SCROLL_IF_CONTENT_SCROLLS

  @JvmStatic
  @OnLoadStyle
  fun onLoadStyle(c: ComponentContext, scrollbarEnabled: Output<Boolean>) {
    val a = c.obtainStyledAttributes(R.styleable.HorizontalScroll, 0)

    for (i in 0..<a.indexCount) {
      val attr = a.getIndex(i)
      if (attr == R.styleable.HorizontalScroll_android_scrollbars) {
        scrollbarEnabled.set(a.getInt(attr, 0) != 0)
      }
    }

    a.recycle()
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
      context: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size?,
      @Prop(optional = true) wrapContent: Boolean, // TODO:T182959582
      @Prop contentProps: Component,
      @CachedValue childComponentTree: ComponentTree,
      measuredComponentWidth: Output<Int>,
      measuredComponentHeight: Output<Int>
  ) {
    if (childComponentTree.isReleased) {
      if (size != null) {
        measuredComponentWidth.set(0)
        measuredComponentHeight.set(0)
        size.width = max(0.0, size.width.toDouble()).toInt()
        size.height = max(0.0, size.height.toDouble()).toInt()
      }
      return
    }

    val contentSize = Size()

    // Measure the component with undefined width spec, as the contents of the
    // hscroll have unlimited horizontal space.
    childComponentTree.setRootAndSizeSpecSync(
        contentProps, SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED), heightSpec, contentSize)

    val measuredWidth = contentSize.width
    val measuredHeight = contentSize.height

    measuredComponentWidth.set(measuredWidth)
    measuredComponentHeight.set(measuredHeight)

    checkNotNull(size)
    val sizeSpecMode = SizeSpec.getMode(widthSpec)
    val sizeSpecWidth = SizeSpec.getSize(widthSpec)
    if (sizeSpecMode == SizeSpec.UNSPECIFIED) {
      size.width = measuredWidth
    } else if (sizeSpecMode == SizeSpec.AT_MOST && wrapContent) {
      size.width = min(measuredWidth, sizeSpecWidth)
    } else {
      size.width = sizeSpecWidth
    }
    size.height = measuredHeight
  }

  @JvmStatic
  @OnBoundsDefined
  fun onBoundsDefined(
      context: ComponentContext,
      layout: ComponentLayout,
      @Prop contentProps: Component,
      @Prop(optional = true) fillViewport: Boolean,
      @CachedValue childComponentTree: ComponentTree,
      @FromMeasure measuredComponentWidth: Int?,
      @FromMeasure measuredComponentHeight: Int?,
      componentWidth: Output<Int>,
      componentHeight: Output<Int>,
      layoutDirection: Output<YogaDirection>
  ) {
    val layoutWidth = layout.width - layout.paddingLeft - layout.paddingRight

    // If onMeasure() has been called, this means the content component already
    // has a defined size, no need to calculate it again.
    if (measuredComponentWidth != null && measuredComponentHeight != null) {
      componentWidth.set(max(measuredComponentWidth, if (fillViewport) layoutWidth else 0))
      componentHeight.set(measuredComponentHeight)
    } else {
      if (childComponentTree.isReleased) {
        componentWidth.set(0)
        componentHeight.set(0)
        return
      }

      val contentSize = Size()
      childComponentTree.setRootAndSizeSpecSync(
          contentProps,
          SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
          SizeSpec.makeSizeSpec(layout.height, SizeSpec.EXACTLY),
          contentSize)

      val measuredWidth = max(contentSize.width, (if (fillViewport) layoutWidth else 0))
      val measuredHeight = contentSize.height

      componentWidth.set(measuredWidth)
      componentHeight.set(measuredHeight)
    }

    layoutDirection.set(layout.resolvedLayoutDirection)
  }

  @JvmStatic
  @OnCreateMountContent
  fun onCreateMountContent(c: Context): HorizontalScrollLithoView = HorizontalScrollLithoView(c)

  @JvmStatic
  @OnMount
  fun onMount(
      context: ComponentContext,
      horizontalScrollLithoView: HorizontalScrollLithoView,
      @Prop(optional = true, resType = ResType.BOOL) scrollbarEnabled: Boolean,
      @Prop(optional = true) eventsController: HorizontalScrollEventsController?,
      @Prop(optional = true)
      onScrollChangeListener: HorizontalScrollLithoView.OnScrollChangeListener?,
      @Prop(optional = true) scrollStateListener: ScrollStateListener?,
      @Prop(optional = true) incrementalMountEnabled: Boolean,
      @Prop(optional = true) overScrollMode: Int,
      @Prop(optional = true) horizontalFadingEdgeEnabled: Boolean,
      @Prop(optional = true) fadingEdgeLength: Int,
      @State lastScrollPosition: HorizontalScrollLithoView.ScrollPosition,
      @CachedValue childComponentTree: ComponentTree,
      @FromBoundsDefined componentWidth: Int?,
      @FromBoundsDefined componentHeight: Int?,
      @FromBoundsDefined layoutDirection: YogaDirection
  ) {
    horizontalScrollLithoView.isHorizontalScrollBarEnabled = scrollbarEnabled
    horizontalScrollLithoView.overScrollMode = overScrollMode
    horizontalScrollLithoView.isHorizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled
    horizontalScrollLithoView.setFadingEdgeLength(fadingEdgeLength)
    horizontalScrollLithoView.mount(
        childComponentTree,
        lastScrollPosition,
        componentWidth ?: 0,
        componentHeight ?: 0,
        lastScrollPosition.x,
        onScrollChangeListener,
        scrollStateListener)
    OneShotPreDrawListener.add(horizontalScrollLithoView) {
      if (lastScrollPosition.x == LAST_SCROLL_POSITION_UNSET) {
        if (layoutDirection == YogaDirection.RTL) {
          horizontalScrollLithoView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }
        lastScrollPosition.x = horizontalScrollLithoView.scrollX
      } else {
        horizontalScrollLithoView.scrollX = lastScrollPosition.x
      }
    }

    eventsController?.setScrollableView(horizontalScrollLithoView)
  }

  @JvmStatic
  @OnUnmount
  fun onUnmount(
      context: ComponentContext,
      mountedView: HorizontalScrollLithoView,
      @Prop(optional = true) eventsController: HorizontalScrollEventsController?
  ) {
    mountedView.unmount()

    eventsController?.setScrollableView(null)
  }

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      lastScrollPosition: StateValue<HorizontalScrollLithoView.ScrollPosition>,
      @Prop(optional = true) initialScrollPosition: Int
  ) {
    lastScrollPosition.set(HorizontalScrollLithoView.ScrollPosition(initialScrollPosition))
  }
}
