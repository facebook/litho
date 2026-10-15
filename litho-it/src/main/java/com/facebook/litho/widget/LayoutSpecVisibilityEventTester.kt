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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Output
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.eventHandler
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.view.wrapInView
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.visibility.VisibleEvent

class LayoutSpecVisibilityEventTester(
    private val textOutput: Output<Any?>,
    private val viewOutput: Output<Any?>,
    private val nullOutput: Output<Any?>,
) : KComponent() {

  override fun ComponentScope.render(): Component {

    return Column(style = Style.wrapInView().onVisible { onViewVisible(viewOutput, it.content) }) {
      child(
          Row(style = Style.onVisible { onNotAViewVisible(nullOutput, it.content) }) {
            child(
                Text.create(context)
                    .text("test1")
                    .visibleHandler(
                        eventHandler<VisibleEvent> { onTextVisible(textOutput, it.content) },
                    )
                    .textSizeDip(30f)
                    .build(),
            )
          },
      )
    }
  }

  private fun ResourcesScope.onTextVisible(textOutput: Output<Any?>, content: Any?) {
    textOutput.set(content)
  }

  private fun ResourcesScope.onViewVisible(viewOutput: Output<Any?>, content: Any?) {
    viewOutput.set(content)
  }

  private fun ResourcesScope.onNotAViewVisible(
      nullOutput: Output<Any?>,
      content: Any?,
  ) {
    nullOutput.set(content)
  }
}
