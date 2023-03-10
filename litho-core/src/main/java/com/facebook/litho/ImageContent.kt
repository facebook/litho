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

import android.graphics.drawable.Drawable
import kotlin.jvm.JvmField

/** A UI element that contains simple resource drawables. */
interface ImageContent {

  /**
   * @return the list of image drawables that are rendered by this UI element. The list returned
   *   should not be modified and may be unmodifiable.
   */
  val imageItems: List<Drawable>

  companion object {
    /** An empty instance of [ImageContent]. */
    @JvmField
    val EMPTY: ImageContent =
        object : ImageContent {
          override val imageItems: List<Drawable>
            get() = emptyList()
        }
  }
}
