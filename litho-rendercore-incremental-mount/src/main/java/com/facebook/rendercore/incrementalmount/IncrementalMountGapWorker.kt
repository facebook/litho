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

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Display
import com.facebook.rendercore.MountDelegate
import com.facebook.rendercore.Systracer
import com.facebook.rendercore.incrementalmount.IncrementalMountExtensionConfigs.gapWorkerDeadlineBufferMs
import java.util.concurrent.TimeUnit

class IncrementalMountGapWorker
private constructor(
    private val frameIntervalNs: Long,
    private val tracer: Systracer,
) : Choreographer.FrameCallback, Runnable {

  private val handler: Handler = Handler(Looper.getMainLooper())
  private val delegates: MutableSet<MountDelegate> by lazy { mutableSetOf() }

  private var latestFrameTimeNs: Long = 0
  private var isRunning: Boolean = false

  /**
   * On every frame update the frame time. If there are items to mount then it also posts a
   * [Runnable] on the UI Thread to premount content during any gaps.
   */
  override fun doFrame(frameTimeNanos: Long) {
    latestFrameTimeNs = frameTimeNanos // update the frame time

    if (delegates.isNotEmpty()) {
      val isTracing = tracer.isTracing
      if (isTracing) {
        tracer.beginSection("IncrementalMountGapWorker::doFrame")
      }
      handler.post(this)
      Choreographer.getInstance().postFrameCallback(this)
      if (isTracing) {
        tracer.endSection()
      }
    } else {
      isRunning = false
    }
  }

  /** The deadline is [deadlineBufferNs] nanoseconds before the next frame. */
  override fun run() {
    premount(latestFrameTimeNs + frameIntervalNs - deadlineBufferNs)
  }

  private fun premount(deadlineNs: Long) {
    val isTracing = tracer.isTracing
    if (isTracing) {
      tracer.beginSection("premount")
    }
    try {
      val iterator = delegates.iterator()
      while (iterator.hasNext() && !isTimeUp(deadlineNs)) {
        if (!premountNext(iterator.next(), deadlineNs)) {
          iterator.remove()
        }
      }
    } finally {
      if (isTracing) {
        tracer.endSection()
      }
    }
  }

  /** returns `true` iff there are still items left to mount */
  private fun premountNext(
      delegate: MountDelegate,
      deadlineNs: Long,
  ): Boolean {
    val isTracing = tracer.isTracing
    if (isTracing) {
      tracer.beginSection("premount-item")
    }
    try {
      // While there are items to mount and there is time left to mount
      while (delegate.hasItemToMount() && !isTimeUp(deadlineNs)) {
        delegate.premountNext()
      }
    } finally {
      if (isTracing) {
        tracer.endSection()
      }
    }

    return delegate.hasItemToMount()
  }

  private fun startIfNotRunning(updatedFrameTimeMs: Long?) {
    if (updatedFrameTimeMs != null) {
      val updatedFrameTimeNs = TimeUnit.MILLISECONDS.toNanos(updatedFrameTimeMs)
      if (updatedFrameTimeNs > latestFrameTimeNs) {
        latestFrameTimeNs = updatedFrameTimeNs
      }
    }
    if (isRunning) {
      return
    }
    if (delegates.isNotEmpty()) {
      isRunning = true
      handler.post(this) // post to premount in this frame
      Choreographer.getInstance().postFrameCallback(this)
    }
  }

  /**
   * Registers a [MountDelegate] with the [IncrementalMountGapWorker]. This will also start the
   * [IncrementalMountGapWorker] if it isn't already running.
   */
  fun add(root: MountDelegate, frameTimeMs: Long?) {
    delegates.add(root)
    startIfNotRunning(frameTimeMs)
  }

  /**
   * Removes a [MountDelegate] registered with the [IncrementalMountGapWorker]. This will also
   * disable the [IncrementalMountGapWorker] if there are no more [MountDelegate]s.
   */
  fun remove(root: MountDelegate) {
    delegates.remove(root)
  }

  companion object {

    private var worker: IncrementalMountGapWorker? = null

    @JvmStatic
    fun get(display: Display?, tracer: Systracer): IncrementalMountGapWorker {
      val frameInterval = getFrameIntervalInNs(display)
      return IncrementalMountGapWorker(frameInterval, tracer).apply { worker = this }
    }

    @JvmStatic
    private fun getFrameIntervalInNs(display: Display?): Long {

      // This logic was been copied from the RecyclerView
      // break 60 fps assumption if data from display appears valid
      // NOTE: Do only once because it's very expensive (> 1ms)
      var refreshRate = 60.0f
      if (display != null) {
        val displayRefreshRate = display.refreshRate
        if (displayRefreshRate >= 30.0f) {
          refreshRate = displayRefreshRate
        }
      }

      return (1000000000 / refreshRate).toLong()
    }

    fun isTimeUp(deadlineNs: Long): Boolean = System.nanoTime() >= deadlineNs

    private val deadlineBufferNs: Long = TimeUnit.MILLISECONDS.toNanos(gapWorkerDeadlineBufferMs)
  }
}
