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

package com.facebook.litho.widget

import com.facebook.litho.ComponentTree

interface HasStickyHeader : ViewportInfo {
  /**
   * @param position
   * @return Whether the item at given position is *sticky*
   */
  fun isSticky(position: Int): Boolean

  /**
   * @param position
   * @return Whether the item at given position is *valid*, i.e. whether such position exists.
   */
  fun isValidPosition(position: Int): Boolean

  /**
   * @param position
   * @return a component tree for the item at position.
   */
  fun getComponentForStickyHeaderAt(position: Int): ComponentTree?
}
