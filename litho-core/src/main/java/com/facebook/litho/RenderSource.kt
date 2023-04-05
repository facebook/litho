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

package com.facebook.litho

import androidx.annotation.IntDef

@IntDef(
    RenderSource.TEST,
    RenderSource.NONE,
    RenderSource.SET_ROOT_SYNC,
    RenderSource.SET_ROOT_ASYNC,
    RenderSource.SET_SIZE_SPEC_SYNC,
    RenderSource.SET_SIZE_SPEC_ASYNC,
    RenderSource.UPDATE_STATE_SYNC,
    RenderSource.UPDATE_STATE_ASYNC,
    RenderSource.MEASURE_SET_SIZE_SPEC,
    RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC,
    RenderSource.RELOAD_PREVIOUS_STATE)
annotation class RenderSource {
  companion object {
    const val TEST = -2
    const val NONE = -1
    const val SET_ROOT_SYNC = 0
    const val SET_ROOT_ASYNC = 1
    const val SET_SIZE_SPEC_SYNC = 2
    const val SET_SIZE_SPEC_ASYNC = 3
    const val UPDATE_STATE_SYNC = 4
    const val UPDATE_STATE_ASYNC = 5
    const val MEASURE_SET_SIZE_SPEC = 6
    const val MEASURE_SET_SIZE_SPEC_ASYNC = 7
    const val RELOAD_PREVIOUS_STATE = 8
  }
}

object RenderSourceUtils {
  @JvmStatic
  fun getExecutionMode(@RenderSource source: Int): String {
    return when (source) {
      RenderSource.SET_ROOT_SYNC,
      RenderSource.UPDATE_STATE_SYNC,
      RenderSource.SET_SIZE_SPEC_SYNC,
      RenderSource.MEASURE_SET_SIZE_SPEC -> "sync"
      RenderSource.SET_ROOT_ASYNC,
      RenderSource.UPDATE_STATE_ASYNC,
      RenderSource.SET_SIZE_SPEC_ASYNC,
      RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC -> "async"
      RenderSource.TEST -> "test"
      RenderSource.NONE -> "none"
      else -> "unknown"
    }
  }

  @JvmStatic
  fun getSource(@RenderSource source: Int): String {
    return when (source) {
      RenderSource.SET_ROOT_SYNC,
      RenderSource.SET_ROOT_ASYNC -> "set-root"
      RenderSource.UPDATE_STATE_SYNC,
      RenderSource.UPDATE_STATE_ASYNC -> "update-state"
      RenderSource.SET_SIZE_SPEC_SYNC,
      RenderSource.SET_SIZE_SPEC_ASYNC -> "set-size"
      RenderSource.MEASURE_SET_SIZE_SPEC,
      RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC -> "measure"
      RenderSource.TEST -> "test"
      RenderSource.NONE -> "none"
      else -> "unknown"
    }
  }
}
