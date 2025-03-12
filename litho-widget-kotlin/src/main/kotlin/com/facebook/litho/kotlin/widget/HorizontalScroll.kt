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

import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.kotlinStyle
import com.facebook.litho.widget.ExperimentalHorizontalScroll
import com.facebook.litho.widget.HorizontalScroll
import com.facebook.litho.widget.HorizontalScrollEventsController
import com.facebook.litho.widget.ScrollStateListener
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp

/** Builder function for creating [HorizontalScrollSpec] components. */
@Suppress("FunctionName")
inline fun ResourcesScope.HorizontalScroll(
    initialScrollPosition: Dimen = ExperimentalHorizontalScroll.LAST_SCROLL_POSITION_UNSET.dp,
    scrollbarEnabled: Boolean = true,
    wrapContent: Boolean = false,
    fillViewport: Boolean = false,
    eventsController: HorizontalScrollEventsController? = null,
    noinline onScrollChange: ((View, scrollX: Int, oldScrollX: Int) -> Unit)? = null,
    horizontalFadingEdgeEnabled: Boolean = false,
    incrementalMountEnabled: Boolean = false,
    overScrollMode: Int = View.OVER_SCROLL_IF_CONTENT_SCROLLS,
    scrollStateListener: ScrollStateListener? = null,
    fadingEdgeLength: Int = 0,
    style: Style? = null,
    crossinline child: ResourcesScope.() -> Component
): Component {
  return if (ComponentsConfiguration.usePrimitiveHorizontalScroll) {
    ExperimentalHorizontalScroll(
        wrapContent = wrapContent,
        fillViewport = fillViewport,
        scrollbarEnabled = scrollbarEnabled,
        eventsController = eventsController,
        onScrollChangeListener = onScrollChange,
        scrollStateListener = scrollStateListener,
        overScrollMode = overScrollMode,
        horizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled,
        fadingEdgeLength = fadingEdgeLength.dp,
        incrementalMountEnabled = incrementalMountEnabled,
        initialScrollPosition = initialScrollPosition,
        child = child(),
        style = style)
  } else {
    HorizontalScroll.create(context)
        .contentProps(child())
        .initialScrollPosition(initialScrollPosition.toPixels())
        .scrollbarEnabled(scrollbarEnabled)
        .wrapContent(wrapContent)
        .fillViewport(fillViewport)
        .eventsController(eventsController)
        .onScrollChangeListener(onScrollChange)
        .horizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled)
        .fadingEdgeLength(fadingEdgeLength)
        .kotlinStyle(style)
        .build()
  }
}
