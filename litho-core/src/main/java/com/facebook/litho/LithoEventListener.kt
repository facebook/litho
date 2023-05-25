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

import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventSubscriber

/**
 * This is used as a listener for events that happen on the scope of a given [ComponentTree]. These
 * events can be related with both the `render` UI pipeline, or with the view-side events created by
 * rendercore in the [LithoView] that is associated to the [ComponentTree] to which this listener
 * observes.
 *
 * In order to specify which events you are interested in, you should override the
 * [ComponentTreeDebugEventListener.events] and list any of the events present in [LithoDebugEvent]
 * or [DebugEvent].
 *
 * A client can attach one of these listeners during the creation of a [ComponentTree]:
 * ```
 * val listener = object: ComponentTreeDebugEventListener {
 *   override fun onEvent(debugEvent: DebugEvent) {
 *      /* do your logging / business logic */
 *   }
 *
 *   override val events = setOf(LayoutCommitted, MountItemMount)
 * }
 *
 * val componentTree = ComponentTree.create(context, MyComponent())
 *  .withComponentTreeDebugEventListener(listener)
 *  .create()
 *
 * val lithoView = LithoView.create(context, componentTree)
 * ```
 */
interface ComponentTreeDebugEventListener {

  fun onEvent(debugEvent: DebugEvent)

  val events: Set<String>
}

/**
 * This abstraction acts as a wrapper which guarantees that it will act only in events related to
 * the [componentTreeId].
 *
 * This is meant to be used only by the internals of the [ComponentTree], by wrapping any
 * [ComponentTreeDebugEventListener] passed in by the clients. This is a mechanism so that we can
 * guarantee that the client's listener will only observe events related to the [ComponentTree] to
 * which it is associated.
 *
 * *Note:* ideally this code would belong inside the [ComponentTree], but to keep it in Kotlin we
 * are leaving it outside it until finish the Kotlin migration.
 */
internal class ComponentTreeDebugEventsSubscriber(
    componentTreeId: Int,
    eventsToObserve: Set<String>,
    private val onComponentTreeEvent: (DebugEvent) -> Unit,
) : DebugEventSubscriber(*eventsToObserve.toTypedArray()) {

  private val componentTreeIdStr = componentTreeId.toString()

  override fun onEvent(event: DebugEvent) {
    if (event.renderStateId != componentTreeIdStr) {
      return
    }

    onComponentTreeEvent(event)
  }
}
