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

@file:JvmName("EventHandlerHelper")

package com.facebook.litho

import com.facebook.litho.annotations.EventHandlerRebindMode
import com.facebook.rendercore.RenderCoreSystrace

@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Any, R> eventHandlerWithReturn(
    noinline onEvent: (E) -> R
): EventHandler<E> = KEventHandler(onEvent = onEvent)

@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Any> eventHandler(
    noinline onEvent: (E) -> Unit,
): EventHandler<E> = eventHandler(onEvent, null)

@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Any> eventHandler(
    noinline onEvent: (E) -> Unit,
    tag: String?
): EventHandler<E> = KEventHandler(onEvent = onEvent, tag = tag)

fun <E : Any, R> create(onEvent: (E) -> R): EventHandler<E> {
  return KEventHandler(onEvent = onEvent)
}

fun <E : Any, R> create(id: Int, params: Array<Any?>?, onEvent: (E) -> R): EventHandler<E> {
  return KEventHandler(id = id, params = params, onEvent = onEvent)
}

/**
 * [EventHandler] for codegen-free Components which squashes [EventHandler], [HasEventDispatcher]
 * and [EventDispatcher] together in one object allocation.
 */
@PublishedApi
internal class KEventHandler<E : Any, R>(
    id: Int = -1,
    params: Array<Any?>? = null,
    private val onEvent: (event: E) -> R,
    private val tag: String? = null,
) :
    EventHandler<E>(id, EventHandlerRebindMode.NONE, EventDispatchInfo(null, null), params),
    HasEventDispatcher,
    EventDispatcher {

  init {
    dispatchInfo.hasEventDispatcher = this
    dispatchInfo.tag = tag
  }

  override fun dispatchEvent(event: E): Any? {
    applyOnValidTag { RenderCoreSystrace.beginSection("onEvent: $tag") }

    val result = onEvent(event)

    applyOnValidTag { RenderCoreSystrace.endSection() }

    return result
  }

  private fun applyOnValidTag(block: () -> Unit) {
    if (!tag.isNullOrEmpty()) block()
  }

  override fun dispatchOnEvent(eventHandler: EventHandler<*>, eventState: Any): R {
    @Suppress("UNCHECKED_CAST")
    return onEvent(eventState as E)
  }

  override fun getEventDispatcher(): EventDispatcher {
    return this
  }

  override fun isEquivalentTo(other: EventHandler<*>?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false

    other as KEventHandler<*, *>

    return onEvent == other.onEvent
  }
}
