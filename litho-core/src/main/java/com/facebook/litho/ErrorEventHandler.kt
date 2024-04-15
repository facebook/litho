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

import com.facebook.litho.annotations.EventHandlerRebindMode

/**
 * This class is an error event handler that clients can optionally set on a [ComponentTree] to
 * gracefully handle uncaught/unhandled exceptions thrown from the framework while resolving a
 * layout.
 */
abstract class ErrorEventHandler :
    EventHandler<ErrorEvent>(
        Component.ERROR_EVENT_HANDLER_ID, EventHandlerRebindMode.NONE, EventDispatchInfo(), null),
    HasEventDispatcher,
    EventDispatcher {

  init {
    // sets up HasEventDispatcher immediately after constructing EventHandler
    dispatchInfo?.hasEventDispatcher = this
  }

  override fun dispatchOnEvent(eventHandler: EventHandler<*>, eventState: Any): Any? {
    if (eventHandler.id == Component.ERROR_EVENT_HANDLER_ID) {
      val e = checkNotNull((eventState as ErrorEvent).exception)
      val cc = checkNotNull(eventState.componentContext)
      val component = onError(cc, e)
      if (component != null && cc.errorComponentReceiver != null) {
        cc.errorComponentReceiver?.onErrorComponent(component)
      }
    }
    return null
  }

  override fun dispatchEvent(event: ErrorEvent): Any? {
    return dispatchOnEvent(this, event)
  }

  override fun getEventDispatcher(): EventDispatcher? {
    return this
  }

  /** Action performed when exception occurred. */
  abstract fun onError(cc: ComponentContext, e: Exception): Component?
}
