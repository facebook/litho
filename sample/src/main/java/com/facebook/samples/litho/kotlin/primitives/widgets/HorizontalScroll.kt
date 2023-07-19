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

import android.view.View
import android.view.ViewTreeObserver
import android.widget.HorizontalScrollView
import com.facebook.litho.Component
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoLayoutContextExtraData.LithoLayoutExtraData
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.R
import com.facebook.litho.Size
import com.facebook.litho.Style
import com.facebook.litho.useState
import com.facebook.litho.widget.HorizontalScrollEventsController
import com.facebook.litho.widget.HorizontalScrollLithoView
import com.facebook.litho.widget.ScrollStateListener
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.utils.MeasureSpecUtils
import com.facebook.yoga.YogaDirection
import kotlin.math.max

private const val LAST_SCROLL_POSITION_UNSET = -1
private const val DEFAULT_OVER_SCROLL_MODE = View.OVER_SCROLL_IF_CONTENT_SCROLLS

fun HorizontalScroll(
    scrollbarEnabled: Boolean = true,
    eventsController: HorizontalScrollEventsController? = null,
    onScrollChangeListener: HorizontalScrollLithoView.OnScrollChangeListener? = null,
    scrollStateListener: ScrollStateListener? = null,
    incrementalMountEnabled: Boolean = false,
    overScrollMode: Int = DEFAULT_OVER_SCROLL_MODE,
    initialScrollPosition: Int = LAST_SCROLL_POSITION_UNSET,
    fillViewport: Boolean = false,
    style: Style? = null,
    child: () -> Component,
): Component {

  return HorizontalScroll(
      scrollbarEnabled = scrollbarEnabled,
      eventsController = eventsController,
      onScrollChangeListener = onScrollChangeListener,
      scrollStateListener = scrollStateListener,
      incrementalMountEnabled = incrementalMountEnabled,
      overScrollMode = overScrollMode,
      initialScrollPosition = initialScrollPosition,
      fillViewport = fillViewport,
      child = child(),
      style = style,
  )
}

class HorizontalScroll(
    private val scrollbarEnabled: Boolean = true,
    private val eventsController: HorizontalScrollEventsController? = null,
    private val onScrollChangeListener: HorizontalScrollLithoView.OnScrollChangeListener? = null,
    private val scrollStateListener: ScrollStateListener? = null,
    private val incrementalMountEnabled: Boolean = false,
    private val overScrollMode: Int = DEFAULT_OVER_SCROLL_MODE,
    private val initialScrollPosition: Int = LAST_SCROLL_POSITION_UNSET,
    private val fillViewport: Boolean = false,
    private val child: Component,
    private val style: Style? = null
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val lastScrollPosition = useState {
      HorizontalScrollLithoView.ScrollPosition(initialScrollPosition)
    }

    val childComponentTree = useState {
      ComponentTree.createNestedComponentTree(context, child)
          .incrementalMount(incrementalMountEnabled)
          .build()
    }

    val scrollbarsAttr =
        getIntAttrValue(
            context,
            R.styleable.HorizontalScroll_android_scrollbars,
            R.styleable.HorizontalScroll,
            -1)

    val resolvedScrollbarEnabled =
        if (scrollbarsAttr == -1) scrollbarEnabled else scrollbarsAttr != 0

    return LithoPrimitive(
        layoutBehavior =
            HorizontalScrollLayoutBehavior(fillViewport, child, childComponentTree.value),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> HorizontalScrollLithoView(context) }) {
              doesMountRenderTreeHosts = true

              resolvedScrollbarEnabled.bindTo(
                  HorizontalScrollLithoView::setHorizontalScrollBarEnabled, true)

              overScrollMode.bindTo(
                  HorizontalScrollLithoView::setOverScrollMode, DEFAULT_OVER_SCROLL_MODE)

              bind(eventsController) { content ->
                eventsController?.setScrollableView(content)
                onUnbind { eventsController?.setScrollableView(null) }
              }

              bindWithLayoutData<HorizontalScrollLayoutData>(
                  childComponentTree.value,
                  lastScrollPosition.value,
                  onScrollChangeListener,
                  scrollStateListener) { content, horizontalScrollLayoutData ->
                    content.mount(
                        childComponentTree.value,
                        lastScrollPosition.value,
                        horizontalScrollLayoutData.measuredWidth,
                        horizontalScrollLayoutData.measuredHeight,
                        onScrollChangeListener,
                        scrollStateListener)
                    onUnbind { content.unmount() }
                  }

              bindWithLayoutData<HorizontalScrollLayoutData>(lastScrollPosition.value) {
                  content,
                  horizontalScrollLayoutData ->
                val onPreDrawListener =
                    object : ViewTreeObserver.OnPreDrawListener {
                      override fun onPreDraw(): Boolean {
                        if (!content.viewTreeObserver.isAlive) {
                          return true
                        }
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        if (lastScrollPosition.value.x == LAST_SCROLL_POSITION_UNSET) {
                          if (horizontalScrollLayoutData.layoutDirection == YogaDirection.RTL) {
                            content.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
                          }
                          lastScrollPosition.value.x = content.scrollX
                        } else {
                          content.scrollX = lastScrollPosition.value.x
                        }
                        return true
                      }
                    }

                content.viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
                onUnbind {
                  if (content.viewTreeObserver.isAlive) {
                    content.viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
                  }
                }
              }
            },
        style = style)
  }
}

internal class HorizontalScrollLayoutBehavior(
    private val fillViewport: Boolean,
    private val childComponent: Component,
    private val childComponentTree: ComponentTree
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    val size = Size()

    // Measure the component with undefined width spec, as the contents of the
    // hscroll have unlimited horizontal space.
    childComponentTree.setRootAndSizeSpecSync(
        childComponent, MeasureSpecUtils.unspecified(), sizeConstraints.toHeightSpec(), size)

    size.width = max(0, max(size.width, if (fillViewport) sizeConstraints.maxWidth else 0))
    size.height = max(0, size.height)

    val extraLayoutData: LithoLayoutExtraData? =
        layoutContext.layoutContextExtraData?.extraLayoutData as? LithoLayoutExtraData?
    val direction = extraLayoutData?.layoutDirection ?: DEFAULT_LAYOUT_DIRECTION
    return PrimitiveLayoutResult(
        width = size.width,
        height = size.height,
        layoutData = HorizontalScrollLayoutData(size.width, size.height, direction))
  }

  companion object {
    private val DEFAULT_LAYOUT_DIRECTION = YogaDirection.LTR
  }
}

internal data class HorizontalScrollLayoutData(
    val measuredWidth: Int,
    val measuredHeight: Int,
    val layoutDirection: YogaDirection
)
