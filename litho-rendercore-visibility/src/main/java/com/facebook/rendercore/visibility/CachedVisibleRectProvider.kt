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

package com.facebook.rendercore.visibility

import android.graphics.Rect

/**
 * Implemented by parent ViewGroups (e.g., RecyclerView) that cache their own
 * [View.getLocalVisibleRect] result during batch child offset operations.
 *
 * Direct children can read [cachedLocalVisibleRect] and derive their own local visible rect via
 * simple intersection + offset, avoiding the O(view-hierarchy-depth) traversal that
 * [View.getLocalVisibleRect] performs per child.
 *
 * **Contract:** The cached rect MUST be equivalent to calling [View.getLocalVisibleRect] on the
 * implementor — it must account for clipping by ALL ancestors up to the window, not just the
 * immediate parent's bounds. Returning a rect that only reflects the implementor's own bounds
 * (without ancestor clipping) will cause children to compute incorrect visible rects, breaking
 * visibility events and incremental mount for partially-occluded hierarchies.
 *
 * The rect is only valid during the synchronous batch operation (e.g., `offsetChildrenVertical`).
 * Implementors must set it to `null` after the batch completes.
 *
 * This optimization is safe for direct children only. Nested views (e.g., a LithoView inside
 * another LithoView) should fall back to [View.getLocalVisibleRect] since their parent typically
 * does not implement this interface.
 */
interface CachedVisibleRectProvider {
  val cachedLocalVisibleRect: Rect?
}
