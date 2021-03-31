/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import androidx.core.widget.NestedScrollView
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.Dp
import com.facebook.litho.Px
import com.facebook.litho.Style
import com.facebook.litho.dp
import com.facebook.litho.px

/** Builder function for creating [VerticalScrollSpec] components. */
@Suppress("FunctionName")
inline fun ComponentScope.VerticalScroll(
    initialScrollPosition: Px = 0.px,
    scrollbarEnabled: Boolean = false,
    scrollbarFadingEnabled: Boolean = true,
    verticalFadingEdgeEnabled: Boolean = false,
    nestedScrollingEnabled: Boolean = false,
    fadingEdgeLength: Dp = 0.dp,
    fillViewport: Boolean = false,
    incrementalMountEnabled: Boolean = false,
    noinline onScrollChange: ((NestedScrollView, scrollY: Int, oldScrollY: Int) -> Unit)? = null,
    noinline onInterceptTouch: ((NestedScrollView, event: MotionEvent) -> Boolean)? = null,
    style: Style? = null,
    child: ComponentScope.() -> Component
): VerticalScroll =
    VerticalScroll.create(context)
        .childComponent(child())
        .initialScrollOffsetPixels(initialScrollPosition.value)
        .scrollbarEnabled(scrollbarEnabled)
        .scrollbarFadingEnabled(scrollbarFadingEnabled)
        .verticalFadingEdgeEnabled(verticalFadingEdgeEnabled)
        .nestedScrollingEnabled(nestedScrollingEnabled)
        .fadingEdgeLengthDip(fadingEdgeLength.value)
        .fillViewport(fillViewport)
        .incrementalMountEnabled(incrementalMountEnabled)
        .apply {
          onScrollChange?.let {
            onScrollChangeListener { v, _, scrollY, _, oldScrollY -> it(v, scrollY, oldScrollY) }
          }
        }
        .onInterceptTouchListener(onInterceptTouch)
        .build()
        .apply { applyStyle(style) }
