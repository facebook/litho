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

import android.graphics.Rect
import androidx.annotation.VisibleForTesting

/**
 * Stores information about a [Component] which is only available when tests are run. TestOutputs
 * are calculated in [LayoutState] and transformed into [TestItem]s while mounting.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class TestOutput {
  var testKey: String? = null
  var hostMarker: Long = -1
  var layoutOutputId: Long = -1
  private val _bounds = Rect()
  var bounds: Rect
    get() = _bounds
    set(bounds) {
      _bounds.set(bounds)
    }

  fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
    _bounds.set(left, top, right, bottom)
  }
}
