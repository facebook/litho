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

import android.view.View
import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.rendercore.dp

@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec
object VerticalScrollSpec {

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop childComponent: Component?,
      @Prop(optional = true) initialScrollPosition: Int = 0,
      @Prop(optional = true) scrollbarEnabled: Boolean,
      @Prop(optional = true) scrollbarFadingEnabled: Boolean = true,
      @Prop(optional = true) verticalFadingEdgeEnabled: Boolean,
      @Prop(optional = true) nestedScrollingEnabled: Boolean,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) fadingEdgeLength: Int,
      @Prop(optional = true, resType = ResType.COLOR) fadingEdgeColor: Int? = null,
      @Prop(optional = true) fillViewport: Boolean,
      @Prop(optional = true) overScrollMode: Int = View.OVER_SCROLL_IF_CONTENT_SCROLLS,
      @Prop(optional = true) initialScrollOffsetPixels: Int,
      @Prop(optional = true) eventsController: VerticalScrollEventsController?,
      @Prop(optional = true) shouldCompareCommonProps: Boolean,
      @Prop(optional = true) onScrollChangeListener: OnScrollChangeListener?,
      @Prop(optional = true) scrollStateListener: ScrollStateListener?,
      @Prop(optional = true) onInterceptTouchListener: LithoScrollView.OnInterceptTouchListener?,
      @Prop(optional = true) incrementalMountEnabled: Boolean,
  ): Component? {
    requireNotNull(childComponent)
    return if (ComponentsConfiguration.usePrimitiveVerticalScroll) {
      ExperimentalVerticalScroll(
          scrollbarEnabled = scrollbarEnabled,
          nestedScrollingEnabled = nestedScrollingEnabled,
          verticalFadingEdgeEnabled = verticalFadingEdgeEnabled,
          fillViewport = fillViewport,
          scrollbarFadingEnabled = scrollbarFadingEnabled,
          overScrollMode = overScrollMode,
          fadingEdgeLength = fadingEdgeLength.dp,
          fadingEdgeColor = fadingEdgeColor,
          initialScrollPosition = initialScrollPosition.dp,
          eventsController = eventsController,
          onScrollChange =
              onScrollChangeListener?.let {
                { view, scrollY, oldScrollY -> it.onScrollChange(view, 0, scrollY, 0, oldScrollY) }
              },
          onInterceptTouch =
              onInterceptTouchListener?.let { { view, event -> it.onInterceptTouch(view, event) } },
          onScrollStateChange =
              scrollStateListener?.let {
                { view, scrollState -> it.onScrollStateChanged(view, scrollState) }
              },
          incrementalMountEnabled = incrementalMountEnabled,
          child = childComponent,
          style = null)
    } else {
      VerticalScrollComponent.create(c)
          .childComponent(childComponent)
          .initialScrollOffsetPixels(initialScrollOffsetPixels)
          .scrollbarEnabled(scrollbarEnabled)
          .scrollbarFadingEnabled(scrollbarFadingEnabled)
          .verticalFadingEdgeEnabled(verticalFadingEdgeEnabled)
          .nestedScrollingEnabled(nestedScrollingEnabled)
          .fadingEdgeLengthPx(fadingEdgeLength)
          .fadingEdgeColor(fadingEdgeColor)
          .fillViewport(fillViewport)
          .eventsController(eventsController)
          .overScrollMode(overScrollMode)
          .scrollStateListener(scrollStateListener)
          .shouldCompareCommonProps(shouldCompareCommonProps)
          .onScrollChangeListener(onScrollChangeListener)
          .onInterceptTouchListener(onInterceptTouchListener)
          .incrementalMountEnabled(incrementalMountEnabled)
          .build()
    }
  }
}
