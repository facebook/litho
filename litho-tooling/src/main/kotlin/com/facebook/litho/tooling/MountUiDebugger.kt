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

package com.facebook.litho.tooling

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable
import com.facebook.litho.LithoView
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute
import com.facebook.rendercore.debug.DebugEventSubscriber
import com.facebook.rendercore.debug.Duration

/**
 * This [DebugEventSubscriber] listens for mount related events for the [view] passed in the
 * constructor and it renders a flashing overlay on top of the view. This overlay will have a
 * different color according to the time the [RenderTree] takes to mount.
 *
 * It will also add a border around the items that are mounted, unmounted or updated with different
 * colors.
 */
class MountUiDebugger(private val view: LithoView) :
    DebugEventSubscriber(
        DebugEvent.RenderTreeMounted,
        DebugEvent.RenderUnitMounted,
        DebugEvent.RenderUnitUnmounted,
        DebugEvent.RenderUnitUpdated) {

  override fun onEvent(event: DebugEvent) {
    val hostHashCode: Int = event.attribute(DebugEventAttribute.RootHostHashCode)
    if (hostHashCode != view.hashCode()) return

    when (event.type) {
      DebugEvent.RenderTreeMounted -> onRenderTreeMounted(event)
      DebugEvent.RenderUnitMounted -> onRenderUnitMounted(event)
      DebugEvent.RenderUnitUnmounted -> onRenderUnitUnmounted(event)
      DebugEvent.RenderUnitUpdated -> onRenderUnitUpdated(event)
    }
  }

  private fun onRenderTreeMounted(event: DebugEvent) {
    val duration: Duration = event.attribute(DebugEventAttribute.duration)

    val color =
        when {
          duration.value < 4_000_000 -> GOOD
          duration.value < 8_000_000 -> MEDIUM
          else -> BAD
        }

    flash(color)
  }

  private fun onRenderUnitMounted(event: DebugEvent) =
      drawBordersOnBounds(Color.YELLOW, event.attribute(DebugEventAttribute.Bounds))

  private fun onRenderUnitUnmounted(event: DebugEvent) =
      drawBordersOnBounds(Color.RED, event.attribute(DebugEventAttribute.Bounds))

  private fun onRenderUnitUpdated(event: DebugEvent) =
      drawBordersOnBounds(Color.GREEN, event.attribute(DebugEventAttribute.Bounds))

  private fun drawBordersOnBounds(color: Int, bounds: Rect) {
    val d = ShapeDrawable()
    d.paint.style = Paint.Style.STROKE
    d.paint.color = color
    d.bounds = bounds
    view.overlay.add(d)
  }

  private fun flash(color: Int) {
    val d: Drawable = PaintDrawable(color)
    d.alpha = 125
    view.post {
      d.setBounds(0, 0, view.width, view.height)
      view.overlay.add(d)
      view.postDelayed({ view.overlay.remove(d) }, 500)
    }
  }

  private val GOOD = Color.parseColor("#81C784")
  private val MEDIUM = Color.parseColor("#FDDA0D")
  private val BAD = Color.parseColor("#E74C3C")
}
