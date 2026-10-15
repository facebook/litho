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

package com.facebook.litho.debug

import com.facebook.litho.Component
import com.facebook.litho.config.LithoDebugConfigurations

internal class VisibilityCallbackCommonPropsTracker(
    val component: Component,
    val owner: Component?,
) {
  private var onVisibleCount: Int = 0
  private var onInvisibleCount: Int = 0
  private var onFocusedCount: Int = 0
  private var onUnfocusedCount: Int = 0
  private var onFullImpressionCount: Int = 0
  private var onVisibilityChangedCount: Int = 0

  fun incrementOnVisibleCount() {
    onVisibleCount++
    log()
  }

  fun incrementOnInvisibleCount() {
    onInvisibleCount++
    log()
  }

  fun incrementOnFocusedCount() {
    onFocusedCount++
    log()
  }

  fun incrementOnUnfocusedCount() {
    onUnfocusedCount++
    log()
  }

  fun incrementOnFullImpressionCount() {
    onFullImpressionCount++
    log()
  }

  fun incrementOnVisibilityChangedCount() {
    onVisibilityChangedCount++
    log()
  }

  private fun log() {
    if (!LithoDebugConfigurations.isVisibilityCallbackCommonPropTrackerEnabled) {
      return
    }

    if (
        onVisibleCount <= 1 &&
            onInvisibleCount <= 1 &&
            onFocusedCount <= 1 &&
            onUnfocusedCount <= 1 &&
            onFullImpressionCount <= 1 &&
            onVisibilityChangedCount <= 1
    ) {
      return
    }

    DebugInfoReporter.report("MultipleVisibilityCallbacks") {
      this["component"] = component.simpleName
      this["owner"] = owner?.simpleName
      if (onVisibleCount > 1) this["onVisibleCount"] = onVisibleCount
      if (onInvisibleCount > 1) this["onInvisibleCount"] = onInvisibleCount
      if (onFocusedCount > 1) this["onFocusedCount"] = onFocusedCount
      if (onUnfocusedCount > 1) this["onUnfocusedCount"] = onUnfocusedCount
      if (onFullImpressionCount > 1) this["onFullImpressionCount"] = onFullImpressionCount
      if (onVisibilityChangedCount > 1) this["onVisibilityChangedCount"] = onVisibilityChangedCount
    }
  }
}
