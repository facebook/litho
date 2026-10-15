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
import com.facebook.litho.ComponentsSystrace.trace
import com.facebook.litho.debug.LithoTouchEventDebugger
import com.facebook.litho.debug.TouchEventType

/**
 * Click listener that triggers its underlying [eventHandler] and, optionally, a list of [extra]
 * lambdas registered via `Style.clickListener(key, …)`.
 *
 * Dispatch order on click: [eventHandler] first, then each entry of [extra] in iteration order.
 * Extras fire inside the same `ComponentsSystrace` + [LithoTouchEventDebugger] envelope as the
 * primary so they show up in the same trace as the click that triggered them.
 *
 * Composition (which listeners constitute primary + extras, and the "new primary clears prior
 * extras" rule) is decided at render time when the `ViewAttributes` are assembled — never at bind /
 * unbind. At mount this listener is installed atomically via `view.setOnClickListener(…)`; at
 * unmount it is cleared via `view.setOnClickListener(null)`.
 */
internal class ComponentClickListener(
    private val eventHandler: EventHandler<ClickEvent>,
    private val extra: List<(ClickEvent) -> Unit> = emptyList(),
) : View.OnClickListener {

  override fun onClick(view: View) {
    trace(
        {
          val componentClassName =
              (eventHandler.dispatchInfo.hasEventDispatcher?.javaClass?.name ?: "").take(100)
          "onClick_<cls>$componentClassName</cls>"
        },
    ) {
      LithoTouchEventDebugger.report(
          type = TouchEventType.Click,
          view = view,
          handler = eventHandler,
      )
      EventDispatcherUtils.dispatchOnClick(eventHandler, view)
      if (extra.isNotEmpty()) {
        val event = ClickEvent().apply { this.view = view }
        for (i in extra.indices) {
          extra[i](event)
        }
      }
    }
  }
}
