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

package com.facebook.litho.animation

import com.facebook.rendercore.IntSize
import com.facebook.rendercore.ms
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CompositeIntSizeSpecTest {

  private fun seconds(s: Double): Long = (s * 1_000_000_000).toLong()

  @Test
  fun animatesEachAxisIndependently() {
    val animator =
        CompositeIntSizeSpec(spring(), spring())
            .createAnimator(IntSize(0, 0), IntSize(100, 200), IntSizeTypeConverter)
    val mid = animator.getValue(seconds(0.05))
    assertThat(mid.width).isGreaterThan(0).isLessThan(100)
    assertThat(mid.height).isGreaterThan(0).isLessThan(200)
  }

  @Test
  fun reachesTargetPerAxis() {
    val animator =
        CompositeIntSizeSpec(spring(), spring())
            .createAnimator(IntSize(0, 0), IntSize(100, 200), IntSizeTypeConverter)
    var settled = false
    for (i in 1..600) {
      val t = seconds(i * 0.01)
      if (animator.isFinished(t)) {
        assertThat(animator.getValue(t)).isEqualTo(IntSize(100, 200))
        settled = true
        break
      }
    }
    assertThat(settled).isTrue()
  }

  @Test
  fun supportsKeyframesPerAxis() {
    // The point of composing rather than reinterpreting: width is a scalar, so a per-axis keyframes
    // is valid — it would have thrown as a reinterpreted `keyframes<Float>` at IntSize.
    val width = keyframes<Float>(200.ms) { 40f at 100.ms }
    val animator =
        CompositeIntSizeSpec(width, spring())
            .createAnimator(IntSize(0, 0), IntSize(100, 200), IntSizeTypeConverter)
    val atStop = animator.getValue(seconds(0.1))
    assertThat(atStop.width).isEqualTo(40) // sits on its declared waypoint
    assertThat(atStop.height).isGreaterThan(0).isLessThan(200)
  }

  @Test
  fun carriesPerAxisVelocity() {
    val animator =
        CompositeIntSizeSpec(spring(), spring())
            .createAnimator(IntSize(0, 0), IntSize(100, 300), IntSizeTypeConverter)
    val velocity = IntSizeTypeConverter.newVector()
    animator.getVelocity(seconds(0.05), velocity)
    // Each axis reports its own velocity; the further-traveling axis is faster.
    assertThat(velocity[0]).isGreaterThan(0f)
    assertThat(velocity[1]).isGreaterThan(0f)
    assertThat(velocity[0]).isNotEqualTo(velocity[1])
  }
}
