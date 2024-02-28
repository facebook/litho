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

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.widget.NestedScrollView
import com.facebook.litho.Component
import com.facebook.litho.LayoutState
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.LithoRenderTreeView
import com.facebook.litho.NestedLithoTree
import com.facebook.litho.NestedLithoTreeState
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.ResolveResult
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.bindToRenderTreeView
import com.facebook.litho.kotlinStyle
import com.facebook.litho.useNestedTree
import com.facebook.litho.useState
import com.facebook.litho.widget.LithoScrollView
import com.facebook.litho.widget.VerticalScrollEventsController
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import kotlin.math.max
import kotlin.math.min

typealias VerticalScrollSpecComponent = com.facebook.litho.widget.VerticalScroll

/** Builder function for creating Vertical Scroll Primitive. */
@Suppress("FunctionName")
inline fun ResourcesScope.VerticalScroll(
    initialScrollPosition: Dimen = 0.px,
    scrollbarEnabled: Boolean = false,
    scrollbarFadingEnabled: Boolean = true,
    verticalFadingEdgeEnabled: Boolean = false,
    nestedScrollingEnabled: Boolean = false,
    fadingEdgeLength: Dimen = 0.dp,
    fillViewport: Boolean = false,
    overScrollMode: Int = View.OVER_SCROLL_IF_CONTENT_SCROLLS,
    eventsController: VerticalScrollEventsController? = null,
    noinline onScrollChange: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)? = null,
    noinline onScrollStateChange: ((View, Int) -> Unit)? = null,
    noinline onInterceptTouch: ((NestedScrollView, event: MotionEvent) -> Boolean)? = null,
    style: Style? = null,
    child: ResourcesScope.() -> Component
): Component {
  return if (context.isPrimitiveVerticalScrollEnabled) {
    return VerticalScrollComponent(
        scrollbarEnabled = scrollbarEnabled,
        nestedScrollingEnabled = nestedScrollingEnabled,
        verticalFadingEdgeEnabled = verticalFadingEdgeEnabled,
        fillViewport = fillViewport,
        scrollbarFadingEnabled = scrollbarFadingEnabled,
        overScrollMode = overScrollMode,
        fadingEdgeLength = fadingEdgeLength,
        initialScrollPosition = initialScrollPosition,
        eventsController = eventsController,
        onScrollChange = onScrollChange,
        onInterceptTouch = onInterceptTouch,
        onScrollStateChange = onScrollStateChange,
        child = child(),
        style = style,
    )
  } else {
    VerticalScrollSpecComponent.create(context)
        .childComponent(child())
        .initialScrollOffsetPixels(initialScrollPosition.toPixels())
        .scrollbarEnabled(scrollbarEnabled)
        .scrollbarFadingEnabled(scrollbarFadingEnabled)
        .verticalFadingEdgeEnabled(verticalFadingEdgeEnabled)
        .nestedScrollingEnabled(nestedScrollingEnabled)
        .fadingEdgeLengthPx(fadingEdgeLength.toPixels())
        .fillViewport(fillViewport)
        .eventsController(eventsController)
        .apply {
          onScrollChange?.let {
            onScrollChangeListener { v, _, scrollY, _, oldScrollY -> it(v, scrollY, oldScrollY) }
          }
        }
        .onInterceptTouchListener(onInterceptTouch)
        .kotlinStyle(style)
        .build()
  }
}

