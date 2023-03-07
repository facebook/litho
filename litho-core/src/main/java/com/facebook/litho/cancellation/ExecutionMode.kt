// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho.cancellation

import com.facebook.litho.LayoutState

enum class ExecutionMode {
  SYNC,
  ASYNC
}

private val executionsModesBySource =
    mapOf(
        LayoutState.CalculateLayoutSource.SET_ROOT_SYNC to ExecutionMode.SYNC,
        LayoutState.CalculateLayoutSource.SET_ROOT_ASYNC to ExecutionMode.ASYNC,
        LayoutState.CalculateLayoutSource.UPDATE_STATE_SYNC to ExecutionMode.SYNC,
        LayoutState.CalculateLayoutSource.UPDATE_STATE_ASYNC to ExecutionMode.ASYNC,
        LayoutState.CalculateLayoutSource.SET_SIZE_SPEC_SYNC to ExecutionMode.SYNC,
        LayoutState.CalculateLayoutSource.SET_SIZE_SPEC_ASYNC to ExecutionMode.ASYNC,
        LayoutState.CalculateLayoutSource.MEASURE_SET_SIZE_SPEC to ExecutionMode.SYNC,
        LayoutState.CalculateLayoutSource.MEASURE_SET_SIZE_SPEC_ASYNC to ExecutionMode.ASYNC)

internal fun getExecutionMode(source: Int): ExecutionMode =
    executionsModesBySource[source] ?: error("Unexpected source for resolve: $source")
