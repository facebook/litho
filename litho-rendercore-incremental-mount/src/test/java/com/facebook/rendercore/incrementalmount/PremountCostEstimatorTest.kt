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

package com.facebook.rendercore.incrementalmount

import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PremountCostEstimatorTest {

  @Test
  fun `returns no estimate before the first sample`() {
    val estimator = EwmaPremountCostEstimator()

    assertThat(estimator.estimateNs(CONTENT_TYPE)).isNull()
  }

  @Test
  fun `returns an estimate after the first sample`() {
    val estimator = EwmaPremountCostEstimator()

    estimator.record(CONTENT_TYPE, 17L)

    assertThat(estimator.estimateNs(CONTENT_TYPE)).isEqualTo(17L)
  }

  @Test
  fun `weights history three to one against a new sample`() {
    val estimator = EwmaPremountCostEstimator()
    estimator.record(CONTENT_TYPE, 100L)
    estimator.record(CONTENT_TYPE, 200L)

    // 100 * 3/4 + 200 * 1/4
    assertThat(estimator.estimateNs(CONTENT_TYPE)).isEqualTo(125L)
  }

  @Test
  fun `decays an over-estimate as cheaper samples arrive`() {
    val estimator = EwmaPremountCostEstimator()
    estimator.record(CONTENT_TYPE, 1_000L)

    // The whole point of the decay: one slow mount does not strand a content type above the
    // budget forever, which it would if nothing ever mounted it again to correct the estimate.
    estimator.record(CONTENT_TYPE, 0L)
    assertThat(estimator.estimateNs(CONTENT_TYPE)).isEqualTo(750L)

    estimator.record(CONTENT_TYPE, 0L)
    assertThat(estimator.estimateNs(CONTENT_TYPE)).isEqualTo(561L)
  }

  @Test
  fun `one slow sample moves the estimate by a quarter of its excess`() {
    val estimator = EwmaPremountCostEstimator()
    estimator.record(CONTENT_TYPE, 100L)

    estimator.record(CONTENT_TYPE, 500L)

    // Not 500: an outlier is damped rather than adopted wholesale.
    assertThat(estimator.estimateNs(CONTENT_TYPE)).isEqualTo(200L)
  }

  @Test
  fun `rejects background-thread access`() {
    val estimator = EwmaPremountCostEstimator()
    val failure = AtomicReference<Throwable?>()
    val thread = Thread {
      try {
        estimator.record(CONTENT_TYPE, 1L)
      } catch (throwable: Throwable) {
        failure.set(throwable)
      }
    }

    thread.start()
    thread.join()

    assertThat(failure.get())
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining("main thread")
  }

  @Test
  fun `evicts the least recently accessed content type once the cap is exceeded`() {
    val cap = EwmaPremountCostEstimator.MAX_CONTENT_TYPES
    val estimator = EwmaPremountCostEstimator()
    for (contentType in 0 until cap) {
      estimator.record(contentType, contentType.toLong())
    }

    // Reading 0 makes it the most recently used, so 1 becomes the eviction candidate.
    assertThat(estimator.estimateNs(0)).isZero()
    estimator.record(cap, cap.toLong())

    assertThat(estimator.estimateNs(0)).isZero()
    assertThat(estimator.estimateNs(1)).isNull()
    assertThat(estimator.estimateNs(cap)).isEqualTo(cap.toLong())
  }

  private companion object {
    private val CONTENT_TYPE = Any()
  }
}
