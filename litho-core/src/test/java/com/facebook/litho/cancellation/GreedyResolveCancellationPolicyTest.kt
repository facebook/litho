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

import com.facebook.litho.RenderSource
import com.facebook.litho.TreeProps
import com.facebook.litho.cancellation.CancellationPolicy.CancellationExecutionMode
import com.facebook.litho.cancellation.CancellationPolicy.Result
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GreedyResolveCancellationPolicyTest {

  private val cancellationEvaluator =
      ResolveCancellationPolicy.Greedy(cancellationMode = CancellationExecutionMode.INTERRUPT)

  // #################
  // ## NON-EQUIVALENT
  // ##################

  @Test
  fun `on sync resolve request with non-equivalent async request running - cancel running request`() {
    val runningResolves =
        listOf(
            resolveMetadata.copy(
                componentId = 1, localVersion = 0, executionMode = ExecutionMode.ASYNC))

    val incomingResolve =
        resolveMetadata.copy(componentId = 2, executionMode = ExecutionMode.SYNC, localVersion = 1)
    val result = cancellationEvaluator.evaluate(runningResolves, incomingResolve)

    assertThat(result)
        .isEqualTo(Result.CancelRunningRequests(runningResolves.map(ResolveMetadata::localVersion)))
  }

  @Test
  fun `on sync resolve request with non-equivalent sync request running - process incoming request`() {
    val runningResolves =
        listOf(
            resolveMetadata.copy(
                componentId = 1, localVersion = 0, executionMode = ExecutionMode.SYNC))

    val incomingResolve =
        resolveMetadata.copy(componentId = 2, executionMode = ExecutionMode.SYNC, localVersion = 1)

    val result = cancellationEvaluator.evaluate(runningResolves, incomingResolve)

    assertThat(result).isEqualTo(Result.ProcessIncomingRequest)
  }

  @Test
  fun `on async resolve request with non-equivalent async request running - cancel running request`() {
    val runningResolves =
        listOf(
            resolveMetadata.copy(
                componentId = 1, localVersion = 0, executionMode = ExecutionMode.ASYNC))

    val incomingResolve =
        resolveMetadata.copy(componentId = 2, executionMode = ExecutionMode.ASYNC, localVersion = 1)

    val result = cancellationEvaluator.evaluate(runningResolves, incomingResolve)

    assertThat(result)
        .isEqualTo(Result.CancelRunningRequests(runningResolves.map(ResolveMetadata::localVersion)))
  }

  @Test
  fun `on async resolve request with non-equivalent sync request running - process incoming request`() {
    val runningResolves =
        listOf(
            resolveMetadata.copy(
                componentId = 1, localVersion = 0, executionMode = ExecutionMode.SYNC))

    val incomingResolve =
        resolveMetadata.copy(componentId = 2, executionMode = ExecutionMode.ASYNC, localVersion = 1)

    val result = cancellationEvaluator.evaluate(runningResolves, incomingResolve)

    assertThat(result).isEqualTo(Result.ProcessIncomingRequest)
  }

  // #################
  // ## EQUIVALENT
  // ##################

  @Test
  fun `on async resolve request with equivalent sync request running - drop incoming request`() {
    val runningResolves =
        listOf(resolveMetadata.copy(localVersion = 0, executionMode = ExecutionMode.SYNC))

    val incomingResolve =
        resolveMetadata.copy(executionMode = ExecutionMode.ASYNC, localVersion = 1)

    val result = cancellationEvaluator.evaluate(runningResolves, incomingResolve)

    assertThat(result).isEqualTo(Result.DropIncomingRequest)
  }

  @Test
  fun `on async non-state-update resolve request with equivalent async request running - drop incoming request`() {
    val runningResolves =
        listOf(
            resolveMetadata.copy(
                localVersion = 0,
                executionMode = ExecutionMode.ASYNC,
                source = RenderSource.UPDATE_STATE_ASYNC))

    val incomingResolve =
        resolveMetadata.copy(
            executionMode = ExecutionMode.ASYNC,
            source = RenderSource.SET_ROOT_ASYNC,
            localVersion = 1)

    val result = cancellationEvaluator.evaluate(runningResolves, incomingResolve)

    assertThat(result).isEqualTo(Result.DropIncomingRequest)
  }

  @Test
  fun `on async state update resolve request with equivalent async state update request running - cancel running request`() {
    val runningResolves =
        listOf(
            resolveMetadata.copy(
                localVersion = 0,
                executionMode = ExecutionMode.ASYNC,
                source = RenderSource.UPDATE_STATE_ASYNC))

    val incomingResolve =
        resolveMetadata.copy(
            executionMode = ExecutionMode.ASYNC,
            source = RenderSource.UPDATE_STATE_ASYNC,
            localVersion = 1)

    val result = cancellationEvaluator.evaluate(runningResolves, incomingResolve)

    assertThat(result)
        .isEqualTo(Result.CancelRunningRequests(runningResolves.map(ResolveMetadata::localVersion)))
  }

  @Test
  fun `on sync resolve request with equivalent sync request running - process incoming request`() {
    val runningResolves =
        listOf(resolveMetadata.copy(localVersion = 0, executionMode = ExecutionMode.SYNC))

    val incomingResolve = resolveMetadata.copy(executionMode = ExecutionMode.SYNC, localVersion = 1)

    val result = cancellationEvaluator.evaluate(runningResolves, incomingResolve)

    assertThat(result).isEqualTo(Result.ProcessIncomingRequest)
  }

  @Test
  fun `on sync resolve request with equivalent async request running - cancel running request`() {
    val runningResolves =
        listOf(resolveMetadata.copy(localVersion = 0, executionMode = ExecutionMode.ASYNC))

    val incomingResolve = resolveMetadata.copy(executionMode = ExecutionMode.SYNC, localVersion = 1)

    val result = cancellationEvaluator.evaluate(runningResolves, incomingResolve)

    assertThat(result)
        .isEqualTo(Result.CancelRunningRequests(runningResolves.map(ResolveMetadata::localVersion)))
  }

  private val resolveMetadata =
      ResolveMetadata(
          componentId = 1,
          localVersion = 0,
          treeProps = TreeProps().apply { put(String::class.java, "a property") },
          executionMode = ExecutionMode.SYNC,
          source = RenderSource.SET_ROOT_SYNC)
}
