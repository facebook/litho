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

package com.facebook.litho

import android.view.View
import com.facebook.litho.ComponentsSystrace.isTracing

/** Click listener that triggers its underlying event handler. */
internal class ComponentClickListener(private val eventHandler: EventHandler<ClickEvent>) :
    View.OnClickListener {

  override fun onClick(view: View) {
    val tracingEnabled = isTracing

    if (tracingEnabled) {
      val componentClassName =
          (eventHandler.dispatchInfo.hasEventDispatcher?.javaClass?.name ?: "").take(100)

      ComponentsSystrace.beginSection("onClick_<cls>$componentClassName</cls>")
    }

    try {
      EventDispatcherUtils.dispatchOnClick(eventHandler, view)
    } finally {
      if (tracingEnabled) {
        ComponentsSystrace.endSection()
      }
    }
  }
}
