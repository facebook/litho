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

/**
 * Allows a top-down communication with a component and its immediate parent. The component must be
 * able to handle [com.facebook.litho.annotations.OnTrigger] events in order to accept an
 * EventTrigger.
 */
class EventTrigger<E>(parentKey: String, val id: Int, childKey: String, val handle: Handle?) {

  val key: String = parentKey + id + childKey
  var triggerTarget: EventTriggerTarget? = null
  var componentContext: ComponentContext? = null

  @JvmOverloads
  fun dispatchOnTrigger(event: E, params: Array<Any> = arrayOf()): Any? {
    return triggerTarget?.acceptTriggerEvent(this, event as Any, params)
  }
}
