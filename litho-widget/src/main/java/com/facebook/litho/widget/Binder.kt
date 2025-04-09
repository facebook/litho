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

import android.view.View
import android.view.ViewGroup
import com.facebook.litho.Component
import com.facebook.litho.ComponentTree
import com.facebook.litho.EventHandler
import com.facebook.litho.Size
import com.facebook.litho.widget.ViewportInfo.ViewportChanged

/**
 * This binder class is used to asynchronously layout Components given a list of [Component] and
 * attaching them to a [ViewGroup] through the bind(ViewGroup) method.
 */
interface Binder<V : ViewGroup> {
  /**
   * Set the width and height of the [View] that will be passed to the subsequent mount(ViewGroup),
   * bind(ViewGroup) and unmount(ViewGroup) calls. Can be called by any thread.
   *
   * @param width Usually the view width minus horizontal padding.
   * @param height Usually the view height minus vertical padding.
   */
  fun setSize(width: Int, height: Int)

  /** Measure the content of this Binder. Call this method from the Component's onMeasure. */
  fun measure(
      outSize: Size,
      widthSpec: Int,
      heightSpec: Int,
      reMeasureEventHandler: EventHandler<ReMeasureEvent>?
  )

  /** Returns the component at the given position in the binder. */
  fun getComponentAt(position: Int): ComponentTree?

  /**
   * Call this method before the [View] is mounted, i.e. within Component.onMount(ComponentContext,
   * Object)
   */
  fun mount(view: V)

  /**
   * Call this method when the view is unmounted.
   *
   * @param view the view being unmounted.
   */
  fun unmount(view: V)

  /**
   * Bind a [ViewportInfo.ViewportChanged] listener to this [Binder]. The listener will be notified
   * of Viewport changes.
   *
   * @param viewportChangedListener
   */
  fun setViewportChangedListener(viewportChangedListener: ViewportChanged?)

  /** Return true if wrap content is enabled for the main axis, false otherwise. */
  val isMainAxisWrapContent: Boolean

  /** Return true if wrap content is enabled for the cross axis, false otherwise. */
  val isCrossAxisWrapContent: Boolean

  /** Detach items under the hood. */
  fun detach()

  fun getChildWidthSpec(index: Int): Int

  fun getChildHeightSpec(index: Int): Int
}
