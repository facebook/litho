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
import com.facebook.litho.layoutVersion
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
import com.facebook.rendercore.px
import com.facebook.rendercore.toWidthSpec
import com.facebook.rendercore.utils.MeasureSpecUtils.getMode
import kotlin.math.max
import kotlin.math.min

class VerticalScroll(
    val child: Component,
    val scrollbarEnabled: Boolean = false,
    val nestedScrollingEnabled: Boolean = false,
    val verticalFadingEdgeEnabled: Boolean = false,
    val fillViewport: Boolean = false,
    val scrollbarFadingEnabled: Boolean = true,
    val overScrollMode: Int = android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS,
    val fadingEdgeLength: Dimen = 0.px,
    @param:ColorInt val fadingEdgeColor: Int? = null,
    val initialScrollPosition: Dimen = 0.px,
    val eventsController: VerticalScrollEventsController? = null,
    val onScrollChange: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)? = null,
    val onInterceptTouch: ((NestedScrollView, event: MotionEvent) -> Boolean)? = null,
    val onScrollStateChange: ScrollStateListener? = null,
    val incrementalMountEnabled: Boolean = true,
    val style: Style = Style,
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
    ) = useNestedTree(
        root = child,
        config = config,
        treeProps = context.treePropContainer,
    )

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
                ViewAllocator { context -> LithoScrollView(context, LithoRenderTreeView(context)) },
            ) {
              // bind to LithoRenderTreeView
              bindToRenderTreeView(state = state) { renderTreeView as LithoRenderTreeView }

              withDescription("onScrollStateChange") {
                onScrollStateChange.bindTo(LithoScrollView::setScrollStateListener, null)
              }

              withDescription("scrollPosition") {
                // Scroll position and eventsController are combined into a single binding
                // because the controller needs to mediate scroll position persistence across
                // unbind/bind cycles (e.g., view recycling). When a controller is present,
                // it holds the ScrollPosition reference and restores it when a new
                // LithoScrollView is bound. Without a controller, scroll position is
                // managed directly on the view as before.
                bind(scrollPosition.value, eventsController) { content ->
                  if (eventsController != null) {
                    // Let the controller own scroll position so it survives view recycling.
                    eventsController.setScrollPosition(scrollPosition.value)
                    eventsController.setScrollView(content)
                  } else {
                    content.setScrollPosition(scrollPosition.value)
                  }
                  onUnbind {
                    if (eventsController != null) {
                      // Detach the view; controller retains scrollPosition for the next bind.
                      eventsController.setScrollView(null)
                    } else {
                      content.setScrollPosition(null)
                    }
                  }
                }
              }
              withDescription("nestedScrollingEnabled") {
                nestedScrollingEnabled.bindTo(LithoScrollView::setNestedScrollingEnabled, false)
              }
              withDescription("verticalFadingEdgeEnabled") {
                verticalFadingEdgeEnabled.bindTo(
                    LithoScrollView::setVerticalFadingEdgeEnabled,
                    false,
                )
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
            },
        style = style,
    )
  }

  /**
   * Builder for constructing [VerticalScroll] from Java code. Allows setting only the properties
   * you need — all others use sensible defaults.
   *
   * Usage:
   * ```java
   * new VerticalScroll.Builder(child)
   *     .scrollbarEnabled(true)
   *     .nestedScrollingEnabled(true)
   *     .build();
   * ```
   */
  @SinceKotlin("1000.0") // make unavailable to Kotlin
  class Builder(private val child: Component) {
    private var scrollbarEnabled: Boolean = false
    private var nestedScrollingEnabled: Boolean = false
    private var verticalFadingEdgeEnabled: Boolean = false
    private var fillViewport: Boolean = false
    private var scrollbarFadingEnabled: Boolean = true
    private var overScrollMode: Int = android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS
    private var fadingEdgeLengthPx: Int = 0
    @ColorInt private var fadingEdgeColor: Int? = null
    private var initialScrollPositionPx: Int = 0
    private var eventsController: VerticalScrollEventsController? = null
    private var onScrollChange: ((NestedScrollView, Int, Int) -> Unit)? = null
    private var onInterceptTouch: ((NestedScrollView, MotionEvent) -> Boolean)? = null
    private var onScrollStateChange: ScrollStateListener? = null
    private var incrementalMountEnabled: Boolean = true
    private var style: Style = Style

    fun scrollbarEnabled(enabled: Boolean) = apply { this.scrollbarEnabled = enabled }

    fun nestedScrollingEnabled(enabled: Boolean) = apply { this.nestedScrollingEnabled = enabled }

    fun verticalFadingEdgeEnabled(enabled: Boolean) = apply {
      this.verticalFadingEdgeEnabled = enabled
    }

    fun fillViewport(fillViewport: Boolean) = apply { this.fillViewport = fillViewport }

    fun scrollbarFadingEnabled(enabled: Boolean) = apply { this.scrollbarFadingEnabled = enabled }

    fun overScrollMode(mode: Int) = apply { this.overScrollMode = mode }

    fun fadingEdgeLengthPx(px: Int) = apply { this.fadingEdgeLengthPx = px }

    fun fadingEdgeColor(@ColorInt color: Int?) = apply { this.fadingEdgeColor = color }

    fun initialScrollPositionPx(px: Int) = apply { this.initialScrollPositionPx = px }

    fun eventsController(controller: VerticalScrollEventsController?) = apply {
      this.eventsController = controller
    }

    fun onScrollChange(listener: ((NestedScrollView, Int, Int) -> Unit)?) = apply {
      this.onScrollChange = listener
    }

    fun onInterceptTouch(listener: ((NestedScrollView, MotionEvent) -> Boolean)?) = apply {
      this.onInterceptTouch = listener
    }

    fun onScrollStateChange(listener: ScrollStateListener?) = apply {
      this.onScrollStateChange = listener
    }

    fun incrementalMountEnabled(enabled: Boolean) = apply { this.incrementalMountEnabled = enabled }

    fun style(style: Style) = apply { this.style = style }

    fun build(): VerticalScroll = VerticalScroll(
        child = child,
        scrollbarEnabled = scrollbarEnabled,
        nestedScrollingEnabled = nestedScrollingEnabled,
        verticalFadingEdgeEnabled = verticalFadingEdgeEnabled,
        fillViewport = fillViewport,
        scrollbarFadingEnabled = scrollbarFadingEnabled,
        overScrollMode = overScrollMode,
        fadingEdgeLength = fadingEdgeLengthPx.px,
        fadingEdgeColor = fadingEdgeColor,
        initialScrollPosition = initialScrollPositionPx.px,
        eventsController = eventsController,
        onScrollChange = onScrollChange,
        onInterceptTouch = onInterceptTouch,
        onScrollStateChange = onScrollStateChange,
        incrementalMountEnabled = incrementalMountEnabled,
        style = style,
    )
  }

  companion object {
    /**
     * Java-compatible factory method that accepts pixel values instead of Dimen. Use this method
     * when calling from Java code since the primary constructor with Dimen parameters is not
     * accessible from Java due to inline class limitations.
     *
     * [JvmOverloads] generates one overload per trailing parameter, so Java callers bind arguments
     * by position. New parameters must therefore be appended here, never inserted, even when the
     * primary constructor above orders them differently.
     */
    @JvmStatic
    @JvmOverloads
    @SinceKotlin("1000.0") // make unavailable to Kotlin
    fun create(
        child: Component,
        scrollbarEnabled: Boolean = false,
        nestedScrollingEnabled: Boolean = false,
        verticalFadingEdgeEnabled: Boolean = false,
        fillViewport: Boolean = false,
        scrollbarFadingEnabled: Boolean = true,
        overScrollMode: Int = android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS,
        fadingEdgeLengthPx: Int = 0,
        @ColorInt fadingEdgeColor: Int? = null,
        initialScrollPositionPx: Int = 0,
        eventsController: VerticalScrollEventsController? = null,
        onScrollChange: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)? = null,
        onInterceptTouch: ((NestedScrollView, event: MotionEvent) -> Boolean)? = null,
        onScrollStateChange: ScrollStateListener? = null,
        incrementalMountEnabled: Boolean = true,
        style: Style? = null,
    ): VerticalScroll {
      return VerticalScroll(
          child = child,
          scrollbarEnabled = scrollbarEnabled,
          nestedScrollingEnabled = nestedScrollingEnabled,
          verticalFadingEdgeEnabled = verticalFadingEdgeEnabled,
          fillViewport = fillViewport,
          scrollbarFadingEnabled = scrollbarFadingEnabled,
          overScrollMode = overScrollMode,
          fadingEdgeLength = fadingEdgeLengthPx.px,
          fadingEdgeColor = fadingEdgeColor,
          initialScrollPosition = initialScrollPositionPx.px,
          eventsController = eventsController,
          onScrollChange = onScrollChange,
          onInterceptTouch = onInterceptTouch,
          onScrollStateChange = onScrollStateChange,
          incrementalMountEnabled = incrementalMountEnabled,
          style = style ?: Style,
      )
    }
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
                this["component"] = resolveResult.component.simpleName
                this["sizeConstraints"] = sizeConstraints.toString()
                this["MaxPossibleHeightValue"] = sizeConstraints.MaxPossibleHeightValue
              }
            }
            sizeConstraints.copy(minHeight = height, maxHeight = SizeConstraints.Infinity)
          }
        } else {
          SizeConstraints(
              maxWidth = sizeConstraints.maxWidth,
              maxHeight = SizeConstraints.Infinity,
          )
        }

    val (minMeasureWidth, maxMeasureWidth) =
        getChildMeasureSize(
            constraints.minWidth,
            constraints.maxWidth,
            ViewGroup.LayoutParams.MATCH_PARENT,
            getMode(sizeConstraints.toWidthSpec()),
        )
    val childConstraints = constraints.copy(minWidth = minMeasureWidth, maxWidth = maxMeasureWidth)

    // Use the layout version to ensure version consistency during double measures
    val layoutState =
        NestedLithoTree.layout(
            result = resolveResult,
            sizeConstraints = childConstraints,
            current = previousLayoutData as LayoutState?,
            version = layoutVersion,
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
