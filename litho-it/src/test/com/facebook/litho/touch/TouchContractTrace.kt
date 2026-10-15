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

package com.facebook.litho.touch

import android.view.MotionEvent
import android.view.View
import com.facebook.litho.Style
import com.facebook.litho.view.onClick
import com.facebook.litho.view.onInterceptTouch
import com.facebook.litho.view.onLongClick
import com.facebook.litho.view.onTouch

/**
 * Records the exact sequence of touch-handler invocations produced by a gesture.
 *
 * The dispatch refactor must be semantics-preserving, so the bar is not "the behaviors we thought
 * to assert still hold" but "the observable contract is identical". A trace captures the whole
 * surface per invocation — which handler on which component, for which action, with which
 * `event.view`, at which coordinates — so a change nobody thought to assert still fails the
 * comparison.
 */
class TouchContractTrace {

  private val entries = mutableListOf<String>()
  private val seenViews = java.util.IdentityHashMap<View, Int>()

  /** Coordinates are truncated to int: sub-pixel noise is not part of the contract. */
  private fun record(
      component: String,
      handler: String,
      view: View?,
      event: MotionEvent?,
      returned: Any?,
  ) {
    val action = event?.let { actionName(it.actionMasked) } ?: "-"
    val where = event?.let { "(${it.x.toInt()},${it.y.toInt()})" } ?: ""
    val pointers = if ((event?.pointerCount ?: 1) > 1) " p=${event?.pointerCount}" else ""
    val ret = if (returned == null) "" else " -> $returned"
    entries.add("$component.$handler[$action] view=${label(view)}$where$pointers$ret")
  }

  /**
   * Attaches every touch handler to a component, so the trace captures the full surface.
   *
   * [interceptWhen] overrides [onInterceptTouchReturns] per event, for a parent that steals the
   * stream partway through a gesture rather than at DOWN. [onDown] runs before the DOWN is
   * recorded, for a child that needs to act on the gesture start.
   */
  fun styleFor(
      component: String,
      onTouchReturns: Boolean = false,
      onLongClickReturns: Boolean = true,
      onInterceptTouchReturns: Boolean = false,
      interceptWhen: ((MotionEvent) -> Boolean)? = null,
      onDown: ((MotionEvent, View) -> Unit)? = null,
  ): Style {
    val style =
        Style.onTouch { event ->
              if (event.motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                onDown?.invoke(event.motionEvent, event.view)
              }
              record(component, "onTouch", event.view, event.motionEvent, onTouchReturns)
              onTouchReturns
            }
            .onInterceptTouch { event ->
              val result = interceptWhen?.invoke(event.motionEvent) ?: onInterceptTouchReturns
              record(component, "onInterceptTouch", event.view, event.motionEvent, result)
              result
            }
            .onLongClick { event ->
              record(component, "onLongClick", event.view, null, onLongClickReturns)
              onLongClickReturns
            }
    return style.onClick { event -> record(component, "onClick", event.view, null, null) }
  }

  /** A lone [Style.onTouch], so nothing else sets the clickable flag on the host. */
  fun touchOnlyStyleFor(component: String, returns: Boolean = false): Style =
      Style.onTouch { event ->
        record(component, "onTouch", event.view, event.motionEvent, returns)
        returns
      }

  /** The recorded trace, one invocation per line, in dispatch order. */
  fun asText(): String = entries.joinToString("\n")

  /**
   * Identifies the View by class plus the order it was first seen, so the trace records *which*
   * host received the event without depending on a view tag. A dispatcher handing over the wrong
   * host, or the LithoView itself, changes this field.
   */
  private fun label(view: View?): String {
    if (view == null) {
      return "null"
    }
    val ordinal = seenViews.getOrPut(view) { seenViews.size + 1 }
    return "${view.javaClass.simpleName}#$ordinal"
  }

  private fun actionName(action: Int): String =
      when (action) {
        MotionEvent.ACTION_DOWN -> "DOWN"
        MotionEvent.ACTION_MOVE -> "MOVE"
        MotionEvent.ACTION_UP -> "UP"
        MotionEvent.ACTION_CANCEL -> "CANCEL"
        MotionEvent.ACTION_POINTER_DOWN -> "POINTER_DOWN"
        MotionEvent.ACTION_POINTER_UP -> "POINTER_UP"
        else -> "ACTION_$action"
      }
}
