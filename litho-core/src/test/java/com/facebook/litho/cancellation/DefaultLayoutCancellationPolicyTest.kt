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

import com.facebook.litho.EmptyComponent
import com.facebook.litho.MeasuredResultCache
import com.facebook.litho.ResolveResult
import com.facebook.litho.cancellation.CancellationPolicy.Result
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class DefaultLayoutCancellationPolicyTest {

  private val cancellationEvaluator = LayoutCancellationPolicy.Default()

  /*
   * ######################
   * Non Equivalent Layouts
   * ######################
   */

  @Test
  fun `on sync layout request with non-equivalent async request running - cancel running request`() {
    val runningLayouts =
        listOf(
            layoutMetadata.copy(
                localVersion = 0, widthSpec = 100, executionMode = ExecutionMode.ASYNC))

    val incomingLayouts =
        layoutMetadata.copy(localVersion = 1, widthSpec = 200, executionMode = ExecutionMode.SYNC)

    val result = cancellationEvaluator.evaluate(runningLayouts, incomingLayouts)

    assertThat(result)
        .isEqualTo(Result.CancelRunningRequests(runningLayouts.map(LayoutMetadata::localVersion)))
  }

  @Test
  fun `on async layout request with non-equivalent async request running - cancel running request`() {
    val runningLayouts =
        listOf(
            layoutMetadata.copy(
                localVersion = 0, widthSpec = 100, executionMode = ExecutionMode.ASYNC))

    val incomingLayouts =
        layoutMetadata.copy(localVersion = 1, widthSpec = 200, executionMode = ExecutionMode.ASYNC)

    val result = cancellationEvaluator.evaluate(runningLayouts, incomingLayouts)

    assertThat(result)
        .isEqualTo(Result.CancelRunningRequests(runningLayouts.map(LayoutMetadata::localVersion)))
  }

  @Test
  fun `on sync layout request with non-equivalent sync request running - process incoming request`() {
    val runningLayouts =
        listOf(
            layoutMetadata.copy(
                localVersion = 0, widthSpec = 100, executionMode = ExecutionMode.SYNC))

    val incomingLayouts =
        layoutMetadata.copy(localVersion = 1, widthSpec = 200, executionMode = ExecutionMode.SYNC)

    val result = cancellationEvaluator.evaluate(runningLayouts, incomingLayouts)

    assertThat(result).isEqualTo(Result.ProcessIncomingRequest)
  }

  /*
   * #####################
   * Equivalent Layouts
   * #####################
   */

  @Test
  fun `on async layout request with equivalent sync request running - drop incoming request`() {
    val runningLayouts =
        listOf(layoutMetadata.copy(localVersion = 0, executionMode = ExecutionMode.SYNC))

    val incomingLayouts = layoutMetadata.copy(localVersion = 1, executionMode = ExecutionMode.ASYNC)

    val result = cancellationEvaluator.evaluate(runningLayouts, incomingLayouts)

    assertThat(result).isEqualTo(Result.DropIncomingRequest)
  }

  @Test
  fun `on async layout request with equivalent async request running - drop incoming request`() {
    val runningLayouts =
        listOf(layoutMetadata.copy(localVersion = 0, executionMode = ExecutionMode.ASYNC))

    val incomingLayouts = layoutMetadata.copy(localVersion = 1, executionMode = ExecutionMode.ASYNC)

    val result = cancellationEvaluator.evaluate(runningLayouts, incomingLayouts)

    assertThat(result).isEqualTo(Result.DropIncomingRequest)
  }

  @Test
  fun `on sync layout request with equivalent sync request running - process incoming request`() {
    val runningLayouts =
        listOf(layoutMetadata.copy(localVersion = 0, executionMode = ExecutionMode.SYNC))

    val incomingLayouts = layoutMetadata.copy(localVersion = 1, executionMode = ExecutionMode.SYNC)

    val result = cancellationEvaluator.evaluate(runningLayouts, incomingLayouts)

    assertThat(result).isEqualTo(Result.ProcessIncomingRequest)
  }

  @Test
  fun `on sync layout request with equivalent async request running - cancel ongoing request`() {
    val runningLayouts =
        listOf(layoutMetadata.copy(localVersion = 0, executionMode = ExecutionMode.ASYNC))

    val incomingLayouts = layoutMetadata.copy(localVersion = 1, executionMode = ExecutionMode.SYNC)

    val result = cancellationEvaluator.evaluate(runningLayouts, incomingLayouts)

    assertThat(result)
        .isEqualTo(Result.CancelRunningRequests(runningLayouts.map(LayoutMetadata::localVersion)))
  }

  private val resolveResult: ResolveResult =
      ResolveResult(
          node = null,
          context = mock(),
          component = EmptyComponent(),
          cache = MeasuredResultCache(),
          treeState = mock(),
          isPartialResult = false,
          version = 0,
          createdEventHandlers = null,
          attachables = null,
          contextForResuming = null)

  private val layoutMetadata =
      LayoutMetadata(
          localVersion = 0,
          widthSpec = 100,
          heightSpec = 200,
          executionMode = ExecutionMode.SYNC,
          resolveResult = resolveResult)
}
