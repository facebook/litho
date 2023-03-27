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
import android.widget.Checkable
import androidx.annotation.VisibleForTesting
import com.facebook.proguard.annotations.DoNotStrip
import java.lang.UnsupportedOperationException

/** Holds information about a [TestOutput]. */
@DoNotStrip
class TestItem {

  private val _bounds = Rect()

  @get:VisibleForTesting @get:DoNotStrip var testKey: String? = null
  @get:VisibleForTesting @get:DoNotStrip var host: ComponentHost? = null
  var content: Any? = null

  /** Unique key to identify if this test-item was reused */
  @get:DoNotStrip val acquireKey: AcquireKey = AcquireKey()

  @get:VisibleForTesting
  @get:DoNotStrip
  var bounds: Rect
    get() = _bounds
    set(bounds) {
      _bounds.set(bounds)
    }

  fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
    _bounds[left, top, right] = bottom
  }

  @get:VisibleForTesting
  @get:DoNotStrip
  val textContent: String
    get() {
      return textItems.joinToString()
    }

  val textItems: List<CharSequence>
    get() = ComponentHostUtils.extractTextContent(listOf(content)).flatMap { it.textList }

  val isChecked: Boolean
    get() =
        (content as? Checkable)?.isChecked
            ?: throw UnsupportedOperationException(
                "This Litho component can't be checked, we can't determine its check state.")

  @DoNotStrip class AcquireKey
}
