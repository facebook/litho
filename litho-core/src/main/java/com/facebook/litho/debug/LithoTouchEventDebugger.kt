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

package com.facebook.litho.debug

import android.graphics.Point
import android.graphics.Rect
import android.view.View
import com.facebook.litho.EventHandler
import com.facebook.litho.debug.LithoDebugEvent.NoRenderStateId
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventDispatcher

object LithoTouchEventDebugger {

  @JvmStatic var isEnabled: Boolean = false

  fun report(
      type: TouchEventType,
      view: View,
      handler: EventHandler<*>,
      event: Any? = null,
  ) {
    if (isEnabled) {
      DebugEventDispatcher.dispatch(type.toString(), NoRenderStateId) { attrs ->
        attrs[ATTRIBUTE_TYPE] = type.toString()
        attrs[ATTRIBUTE_EVENT] = event
        attrs[ATTRIBUTE_HANDLER] = handler.toString()
        attrs[ATTRIBUTE_BOUNDS] = Rect(view.left, view.top, view.right, view.bottom)
        attrs[ATTRIBUTE_VIEW_TYPE] = "<cls>${view.javaClass.name}</cls>"
        attrs[ATTRIBUTE_VIEW_ID] = view.id
        val globalPosition = IntArray(2)
        view.getLocationOnScreen(globalPosition)
        attrs[ATTRIBUTE_VIEW_GLOBAL_POSITION] = Point(globalPosition[0], globalPosition[1])
        attrs[ATTRIBUTE_VIEW_HASH_CODE] = view.hashCode()
        attrs[ATTRIBUTE_VIEW_TAG] = view.tag
        attrs[ATTRIBUTE_HANDLER_TAG] = handler.tag
        attrs[ATTRIBUTE_GLOBAL_KEY] = handler.dispatchInfo.componentContext?.nullableGlobalKey
        attrs[ATTRIBUTE_CONTENT_DESC] = view.contentDescription
      }
    }
  }

  const val ATTRIBUTE_TYPE: String = "event-type"
  const val ATTRIBUTE_EVENT: String = "event-object"
  const val ATTRIBUTE_HANDLER: String = "handler"
  const val ATTRIBUTE_BOUNDS: String = DebugEventAttribute.Bounds
  const val ATTRIBUTE_VIEW_TYPE: String = "view-type"
  const val ATTRIBUTE_VIEW_ID: String = "view-id"
  const val ATTRIBUTE_VIEW_GLOBAL_POSITION: String = "view-global-position"
  const val ATTRIBUTE_VIEW_HASH_CODE: String = "view-hash-code"
  const val ATTRIBUTE_VIEW_TAG: String = "view-tag"
  const val ATTRIBUTE_HANDLER_TAG: String = "handler-tag"
  const val ATTRIBUTE_GLOBAL_KEY: String = "global-key"
  const val ATTRIBUTE_CONTENT_DESC: String = "content-desc"
}

@JvmInline
value class TouchEventType(val value: Int) {

  override fun toString(): String {
    return when (this) {
      Click -> "Click"
      LongClick -> "LongClick"
      Touch -> "Touch"
      else -> throw IllegalArgumentException("Unknown touch event type $value")
    }
  }

  companion object {
    val Click: TouchEventType = TouchEventType(0)
    val LongClick: TouchEventType = TouchEventType(1)
    val Touch: TouchEventType = TouchEventType(2)

    fun fromString(name: String): TouchEventType {
      return when (name) {
        Click.toString() -> Click
        LongClick.toString() -> LongClick
        Touch.toString() -> Touch
        else -> throw IllegalArgumentException("Unknown touch event type $name")
      }
    }

    val ALL_TOUCH_EVENT_NAMES: Array<String> = arrayOf(
        Click.toString(),
        LongClick.toString(),
        Touch.toString(),
    )
  }
}
