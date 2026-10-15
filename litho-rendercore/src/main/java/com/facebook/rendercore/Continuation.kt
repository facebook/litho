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

package com.facebook.rendercore

/**
 * A continuation encapsulates a restartable piece of a [Node]'s rendering logic that does not
 * materially affect its rendering output. For instance, scheduling asynchronous background layout
 * of the cells of a collection is logic that can be wrapped in a continuation.
 *
 * If some logic should affect or inform the rendering output of a [Node], then it should not be
 * represented as a continuation.
 *
 * A continuation will be continuously invoked by the integrating framework whenever the associated
 * rendering phase for an owning [Node] skips execution. It is the responsibility of the integrating
 * framework to manage all continuations that a [Node] may declare.
 *
 * For instance, in the example below, an integrating framework provides a means of registering
 * continuations in layout while a layout node takes advantage of the API to create layout futures
 * that will be scheduled to execute at some future point in time
 *
 * ```kotlin
 * fun calculateLayout(
 *   node: Node<T>,
 *   c: LayoutContext<T>,
 *   constraints: SizeConstraints
 * ): LayoutResult {
 *   // register a continuation to continuously re-schedule the node's layout futures
 *   registerLayoutContinuation { continuationContext: LayoutContinuationContext ->
 *     val (width, height) = resolveConstraints(constraints)
 *     // access the previous future results from the continuation context
 *     val previousResults: List<RenderResultFuture<T, S>> = continuationContext.get(node)
 *     // create new futures using the continuation context
 *     val futures: List<RenderResultFuture<T, S>> =
 *        createFutures(continuationContext, width, height, previousResults)
 *     // schedule the futures for computation
 *     LayoutScheduler.prepare(futures)
 *   }
 *
 *   return MountableLayoutResult(...)
 * }
 *
 * class LayoutContinuationContext(...) : ContinuationContext
 * ```
 *
 * Because a continuation may be executed outside of the normal rendering flow i.e outside of root
 * layout or mount, it may need to access contextual values to execute its logic. This access should
 * be performed through a [ContinuationContext] during execution.
 *
 * It is incorrect for a continuation to capture scoped contextual variables with values that can be
 * inconsistent across execution (for instance, [LayoutContext]). Scoped value access should instead
 * happen through the receiving [ContinuationContext]
 */
fun interface Continuation<R, C : ContinuationContext> {
  fun C.invoke(): R
}

/**
 * Execution context for a [Continuation]. A continuation context acts a means of accessing
 * contextual values that may change over the execution of a [Continuation].
 */
interface ContinuationContext
