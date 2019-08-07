/*
* Copyright 2019-present Facebook, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.facebook.litho.codelab.auxiliary

import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.codelab.LifecycleEvent
import com.facebook.litho.codelab.LifecycleEventType
import com.facebook.litho.codelab.R
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.annotations.OnDataBound
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo

@GroupSectionSpec
object TimelineSectionSpec {

  @OnCreateChildren
  fun onCreateChildren(
      c: SectionContext,
      @Prop events: List<LifecycleEvent>
  ): Children {
    return Children.create()
        .child(
            DataDiffSection.create<LifecycleEvent>(c)
                .data(events)
                .renderEventHandler(TimelineSection.onRender(c))
        )
        .build()
  }

  @OnEvent(RenderEvent::class)
  fun onRender(c: SectionContext, @FromEvent model: LifecycleEvent): RenderInfo {
    val (typeStringId, colorId) = genLifecycleEventPair(model)
    return ComponentRenderInfo.create()
        .component(
            TimelineComponent.create(c)
                .typeRes(typeStringId)
                .colorRes(colorId)
                .timestamp(model.endTime))
        .build()
  }

  @OnDataBound
  fun onDataBound(
      c: SectionContext,
      @Prop events: List<LifecycleEvent>,
      @Prop eventsController: RecyclerCollectionEventsController
  ) {
    eventsController.requestScrollToPosition(events.size - 1, false)
  }

  private fun genLifecycleEventPair(model: LifecycleEvent) =
      when (model.eventType) {
        LifecycleEventType.ON_CREATE_INITIAL_STATE ->
          R.string.onCreateInitialState to R.color.colorOnCreateInitialState

        LifecycleEventType.ON_CREATE_TREE_PROP ->
          R.string.onCreateTreeProp to R.color.colorOnCreateTreeProp

        LifecycleEventType.ON_CREATE_SERVICE ->
          R.string.onCreateService to R.color.colorOnCreateService

        LifecycleEventType.ON_BIND_SERVICE ->
          R.string.onBindService to R.color.colorOnBindService

        LifecycleEventType.ON_UNBIND_SERVICE ->
          R.string.onUnbindService to R.color.colorOnUnbindService

        LifecycleEventType.ON_CREATE_CHILDREN ->
          R.string.onCreateChildren to R.color.colorOnCreateChildren

        LifecycleEventType.ON_DATA_BOUND ->
          R.string.onDataBound to R.color.colorOnDataBound

        LifecycleEventType.ON_DATA_RENDERED ->
          R.string.onDataRendered to R.color.colorOnDataRendered

        LifecycleEventType.ON_VIEWPORT_CHANGED ->
          R.string.onViewportChanged to R.color.colorOnViewportChanged

        LifecycleEventType.ON_REFRESH ->
          R.string.onRefresh to R.color.colorOnRefresh
      }
}
