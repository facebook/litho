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

/**
 * This is used as a listener for events that happen on the scope of a given [ComponentTree]. These
 * events can be related with both the `render` UI pipeline, or with the view-side events created by
 * rendercore in the [LithoView] that is associated to the [ComponentTree] to which this listener
 * observes.
 *
 * In order to specify which events you are interested in, you should override the
 * [ComponentTreeDebugEventListener.events] and list any of the events present in [LithoDebugEvent]
 * or [DebugEvent].
 */
interface ComponentTreeDebugEventListener {

  fun onEvent(debugEvent: DebugEvent)

  val events: Set<String>
}
