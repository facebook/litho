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
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.NestedScrollView
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.Dimen
import com.facebook.litho.MeasureScope
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.litho.Wrapper
import com.facebook.litho.px
import com.facebook.litho.useState
import com.facebook.litho.widget.LithoScrollView
import com.facebook.litho.widget.R
import com.facebook.litho.widget.ScrollStateListener
import com.facebook.litho.widget.VerticalScrollEventsController
import com.facebook.rendercore.MeasureResult
import kotlin.math.max
import kotlin.math.min

fun ExperimentalVerticalScroll(
    scrollbarEnabled: Boolean = false,
    nestedScrollingEnabled: Boolean = false,
    verticalFadingEdgeEnabled: Boolean = false,
    fillViewport: Boolean = false,
    scrollbarFadingEnabled: Boolean = true,
    incrementalMountEnabled: Boolean = false,
    overScrollMode: Int = View.OVER_SCROLL_IF_CONTENT_SCROLLS,
    fadingEdgeLength: Int = 0,
    initialScrollPosition: Dimen = 0.px,
    eventsController: VerticalScrollEventsController? = null,
    onScrollChange: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)? = null,
    onInterceptTouch: ((NestedScrollView, event: MotionEvent) -> Boolean)? = null,
    onScrollStateChange: ((View, Int) -> Unit)? = null,
    style: Style? = null,
    child: () -> Component,
): Component {

  return ExperimentalVerticalScroll(
      scrollbarEnabled,
      nestedScrollingEnabled,
      verticalFadingEdgeEnabled,
      fillViewport,
      scrollbarFadingEnabled,
      incrementalMountEnabled,
      overScrollMode,
      fadingEdgeLength,
      initialScrollPosition,
      eventsController,
      onScrollChange,
      onInterceptTouch,
      onScrollStateChange,
      child(),
      style,
  )
}

class ExperimentalVerticalScroll(
    val scrollbarEnabled: Boolean = false,
    val nestedScrollingEnabled: Boolean = false,
    val verticalFadingEdgeEnabled: Boolean = false,
    val fillViewport: Boolean = false,
    val scrollbarFadingEnabled: Boolean = true,
    val incrementalMountEnabled: Boolean = false,
    val overScrollMode: Int = View.OVER_SCROLL_IF_CONTENT_SCROLLS,
    val fadingEdgeLength: Int = 0,
    val initialScrollPosition: Dimen = 0.px,
    val eventsController: VerticalScrollEventsController? = null,
    val onScrollChange: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)? = null,
    val onInterceptTouch: ((NestedScrollView, event: MotionEvent) -> Boolean)? = null,
    val onScrollStateChange: ((View, Int) -> Unit)? = null,
    val child: Component,
    val style: Style? = null,
) : MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {

    val scrollPosition = useState {
      LithoScrollView.ScrollPosition(initialScrollPosition.toPixels())
    }

    val componentTree = useState {
      ComponentTree.createNestedComponentTree(context, child)
          .incrementalMount(incrementalMountEnabled)
          .build()
    }

    return MountableRenderResult(
        VerticalScrollMountable(
            child,
            scrollbarEnabled,
            nestedScrollingEnabled,
            verticalFadingEdgeEnabled,
            fillViewport,
            scrollbarFadingEnabled,
            incrementalMountEnabled,
            overScrollMode,
            fadingEdgeLength,
            eventsController,
            onScrollChange,
            onScrollStateChange,
            onInterceptTouch,
            scrollPosition.value,
            componentTree.value,
        ),
        style)
  }
}

