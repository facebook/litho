/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Any> eventHandler(noinline onEvent: (E) -> Unit): EventHandler<E> =
     KEventHandler<E>(onEvent)

/**
 * [EventHandler] for codegen-free Components which squashes [EventHandler], [HasEventDispatcher]
 * and [EventDispatcher] together in one object allocation.
 */
class KEventHandler<E : Any>(
    private val onEvent: (event: E) -> Unit
) : EventHandler<E>(null, -1), HasEventDispatcher, EventDispatcher {

  init {
    mHasEventDispatcher = this
  }

  override fun dispatchEvent(event: E) {
    onEvent(event)
  }

  override fun dispatchOnEvent(eventHandler: EventHandler<*>, eventState: Any): Any? {
    @Suppress("UNCHECKED_CAST")
    return onEvent(eventState as E)
  }

  override fun getEventDispatcher(): EventDispatcher {
    return this
  }

  override fun isEquivalentTo(other: EventHandler<*>?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as KEventHandler<*>

    return onEvent == other.onEvent
  }
}
