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

package com.facebook.litho.dataflow

import com.facebook.litho.choreographercompat.ChoreographerCompat
import com.facebook.litho.choreographercompat.ChoreographerCompat.FrameCallback
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.ArrayList
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class MockTimingSourceTest {

  private lateinit var timingSource: MockTimingSource

  @Before
  fun setUp() {
    timingSource = MockTimingSource()
    DataFlowGraph.create(timingSource)
    timingSource.start()
  }

  @Test
  fun testPostFrameCallback() {
    val callbacks = ArrayList<ChoreographerCompat.FrameCallback>()
    for (i in 0..2) {
      val callback = mock<ChoreographerCompat.FrameCallback>()
      callbacks.add(callback)
      timingSource.postFrameCallback(callback)
    }
    timingSource.step(1)
    for (i in callbacks.indices) {
      verify(callbacks[i]).doFrame(ArgumentMatchers.anyLong())
    }
  }

  @Test
  fun testPostFrameCallbackDelayed() {
    val callback1 = mock<ChoreographerCompat.FrameCallback>()
    val callback2 = mock<ChoreographerCompat.FrameCallback>()
    val delayedCallback = mock<ChoreographerCompat.FrameCallback>()
    timingSource.postFrameCallback(callback1)
    timingSource.postFrameCallbackDelayed(delayedCallback, 20)
    timingSource.postFrameCallback(callback2)
    timingSource.step(1)
    verify(callback1).doFrame(ArgumentMatchers.anyLong())
    verify(callback2).doFrame(ArgumentMatchers.anyLong())
    verify(delayedCallback, never()).doFrame(ArgumentMatchers.anyLong())
    timingSource.step(1)
    verify(delayedCallback).doFrame(ArgumentMatchers.anyLong())
  }

  @Test
  fun testNestedFrameCallbacks() {
    val callback: ChoreographerCompat.FrameCallback =
        object : ChoreographerCompat.FrameCallback() {
          override fun doFrame(frameTimeNanos: Long) {
            timingSource.postFrameCallback(
                object : ChoreographerCompat.FrameCallback() {
                  override fun doFrame(frameTimeNanos: Long) {
                    fail("Nested FrameCallback should not be called in the same step")
                  }
                })
          }
        }
    timingSource.postFrameCallback(callback)
    timingSource.step(1)
  }
}
