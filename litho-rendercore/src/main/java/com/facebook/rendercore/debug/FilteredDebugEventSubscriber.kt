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

package com.facebook.rendercore.debug

/**
 * This Subscriber can be used to listen to specified event types. The filter can be further refined
 * by using an optional [DebugEventAttributeMatcher]. The callback will be invoked for any of the
 * [events], but only if the event's attributes passes the [matcher]. If the [matcher] is `null`
 * then the attributes are not checked.
 */
class FilteredDebugEventSubscriber(
    vararg events: String,
    private val matcher: DebugEventAttributeMatcher? = null,
    private val callback: (DebugEvent) -> Unit,
) : DebugEventSubscriber(events = events) {
  override fun onEvent(event: DebugEvent) {
    if (events.contains(event.type) && (matcher == null || matcher.matches(event.attributes))) {
      callback(event)
    }
  }
}

fun interface DebugEventAttributeMatcher {
  fun matches(attributes: Map<String, Any?>): Boolean
}

/**
 * This is a simple attribute matcher that pass if and only if the event's attributes have matches
 * all the attribute values from [toMatch].
 */
class StringAttributeMatcher(
    private vararg val toMatch: Pair<String, Any?>,
) : DebugEventAttributeMatcher {
  override fun matches(attributes: Map<String, Any?>): Boolean {
    return toMatch.all {
      val (attribute, value) = it
      value == attributes[attribute]
    }
  }
}
