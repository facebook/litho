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

package com.facebook.litho.config

import com.facebook.litho.SpecGeneratedComponent
import com.facebook.litho.annotations.LayoutSpec

/**
 * Defines how components of the same type should be compared for equality.
 *
 * Note that this logic may still be overridden by manually implemented [SpecGeneratedComponent]s.
 */
enum class ComponentEqualityMode {
  /**
   * Default equality mode.
   *
   * This mode typically collects all the component props via reflection and compares them against
   * those from another component.
   */
  DEFAULT,
  /**
   * Equality mode specifically optimized for [LayoutSpec] components.
   *
   * Instead of reflection, this mode takes advantage of a build-time generated method to optimize
   * the equality check. The method returns a list of props that should be compared against those
   * from another layout-spec component.
   */
  LAYOUT_SPECS,
  /**
   * Equality mode optimized for all spec generated components.
   *
   * Instead of reflection, this mode takes advantage of a build-time generated method to optimize
   * the equality check. The method returns a list of props that should be compared against those
   * from another spec component.
   */
  SPECS
}
