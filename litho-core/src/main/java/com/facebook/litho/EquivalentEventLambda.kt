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
 * A [Function1] event lambda that is backed by a real [EventHandler], so that lambda-shaped event
 * handlers behave exactly like the ones produced by the generated non-lambda `on<Event>(c)`
 * factory.
 *
 * The generated `*AsLambda` factories used to allocate a fresh anonymous [Function1] that closed
 * over the owning component/section instance (`_ref`) and its [ComponentContext]. That has two
 * problems: the fresh lambda is never structurally equal to the previous render's (breaking
 * equivalence and forcing redundant re-resolves), and — more importantly — a reused lambda would
 * dispatch against the captured, now-stale `@Prop`/`@State`/context snapshot.
 *
 * This wrapper captures nothing render-specific. It holds the [EventHandler] created by the sibling
 * `on<Event>(c)` factory and delegates to it for both behaviors:
 * - [invoke] routes through [EventHandler.dispatchEvent], which reads `@Prop`/`@State` live from
 *   the dispatcher and context that [EventHandlersController] / [TreeState.updateEventDispatchers]
 *   rebind to the latest committed owner every commit. `@Param` values come from the handler's own
 *   `params`.
 * - [equals] delegates to [EventHandler.isEquivalentTo], so two instances compare exactly as the
 *   underlying `EventHandler`s would (stable id, mode, shared dispatch-info, and `@Param` values).
 *
 * The result is that a lambda returned by `*AsLambda` is equivalence-stable across renders and
 * always dispatches against the current owner — identical to a plain [EventHandler].
 *
 * @param eventHandler the real handler from the `on<Event>(c)` factory; the single source of truth
 *   for dispatch and equivalence.
 * @param returnsUnit whether the underlying event method returns `Unit` (`void`); when true
 *   [invoke] returns [Unit] rather than the dispatch result (which is `null` for void events).
 */
class EquivalentEventLambda<E, R>(
    private val eventHandler: EventHandler<E>,
    private val returnsUnit: Boolean,
) : Function1<E, R> {

  override fun invoke(event: E): R {
    val result = eventHandler.dispatchEvent(event)
    @Suppress("UNCHECKED_CAST")
    return (if (returnsUnit) Unit else result) as R
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is EquivalentEventLambda<*, *>) {
      return false
    }
    return eventHandler.isEquivalentTo(other.eventHandler)
  }

  override fun hashCode(): Int = eventHandler.id
}
