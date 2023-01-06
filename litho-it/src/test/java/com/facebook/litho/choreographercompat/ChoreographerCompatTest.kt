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

package com.facebook.litho.choreographercompat

import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ChoreographerCompatTest {

  @Test
  fun testCreationFromMainThread() {
    ShadowLooper.pauseMainLooper()
    val firstCallback = AtomicBoolean(false)
    val choreographerCompat = ChoreographerCompatImpl()
    assertThat(choreographerCompat.isUsingChoreographer).isTrue
    ChoreographerCompatImpl()
        .postFrameCallback(
            object : ChoreographerCompat.FrameCallback() {
              override fun doFrame(frameTimeNanos: Long) {
                firstCallback.set(true)
              }
            })
    assertThat(firstCallback.get()).isFalse
    ShadowLooper.runUiThreadTasks()
    assertThat(firstCallback.get()).isTrue
  }

  @Test
  fun testCreationFromBGThread() {
    ShadowLooper.pauseMainLooper()
    val latch = CountDownLatch(1)
    val ref = AtomicReference<ChoreographerCompatImpl>()
    Thread {
          ref.set(ChoreographerCompatImpl())
          latch.countDown()
        }
        .start()
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue
    assertThat(ref.get().isUsingChoreographer).isFalse
    val firstCallback = AtomicBoolean(false)
    ref.get()
        .postFrameCallback(
            object : ChoreographerCompat.FrameCallback() {
              override fun doFrame(frameTimeNanos: Long) {
                firstCallback.set(true)
              }
            })
    assertThat(firstCallback.get()).isFalse
    ShadowLooper.runUiThreadTasks()
    assertThat(firstCallback.get()).isTrue
    ShadowLooper.pauseMainLooper()
    assertThat(ref.get().isUsingChoreographer).isTrue
    val secondCallback = AtomicBoolean(false)
    ref.get()
        .postFrameCallback(
            object : ChoreographerCompat.FrameCallback() {
              override fun doFrame(frameTimeNanos: Long) {
                secondCallback.set(true)
              }
            })
    assertThat(secondCallback.get()).isFalse
    ShadowLooper.runUiThreadTasks()
    assertThat(secondCallback.get()).isTrue
  }
}
