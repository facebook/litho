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

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.TransitionId

/**
 * A pair of transition key and [AnimatedProperty] which can be used to identify a single animating
 * component property at runtime.
 */
@DataClassGenerate
data class PropertyHandle(val transitionId: TransitionId, val property: AnimatedProperty) {

  override fun toString(): String =
      "PropertyHandle{ mTransitionId='$transitionId', mProperty=$property}"
}
