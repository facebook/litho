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
import com.facebook.litho.LayoutState.CalculateLayoutSource
import com.facebook.litho.PotentiallyPartialResult
import com.facebook.litho.TreeFuture
import com.facebook.litho.TreeFuture.TreeFutureResult
import com.facebook.litho.cancellation.CancellationPolicy.CancellationExecutionMode
import java.util.concurrent.CancellationException

/**
 * This is used to run either *Resolve* or *Layout* with a cancellation strategy.
 *
 * This allow us to not commit to big architectural changes and keep some of the most important
 * flows of the app free of clutter until we verify if this is a worthy approach to fllowl.
 */
internal fun <F, M, T> trackAndRunTreeFutureWithCancellation(
    treeFuture: F,
    futures: List<F>,
    @CalculateLayoutSource source: Int,
    mutex: Any,
    futureExecutionListener: TreeFuture.FutureExecutionListener?,
    cancellationPolicy: CancellationPolicy<M>,
): TreeFutureResult<T>? where
T : PotentiallyPartialResult,
F : TreeFuture<T>,
F : RequestMetadataSupplier<M> {

  val result: CancellationPolicy.Result =
      cancellationPolicy.evaluate(
          ongoingRequests = futures.map { it.getMetadata() },
          incomingRequest = treeFuture.getMetadata())

  when (result) {
    is CancellationPolicy.Result.ProcessIncomingRequest -> {
      debugLog { "Processing incoming request." }
    }
    is CancellationPolicy.Result.DropIncomingRequest -> {
      debugLog { "Dropping incoming request." }
      return TreeFutureResult.cancelled()
    }
    is CancellationPolicy.Result.CancelRunningRequests -> {
      debugLog { "Will attempt to cancel running requests: ${result.requestIds}" }
      result.requestIds.iterator().forEach { version ->
        val futureToCancel = futures.firstOrNull { it.version == version }
        if (futureToCancel != null) {
          when (cancellationPolicy.cancellationMode) {
            CancellationExecutionMode.SHORT_CIRCUIT -> futureToCancel.release()
            CancellationExecutionMode.INTERRUPT -> futureToCancel.forceCancellation()
          }
          debugLog { "Cancelled future (${futureToCancel.version}) with success." }
        }
      }
    }
  }

  return try {
    TreeFuture.trackAndRunTreeFuture(treeFuture, futures, source, mutex, futureExecutionListener)
  } catch (exception: CancellationException) {
    if (exception.cause is CancellationException) {
      debugLog { "A cancellation exception on future ${treeFuture.version}" }
    }
    TreeFutureResult.cancelled()
  }
}

private fun debugLog(msgBuilder: () -> String) {
  if (isDebugEnabled) {
    Log.d(TAG, msgBuilder())
  }
}

interface RequestMetadataSupplier<M> {

  fun getMetadata(): M
}

private const val isDebugEnabled = false
private const val TAG = "Cancellation"
