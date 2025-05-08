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

import androidx.core.widget.NestedScrollView.OnScrollChangeListener
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.Reason
import com.facebook.rendercore.px

@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec
object HorizontalScrollSpec {

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      context: ComponentContext,
      @Prop contentProps: Component?,
      @Prop(optional = true) initialScrollPosition: Int,
      @Prop(optional = true) scrollbarEnabled: Boolean,
      @Prop(optional = true) wrapContent: Boolean = false,
      @Prop(optional = true) fillViewport: Boolean,
      @Prop(optional = true) eventsController: HorizontalScrollEventsController?,
      @Prop(optional = true)
      onScrollChangeListener: HorizontalScrollLithoView.OnScrollChangeListener?,
      @Prop(optional = true) scrollStateListener: ScrollStateListener?,
      @Prop(optional = true) incrementalMountEnabled: Boolean,
      @Prop(optional = true) overScrollMode: Int,
      @Prop(optional = true) horizontalFadingEdgeEnabled: Boolean,
      @Prop(optional = true) fadingEdgeLength: Int,
  ): Component? {
    requireNotNull(contentProps)
    return ExperimentalHorizontalScroll(
        wrapContent = wrapContent,
        fillViewport = fillViewport,
        scrollbarEnabled = scrollbarEnabled,
        eventsController = eventsController,
        onScrollChangeListener = onScrollChangeListener,
        scrollStateListener = scrollStateListener,
        overScrollMode = overScrollMode,
        horizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled,
        fadingEdgeLength = fadingEdgeLength.px,
        incrementalMountEnabled = incrementalMountEnabled,
        initialScrollPosition = initialScrollPosition.px,
        child = contentProps,
        style = null)
  }
}
