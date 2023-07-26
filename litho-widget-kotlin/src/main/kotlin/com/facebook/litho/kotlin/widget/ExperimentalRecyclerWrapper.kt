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

package main.kotlin.com.facebook.litho.kotlin.widget

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.ExperimentalRecycler
import com.facebook.litho.widget.Binder
import com.facebook.litho.widget.LithoRecyclerView
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.SectionsRecyclerView
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.dp

fun ExperimentalRecyclerWrapper(
    binder: Binder<RecyclerView>,
    hasFixedSize: Boolean? = null,
    isClipToPaddingEnabled: Boolean? = null,
    refreshProgressBarBackgroundColor: Int? = null,
    refreshProgressBarColor: Int? = null,
    isClipChildrenEnabled: Boolean? = null,
    nestedScrollingEnabled: Boolean? = null,
    scrollBarStyle: Int? = null,
    itemDecoration: RecyclerView.ItemDecoration? = null,
    horizontalFadingEdgeEnabled: Boolean? = null,
    verticalFadingEdgeEnabled: Boolean? = null,
    fadingEdgeLength: Dimen? = null,
    recyclerViewId: Int? = null,
    overScrollMode: Int? = null,
    contentDescription: CharSequence? = null,
    itemAnimator: RecyclerView.ItemAnimator? = ExperimentalRecycler.DEFAULT_ITEM_ANIMATOR,
    recyclerEventsController: RecyclerEventsController? = null,
    onScrollListeners: List<RecyclerView.OnScrollListener?>? = null,
    snapHelper: SnapHelper? = null,
    pullToRefresh: Boolean? = null,
    touchInterceptor: LithoRecyclerView.TouchInterceptor? = null,
    onItemTouchListener: RecyclerView.OnItemTouchListener? = null,
    onRefresh: (() -> Unit)? = null,
    sectionsViewLogger: SectionsRecyclerView.SectionsRecyclerViewLogger? = null,
    useTwoBindersRecycler: Boolean = false,
    enableSeparateAnimatorBinder: Boolean = false,
    leftPadding: Int = 0,
    topPadding: Int = 0,
    rightPadding: Int = 0,
    bottomPadding: Int = 0,
    style: Style? = null
): ExperimentalRecycler =
    ExperimentalRecycler(
        binder = binder,
        hasFixedSize = hasFixedSize ?: true,
        isClipToPaddingEnabled = isClipToPaddingEnabled ?: true,
        refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
        refreshProgressBarColor = refreshProgressBarColor ?: Color.BLACK,
        isClipChildrenEnabled = isClipChildrenEnabled ?: true,
        isNestedScrollingEnabled = nestedScrollingEnabled ?: true,
        scrollBarStyle = scrollBarStyle ?: View.SCROLLBARS_INSIDE_OVERLAY,
        itemDecoration = itemDecoration,
        isHorizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled ?: false,
        isVerticalFadingEdgeEnabled = verticalFadingEdgeEnabled ?: false,
        fadingEdgeLength = fadingEdgeLength ?: 0.dp,
        recyclerViewId = recyclerViewId ?: View.NO_ID,
        overScrollMode = overScrollMode ?: View.OVER_SCROLL_ALWAYS,
        contentDescription = contentDescription,
        itemAnimator = itemAnimator ?: ExperimentalRecycler.DEFAULT_ITEM_ANIMATOR,
        recyclerEventsController = recyclerEventsController,
        onScrollListeners = onScrollListeners ?: emptyList(),
        snapHelper = snapHelper,
        isPullToRefreshEnabled = pullToRefresh ?: true,
        touchInterceptor = touchInterceptor,
        onItemTouchListener = onItemTouchListener,
        onRefresh = onRefresh,
        sectionsViewLogger = sectionsViewLogger,
        leftPadding = leftPadding,
        topPadding = topPadding,
        rightPadding = rightPadding,
        bottomPadding = bottomPadding,
        style = style)
