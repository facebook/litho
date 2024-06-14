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

/** Interface used to expose a limited API to the animations package. */
interface AnimatableItem {

  fun getId(): Long

  fun getTransitionId(): TransitionId?

  @OutputUnitType fun getOutputType(): Int

  fun getAbsoluteBounds(): Rect

  fun getScale(): Float

  fun getAlpha(): Float

  fun getRotation(): Float

  fun getRotationX(): Float

  fun getRotationY(): Float

  fun isScaleSet(): Boolean

  fun isAlphaSet(): Boolean

  fun isRotationSet(): Boolean

  fun isRotationXSet(): Boolean

  fun isRotationYSet(): Boolean
}
