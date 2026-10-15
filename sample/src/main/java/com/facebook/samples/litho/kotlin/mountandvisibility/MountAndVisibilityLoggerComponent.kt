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

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.visibility.onFocusedVisible
import com.facebook.litho.visibility.onFullImpression
import com.facebook.litho.visibility.onInvisible
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaAlign

/**
 * Callback interface for mount and visibility events.
 *
 * This allows the parent component to receive and display events in a log.
 */
interface EventLogger {
  fun log(itemId: String, eventType: EventType, details: String = "")
}

/** Types of events that can occur for a component. */
enum class EventType(val label: String) {
  // Mount events (View lifecycle)
  MOUNTED("MOUNTED"),
  UNMOUNTED("UNMOUNTED"),

  // Visibility events (User perception)
  VISIBLE("onVisible"),
  INVISIBLE("onInvisible"),
  FOCUSED("onFocused"),
  FULL_IMPRESSION("onFullImpression"),
}

/** A component that logs both mount/unmount and visibility events. */
class MountAndVisibilityLoggerComponent(
    private val itemId: String,
    private val eventLogger: EventLogger,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val isEven = itemId.toIntOrNull()?.let { it % 2 == 0 } ?: false
    val bgColor = if (isEven) "#F0F0F0".toColorInt() else Color.WHITE

    return Row(
        alignItems = YogaAlign.CENTER,
        style =
            style +
                Style.padding(horizontal = 16.dp, vertical = 12.dp)
                    .margin(vertical = 1.dp, horizontal = 8.dp)
                    .backgroundColor(bgColor)
                    .onVisible { eventLogger.log(itemId, EventType.VISIBLE) }
                    .onInvisible { eventLogger.log(itemId, EventType.INVISIBLE) }
                    .onFocusedVisible { eventLogger.log(itemId, EventType.FOCUSED) }
                    .onFullImpression { eventLogger.log(itemId, EventType.FULL_IMPRESSION) },
    ) {
      child(
          MountIndicatorPrimitive(
              itemId = itemId,
              eventLogger = eventLogger,
              style = Style.width(20.dp).height(20.dp).margin(end = 12.dp),
          ),
      )
      child(Text(text = "Item $itemId", textSize = 16.sp))
    }
  }
}

/** A Primitive component that logs mount/unmount events. */
class MountIndicatorPrimitive(
    private val itemId: String,
    private val eventLogger: EventLogger,
    private val style: Style? = null,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = FixedSizeLayoutBehavior(24.px, 24.px),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> View(context) }) {
              bind(itemId) { content ->
                content.background = Color.GRAY.toDrawable()
                eventLogger.log(itemId, EventType.MOUNTED)

                onUnbind {
                  content.background = Color.LTGRAY.toDrawable()
                  eventLogger.log(itemId, EventType.UNMOUNTED)
                }
              }
            },
        style = style,
    )
  }
}

/**
 * Data class representing a logged event.
 *
 * Used to display events in the event log UI.
 */
data class LoggedEvent(
    val timestamp: Long,
    val itemId: String,
    val eventType: EventType,
    val details: String = "",
) {
  fun format(): String {
    return "Item $itemId | ${eventType.label}"
  }
}

/** A simple event log display component. */
class EventLogComponent(
    private val events: List<LoggedEvent>,
    private val maxEvents: Int = 10,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val recentEvents = events.takeLast(maxEvents).reversed()

    return Column(
        style =
            style +
                Style.flex(shrink = 0f)
                    .padding(all = 12.dp)
                    .backgroundColor("#F5F5F5".toColorInt()),
    ) {
      child(
          Text(
              text = "Event Log",
              textSize = 16.sp,
              textStyle = Typeface.BOLD,
              style = Style.flex(shrink = 0f).margin(bottom = 8.dp),
          ),
      )

      if (recentEvents.isEmpty()) {
        child(Text(text = "Scroll the list to see events", textSize = 14.sp))
      } else {
        recentEvents.forEach { event ->
          child(
              Text(text = event.format(), textSize = 13.sp, style = Style.margin(vertical = 2.dp)),
          )
        }
      }
    }
  }
}
