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

package com.facebook.litho.testing.assertj

import com.facebook.litho.Component
import com.facebook.litho.layout.flexbox.FlexboxContainerComponent

/**
 * Reads [FlexboxContainerComponent]'s `internal` state so [LithoAssertions]' Java child-assertions
 * can inspect a built Row/Column directly, without resolving the layout tree.
 */
object FlexboxContainerAssertSupport {

  /**
   * Returns the declared children of [component] if it is a Row/Column
   * ([FlexboxContainerComponent]), or `null` if it is any other kind of component.
   */
  @JvmStatic
  fun childrenOrNull(component: Component): List<Component>? =
      (component as? FlexboxContainerComponent)?.children
}
