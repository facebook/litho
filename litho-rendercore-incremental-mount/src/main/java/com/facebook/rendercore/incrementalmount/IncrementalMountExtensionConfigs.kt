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

/**
 * This class contains mainly flags for features that are either used for development, or are not
 * ready for public consumption, or for use in experiments.
 *
 * The current values are safe defaults and should not require manual changes.
 */
object IncrementalMountExtensionConfigs {
  const val DEBUG_TAG: String = "IncrementalMount"

  /** Set this to true to enable debug logs for the incremental mount extension. */
  @JvmField var isDebugLoggingEnabled: Boolean = false

  var gapWorkerDeadlineBufferMs: Long = 4L

  /**
   * When positive, the gap worker holds back this fraction of the frame interval instead of the
   * absolute [gapWorkerDeadlineBufferMs]. `0.24` reproduces 4ms at 60Hz. A fraction at or above
   * `1.0` holds back the whole frame, which stops premounting.
   */
  var gapWorkerMarginFraction: Double = 0.0
}
