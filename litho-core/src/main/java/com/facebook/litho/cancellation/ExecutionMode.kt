// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho.cancellation

import com.facebook.litho.CalculateLayoutSource

enum class ExecutionMode {
  SYNC,
  ASYNC
}

private val executionsModesBySource =
    mapOf(
        CalculateLayoutSource.SET_ROOT_SYNC to ExecutionMode.SYNC,
        CalculateLayoutSource.SET_ROOT_ASYNC to ExecutionMode.ASYNC,
        CalculateLayoutSource.UPDATE_STATE_SYNC to ExecutionMode.SYNC,
        CalculateLayoutSource.UPDATE_STATE_ASYNC to ExecutionMode.ASYNC,
        CalculateLayoutSource.SET_SIZE_SPEC_SYNC to ExecutionMode.SYNC,
        CalculateLayoutSource.SET_SIZE_SPEC_ASYNC to ExecutionMode.ASYNC,
        CalculateLayoutSource.MEASURE_SET_SIZE_SPEC to ExecutionMode.SYNC,
        CalculateLayoutSource.MEASURE_SET_SIZE_SPEC_ASYNC to ExecutionMode.ASYNC)

internal fun getExecutionMode(source: Int): ExecutionMode =
    executionsModesBySource[source] ?: error("Unexpected source for resolve: $source")
