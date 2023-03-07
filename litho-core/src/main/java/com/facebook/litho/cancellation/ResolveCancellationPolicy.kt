// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

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

import com.facebook.litho.cancellation.CancellationPolicy.Result

/**
 * This policy is responsible for evaluating the context around the current executions of *Resolve*s
 * and for the incoming request and act on any possible cancellation.
 *
 * This process is started by calling [onResolveRequest]. This method will return a [Result] that
 * should indicate which action should be taken.
 */
interface ResolveCancellationPolicy : CancellationPolicy<ResolveMetadata> {

  override fun evaluate(
      ongoingRequests: List<ResolveMetadata>,
      incomingRequest: ResolveMetadata
  ): Result

  class Default(override val cancellationMode: CancellationPolicy.CancellationExecutionMode) :
      ResolveCancellationPolicy {

    /**
     * This method identifies which action to take whenever a new Resolve request happens.
     *
     * For each of the on-going Resolve's metadata, we will verify if the incoming Resolve is an
     * equivalent one to decide on the course of action. If both requests are equivalent, but the
     * incoming one is asynchronous, we can drop it. The reasoning is that a synchronous or
     * asynchronous one is already running, so there is no need to duplicate that work. If the
     * incoming one is synchronous, we prefer to attempt to process the request because it will
     * likely transfer work from the equivalent one.
     *
     * When requests are not equivalent, then if the running request runs asynchronously, we can
     * cancel it because they are redundant, and the new one will effectively replace it.
     */
    override fun evaluate(
        ongoingRequests: List<ResolveMetadata>,
        incomingRequest: ResolveMetadata
    ): Result {
      val cancellableResolves = mutableListOf<ResolveMetadata>()

      for (runningResolve in ongoingRequests) {
        if (runningResolve.isEquivalentTo(incomingRequest)) {
          if (incomingRequest.executionMode == ExecutionMode.ASYNC) {
            return Result.DropIncomingRequest
          }
        } else {
          if (runningResolve.executionMode == ExecutionMode.ASYNC) {
            cancellableResolves.add(runningResolve)
          }
        }
      }

      return if (cancellableResolves.isNotEmpty()) {
        Result.CancelRunningRequests(ongoingRequests.map { it.id })
      } else {
        Result.ProcessIncomingRequest
      }
    }
  }
}
