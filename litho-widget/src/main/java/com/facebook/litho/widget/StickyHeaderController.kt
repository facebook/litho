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

import androidx.recyclerview.widget.RecyclerView

interface StickyHeaderController {

  /** Called by the SectionsRecyclerView onScrolled event */
  fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int)

  /** Bind the onScroll listener to the SectionsRecyclerView param */
  fun init(sectionsRecyclerView: SectionsRecyclerView)

  /** Reset the controller */
  fun reset()
}
