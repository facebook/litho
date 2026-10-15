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

import androidx.collection.LongSet
import com.facebook.rendercore.RenderTreeHost
import com.facebook.rendercore.RenderUnit

/** This APIs declares that inputs required by the Visibility Extensions. */
interface VisibilityExtensionInput {

  /** returns a list of items for which visibility events will be processed */
  val visibilityOutputs: List<VisibilityOutput>

  /** Returns the set of ids of [RenderUnit]s which host [RenderTreeHost]. */
  val renderUnitIdsWhichHostRenderTrees: LongSet?

  val isProcessingVisibilityOutputsEnabled: Boolean
  val visibilityBoundsTransformer: VisibilityBoundsTransformer?

  /**
   * When true, uses an empty visible rect for visibility processing when the root host view is
   * detached from the window. This causes all visibility outputs to be treated as invisible,
   * properly firing InvisibleEvent, UnfocusedVisibleEvent, etc. This prevents incorrect visibility
   * events caused by [android.view.View.getLocalVisibleRect] returning stale/full rects for
   * detached views.
   *
   * Note: This intentionally uses [android.view.View.isAttachedToWindow] which remains true during
   * temporary detaches (e.g. RecyclerView item recycling, disappearing transitions), so those
   * legitimate cases are not affected.
   */
  val useEmptyVisibleRectOnDetach: Boolean
    get() = false
}
