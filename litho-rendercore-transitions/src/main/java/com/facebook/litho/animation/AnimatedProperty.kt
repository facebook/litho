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

package com.facebook.litho.animation

import com.facebook.litho.AnimatableItem

/** A property on a mount content (View or Drawable), e.g. x, y, scale, text color, etc. */
interface AnimatedProperty {

  /**
   * @return the name of this property. It should be unique across properties that a mount content
   *   can have. It's used for identification purposes only. E.g., "x", "y", etc.
   */
  fun getName(): String

  /** @return the current value of this property on the given mount content (View or Drawable). */
  operator fun get(mountContent: Any): Float

  /** @return the current value of this property on the given [AnimatableItem]. */
  operator fun get(animatableItem: AnimatableItem): Float

  /** Updates the value of this property on the given mount content to the given value. */
  operator fun set(mountContent: Any, value: Float)

  /** Resets this property to its default value on this mount content. */
  fun reset(mountContent: Any)
}
