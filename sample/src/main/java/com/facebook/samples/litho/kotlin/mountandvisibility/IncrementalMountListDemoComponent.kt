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

package com.facebook.samples.litho.kotlin.mountandvisibility

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.flexbox.flex
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useState
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp

// start_example
/** Demo showing the difference between Incremental Mount and Visibility Events. */
class IncrementalMountListDemoComponent(
    private val itemCount: Int = 100,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val events = useState { listOf<LoggedEvent>() }

    val eventLogger =
        object : EventLogger {
          override fun log(itemId: String, eventType: EventType, details: String) {
            val newEvent = LoggedEvent(
                timestamp = System.currentTimeMillis(),
                itemId = itemId,
                eventType = eventType,
                details = details,
            )
            events.update { currentEvents -> (listOf(newEvent) + currentEvents).take(100) }
          }
        }

    return Column(style = style + Style.flex(grow = 1f)) {
      child(
          EventLogComponent(
              events = events.value,
              maxEvents = 8,
              style = Style.flex(shrink = 0f).height(200.dp).margin(all = 8.dp),
          ),
      )

      // Scrollable list of 100 items
      child(
          LazyList(style = Style.flex(grow = 1f)) {
            for (i in 1..itemCount) {
              child(
                  id = i,
                  component =
                      MountAndVisibilityLoggerComponent(
                          itemId = i.toString(),
                          eventLogger = eventLogger,
                      ),
              )
            }
          },
      )
    }
  }
}
// end_example
