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
import androidx.core.widget.NestedScrollView
import com.facebook.litho.Component
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.widget.ExperimentalVerticalScroll
import com.facebook.litho.widget.VerticalScrollEventsController
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp
import com.facebook.rendercore.px

/** Builder function for creating Vertical Scroll Primitive. */
@Suppress("FunctionName")
inline fun ResourcesScope.VerticalScroll(
    initialScrollPosition: Dimen = 0.px,
    scrollbarEnabled: Boolean = false,
    scrollbarFadingEnabled: Boolean = true,
    verticalFadingEdgeEnabled: Boolean = false,
    nestedScrollingEnabled: Boolean = false,
    fadingEdgeLength: Dimen = 0.dp,
    fadingEdgeColor: Int? = null,
    fillViewport: Boolean = false,
    overScrollMode: Int = View.OVER_SCROLL_IF_CONTENT_SCROLLS,
    eventsController: VerticalScrollEventsController? = null,
    shouldCompareCommonProps: Boolean = false,
    incrementalMountEnabled: Boolean = true,
    noinline onScrollChange: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)? = null,
    noinline onScrollStateChange: ((View, Int) -> Unit)? = null,
    noinline onInterceptTouch: ((NestedScrollView, event: MotionEvent) -> Boolean)? = null,
    style: Style? = null,
    crossinline child: ResourcesScope.() -> Component
): Component {
  return ExperimentalVerticalScroll(
      scrollbarEnabled = scrollbarEnabled,
      nestedScrollingEnabled = nestedScrollingEnabled,
      verticalFadingEdgeEnabled = verticalFadingEdgeEnabled,
      fillViewport = fillViewport,
      scrollbarFadingEnabled = scrollbarFadingEnabled,
      overScrollMode = overScrollMode,
      fadingEdgeLength = fadingEdgeLength,
      fadingEdgeColor = fadingEdgeColor,
      initialScrollPosition = initialScrollPosition,
      eventsController = eventsController,
      onScrollChange = onScrollChange,
      onInterceptTouch = onInterceptTouch,
      onScrollStateChange = onScrollStateChange,
      incrementalMountEnabled = incrementalMountEnabled,
      child = child(),
      style = style,
  )
}
