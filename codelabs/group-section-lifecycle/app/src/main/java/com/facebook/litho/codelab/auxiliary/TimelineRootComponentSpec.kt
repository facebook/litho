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

package com.facebook.litho.codelab.auxiliary

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.codelab.LifecycleEvent
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.GridRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController

@Suppress("MagicNumber")
@LayoutSpec
object TimelineRootComponentSpec {

  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      eventsController: StateValue<RecyclerCollectionEventsController>
  ) {
    eventsController.set(RecyclerCollectionEventsController())
  }

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop lifecycleEvents: List<LifecycleEvent>,
      @State eventsController: RecyclerCollectionEventsController
  ): Component {
    return RecyclerCollectionComponent.create(c)
        .recyclerConfiguration(GridRecyclerConfiguration.create().numColumns(3).build())
        .disablePTR(true)
        .section(
            TimelineSection.create(SectionContext(c))
                .eventsController(eventsController)
                .events(lifecycleEvents))
        .eventsController(eventsController)
        .build()
  }
}
