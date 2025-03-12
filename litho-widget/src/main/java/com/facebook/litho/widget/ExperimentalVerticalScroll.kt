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

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
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
import com.facebook.litho.Style
import com.facebook.litho.bindToRenderTreeView
import com.facebook.litho.debug.DebugInfoReporter
import com.facebook.litho.useCached
import com.facebook.litho.useNestedTree
import com.facebook.litho.useState
import com.facebook.litho.utils.MeasureUtils.getChildMeasureSize
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.MaxPossibleHeightValue
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.toWidthSpec
import com.facebook.rendercore.utils.MeasureSpecUtils.getMode
import kotlin.math.max
import kotlin.math.min

class ExperimentalVerticalScroll(
    val scrollbarEnabled: Boolean,
    val nestedScrollingEnabled: Boolean,
    val verticalFadingEdgeEnabled: Boolean,
    val fillViewport: Boolean,
    val scrollbarFadingEnabled: Boolean,
    val overScrollMode: Int,
    val fadingEdgeLength: Dimen,
    @ColorInt val fadingEdgeColor: Int?,
    val initialScrollPosition: Dimen,
    val eventsController: VerticalScrollEventsController?,
    val onScrollChange: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)?,
    val onInterceptTouch: ((NestedScrollView, event: MotionEvent) -> Boolean)?,
    val onScrollStateChange: ((View, Int) -> Unit)?,
    val incrementalMountEnabled: Boolean = true,
    val child: Component,
    val style: Style?,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {

    val fadingEdgeLengthPx = fadingEdgeLength.toPixels()
    val config =
        useCached(incrementalMountEnabled) {
          val defaultConfig = context.lithoConfiguration.componentsConfig
          if (incrementalMountEnabled != defaultConfig.incrementalMountEnabled) {
            defaultConfig.copy(incrementalMountEnabled = incrementalMountEnabled)
          } else {
            defaultConfig
          }
        }
    val (
        state: NestedLithoTreeState,
        resolveResult: ResolveResult,
    ) = useNestedTree(root = child, config = config, treeProps = context.treePropContainer)

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
                { "VerticalScroll" },
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
                  withDescription("nestedScrollingEnabled") {
                    nestedScrollingEnabled.bindTo(LithoScrollView::setNestedScrollingEnabled, false)
                  }
                  withDescription("verticalFadingEdgeEnabled") {
                    verticalFadingEdgeEnabled.bindTo(
                        LithoScrollView::setVerticalFadingEdgeEnabled, false)
                  }

                  withDescription("scrollbarFadingEnabled") {
                    scrollbarFadingEnabled.bindTo(LithoScrollView::setScrollbarFadingEnabled, false)
                  }

                  withDescription("fadingEdgeLengthPx") {
                    fadingEdgeLengthPx.bindTo(
                        LithoScrollView::setFadingEdgeLength,
                        0,
                    )
                  }

                  withDescription("fadingEdgeColor") {
                    fadingEdgeColor.bindTo(LithoScrollView::setFadingEdgeColor, null)
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
                        overScrollMode,
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
          if (!sizeConstraints.hasBoundedHeight) {
            sizeConstraints.copy(
                minHeight = 0,
                maxHeight = sizeConstraints.maxHeight,
            )
          } else {
            val height = min(sizeConstraints.MaxPossibleHeightValue, sizeConstraints.maxHeight)
            if (height != sizeConstraints.maxHeight) {
              DebugInfoReporter.report(category = "SizeConstraintViolation") {
                this["component"] = resolveResult.component.getSimpleName()
                this["sizeConstraints"] = sizeConstraints.toString()
                this["MaxPossibleHeightValue"] = sizeConstraints.MaxPossibleHeightValue
              }
            }
            sizeConstraints.copy(minHeight = height, maxHeight = SizeConstraints.Infinity)
          }
        } else {
          SizeConstraints(
              minWidth = 0,
              maxWidth = sizeConstraints.maxWidth,
              minHeight = 0,
              maxHeight = SizeConstraints.Infinity,
          )
        }

    val (minMeasureWidth, maxMeasureWidth) =
        getChildMeasureSize(
            constraints.minWidth,
            constraints.maxWidth,
            ViewGroup.LayoutParams.MATCH_PARENT,
            getMode(sizeConstraints.toWidthSpec()))
    val childConstraints = constraints.copy(minWidth = minMeasureWidth, maxWidth = maxMeasureWidth)
    val layoutState =
        NestedLithoTree.layout(
            result = resolveResult,
            sizeConstraints = childConstraints,
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