internal class VerticalScrollMountable(
    val component: Component,
    val scrollbarEnabled: Boolean = false,
    val nestedScrollingEnabled: Boolean = false,
    val verticalFadingEdgeEnabled: Boolean = false,
    val fillViewport: Boolean = false,
    val scrollbarFadingEnabled: Boolean = true,
    val incrementalMountEnabled: Boolean = false,
    val overScrollMode: Int = View.OVER_SCROLL_IF_CONTENT_SCROLLS,
    val fadingEdgeLength: Int = 0,
    val eventsController: VerticalScrollEventsController? = null,
    val onScrollChangeListener: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)? = null,
    val scrollStateListener: ScrollStateListener? = null,
    val onInterceptTouchListener: ((NestedScrollView, event: MotionEvent) -> Boolean)? = null,
    val scrollPosition: LithoScrollView.ScrollPosition,
    val componentTree: ComponentTree,
) : SimpleMountable<LithoScrollView>(RenderType.VIEW) {

  override fun doesMountRenderTreeHosts(): Boolean = true

  override fun createContent(context: Context): LithoScrollView =
      LayoutInflater.from(context).inflate(R.layout.litho_scroll_view, null, false)
          as LithoScrollView

  override fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult {
    val heightMode = SizeSpec.getMode(heightSpec)
    val height = max(0, SizeSpec.getSize(heightSpec))

    // If fillViewport is true, then set a minimum height to ensure that the viewport is filled.
    val actualComponent =
        if (fillViewport) {
          Wrapper.create(ComponentContext(androidContext))
              .delegate(component)
              .minHeightPx(height)
              .build()
        } else {
          component
        }

    val size = Size()

    componentTree.setRootAndSizeSpecSync(
        actualComponent,
        widthSpec,
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        size,
    )

    // Compute the appropriate size depending on the heightSpec
    when (heightMode) {
      // If this Vertical scroll is being measured with a fixed height we don't care
      // about the size of the content and just use that instead
      SizeSpec.EXACTLY -> size.height = height
      // For at most we want the VerticalScroll to be as big as its content up to
      // the maximum height specified in the heightSpec
      SizeSpec.AT_MOST -> size.height = max(0, min(height, size.height))
    }

    // Ensure that width is not less than 0
    size.width = max(0, size.width)

    return MeasureResult(size.width, size.height, VerticalScrollLayoutData(size.width, size.height))
  }

  override fun mount(c: Context, content: LithoScrollView, layoutData: Any?) {
    content.mount(
        componentTree,
        scrollPosition,
        scrollStateListener,
    )
    content.isScrollbarFadingEnabled = scrollbarFadingEnabled
    content.isNestedScrollingEnabled = nestedScrollingEnabled
    content.isVerticalFadingEdgeEnabled = verticalFadingEdgeEnabled
    content.setFadingEdgeLength(fadingEdgeLength)

    // On older versions we need to disable the vertical scroll bar as otherwise we run into an NPE
    // that was only fixed in Lollipop - see
    // https://github.com/aosp-mirror/platform_frameworks_base/commit/6c8fef7fb866d244486a962dd82f4a6f26505f16#diff-7c8b4c8147fbbbf69293775bca384f31.

    // On older versions we need to disable the vertical scroll bar as otherwise we run into an NPE
    // that was only fixed in Lollipop - see
    // https://github.com/aosp-mirror/platform_frameworks_base/commit/6c8fef7fb866d244486a962dd82f4a6f26505f16#diff-7c8b4c8147fbbbf69293775bca384f31.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      content.isVerticalScrollBarEnabled = false
    } else {
      content.isVerticalScrollBarEnabled = scrollbarEnabled
    }

    onScrollChangeListener?.let {
      content.setOnScrollChangeListener(
          NestedScrollView.OnScrollChangeListener {
              nestedScrollView: NestedScrollView,
              _: Int,
              scrollY: Int,
              _: Int,
              oldScrollY: Int ->
            onScrollChangeListener.invoke(nestedScrollView, scrollY, oldScrollY)
          })
    }

    content.setOnInterceptTouchListener(onInterceptTouchListener)
    content.overScrollMode = overScrollMode

    eventsController?.setScrollView(content)
  }

  override fun unmount(c: Context, content: LithoScrollView, layoutData: Any?) {
    eventsController?.setScrollView(null)
    content.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    content.setOnInterceptTouchListener(null)
    content.unmount()
  }

  override fun shouldUpdate(
      newMountable: SimpleMountable<LithoScrollView>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    newMountable as VerticalScrollMountable
    return !component.isEquivalentTo(newMountable.component) ||
        scrollbarEnabled != newMountable.scrollbarEnabled ||
        scrollbarFadingEnabled != newMountable.scrollbarFadingEnabled ||
        fillViewport != newMountable.fillViewport ||
        nestedScrollingEnabled != newMountable.nestedScrollingEnabled ||
        incrementalMountEnabled != newMountable.incrementalMountEnabled
  }
}

internal data class VerticalScrollLayoutData(val measuredWidth: Int, val measuredHeight: Int)
