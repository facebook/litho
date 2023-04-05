// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.litho.cancellation

import com.facebook.litho.RenderSource

enum class ExecutionMode {
  SYNC,
  ASYNC
}

private val executionsModesBySource =
    mapOf(
        RenderSource.SET_ROOT_SYNC to ExecutionMode.SYNC,
        RenderSource.SET_ROOT_ASYNC to ExecutionMode.ASYNC,
        RenderSource.UPDATE_STATE_SYNC to ExecutionMode.SYNC,
        RenderSource.UPDATE_STATE_ASYNC to ExecutionMode.ASYNC,
        RenderSource.SET_SIZE_SPEC_SYNC to ExecutionMode.SYNC,
        RenderSource.SET_SIZE_SPEC_ASYNC to ExecutionMode.ASYNC,
        RenderSource.MEASURE_SET_SIZE_SPEC to ExecutionMode.SYNC,
        RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC to ExecutionMode.ASYNC)

internal fun getExecutionMode(source: Int): ExecutionMode =
    executionsModesBySource[source] ?: error("Unexpected source for resolve: $source")
