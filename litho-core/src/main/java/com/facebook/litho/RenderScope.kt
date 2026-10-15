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

/**
 * Identifies the render phase in which a hook executes.
 *
 * Litho components go through two phases: **Resolve** (component tree resolution, state, cached
 * values, resolve-phase effects) and **Layout** (Yoga measurement, layout-phase effects). A
 * [RenderScope] value is attached to every [HookGlobalKey] so that hook identities from different
 * phases never collide.
 */
@JvmInline
value class RenderScope private constructor(val scope: Int) {

  override fun toString(): String =
      when (scope) {
        0 -> "Resolve"
        1 -> "Layout"
        else -> "Unknown($scope)"
      }

  companion object {
    val Resolve = RenderScope(0)
    val Layout = RenderScope(1)
  }
}
