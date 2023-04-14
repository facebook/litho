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

package com.facebook.litho.cancellation

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.TreeFuture

/**
 * This policy is responsible for evaluating the context around the current executions of of a given
 * work and for the incoming request.
 *
 * This is meant to be used in the scope of either the *Resolve* or *Layout* phases of our
 * pipelines.
 *
 * This process is started by calling [evaluate]. This method will return a [Result] that should
 * indicate which action should be taken.
 */
interface CancellationPolicy<M> {

  val cancellationMode: CancellationExecutionMode

  fun evaluate(ongoingRequests: List<M>, incomingRequest: M): Result

  /**
   * This represents the result of the evaluation done by the *Cancellation Policty*. This can be
   * one of three possible outcomes:
   * - Process the incoming request
   * - Drop Incoming Request
   * - Cancel Running Requests
   */
  sealed class Result {

    object ProcessIncomingRequest : Result()

    object DropIncomingRequest : Result()

    @DataClassGenerate data class CancelRunningRequests(val requestIds: List<Int>) : Result()
  }

  /**
   * In the scope of cancelling Resolve's we can perform it with one two approaches.
   *
   * The [INTERRUPT] mode, will make use of [TreeFuture] cancellation APIs, which can interrupt the
   * Thread execution.
   *
   * If [SHORT_CIRCUIT] is used, then it will signal the [TreeFuture] that it is released and this
   * will stop the resolve process as soon as one of the points we check if we should interrupt the
   * resolve process. This happens for example, when [Row] or [Column] are being resolved.
   *
   * Please note, that the [SHORT_CIRCUIT] will only work for [Resolve], and it will not have effect
   * for [Layout] as it has no short-circuit mechanism at this momenet.
   */
  enum class CancellationExecutionMode {
    INTERRUPT,
    SHORT_CIRCUIT
  }
}
