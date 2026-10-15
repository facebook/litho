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

package com.facebook.litho.sections.widget

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.EventHandler
import com.facebook.litho.KComponent
import com.facebook.litho.SizeSpec
import com.facebook.litho.Style
import com.facebook.litho.TouchEvent
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.kotlinStyle
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.widget.LinearLayoutInfo
import com.facebook.litho.widget.LithoRecyclerView
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.SnapUtil

class ViewPagerComponent<T>
@JvmOverloads
constructor(
    private val dataDiffSection: DataDiffSection<T>,
    private val eventsController: RecyclerCollectionEventsController? = null,
    private val initialPageIndex: Int = 0,
    private val disableSwiping: Boolean = false,
    private val pageSelectedEventHandler: EventHandler<PageSelectedEvent>? = null,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val recyclerConfiguration =
        ListRecyclerConfiguration.create()
            .orientation(LinearLayoutManager.HORIZONTAL)
            .snapMode(SnapUtil.SNAP_TO_CENTER)
            .linearLayoutInfoFactory {
                ctx: Context,
                orientation: Int,
                reverseLayout: Boolean,
                _: Boolean ->
              ViewPagerLinearLayoutInfo(ctx, orientation, reverseLayout)
            }
            .build()

    val builder =
        RecyclerCollectionComponent.create(context)
            .flexGrow(1f)
            .disablePTR(true)
            .section(
                ViewPagerHelperSection.create<T>(SectionContext(context))
                    .delegateSection(dataDiffSection)
                    .pageSelectedEventEventHandler(pageSelectedEventHandler)
                    .initialPageIndex(initialPageIndex),
            )
            .eventsController(eventsController)
            .recyclerConfiguration(recyclerConfiguration)
            .kotlinStyle(style)

    if (disableSwiping) {
      // Consume the touch event before it can get to the RV to disable swiping, and also disable
      // the RV's normal touchIntercept behavior by ignoring onInterceptTouchEvent.
      builder
          .touchInterceptor { _, _ -> LithoRecyclerView.TouchInterceptor.Result.IGNORE_TOUCH_EVENT }
          .recyclerTouchEventHandler(eventHandlerWithReturn<TouchEvent, Boolean> { true })
    }

    return builder.build()
  }

  companion object {
    /**
     * Backward-compat helper used by [ViewPagerHelperSectionSpec] (still a Spec-API section) to
     * dispatch a [PageSelectedEvent] to the handler supplied via [pageSelectedEventHandler].
     */
    @JvmStatic
    fun dispatchPageSelectedEvent(
        handler: EventHandler<PageSelectedEvent>,
        selectedPageIndex: Int,
    ) {
      val event = PageSelectedEvent()
      event.selectedPageIndex = selectedPageIndex
      handler.dispatchEvent(event)
    }
  }

  /** Custom implementation of LinearLayout to assign parent's width to items. */
  private class ViewPagerLinearLayoutInfo(
      context: Context,
      orientation: Int,
      reverseLayout: Boolean,
  ) : LinearLayoutInfo(context, orientation, reverseLayout) {

    override fun getChildWidthSpec(widthSpec: Int, renderInfo: RenderInfo): Int {
      val hscrollWidth = SizeSpec.getSize(widthSpec)
      return SizeSpec.makeSizeSpec(hscrollWidth, SizeSpec.EXACTLY)
    }
  }
}
