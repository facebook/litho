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
 * This policy is responsible for evaluating the context around the current executions of *Layout*s
 * and for the incoming request and act on any possible cancellation.
 *
 * This process is started by calling [evaluate]. This method will return a [Result] that should
 * indicate which action should be taken.
 */
interface LayoutCancellationPolicy : CancellationPolicy<LayoutMetadata> {

  override fun evaluate(
      ongoingRequests: List<LayoutMetadata>,
      incomingRequest: LayoutMetadata
  ): Result

  class Default : LayoutCancellationPolicy {

    override val cancellationMode: CancellationPolicy.CancellationExecutionMode =
        CancellationPolicy.CancellationExecutionMode.INTERRUPT

    /**
     * This method identifies which action to take whenever a new Layout request happens.
     *
     * For each of the on-going Layout's metadata, we will verify if the incoming Layout is an
     * equivalent one to decide on the course of action. If both requests are equivalent and the
     * ongoing request is running asynchronously there are two possible courses of action:
     * 1. If the incoming request is synchronous, we can cancel the on-going request.
     * 2. If the incoming request is asynchronous, we can drop it and not start it.
     *
     * When requests are not equivalent, then if the on-going request request runs asynchronously,
     * we can cancel it because they are redundant, and the new one will effectively replace it.
     */
    override fun evaluate(
        ongoingRequests: List<LayoutMetadata>,
        incomingRequest: LayoutMetadata
    ): Result {
      val cancellableRequests = mutableListOf<LayoutMetadata>()

      for (ongoingRequest in ongoingRequests) {
        if (ongoingRequest.isEquivalentTo(incomingRequest)) {
          if (incomingRequest.executionMode == ExecutionMode.ASYNC) {
            return Result.DropIncomingRequest
          } else if (ongoingRequest.executionMode == ExecutionMode.ASYNC) {
            cancellableRequests.add(ongoingRequest)
          }
        } else {
          if (ongoingRequest.executionMode == ExecutionMode.ASYNC) {
            cancellableRequests.add(ongoingRequest)
          }
        }
      }

      return if (cancellableRequests.isNotEmpty()) {
        Result.CancelRunningRequests(ongoingRequests.map(LayoutMetadata::localVersion))
      } else {
        Result.ProcessIncomingRequest
      }
    }
  }
}
