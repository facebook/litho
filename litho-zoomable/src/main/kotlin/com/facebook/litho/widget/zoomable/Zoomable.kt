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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.InterceptTouchEvent
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.TouchEvent
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.binders.onBindHostView
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useCached
import com.facebook.litho.useCallback
import com.facebook.litho.view.onInterceptTouch
import com.facebook.litho.view.onTouch
import com.facebook.rendercore.zoomable.ZoomableViewBaseController
import com.facebook.yoga.YogaAlign

/**
 * Pinch to zoom on [child]: while the fingers zoom it, a scaled copy of it is drawn over the
 * window, and when they lift the copy springs back. [child] keeps its size and stays in this tree.
 *
 * A `SurfaceView` in [child] scales and moves with the copy too on Android 15 and 16; other
 * versions aren't verified. The copy is drawn only inside the window the container is in, so in a
 * dialog or a popup it is clipped to that window.
 *
 * The container is a plain host, which [OverlayZoomController] drives. It takes the gesture from
 * [child] when a second finger lands on it, so content with its own pinch or two-finger gestures,
 * such as a map or an image viewer, stops getting them. It also keeps the touches [child] doesn't
 * handle, so an ancestor's click handler doesn't see those, unless [keepUnhandledTouches] is false:
 * then those touches go on to the ancestors, and an ancestor that handles them passes a pinch to
 * [zoomableTouchGestureListener]. While zooming, the enclosing LithoView keeps all its content
 * mounted.
 */
@ExperimentalLithoApi
class Zoomable(
    private val zoomableTouchGestureListener: ZoomableTouchGestureListener? = null,
    private val zoomableEventListener: ZoomableViewBaseController.Listener? = null,
    private val keepUnhandledTouches: Boolean = true,
    private val style: Style = Style,
    private val child: () -> Component,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val androidContext = context.androidContext
    val controller = useCached(androidContext) { OverlayZoomController(androidContext) }
    val gestureListener =
        useCached(zoomableTouchGestureListener) {
          zoomableTouchGestureListener ?: ZoomableTouchGestureListener()
        }
    val onTouch =
        useCallback<TouchEvent, Boolean> { event ->
          gestureListener.onTouch(event.motionEvent)
        }
    val onInterceptTouch =
        useCallback<InterceptTouchEvent, Boolean> { event ->
          controller.onInterceptTouchEvent(event.motionEvent)
        }
    return Column(
        alignItems = YogaAlign.FLEX_START,
        style =
            style +
                Style.onTouch(onTouch)
                    .onInterceptTouch(true, onInterceptTouch)
                    // Separate binders, so a new listener from a render doesn't end a zoom.
                    .onBindHostView(controller) { host ->
                      controller.bindTo(host)
                      onUnbind {
                        controller.unbind()
                        controller.zoomableEventListener = null
                      }
                    }
                    .onBindHostView(controller, gestureListener) { _ ->
                      gestureListener.addZoomableTouchListener(controller.zoomableTouchListener)
                      onUnbind {
                        gestureListener.removeZoomableTouchListener(
                            controller.zoomableTouchListener,
                        )
                      }
                    }
                    .onBindHostView(controller, keepUnhandledTouches) { _ ->
                      controller.keepsUnhandledTouches = keepUnhandledTouches
                      onUnbind { controller.keepsUnhandledTouches = true }
                    }
                    .onBindHostView(controller, zoomableEventListener) { _ ->
                      controller.zoomableEventListener = zoomableEventListener
                      onUnbind {
                        // Kept during a zoom, so its end still reaches the listener when the host
                        // unbinds.
                        if (!controller.zoomableTouchListener.isCurrentlyInZoom) {
                          controller.zoomableEventListener = null
                        }
                      }
                    },
    ) {
      child(child())
    }
  }
}