@PublishedApi
internal class VerticalScrollComponent(
    val scrollbarEnabled: Boolean,
    val nestedScrollingEnabled: Boolean,
    val verticalFadingEdgeEnabled: Boolean,
    val fillViewport: Boolean,
    val scrollbarFadingEnabled: Boolean,
    val overScrollMode: Int,
    val fadingEdgeLength: Dimen,
    val initialScrollPosition: Dimen,
    val eventsController: VerticalScrollEventsController?,
    val onScrollChange: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)?,
    val onInterceptTouch: ((NestedScrollView, event: MotionEvent) -> Boolean)?,
    val onScrollStateChange: ((View, Int) -> Unit)?,
    val child: Component,
    val style: Style?,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {

    val fadingEdgeLengthPx = fadingEdgeLength.toPixels()

    val (
        state: NestedLithoTreeState,
        resolveResult: ResolveResult,
    ) = useNestedTree(root = child, treeProps = context.treePropContainer)

    val scrollPosition = useState {
      LithoScrollView.ScrollPosition(initialScrollPosition.toPixels())
    }

    return LithoPrimitive(
        layoutBehavior =
            VerticalScrollLayoutBehavior(
                resolveResult = resolveResult,
                fillViewport = fillViewport,
            ),
        mountBehavior =
            MountBehavior(
                "VerticalScroll",
                ViewAllocator { context ->
                  LithoScrollView(context, LithoRenderTreeView(context))
                }) {

                  // bind to LithoRenderTreeView
                  bindToRenderTreeView(state = state) { renderTreeView as LithoRenderTreeView }

                  withDescription("onScrollStateChange") {
                    onScrollStateChange.bindTo(LithoScrollView::setScrollStateListener, null)
                  }

                  withDescription("scrollPosition") {
                    scrollPosition.value.bindTo(LithoScrollView::setScrollPosition, null)
                  }

                  withDescription("scrollbarFadingEnabled") {
                    scrollbarFadingEnabled.bindTo(LithoScrollView::setScrollbarFadingEnabled, false)
                  }

                  withDescription("nestedScrollingEnabled") {
                    nestedScrollingEnabled.bindTo(LithoScrollView::setNestedScrollingEnabled, false)
                  }

                  withDescription("verticalFadingEdgeEnabled") {
                    verticalFadingEdgeEnabled.bindTo(
                        LithoScrollView::setVerticalFadingEdgeEnabled, false)
                  }

                  withDescription("fadingEdgeLengthPx") {
                    fadingEdgeLengthPx.bindTo(
                        LithoScrollView::setFadingEdgeLength,
                        ViewConfiguration.get(androidContext).scaledFadingEdgeLength,
                    )
                  }

                  withDescription("scrollbarEnabled") {
                    bind(scrollbarEnabled) { content ->
                      content.isVerticalScrollBarEnabled = scrollbarEnabled
                      onUnbind { content.isVerticalScrollBarEnabled = false }
                    }
                  }

                  withDescription("onScrollChange") {
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
                            null as NestedScrollView.OnScrollChangeListener?,
                        )
                      }
                    }
                  }

                  withDescription("onInterceptTouch") {
                    bind(onInterceptTouch) { content ->
                      content.setOnInterceptTouchListener(onInterceptTouch)
                      onUnbind { content.setOnInterceptTouchListener(null) }
                    }
                  }

                  withDescription("overScrollMode") {
                    overScrollMode.bindTo(
                        LithoScrollView::setOverScrollMode,
                        View.OVER_SCROLL_IF_CONTENT_SCROLLS,
                    )
                  }

                  withDescription("eventsController") {
                    bind(eventsController) { content ->
                      eventsController?.setScrollView(content)
                      onUnbind { eventsController?.setScrollView(null) }
                    }
                  }
                },
        style = style)
  }
}

internal class VerticalScrollLayoutBehavior(
    private val resolveResult: ResolveResult,
    private val fillViewport: Boolean,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {

    val constraints: SizeConstraints =
        if (fillViewport) {
          sizeConstraints.copy(
              minHeight = sizeConstraints.maxHeight,
              maxHeight = SizeConstraints.Infinity,
          )
        } else {
          SizeConstraints(
              minWidth = 0,
              maxWidth = sizeConstraints.maxWidth,
              minHeight = 0,
              maxHeight = SizeConstraints.Infinity,
          )
        }

    val layoutState =
        NestedLithoTree.layout(
            result = resolveResult,
            sizeConstraints = constraints,
            current = previousLayoutData as LayoutState?,
        )

    // Ensure that width is not less than 0
    val width: Int = max(sizeConstraints.minWidth, layoutState.width)

    // Compute the appropriate size depending on the heightSpec
    val height: Int =
        if (sizeConstraints.hasExactHeight) {
          // If this Vertical scroll is being measured with an exact height we
          // don't care about the size of the content and just use that instead
          sizeConstraints.maxHeight
        } else if (sizeConstraints.hasBoundedHeight) {
          // For bounded height we want the VerticalScroll to be as big as its
          // content up to the maximum height specified in the heightSpec
          max(sizeConstraints.minHeight, min(sizeConstraints.maxHeight, layoutState.height))
        } else {
          max(sizeConstraints.minHeight, layoutState.height)
        }

    layoutState.toRenderTree()

    return PrimitiveLayoutResult(width = width, height = height, layoutData = layoutState)
  }
}
