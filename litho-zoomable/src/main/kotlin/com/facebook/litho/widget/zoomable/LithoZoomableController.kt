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

package com.facebook.litho.widget.zoomable

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.ViewGroup
import com.facebook.litho.LithoRenderTreeView
import com.facebook.litho.findComponentActivity
import com.facebook.rendercore.zoomable.ZoomableViewBaseController

class LithoZoomableController(
    private val controllerContext: Context,
    backgroundDrawable: Drawable = ColorDrawable(BLACK_TRANSPARENT_80),
) :
    ZoomableViewBaseController<LithoZoomableView>(
        context = controllerContext,
        backgroundDrawable = backgroundDrawable,
        isRemoteZoomEnabled = true,
    ) {

  // Computed property: resolves from Context each time, avoiding a strong reference
  // to the Activity's decorView that would prevent GC if the controller outlives the Activity.
  // Only accessed during startRemoteZoom/teardownRemoteZoom (at most twice per gesture).
  override val decorView: ViewGroup
    get() {
      val activity =
          checkNotNull(controllerContext.findComponentActivity()) {
            "LithoZoomableController requires an Activity-backed Context, " +
                "but findComponentActivity() returned null for ${controllerContext.javaClass.name}"
          }
      return checkNotNull(activity.window) {
            "LithoZoomableController: Activity ${activity.javaClass.name} has a null window"
          }
          .decorView as ViewGroup
    }

  override fun getRenderTreeView(): LithoRenderTreeView = requireRootView().renderTreeView

  val zoomableTouchListener: ZoomableTouchListener =
      object : ZoomableTouchListener {
        override fun onTouchEvent(event: MotionEvent): Boolean {
          return onTouch(event)
        }

        override val isCurrentlyInZoom: Boolean
          get() = state != State.IDLE
      }
}
