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

@file:JvmName("CancellationHelper")

package com.facebook.litho.cancellation

import android.util.Log
import com.facebook.litho.PotentiallyPartialResult
import com.facebook.litho.RenderSource
import com.facebook.litho.TreeFuture
import com.facebook.litho.TreeFuture.TreeFutureResult
import com.facebook.litho.cancellation.CancellationPolicy.CancellationExecutionMode
import com.facebook.litho.stats.LithoStats
import java.util.LinkedList

/**
 * This is used to run either *Resolve* or *Layout* with a cancellation strategy.
 *
 * This allow us to not commit to big architectural changes and keep some of the most important
 * flows of the app free of clutter until we verify if this is a worthy approach to fllowl.
 */
internal fun <F, M, T> trackAndRunTreeFutureWithCancellation(
    treeFuture: F,
    futures: MutableList<F>,
    @RenderSource source: Int,
    mutex: Any,
    futureExecutionListener: TreeFuture.FutureExecutionListener?,
    cancellationPolicy: CancellationPolicy<M>,
): TreeFutureResult<T>? where
T : PotentiallyPartialResult,
F : TreeFuture<T>,
F : RequestMetadataSupplier<M> {
  val attribution = treeFuture.description

  val pendingFuturesCopy = synchronized(mutex) { LinkedList(futures) }

  val result: CancellationPolicy.Result =
      cancellationPolicy.evaluate(
          ongoingRequests = pendingFuturesCopy.map { it.getMetadata() },
          incomingRequest = treeFuture.getMetadata())

  when (result) {
    is CancellationPolicy.Result.ProcessIncomingRequest -> {
      debugLog { "Processing incoming request." }
    }
    is CancellationPolicy.Result.DropIncomingRequest -> {
      debugLog { "Dropping incoming request. (${attribution})" }
      incrementCancellationStats(treeFuture.description)
      return TreeFutureResult.cancelled()
    }
    is CancellationPolicy.Result.CancelRunningRequests -> {
      debugLog { "Will attempt to cancel running requests: ${result.requestIds} (${attribution})" }

      val futuresToRemove =
          cancelFutures<F, M, T>(
              result.requestIds,
              pendingFuturesCopy,
              cancellationPolicy.cancellationMode,
              attribution)

      synchronized(mutex) { futures.removeAll(futuresToRemove) }
    }
  }

  return try {
    TreeFuture.trackAndRunTreeFuture(treeFuture, futures, source, mutex, futureExecutionListener)
  } catch (e: FutureCancelledByPolicyException) {
    debugLog { "Future was cancelled by CancellationPolicy" }
    TreeFutureResult.cancelled()
  }
}

/**
 * This method takes a list of versions of the [TreeFuture] that should be cancelled and returns a
 * [Set] with all the [TreeFuture] cancelled.
 *
 * The [CancellationExecutionMode] decides if a short-circuit or interrupt approach should be taken.
 */
private fun <F, M, T> cancelFutures(
    futuresToCancelVersions: List<Int>,
    pendingFutures: List<F>,
    cancellationMode: CancellationExecutionMode,
    attribution: String
): Set<TreeFuture<*>> where
F : TreeFuture<T>,
F : RequestMetadataSupplier<M>,
T : PotentiallyPartialResult {
  val cancelledFutures = mutableSetOf<TreeFuture<*>>()

  futuresToCancelVersions.forEach { version ->
    val futureToCancel = pendingFutures.firstOrNull { it.version == version }
    if (futureToCancel != null) {
      incrementCancellationStats(attribution)

      futureToCancel.cancel(cancellationMode == CancellationExecutionMode.INTERRUPT)
      cancelledFutures.add(futureToCancel)

      debugLog { "Cancelled future (${futureToCancel.version}) with success." }
    }
  }

  return cancelledFutures
}

private fun incrementCancellationStats(attribution: String) {
  when (attribution) {
    "layout" -> LithoStats.incrementCancelledLayout()
    "resolve" -> LithoStats.incrementCancelledResolve()
  }

  debugLog {
    "total cancelled resolves: ${LithoStats.getResolveCancelledCount()}; total cancelled layouts: ${LithoStats.getLayoutCancelledCount()}"
  }
}

private inline fun debugLog(msgBuilder: () -> String) {
  if (isDebugEnabled) {
    Log.d(TAG, msgBuilder())
  }
}

interface RequestMetadataSupplier<M> {

  fun getMetadata(): M
}

private const val isDebugEnabled = false
private const val TAG = "Cancellation"
