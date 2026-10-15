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

import com.facebook.rendercore.ms
import kotlin.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimationControllerTest {

  @Test
  fun withFrameDriverForTest_ticksAnimationsDeterministically() {
    val frameDriver = TestAnimationFrameDriver()
    val frames = mutableListOf<Long>()

    AnimationController.withFrameDriverForTest(frameDriver) {
      AnimationController.register(
          FrameDrivenAnimation { frameTimeNanos ->
            frames.add(frameTimeNanos)
            frames.size < 2
          },
      )

      assertThat(AnimationController.hasRunningAnimations).isTrue()
      assertThat(frameDriver.hasScheduledFrameCallbacks()).isTrue()

      frameDriver.runNextFrame(100.ms)
      assertThat(frames).containsExactly(100.ms.inWholeNanoseconds)
      assertThat(AnimationController.hasRunningAnimations).isTrue()
      assertThat(frameDriver.hasScheduledFrameCallbacks()).isTrue()

      frameDriver.runNextFrame(150.ms)
      assertThat(frames).containsExactly(100.ms.inWholeNanoseconds, 250.ms.inWholeNanoseconds)
      assertThat(AnimationController.hasRunningAnimations).isFalse()
      assertThat(frameDriver.hasScheduledFrameCallbacks()).isFalse()
    }
  }

  @Test
  fun withFrameDriverForTest_unregisterCancelsScheduledFrame() {
    val frameDriver = TestAnimationFrameDriver()
    val animation = FrameDrivenAnimation { true }

    AnimationController.withFrameDriverForTest(frameDriver) {
      AnimationController.register(animation)

      assertThat(AnimationController.hasRunningAnimations).isTrue()
      assertThat(frameDriver.hasScheduledFrameCallbacks()).isTrue()

      AnimationController.unregister(animation)

      assertThat(AnimationController.hasRunningAnimations).isFalse()
      assertThat(frameDriver.hasScheduledFrameCallbacks()).isFalse()
    }
  }

  private class TestAnimationFrameDriver : AnimationFrameDriver {

    private val scheduledCallbacks = linkedSetOf<AnimationFrameCallback>()
    private var currentFrameTimeNanos: Long = 0L

    override fun postFrameCallback(callback: AnimationFrameCallback) {
      scheduledCallbacks.add(callback)
    }

    override fun removeFrameCallback(callback: AnimationFrameCallback) {
      scheduledCallbacks.remove(callback)
    }

    fun hasScheduledFrameCallbacks(): Boolean = scheduledCallbacks.isNotEmpty()

    fun runNextFrame(frameDelay: Duration) {
      currentFrameTimeNanos += frameDelay.inWholeNanoseconds
      val callbacks = scheduledCallbacks.toList()
      scheduledCallbacks.clear()
      callbacks.forEach { it.doFrame(currentFrameTimeNanos) }
    }
  }
}
