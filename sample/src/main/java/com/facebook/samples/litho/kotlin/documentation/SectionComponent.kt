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

package com.facebook.samples.litho.kotlin.documentation

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo

// start_example
class SectionComponent(private val words: List<String>) : KComponent() {
  override fun ComponentScope.render(): Component? {
    return Column(style = Style.width(80.dp).height(80.dp)) {
      child(
          RecyclerCollectionComponent(
              section =
                  DataDiffSection.create<String>(SectionContext(context))
                      .data(words)
                      .renderEventHandler(eventHandlerWithReturn { event -> onRender(event) })
                      .build()))
    }
  }

  private fun ResourcesScope.onRender(event: RenderEvent<String>): RenderInfo {
    return ComponentRenderInfo.create().component(Text(text = event.model)).build()
  }
}
// end_example
