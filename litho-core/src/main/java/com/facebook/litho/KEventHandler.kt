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

@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Any, R> eventHandlerWithReturn(
    noinline onEvent: (E) -> R,
): EventHandler<E> = eventHandlerWithReturn(onEvent, null)

/**
 * Creates a result handler. A callback exception goes to [componentContext]'s error handler and the
 * dispatch returns `null`; without a context, the exception propagates to the caller.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Any, R> eventHandlerWithReturn(
    noinline onEvent: (E) -> R,
    componentContext: ComponentContext?,
): EventHandler<E> = KEventHandler(onEvent = onEvent, componentContext = componentContext)

@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Any> eventHandler(
    noinline onEvent: (E) -> Unit,
): EventHandler<E> = eventHandler(onEvent, null)

@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Any> eventHandler(
    noinline onEvent: (E) -> Unit,
    noinline tag: (() -> String)?,
): EventHandler<E> = KEventHandler(onEvent = onEvent, tagProvider = tag)

@Suppress("NOTHING_TO_INLINE")
inline fun <reified E : Any> eventHandler(
    noinline onEvent: (E) -> Unit,
    noinline tag: (() -> String)?,
    componentContext: ComponentContext?,
): EventHandler<E> = KEventHandler(
    onEvent = onEvent,
    tagProvider = tag,
    componentContext = componentContext,
)

fun <E : Any, R> create(onEvent: (E) -> R): EventHandler<E> {
  return KEventHandler(onEvent = onEvent)
}

fun <E : Any, R> create(id: Int, params: Array<out Any?>?, onEvent: (E) -> R): EventHandler<E> {
  return KEventHandler(id = id, params = params, onEvent = onEvent)
}

/**
 * [EventHandler] for codegen-free Components which squashes [EventHandler], [HasEventDispatcher]
 * and [EventDispatcher] together in one object allocation.
 */
@PublishedApi
internal class KEventHandler<E : Any, R>(
    id: Int = -1,
    params: Array<out Any?>? = null,
    private val onEvent: (event: E) -> R,
    private val tagProvider: (() -> String)? = null,
    private val componentContext: ComponentContext? = null,
) :
    EventHandler<E>(
        id,
        EventHandlerRebindMode.NONE,
        EventDispatchInfo(null, componentContext),
        params,
    ),
    HasEventDispatcher,
    EventDispatcher {

  init {
    dispatchInfo.hasEventDispatcher = this
  }

  override fun dispatchEvent(event: E): Any? {
    val componentScope = componentContext?.componentScope
    val tracingTag =
        if (ComponentsSystrace.isTracing) tagProvider?.invoke() ?: componentScope?.simpleName
        else null
    if (tracingTag != null) {
      ComponentsSystrace.beginSection("onEvent:$tracingTag")
    }

    val result =
        try {
          return onEvent(event)
        } catch (e: Exception) {
          if (componentContext != null) {
            ComponentUtils.handle(componentContext, e)
            null
          } else {
            throw e
          }
        } finally {
          if (tracingTag != null) {
            ComponentsSystrace.endSection()
          }
        }

    return result
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
