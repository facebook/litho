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

package com.facebook.samples.litho.kotlin.primitives.widgets

import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.widget.NestedScrollView
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Size
import com.facebook.litho.Style
import com.facebook.litho.Wrapper
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.widget.LithoScrollView
import com.facebook.litho.widget.VerticalScrollEventsController
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.rendercore.toWidthSpec
import com.facebook.rendercore.utils.MeasureSpecUtils
import kotlin.math.max
import kotlin.math.min

fun VerticalScroll(
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

  return VerticalScroll(
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

class VerticalScroll(
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
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val scrollPosition = useState {
      LithoScrollView.ScrollPosition(initialScrollPosition.toPixels())
    }

    val componentTree = useCached {
      ComponentTree.createNestedComponentTree(context)
          .incrementalMount(incrementalMountEnabled)
          .build()
    }

    return LithoPrimitive(
        layoutBehavior = VerticalScrollLayoutBehavior(child, fillViewport, componentTree),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> LithoScrollView(context) }) {
              doesMountRenderTreeHosts = true

              bind(componentTree, scrollPosition.value, onScrollStateChange) { content ->
                content.mount(
                    componentTree,
                    scrollPosition.value,
                    onScrollStateChange,
                )
                onUnbind { content.unmount() }
              }

              scrollbarFadingEnabled.bindTo(
                  LithoScrollView::setScrollbarFadingEnabled, false) // todo check default

              nestedScrollingEnabled.bindTo(
                  LithoScrollView::setNestedScrollingEnabled, false) // todo check default

              verticalFadingEdgeEnabled.bindTo(
                  LithoScrollView::setVerticalFadingEdgeEnabled, false) // todo check default

              fadingEdgeLength.bindTo(
                  LithoScrollView::setFadingEdgeLength,
                  ViewConfiguration.get(androidContext)
                      .scaledFadingEdgeLength) // todo check default

              bind(scrollbarEnabled) { content ->
                // On older versions we need to disable the vertical scroll bar as otherwise we run
                // into an NPE
                // that was only fixed in Lollipop - see
                // https://github.com/aosp-mirror/platform_frameworks_base/commit/6c8fef7fb866d244486a962dd82f4a6f26505f16#diff-7c8b4c8147fbbbf69293775bca384f31.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                  content.isVerticalScrollBarEnabled = false
                } else {
                  content.isVerticalScrollBarEnabled = scrollbarEnabled
                }
                onUnbind {
                  content.isVerticalScrollBarEnabled = false // todo check default
                }
              }

              bind(onScrollChange) { content ->
                onScrollChange?.let { onScrollChangeListener ->
                  content.setOnScrollChangeListener {
                      nestedScrollView: NestedScrollView,
                      _: Int,
                      scrollY: Int,
                      _: Int,
                      oldScrollY: Int ->
                    onScrollChangeListener(nestedScrollView, scrollY, oldScrollY)
                  }
                }
                onUnbind {
                  content.setOnScrollChangeListener(
                      null as NestedScrollView.OnScrollChangeListener?)
                }
              }

              bind(onInterceptTouch) { content ->
                content.setOnInterceptTouchListener(onInterceptTouch)
                onUnbind { content.setOnInterceptTouchListener(null) }
              }

              overScrollMode.bindTo(
                  LithoScrollView::setOverScrollMode, View.OVER_SCROLL_IF_CONTENT_SCROLLS)

              bind(eventsController) { content ->
                eventsController?.setScrollView(content)
                onUnbind { eventsController?.setScrollView(null) }
              }
            },
        style = style)
  }
}

internal class VerticalScrollLayoutBehavior(
    private val component: Component,
    private val fillViewport: Boolean,
    private val componentTree: ComponentTree
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    if (componentTree.isReleased) {
      return PrimitiveLayoutResult(
          width = 0, height = 0, layoutData = VerticalScrollLayoutData(0, 0))
    }
    // If fillViewport is true, then set a minimum height to ensure that the viewport is filled.
    val actualComponent =
        if (fillViewport) {
          Wrapper.create(ComponentContext(androidContext))
              .delegate(component)
              .minHeightPx(sizeConstraints.maxHeight)
              .build()
        } else {
          component
        }

    val size = Size()

    componentTree.setRootAndSizeSpecSync(
        actualComponent,
        sizeConstraints.toWidthSpec(),
        MeasureSpecUtils.unspecified(),
        size,
    )

    // Compute the appropriate size depending on the heightSpec
    if (sizeConstraints.hasExactHeight) {
      // If this Vertical scroll is being measured with an exact height we don't care
      // about the size of the content and just use that instead
      size.height = sizeConstraints.maxHeight
    } else if (sizeConstraints.hasBoundedHeight) {
      // For bounded height we want the VerticalScroll to be as big as its content up to
      // the maximum height specified in the heightSpec
      size.height = max(0, min(sizeConstraints.maxHeight, size.height))
    }

    // Ensure that width is not less than 0
    size.width = max(0, size.width)

    return PrimitiveLayoutResult(
        width = size.width,
        height = size.height,
        layoutData = VerticalScrollLayoutData(size.width, size.height))
  }
}

internal data class VerticalScrollLayoutData(val measuredWidth: Int, val measuredHeight: Int)
